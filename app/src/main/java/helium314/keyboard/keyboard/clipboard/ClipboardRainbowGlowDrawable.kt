// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.clipboard

import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.LinearGradient
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.drawable.Animatable
import android.graphics.drawable.Drawable
import android.os.Build
import kotlin.math.min

/** Search-only, hardware-rendered moving chromatic haze; no bitmap or host-app capture. */
internal class ClipboardRainbowGlowDrawable(
    private val density: Float,
    private val fill: Boolean
) : Drawable(), Animatable {
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val area = RectF()
    private val shaderMatrix = Matrix()
    private var gradient: LinearGradient? = null
    private var opacity = 255
    private var animator: ValueAnimator? = null
    private var phase = 0f

    override fun onBoundsChange(bounds: Rect) {
        super.onBoundsChange(bounds)
        val pad = 6f * density
        area.set(bounds.left + pad, bounds.top + pad, bounds.right - pad, bounds.bottom - pad)
        gradient = if (area.width() > 0f && area.height() > 0f)
            LinearGradient(area.left - area.width() * 0.5f, area.top,
                area.right + area.width() * 0.5f, area.bottom,
                intArrayOf(0xFFFF53C5.toInt(), 0xFFAD5BFF.toInt(),
                    0xFF27CBE8.toInt(), 0xFF65D9C2.toInt(),
                    0xFFFF51B3.toInt(), 0xFFFFCA62.toInt(), 0xFF9D65FF.toInt()),
                null, Shader.TileMode.MIRROR) else null
    }

    override fun draw(canvas: Canvas) {
        val shader = gradient ?: return
        val radius = min(16f * density, area.height() / 2f)
        shaderMatrix.setTranslate(phase * area.width() * 0.55f, 0f)
        shader.setLocalMatrix(shaderMatrix)
        paint.shader = shader
        paint.color = Color.WHITE
        paint.style = Paint.Style.FILL
        if (fill) {
            paint.alpha = (opacity * 0.82f).toInt()
            canvas.drawRoundRect(area, radius, radius, paint)
        }
        paint.style = Paint.Style.STROKE
        drawHalo(canvas, radius, 16f, 0.13f)
        drawHalo(canvas, radius, 9f, 0.29f)
        drawHalo(canvas, radius, 4f, 0.65f)
        drawHalo(canvas, radius, 1.7f, 0.98f)
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

    override fun start() {
        if (animator != null || Build.VERSION.SDK_INT < Build.VERSION_CODES.O ||
            !ValueAnimator.areAnimatorsEnabled()) return
        animator = ValueAnimator.ofFloat(-1f, 1f).apply {
            duration = 2600L
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            addUpdateListener {
                phase = it.animatedValue as Float
                invalidateSelf()
            }
            start()
        }
    }

    override fun stop() {
        animator?.cancel()
        animator = null
        phase = 0f
        invalidateSelf()
    }

    override fun isRunning(): Boolean = animator?.isRunning == true
}
