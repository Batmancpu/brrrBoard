// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.suggestions

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable

/** Content-sized rounded capsule, always anchored to the center slot. */
internal class SuggestionConfidencePillDrawable(
    private val color: Int,
    private val fromWidth: Float,
    private val toWidth: Float,
    private val fromOpacity: Float,
    private val toOpacity: Float,
    private val verticalInset: Float,
) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var progress = 1f
    private var drawableAlpha = 255

    fun setProgress(value: Float) {
        progress = value.coerceIn(0f, 1f)
        invalidateSelf()
    }

    override fun draw(canvas: Canvas) {
        if (bounds.isEmpty) return
        val eased = progress
        val alpha = fromOpacity + (toOpacity - fromOpacity) * eased
        if (alpha <= 0f) return
        val width = (fromWidth + (toWidth - fromWidth) * eased)
            .coerceAtMost(bounds.width().toFloat())
        val left = bounds.exactCenterX() - width / 2f
        val right = bounds.exactCenterX() + width / 2f
        val top = bounds.top + verticalInset
        val bottom = bounds.bottom - verticalInset
        if (bottom <= top) return
        paint.style = Paint.Style.FILL
        paint.color = color
        paint.alpha = ((color ushr 24) * alpha * drawableAlpha / 255f)
            .toInt().coerceIn(0, 255)
        val radius = (bottom - top) / 2f
        canvas.drawRoundRect(left, top, right, bottom, radius, radius, paint)
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
}

/** Pure sizing rule so one or several words cannot shift the tap target. */
internal object SuggestionConfidencePillGeometry {
    @JvmStatic
    fun width(textWidth: Float, slotWidth: Float, density: Float): Float {
        val maximum = (slotWidth - 8f * density).coerceAtLeast(1f)
        val minimum = (52f * density).coerceAtMost(maximum)
        return (textWidth + 24f * density).coerceIn(minimum, maximum)
    }
}
