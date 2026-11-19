// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PixelFormat
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.view.animation.LinearInterpolator
import kotlin.math.roundToInt

/**
 * Original 24-unit brrBoard smile recreated as vector strokes, with a physically
 * continuous hat / downward glance / smoked sunglasses / specular reveal.
 * No bitmaps, blur layers, additional hitboxes or per-frame layout passes.
 */
internal class FooterIncognitoEmojiDrawable(context: Context) : Drawable() {
    private val iconPx = (31f * context.resources.displayMetrics.density).roundToInt()
    private val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val glass = Paint(Paint.ANTI_ALIAS_FLAG)
    private var themeInk = Color.WHITE
    private var alphaValue = 255
    private var suppliedFilter: ColorFilter? = null
    private var pose = FooterIncognitoMotion.Frame()
    private var wantedIncognito = false
    private var animator: ValueAnimator? = null

    override fun getIntrinsicWidth(): Int = iconPx
    override fun getIntrinsicHeight(): Int = iconPx
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    fun setThemeInk(color: Int) {
        if (themeInk != color) { themeInk = color; invalidateSelf() }
    }

    fun setIncognito(enabled: Boolean, animated: Boolean) {
        if (enabled == wantedIncognito && animator?.isRunning == true) return
        if (enabled == wantedIncognito &&
            pose == FooterIncognitoMotion.settled(enabled)) return
        val from = pose // Preserve the EXACT rendered pose on rapid double-tap reversal.
        animator?.cancel()
        animator = null
        wantedIncognito = enabled
        if (!animated || !KeyboardMotionSpec.enabled()) {
            pose = FooterIncognitoMotion.settled(enabled)
            invalidateSelf()
            return
        }
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = if (enabled) FooterIncognitoMotion.ENTER_MS
                else FooterIncognitoMotion.EXIT_MS
            interpolator = LinearInterpolator()
            addUpdateListener {
                pose = FooterIncognitoMotion.at(from, enabled, it.animatedValue as Float)
                invalidateSelf()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (animator === animation) {
                        animator = null
                        pose = FooterIncognitoMotion.settled(enabled)
                        invalidateSelf()
                    }
                }
            })
            start()
        }
    }

    fun stopAnimation() {
        animator?.cancel()
        animator = null
        pose = FooterIncognitoMotion.settled(wantedIncognito)
        invalidateSelf()
    }

    override fun draw(canvas: Canvas) {
        if (bounds.isEmpty) return
        val checkpoint = canvas.save()
        canvas.translate(bounds.left.toFloat(), bounds.top.toFloat())
        canvas.scale(bounds.width() / 24f, bounds.height() / 24f)
        val p = pose
        if (p.hat > .001f && p.hatBehind > .001f) {
            // The falling hat slips BEHIND the face instead of passing through it.
            val back = canvas.save()
            canvas.clipRect(0f, 0f, 24f, 6.4f)
            drawHat(canvas, p, p.hat * p.hatBehind)
            canvas.restoreToCount(back)
        }
        drawFace(canvas, p)
        if (p.hat > .001f && p.hatBehind < .999f) {
            drawHat(canvas, p, p.hat * (1f - p.hatBehind))
        }
        canvas.restoreToCount(checkpoint)
    }

    private fun ink(color: Int = themeInk, opacity: Float = 1f, stroke: Float = 2.35f) {
        ink.style = Paint.Style.STROKE
        ink.strokeWidth = stroke
        ink.color = Color.argb(
            (Color.alpha(color) * opacity * alphaValue / 255f).roundToInt().coerceIn(0, 255),
            Color.red(color), Color.green(color), Color.blue(color))
        ink.colorFilter = suppliedFilter
    }

    private fun drawFace(c: Canvas, p: FooterIncognitoMotion.Frame) {
        ink(stroke = 2.4f)
        c.drawCircle(12f, 12f, 9.4f, ink)
        val nod = p.nod.coerceIn(0f, 1f)
        val eyes = (1f - p.glasses).coerceIn(0f, 1f)
        if (eyes > .005f) {
            ink(opacity = eyes, stroke = 1f)
            ink.style = Paint.Style.FILL
            val eyeY = 9.75f + nod * 1.35f
            val eyeR = 1.35f - nod * .28f
            c.drawCircle(8.4f, eyeY, eyeR, ink)
            c.drawCircle(15.6f, eyeY, eyeR, ink)
        }
        ink(stroke = 2.35f - nod * .35f)
        val mouth = Path().apply {
            moveTo(7.5f, 13.7f + nod * .7f)
            quadTo(12f, 19.25f - nod * .75f, 16.5f, 13.7f + nod * .7f)
        }
        c.drawPath(mouth, ink)
        if (p.glasses > .005f) drawSunglasses(c, p)
    }

    private fun drawSunglasses(c: Canvas, p: FooterIncognitoMotion.Frame) {
        val opacity = p.glasses.coerceIn(0f, 1f)
        val y = 8.1f + p.nod * 1.05f
        val lenses = arrayOf(
            RectF(5.55f, y, 11.1f, y + 4.05f),
            RectF(12.9f, y, 18.45f, y + 4.05f),
        )
        // The dark lenses are real filled glass, with theme-ink rims; the original
        // line-art smile stays intact beneath them on both light and dark themes.
        glass.style = Paint.Style.FILL
        glass.color = Color.argb((225f * opacity * alphaValue / 255f).roundToInt(), 22, 26, 33)
        glass.colorFilter = null
        ink(opacity = opacity, stroke = 1.4f)
        for (lens in lenses) {
            c.drawRoundRect(lens, 1.55f, 1.55f, glass)
            c.drawRoundRect(lens, 1.55f, 1.55f, ink)
        }
        ink(opacity = opacity, stroke = 1.4f)
        c.drawLine(11.1f, y + 1.35f, 12.9f, y + 1.35f, ink)
        c.drawLine(5.55f, y + 1f, 4.55f, y + .55f, ink)
        c.drawLine(18.45f, y + 1f, 19.45f, y + .55f, ink)

        // Specular reflection SWEeps across the two lens shapes on reveal; a
        // shorter stationary gleam remains at rest. Both remain clipped to glass.
        val shine = p.glint.coerceIn(0f, 1f)
        if (shine > .005f) {
            glass.color = Color.argb((235f * opacity * alphaValue / 255f).roundToInt(), 249, 253, 255)
            val sweep = (shine * 2f - 1f).coerceIn(-1f, 1f)
            for (lens in lenses) {
                val save = c.save()
                val clip = Path().apply { addRoundRect(lens, 1.55f, 1.55f, Path.Direction.CW) }
                c.clipPath(clip)
                val base = lens.left + lens.width() * (if (shine >= .99f) .29f else .36f + .36f * sweep)
                val glimmer = Path().apply {
                    moveTo(base + .55f, lens.top - .5f)
                    lineTo(base + 1.65f, lens.top - .5f)
                    lineTo(base - .45f, lens.bottom + .5f)
                    lineTo(base - 1.55f, lens.bottom + .5f)
                    close()
                }
                c.drawPath(glimmer, glass)
                c.restoreToCount(save)
            }
        }
    }

    private fun drawHat(c: Canvas, p: FooterIncognitoMotion.Frame, opacity: Float) {
        if (opacity <= .001f) return
        val save = c.save()
        // Enter from just above the smile; exit falls in one uninterrupted path.
        val vertical = (1f - p.hat) * -3.7f + p.hatDrop
        c.translate(0f, vertical + p.nod * .22f)
        c.rotate(-2.8f * (1f - p.hat), 12f, 6.3f)
        val crown = Path().apply {
            moveTo(7.65f, 6.15f)
            cubicTo(8.15f, 5.05f, 8.5f, 2.4f, 9.2f, 1.5f)
            quadTo(12f, 1.15f, 14.8f, 1.5f)
            cubicTo(15.5f, 2.4f, 15.85f, 5.05f, 16.35f, 6.15f)
            close()
        }
        glass.style = Paint.Style.FILL
        glass.color = Color.argb((37f * opacity * alphaValue / 255f).roundToInt(),
            Color.red(themeInk), Color.green(themeInk), Color.blue(themeInk))
        glass.colorFilter = suppliedFilter
        c.drawPath(crown, glass)
        ink(opacity = opacity, stroke = 1.55f)
        c.drawPath(crown, ink)
        c.drawLine(8.25f, 5.45f, 15.75f, 5.45f, ink)
        ink(opacity = opacity, stroke = 1.9f)
        val brim = Path().apply {
            moveTo(4.35f, 7.02f)
            quadTo(12f, 8.55f, 19.65f, 7.02f)
        }
        c.drawPath(brim, ink)
        c.restoreToCount(save)
    }

    override fun setAlpha(alpha: Int) {
        alphaValue = alpha.coerceIn(0, 255)
        invalidateSelf()
    }

    override fun setColorFilter(colorFilter: ColorFilter?) {
        suppliedFilter = colorFilter
        invalidateSelf()
    }
}
