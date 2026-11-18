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
        val pad = 4f * density
        area.set(bounds.left + pad, bounds.top + pad, bounds.right - pad, bounds.bottom - pad)
        gradient = if (area.width() > 0f && area.height() > 0f)
            LinearGradient(area.left, area.top, area.right, area.bottom,
                intArrayOf(0xFFB345F2.toInt(), 0xFF653DDA.toInt(),
                    0xFF0FBFDD.toInt(), 0xFF7047DF.toInt(), 0xFFD14CAA.toInt()),
                null, Shader.TileMode.CLAMP) else null
    }

    override fun draw(canvas: Canvas) {
        val shader = gradient ?: return
        val radius = min(16f * density, area.height() / 2f)
        paint.shader = shader
        paint.style = Paint.Style.FILL
        if (fill) {
            paint.alpha = (opacity * 0.65f).toInt()
            canvas.drawRoundRect(area, radius, radius, paint)
        }
        paint.style = Paint.Style.STROKE
        drawHalo(canvas, radius, 7f, 0.17f)
        drawHalo(canvas, radius, 3f, 0.45f)
        drawHalo(canvas, radius, 1.5f, 0.88f)
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
