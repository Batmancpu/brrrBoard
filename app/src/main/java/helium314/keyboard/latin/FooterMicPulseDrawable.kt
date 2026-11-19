// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.drawable.Drawable

/**
 * Scales only the mic glyph inside its unchanged ImageButton and background.
 * The fixed circle can never grow into the keyboard strip or be cropped by footer bounds.
 */
internal class FooterMicPulseDrawable(private val icon: Drawable) : Drawable() {
    private var intensity = 0f

    fun setPulse(phase: Float) {
        val next = phase.coerceIn(0f, 1f)
        if (intensity == next) return
        intensity = next
        invalidateSelf()
    }

    override fun draw(canvas: Canvas) {
        val checkpoint = canvas.save()
        val scale = FooterMicPulsePolicy.scale(intensity)
        canvas.scale(scale, scale, bounds.exactCenterX(), bounds.exactCenterY())
        icon.bounds = bounds
        icon.draw(canvas)
        canvas.restoreToCount(checkpoint)
    }

    override fun setAlpha(alpha: Int) {
        icon.alpha = alpha
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        icon.colorFilter = colorFilter
        invalidateSelf()
    }

    override fun setTintList(tint: ColorStateList?) {
        icon.setTintList(tint)
        invalidateSelf()
    }

    override fun isStateful(): Boolean = icon.isStateful
    override fun onStateChange(state: IntArray): Boolean {
        val changed = icon.setState(state)
        if (changed) invalidateSelf()
        return changed
    }

    override fun getIntrinsicWidth(): Int = icon.intrinsicWidth
    override fun getIntrinsicHeight(): Int = icon.intrinsicHeight
    override fun getOpacity(): Int = icon.opacity
}

internal object FooterMicPulsePolicy {
    fun scale(phase: Float): Float = 1f + 0.085f * phase.coerceIn(0f, 1f)
}
