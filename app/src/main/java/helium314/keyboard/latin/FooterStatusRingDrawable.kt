// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

/** Noninteractive, theme-aware footer circles. The original 48dp hit targets remain. */
internal class FooterStatusRingDrawable(
    private val density: Float,
    private val isMicrophone: Boolean,
) : Drawable() {
    enum class VoiceState { IDLE, LISTENING, PROCESSING }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var dark = false
    private var voiceState = VoiceState.IDLE
    private var pulse = 0f
    private var alpha = 255

    fun setAppearance(isDark: Boolean) {
        dark = isDark
        invalidateSelf()
    }

    fun setVoiceState(state: VoiceState) {
        voiceState = state
        if (state == VoiceState.IDLE) pulse = 0f
        invalidateSelf()
    }

    fun setPulse(level: Float) {
        pulse = level.coerceIn(0f, 1f)
        invalidateSelf()
    }

    override fun draw(canvas: Canvas) {
        val drawableBounds = bounds
        if (drawableBounds.isEmpty) return
        val cx = drawableBounds.exactCenterX()
        // The existing icons use 10dp optical-bottom padding. Center the
        // circle on the actual glyph, not the full 48dp background bounds.
        val cy = drawableBounds.exactCenterY() - 5f * density
        val radius = (if (isMicrophone) 18.5f else 17f) * density
        val active = isMicrophone && voiceState != VoiceState.IDLE
        val red = FooterIndicatorPolicy.voiceTint(dark)
        if (active) {
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = (6f + 2f * pulse) * density
            paint.color = withAlpha(red, if (voiceState == VoiceState.PROCESSING)
                (32 + 40 * pulse).toInt() else (35 + 75 * pulse).toInt())
            canvas.drawCircle(cx, cy, radius + 1.8f * density, paint)
        }

        if (isMicrophone) {
            paint.style = Paint.Style.FILL
            paint.color = if (dark) 0xFF292A32.toInt() else 0xFF292B33.toInt()
            paint.alpha = alpha
            canvas.drawCircle(cx, cy, radius, paint)
        }

        paint.style = Paint.Style.STROKE
        paint.strokeWidth = (if (isMicrophone) 1.35f else 1.25f) * density
        paint.color = if (active) withAlpha(red,
            if (voiceState == VoiceState.PROCESSING) (145 + 70 * pulse).toInt()
            else (168 + 87 * pulse).toInt())
        else if (isMicrophone) {
            if (dark) 0x627E8292 else 0x65575B66
        } else {
            if (dark) 0xB7DCE0E8.toInt() else 0x9B50535D.toInt()
        }
        canvas.drawCircle(cx, cy, radius, paint)
        paint.style = Paint.Style.FILL
    }

    override fun setAlpha(alpha: Int) {
        this.alpha = alpha.coerceIn(0, 255)
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        invalidateSelf()
    }

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    private fun withAlpha(color: Int, alpha: Int): Int =
        (color and 0x00FFFFFF) or (alpha.coerceIn(0, 255) shl 24)
}

internal object FooterIndicatorPolicy {
    fun voiceTint(dark: Boolean): Int =
        if (dark) 0xFFFF453A.toInt() else 0xFFFF3B30.toInt()

    /** Processing remains active but is less like an ongoing recording meter. */
    fun pulseIntensity(phase: Float, processing: Boolean): Float =
        phase.coerceIn(0f, 1f) * if (processing) 0.48f else 1f
}
