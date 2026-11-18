// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorMatrixColorFilter
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
import kotlin.math.ceil
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
    private var emojiToneEnabled = false
    private var emojiToneDark = false
    private var emojiTonePaint: Paint? = null
    // Mask only the in-IME content; do not tint the host-app backdrop.
    private val fadeMask = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
    }
    private val sourcePosition = IntArray(2)
    private val overlayPosition = IntArray(2)
    private var node: RenderNode? = null
    private var blurWidth = 0
    private var blurHeight = 0
    private var blurBleed = -1
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
            blurBleed = -1
            observe()
        }
        this.edge = edge
        val newBase = ColorUtils.setAlphaComponent(color, 255)
        if (newBase != baseColor) headerMaskShader = null
        baseColor = newBase
        invalidate()
    }

    /**
     * Emoji panel/footer opt-in only. A GPU layer with a color filter modifies
     * the RenderNode's own blurred pixels (including their existing alpha),
     * rather than drawing a second dark/white overlay over the host backdrop.
     * Other panels never call this with enabled=true.
     */
    fun setEmojiContentTone(enabled: Boolean, dark: Boolean) {
        if (emojiToneEnabled == enabled && emojiToneDark == dark) return
        emojiToneEnabled = enabled
        emojiToneDark = dark
        emojiTonePaint = EmojiGlassTonePolicy.matrix(enabled, dark)?.let { matrix ->
            Paint(Paint.ANTI_ALIAS_FLAG).apply {
                colorFilter = ColorMatrixColorFilter(matrix)
            }
        }
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
            !content.isShown ||
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
        val blur = 18f * resources.displayMetrics.density
        // Bottom action glass and the separate ABC/mic glass each record the
        // same live source. If each GPU effect ends exactly at its own edge,
        // DECAL samples transparent pixels there, creating a visible seam.
        // Add *source pixels* above/below each window, then crop only the
        // final composited result. The host backdrop, tint and sharp keys are
        // unchanged. Top fading remains deliberately clipped to its header.
        val bleed = if (edge == Edge.BOTTOM) ceil(blur * 1.5f).toInt() else 0
        val recordedHeight = height + bleed * 2
        if (blurWidth != width || blurHeight != height || blurBleed != bleed) {
            blurWidth = width
            blurHeight = height
            blurBleed = bleed
            renderNode.setPosition(0, 0, width, recordedHeight)
            renderNode.setRenderEffect(RenderEffect.createBlurEffect(
                blur, blur, Shader.TileMode.DECAL))
        }
        content.getLocationInWindow(sourcePosition)
        getLocationInWindow(overlayPosition)
        // RecordingCanvas captures GPU drawing commands only; no bitmap or
        // clipboard screenshot is stored. Both bottom strips now use the same
        // continuous source sampling neighborhood around their shared edge.
        val recording = renderNode.beginRecording(width, recordedHeight)
        try {
            val save = recording.save()
            try {
                recording.translate((sourcePosition[0] - overlayPosition[0]).toFloat(),
                    (sourcePosition[1] - overlayPosition[1] + bleed).toFloat())
                content.draw(recording)
            } finally {
                recording.restoreToCount(save)
            }
        } finally {
            renderNode.endRecording()
        }
        // saveLayer's filter is applied only to nontransparent RenderNode
        // samples. In particular, alpha-zero areas remain alpha-zero so the
        // IME's existing host wallpaper blur is never tinted a second time.
        val tonePaint = emojiTonePaint
        val saved = if (tonePaint != null)
            canvas.saveLayer(0f, 0f, width.toFloat(), height.toFloat(), tonePaint)
        else canvas.save()
        try {
            canvas.clipRect(0f, 0f, width.toFloat(), height.toFloat())
            canvas.translate(0f, -bleed.toFloat())
            canvas.drawRenderNode(renderNode)
        } finally {
            canvas.restoreToCount(saved)
        }
    }
}
