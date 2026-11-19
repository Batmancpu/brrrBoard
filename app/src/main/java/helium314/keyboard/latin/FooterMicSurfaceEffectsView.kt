// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View
import androidx.core.graphics.ColorUtils
import kotlin.math.roundToInt

/** The *actual mic outline*, shared by the small cached underlight and discard morph. */
internal object FooterMicSilhouette {
    private val body = Path().apply {
        // Coordinates copied from ic_board_mic.xml, so the glow's mask and
        // the moving cancellation glyph match the real, taller microphone.
        moveTo(12f, 2f)
        cubicTo(9.75f, 2f, 8.3f, 3.75f, 8.3f, 5.85f)
        lineTo(8.3f, 10.8f)
        cubicTo(8.3f, 13.2f, 9.75f, 14.9f, 12f, 14.9f)
        cubicTo(14.25f, 14.9f, 15.7f, 13.2f, 15.7f, 10.8f)
        lineTo(15.7f, 5.85f)
        cubicTo(15.7f, 3.75f, 14.25f, 2f, 12f, 2f)
        close()
    }
    private val stand = Path().apply {
        moveTo(5.2f, 10.8f)
        lineTo(5.2f, 11.05f)
        cubicTo(5.2f, 14.95f, 8f, 17.9f, 12f, 17.9f)
        cubicTo(16f, 17.9f, 18.8f, 14.95f, 18.8f, 11.05f)
        lineTo(18.8f, 10.8f)
        moveTo(12f, 18f)
        lineTo(12f, 21.8f)
        moveTo(8.9f, 21.8f)
        lineTo(15.1f, 21.8f)
    }

    fun draw(canvas: Canvas, cx: Float, cy: Float, pixelSize: Float,
        paint: Paint, alpha: Int, scale: Float = 1f) {
        val saved = canvas.save()
        val s = pixelSize / 24f * scale
        canvas.translate(cx - 12f * s, cy - 12f * s)
        canvas.scale(s, s)
        paint.alpha = alpha.coerceIn(0, 255)
        paint.style = Paint.Style.FILL
        canvas.drawPath(body, paint)
        paint.style = Paint.Style.STROKE
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        paint.strokeWidth = 2.25f
        canvas.drawPath(stand, paint)
        canvas.restoreToCount(saved)
    }
}

/** Small cached blurred silhouette, never a ring or a circular radial badge. */
class FooterMicUnderlightView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
) : View(context, attrs) {
    enum class Phase { IDLE, RECORDING, PROCESSING }

    private var phase = Phase.IDLE
    private var dark = false
    private var intensity = 0f
    private var bitmap: Bitmap? = null
    private var cachedColor = 0
    private val bitmapPaint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
    private val shapePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val density = resources.displayMetrics.density

    fun setPhase(next: Phase, isDark: Boolean) {
        if (phase == next && dark == isDark) return
        phase = next
        dark = isDark
        // The mask is made once per actual phase/tint change, never every frame.
        if (next == Phase.IDLE) releaseMask()
        invalidate()
    }

    fun setPulse(level: Float) {
        val bounded = level.coerceIn(0f, 1f)
        if (intensity == bounded) return
        intensity = bounded
        postInvalidateOnAnimation()
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        releaseMask()
    }

    override fun onDetachedFromWindow() {
        releaseMask()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (phase == Phase.IDLE || width <= 0 || height <= 0) return
        val tint = if (phase == Phase.RECORDING)
            FooterIndicatorPolicy.voiceTint(dark) else FooterIndicatorPolicy.processingTint(dark)
        ensureMask(tint)
        val glow = bitmap ?: return
        val saved = canvas.save()
        // Keep the underlight attached to the taller glyph as it breathes.
        val scale = FooterMicPulsePolicy.scale(intensity)
        canvas.scale(scale, scale, width / 2f, (height - 10f * density) / 2f)
        bitmapPaint.alpha = (if (phase == Phase.RECORDING) 35f + 16f * intensity
            else 29f + 10f * intensity).roundToInt()
        canvas.drawBitmap(glow, 0f, 0f, bitmapPaint)
        canvas.restoreToCount(saved)
    }

    private fun ensureMask(tint: Int) {
        if (bitmap != null && cachedColor == tint) return
        releaseMask()
        if (width <= 0 || height <= 0) return
        cachedColor = tint
        val mask = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val offscreen = Canvas(mask)
        shapePaint.color = tint
        shapePaint.maskFilter = BlurMaskFilter(5.4f * density, BlurMaskFilter.Blur.NORMAL)
        FooterMicSilhouette.draw(offscreen, width / 2f, (height - 10f * density) / 2f,
            31f * density, shapePaint, 255)
        shapePaint.maskFilter = null
        bitmap = mask
    }

    private fun releaseMask() {
        bitmap?.recycle()
        bitmap = null
    }
}

/** One continuous 520 ms cancel morph: bin drops, lid opens, red mic enters, white mic emerges. */
class FooterMicDiscardMorphView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null,
) : View(context, attrs) {
    private val density = resources.displayMetrics.density
    private val whitePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = 1.8f * density
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    private val micPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var progress = 0f
    private var idleTint = Color.WHITE
    private var startingMicTint = 0xFFFF453A.toInt()

    fun setIdleTint(color: Int) { idleTint = color }
    fun setStartingMicTint(color: Int) { startingMicTint = color }

    fun setProgress(value: Float) {
        progress = value.coerceIn(0f, 1f)
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val p = progress
        val micX = width - 24f * density
        val micY = height - 48f * density + 19f * density
        val binX = micX - 23f * density + 23f * density * FooterMicMorphPolicy.returnFraction(p)
        val binY = micY - 4f * density - 45f * density * (1f - FooterMicMorphPolicy.dropFraction(p))
        val binOpacity = (255f * FooterMicMorphPolicy.binAlpha(p)).roundToInt()
        val redOpacity = (255f * FooterMicMorphPolicy.redMicAlpha(p)).roundToInt()
        val whiteOpacity = (255f * FooterMicMorphPolicy.whiteMicAlpha(p)).roundToInt()

        if (redOpacity > 0) {
            val into = FooterMicMorphPolicy.swallowFraction(p)
            micPaint.color = startingMicTint
            FooterMicSilhouette.draw(canvas,
                micX - 23f * density * into,
                micY + 8f * density * into,
                31f * density, micPaint, redOpacity, 1f - .77f * into)
        }
        if (binOpacity > 0) drawBin(canvas, binX, binY, binOpacity,
            FooterMicMorphPolicy.lidOpenFraction(p))
        if (whiteOpacity > 0) {
            micPaint.color = ColorUtils.blendARGB(Color.WHITE, idleTint,
                FooterMicMorphPolicy.idleRevealFraction(p))
            FooterMicSilhouette.draw(canvas, micX, micY, 31f * density,
                micPaint,
                (whiteOpacity * (1f - FooterMicMorphPolicy.idleRevealFraction(p))).roundToInt(),
                .8f + .2f * FooterMicMorphPolicy.returnFraction(p))
        }
    }

    private fun drawBin(canvas: Canvas, x: Float, y: Float, opacity: Int, lidOpen: Float) {
        whitePaint.alpha = opacity
        whitePaint.style = Paint.Style.STROKE
        val d = density
        canvas.drawRoundRect(x - 7f*d, y - 3f*d, x + 7f*d, y + 13f*d, 2f*d, 2f*d, whitePaint)
        canvas.drawLine(x - 2f*d, y + 1f*d, x - 2f*d, y + 9f*d, whitePaint)
        canvas.drawLine(x + 2f*d, y + 1f*d, x + 2f*d, y + 9f*d, whitePaint)
        val saved = canvas.save()
        canvas.rotate(-63f * lidOpen, x - 8f*d, y - 5f*d)
        canvas.drawLine(x - 10f*d, y - 5f*d, x + 10f*d, y - 5f*d, whitePaint)
        canvas.drawLine(x - 3f*d, y - 8f*d, x + 3f*d, y - 8f*d, whitePaint)
        canvas.restoreToCount(saved)
    }
}
