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
    private var shieldProgress = 0f
    private var shieldVisible = true
    private val shieldPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        strokeCap = Paint.Cap.ROUND
        strokeJoin = Paint.Join.ROUND
    }
    // A compact privacy shield/lock sits outside the mic's silhouette so the
    // actual microphone remains easy to recognize and tap.
    private val privacyBadge = Path().apply {
        moveTo(39f, 2.1f)
        cubicTo(37.2f, 3.1f, 35.1f, 3.6f, 33.8f, 3.8f)
        lineTo(34.1f, 8.2f)
        cubicTo(34.4f, 11.4f, 37.1f, 14.1f, 39f, 15.1f)
        cubicTo(40.9f, 14.1f, 43.6f, 11.4f, 43.9f, 8.2f)
        lineTo(44.2f, 3.8f)
        cubicTo(42.9f, 3.6f, 40.8f, 3.1f, 39f, 2.1f)
        close()
    }

    fun setPrivacyShield(progress: Float) {
        val next = progress.coerceIn(0f, 1f)
        if (shieldProgress == next) return
        shieldProgress = next
        postInvalidateOnAnimation()
    }

    fun setPrivacyShieldVisible(visible: Boolean) {
        if (shieldVisible == visible) return
        shieldVisible = visible
        invalidate()
    }

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

    override fun onVisibilityAggregated(isVisible: Boolean) {
        super.onVisibilityAggregated(isVisible)
        if (!isVisible) releaseMask()
    }

    override fun onDetachedFromWindow() {
        releaseMask()
        super.onDetachedFromWindow()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (width <= 0 || height <= 0) return
        if (phase != Phase.IDLE) {
            val tint = if (phase == Phase.RECORDING)
                FooterIndicatorPolicy.voiceTint(dark) else FooterIndicatorPolicy.processingTint(dark)
            ensureMask(tint)
            val glow = bitmap
            if (glow != null) {
                val saved = canvas.save()
                // Keep the underlight attached to the taller glyph as it breathes.
                val scale = FooterMicPulsePolicy.scale(intensity)
                canvas.scale(scale, scale, width / 2f, (height - 10f * density) / 2f)
                bitmapPaint.alpha = (if (phase == Phase.RECORDING) 35f + 16f * intensity
                    else 29f + 10f * intensity).roundToInt()
                canvas.drawBitmap(glow, 0f, 0f, bitmapPaint)
                canvas.restoreToCount(saved)
            }
        }
        // Keep the small privacy mark crisp over the live voice underlight.
        drawPrivacyShield(canvas)
    }

    private fun drawPrivacyShield(canvas: Canvas) {
        if (!shieldVisible || shieldProgress <= .001f) return
        val saved = canvas.save()
        canvas.scale(width / 48f, height / 48f)
        val color = FooterPrivacyPalette.GOLD
        val badgeSave = canvas.save()
        val scale = .72f + .28f * shieldProgress
        canvas.scale(scale, scale, 39f, 8.5f)
        shieldPaint.style = Paint.Style.STROKE
        shieldPaint.strokeWidth = 1f
        shieldPaint.color = ColorUtils.setAlphaComponent(FooterPrivacyPalette.HIGHLIGHT,
            (255f * shieldProgress).roundToInt())
        shieldPaint.style = Paint.Style.FILL
        shieldPaint.color = ColorUtils.setAlphaComponent(Color.rgb(20, 35, 53),
            (224f * shieldProgress).roundToInt())
        canvas.drawPath(privacyBadge, shieldPaint)
        shieldPaint.style = Paint.Style.STROKE
        shieldPaint.strokeWidth = 1f
        shieldPaint.color = ColorUtils.setAlphaComponent(color, (255f * shieldProgress).roundToInt())
        canvas.drawPath(privacyBadge, shieldPaint)
        shieldPaint.strokeWidth = .8f
        canvas.drawArc(37.8f, 6.2f, 40.2f, 8.8f, 180f, 180f, false, shieldPaint)
        shieldPaint.style = Paint.Style.FILL
        canvas.drawRoundRect(37.5f, 7.3f, 40.5f, 9.6f, .4f, .4f, shieldPaint)
        canvas.restoreToCount(badgeSave)
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
    private var startingMicTint = 0xFFAC1537.toInt()

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
