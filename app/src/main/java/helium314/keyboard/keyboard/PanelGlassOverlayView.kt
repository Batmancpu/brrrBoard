// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RenderEffect
import android.graphics.RenderNode
import android.graphics.Shader
import android.os.Build
import android.util.AttributeSet
import android.view.View
import android.view.ViewTreeObserver
import androidx.core.graphics.ColorUtils

/**
 * An in-IME-only backdrop. Records the *current* scroll surface into a GPU
 * RenderNode, applies HWUI blur and composites crisp controls separately in
 * the parent. No bitmap, screenshot, host-app capture or persistent copy.
 *
 * The same source view supplies scroll position and content pixels: mirrored
 * emoji/category rows, toolbar tiles and clipboard cards cannot drift from
 * the live list. Each panel has two small clipped overlays, not a second list.
 */
class PanelGlassOverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null
) : View(context, attrs) {
    enum class Edge { TOP, BOTTOM }

    private var source: View? = null
    private var edge = Edge.BOTTOM
    private var baseColor: Int = Color.rgb(35, 36, 42)
    // Mask only the in-IME content; do not tint the host-app backdrop.
    private val fadeMask = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    }
    private val sourcePosition = IntArray(2)
    private val overlayPosition = IntArray(2)
    private var node: RenderNode? = null
    private var blurWidth = 0
    private var blurHeight = 0
    private var headerMaskShader: LinearGradient? = null
    private var registeredObserver: ViewTreeObserver? = null
    private val scrollObserver = ViewTreeObserver.OnScrollChangedListener { postInvalidateOnAnimation() }

    init {
        // Fixed glass is a touch-exclusion region for covered list items.
        // Foreground controls are later siblings and keep their hit targets.
        isClickable = true
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        setWillNotDraw(false)
    }

    fun configure(content: View?, edge: Edge, color: Int) {
        if (source !== content) {
            unobserve()
            source = content
            // Drop the old GPU display list when leaving a sensitive panel.
            node = null
            blurWidth = 0
            blurHeight = 0
            observe()
        }
        this.edge = edge
        val newBase = ColorUtils.setAlphaComponent(color, 255)
        if (newBase != baseColor) headerMaskShader = null
        baseColor = newBase
        invalidate()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        headerMaskShader = null
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        observe()
    }

    override fun onDetachedFromWindow() {
        unobserve()
        node = null
        super.onDetachedFromWindow()
    }

    private fun observe() {
        if (!isAttachedToWindow || source == null || registeredObserver != null) return
        val observer = source!!.viewTreeObserver
        if (observer.isAlive) {
            observer.addOnScrollChangedListener(scrollObserver)
            registeredObserver = observer
        }
    }

    private fun unobserve() {
        registeredObserver?.takeIf { it.isAlive }?.removeOnScrollChangedListener(scrollObserver)
        registeredObserver = null
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return

        // IME-window glass blurs the host. This second layer blurs ONLY our own
        // Emoji/Clipboard/Toolbar scroll pixels. Empty regions remain identical
        // to the rest of the keyboard: no gray fill or double-tint slab.
        val content = source ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S ||
            !canvas.isHardwareAccelerated || !content.isAttachedToWindow ||
            content.width <= 0 || content.height <= 0) return

        if (edge == Edge.BOTTOM) {
            // No background/tint: only moving, truly blurred list content.
            drawBlurredSource(canvas, content)
            return
        }

        // The top progressively softens underneath the fixed sharp title.
        // DST_IN masks only the blurred content layer, not the host backdrop.
        val saved = canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), null)
        try {
            drawBlurredSource(canvas, content)
            fadeMask.shader = headerMaskShader ?: LinearGradient(
                0f, 0f, 0f, height.toFloat(),
                intArrayOf(Color.WHITE, Color.WHITE, 0xDFFFFFFF.toInt(),
                    0x78FFFFFF, Color.TRANSPARENT),
                floatArrayOf(0f, 0.20f, 0.48f, 0.78f, 1f),
                Shader.TileMode.CLAMP
            ).also { headerMaskShader = it }
            canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), fadeMask)
            fadeMask.shader = null
        } finally {
            canvas.restoreToCount(saved)
        }
    }

    private fun drawBlurredSource(canvas: Canvas, content: View) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S) return
        val renderNode = node ?: RenderNode("brrBoard internal panel glass").also { node = it }
        if (blurWidth != width || blurHeight != height) {
            blurWidth = width
            blurHeight = height
            renderNode.setPosition(0, 0, width, height)
            val blur = 18f * resources.displayMetrics.density
            renderNode.setRenderEffect(RenderEffect.createBlurEffect(
                blur, blur, Shader.TileMode.DECAL))
        }
        content.getLocationInWindow(sourcePosition)
        getLocationInWindow(overlayPosition)
        // RecordingCanvas records draw commands directly in graphics memory;
        // it never allocates a CPU bitmap or stores Clipboard content.
        val recording = renderNode.beginRecording(width, height)
        try {
            val save = recording.save()
            try {
                recording.translate((sourcePosition[0] - overlayPosition[0]).toFloat(),
                    (sourcePosition[1] - overlayPosition[1]).toFloat())
                content.draw(recording)
            } finally {
                recording.restoreToCount(save)
            }
        } finally {
            renderNode.endRecording()
        }
        canvas.drawRenderNode(renderNode)
    }
}
