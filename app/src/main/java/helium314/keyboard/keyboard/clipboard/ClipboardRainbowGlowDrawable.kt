// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.clipboard

import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Drawable
import kotlin.math.min

/** Static multicolor search-match halo, no CPU bitmap or looping animation. */
internal class ClipboardRainbowGlowDrawable(
    private val density: Float,
    private val fill: Boolean
) : Drawable() {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val area = RectF()
    private var gradient: LinearGradient? = null
    private var opacity = 255

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        val pad = 6f * density
        area.set(bounds.left + pad, bounds.top + pad, bounds.right - pad, bounds.bottom - pad)
        gradient = if (area.width() > 0f && area.height() > 0f)
            LinearGradient(area.left, area.top, area.right, area.bottom,
                intArrayOf(0xFFFF53C5.toInt(), 0xFFAD5BFF.toInt(),
                    0xFF27CBE8.toInt(), 0xFF65D9C2.toInt(), 0xFFFF51B3.toInt()),
                null, Shader.TileMode.CLAMP) else null
    }

    override fun draw(canvas: Canvas) {
        val shader = gradient ?: return
        val radius = min(16f * density, area.height() / 2f)
        paint.shader = shader
        paint.style = Paint.Style.FILL
        if (fill) {
            paint.alpha = (opacity * 0.82f).toInt()
            canvas.drawRoundRect(area, radius, radius, paint)
        }
        paint.style = Paint.Style.STROKE
        drawHalo(canvas, radius, 10f, 0.25f)
        drawHalo(canvas, radius, 5f, 0.74f)
        drawHalo(canvas, radius, 2f, 1f)
        paint.shader = null
        paint.style = Paint.Style.FILL
    }

    private fun drawHalo(canvas: Canvas, radius: Float, widthDp: Float, strength: Float) {
        paint.strokeWidth = widthDp * density
        paint.alpha = (opacity * strength).toInt()
        canvas.drawRoundRect(area, radius, radius, paint)
    }

    override fun setAlpha(alpha: Int) {
        opacity = alpha.coerceIn(0, 255)
        invalidateSelf()
    }
    override fun setColorFilter(colorFilter: ColorFilter?) {
        paint.colorFilter = colorFilter
        invalidateSelf()
    }
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT
}
