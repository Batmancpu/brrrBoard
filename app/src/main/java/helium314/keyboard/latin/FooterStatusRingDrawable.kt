// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.RadialGradient
import android.graphics.Shader
import android.graphics.drawable.Drawable

/** Active recording only. No idle Emoji/ABC/mic circles or hard outlines. */
internal class FooterStatusRingDrawable(private val density: Float) : Drawable() {
    enum class VoiceState { IDLE, LISTENING, PROCESSING }

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var dark = false
    private var voiceState = VoiceState.IDLE
    private var pulse = 0f
    private var drawableAlpha = 255

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
        if (voiceState == VoiceState.IDLE || bounds.isEmpty) return
        val cx = bounds.exactCenterX()
        // Center on the icon's optical center, accounting for footer padding.
        val cy = bounds.exactCenterY() - 5f * density
        val red = FooterIndicatorPolicy.voiceTint(dark)
        val haloRadius = 25f * density
        val softHalo = RadialGradient(cx, cy, haloRadius, intArrayOf(
            withAlpha(red, (40f + 75f * pulse).toInt()),
            withAlpha(red, (18f + 39f * pulse).toInt()),
            withAlpha(red, 0),
        ), floatArrayOf(0f, .76f, 1f), Shader.TileMode.CLAMP)
        paint.style = Paint.Style.FILL
        paint.shader = softHalo
        paint.alpha = drawableAlpha
        canvas.drawCircle(cx, cy, haloRadius, paint)
        paint.shader = null

        // The larger filled surface is a soft dark button under the red glyph,
        // not a stroke or permanent outline. Intensity follows one mic timer.
        paint.color = if (dark) 0xFF29292E.toInt() else 0xFF292B32.toInt()
        paint.alpha = drawableAlpha
        canvas.drawCircle(cx, cy, 21.0f * density, paint)
    }

    override fun setAlpha(alpha: Int) {
        drawableAlpha = alpha.coerceIn(0, 255)
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        invalidateSelf()
    }

    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    private fun withAlpha(color: Int, opacity: Int): Int =
        (color and 0x00FFFFFF) or (opacity.coerceIn(0, 255) shl 24)
}

internal object FooterIndicatorPolicy {
    fun voiceTint(dark: Boolean): Int =
        if (dark) 0xFFFF453A.toInt() else 0xFFFF3B30.toInt()

    /** Processing remains active but is less like an ongoing recording meter. */
    fun pulseIntensity(phase: Float, processing: Boolean): Float =
        phase.coerceIn(0f, 1f) * if (processing) 0.48f else 1f
}
