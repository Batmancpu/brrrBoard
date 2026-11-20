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
    private var offline = false
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

    fun setOfflineMode(isOffline: Boolean) {
        offline = isOffline
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
        val tint = if (voiceState == VoiceState.PROCESSING)
            FooterIndicatorPolicy.processingTint(dark)
        else FooterIndicatorPolicy.voiceTint(dark, offline)
        // This disk never changes radius or opacity with the voice meter.
        // It fits inside the 48dp footer hitbox, including the -5dp optical offset.
        paint.style = Paint.Style.FILL
        paint.shader = null
        paint.color = if (dark) 0xFF29292E.toInt() else 0xFF292B32.toInt()
        paint.alpha = drawableAlpha
        canvas.drawCircle(cx, cy, 18f * density, paint)

        // The breathing light originates *behind the mic glyph*, never from the outer circle.
        // It remains entirely inside the fixed disk, avoiding clipping against the key strip.
        val glowRadius = 15f * density
        paint.shader = RadialGradient(cx, cy, glowRadius, intArrayOf(
            withAlpha(tint, (18f + 76f * pulse).toInt()),
            withAlpha(tint, (8f + 31f * pulse).toInt()),
            withAlpha(tint, 0),
        ), floatArrayOf(0f, .50f, 1f), Shader.TileMode.CLAMP)
        paint.alpha = drawableAlpha
        canvas.drawCircle(cx, cy, glowRadius, paint)
        paint.shader = null
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
    fun voiceTint(dark: Boolean, offline: Boolean = false): Int = when {
        offline && dark -> 0xFF56B5FF.toInt()
        offline -> 0xFF167BFF.toInt()
        dark -> 0xFFD83250.toInt() // vivid deep ruby on dark glass
        else -> 0xFFAC1537.toInt() // elegant crimson on light glass
    }

    /** Distinct from both red/server and blue/offline recording; processing only. */
    fun processingTint(dark: Boolean): Int =
        if (dark) 0xFF4DE5D1.toInt() else 0xFF009F97.toInt()

    /** Processing remains active but is less like an ongoing recording meter. */
    fun pulseIntensity(phase: Float, processing: Boolean): Float =
        phase.coerceIn(0f, 1f) * if (processing) 0.48f else 1f
}
