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
import androidx.core.graphics.ColorUtils
import android.view.animation.LinearInterpolator
import kotlin.math.roundToInt

/**
 * Compact filled smile with a true underlit cyan reveal from the lenses.
 * Hat, nod, glasses, highlight and face tint share one interruptible timeline.
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
    private val incognitoCyan = Color.rgb(43, 204, 201) // already blended teal; not two colored halves
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
        if (p.hat > .001f && p.hatBehind > .001f)
            drawHat(canvas, p, p.hat * p.hatBehind)
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
        val spread = p.colorSpread.coerceIn(0f, 1f)
        // A faint glow stays INSIDE/at the surface of the circular face. The
        // cyan originates at the glasses and expands as one smooth color field.
        if (spread > .001f) {
            glass.style = Paint.Style.FILL
            glass.colorFilter = null
            glass.color = Color.argb((26f * spread * alphaValue / 255f).roundToInt(), 43, 204, 201)
            c.drawCircle(12f, 12.45f, 9.5f + spread * 1.6f, glass)
        }
        glass.style = Paint.Style.FILL
        glass.color = ColorUtils.setAlphaComponent(themeInk, (244f * alphaValue / 255f).roundToInt())
        glass.colorFilter = suppliedFilter
        c.drawCircle(12f, 12.45f, 8.85f, glass)
        if (spread > .001f) {
            val clipped = c.save()
            val faceClip = Path().apply { addCircle(12f, 12.45f, 8.85f, Path.Direction.CW) }
            c.clipPath(faceClip)
            glass.color = ColorUtils.setAlphaComponent(incognitoCyan,
                (251f * alphaValue / 255f).roundToInt())
            glass.colorFilter = null
            // At zero the colored point glows inside the two lenses; by one it
            // has covered the face. No hard recolor or two-tone gradient seam.
            c.drawCircle(12f, 11.4f, 1.6f + 13.2f * spread, glass)
            c.restoreToCount(clipped)
        }
        val nod = p.nod.coerceIn(0f, 1f)
        val eyes = (1f - p.glasses).coerceIn(0f, 1f)
        val darkFeature = Color.rgb(23, 33, 41)
        val neutralFeature = if (Color.red(themeInk) + Color.green(themeInk) + Color.blue(themeInk) > 382)
            darkFeature else Color.rgb(250, 251, 252)
        val featureColor = ColorUtils.blendARGB(neutralFeature, darkFeature, spread)
        if (eyes > .005f) {
            ink(color = featureColor, opacity = eyes, stroke = 1f)
            ink.style = Paint.Style.FILL
            val eyeY = 10.5f + nod * 1.25f
            val eyeR = 1.08f - nod * .14f
            c.drawCircle(8.8f, eyeY, eyeR, ink)
            c.drawCircle(15.2f, eyeY, eyeR, ink)
        }
        ink(color = featureColor, stroke = 1.75f - nod * .2f)
        val mouth = Path().apply {
            moveTo(8.15f, 15.45f + nod * .25f)
            quadTo(12f, 19.0f - nod * .6f, 15.85f, 15.45f + nod * .25f)
        }
        c.drawPath(mouth, ink)
        if (p.glasses > .005f) drawSunglasses(c, p)
    }

    private fun drawSunglasses(c: Canvas, p: FooterIncognitoMotion.Frame) {
        val opacity = p.glasses.coerceIn(0f, 1f)
        val y = 10f + p.nod * .75f
        val lenses = arrayOf(
            RectF(5.8f, y, 11f, y + 3.65f),
            RectF(13f, y, 18.2f, y + 3.65f),
        )
        // The dark lenses are real filled glass, with theme-ink rims; the original
        // line-art smile stays intact beneath them on both light and dark themes.
        glass.style = Paint.Style.FILL
        glass.color = Color.argb((235f * opacity * alphaValue / 255f).roundToInt(), 17, 30, 39)
        glass.colorFilter = null
        ink(opacity = opacity, stroke = 1.25f)
        for (lens in lenses) {
            c.drawRoundRect(lens, 1.2f, 1.2f, glass)
            c.drawRoundRect(lens, 1.2f, 1.2f, ink)
        }
        ink(opacity = opacity, stroke = 1.3f)
        c.drawLine(11f, y + 1.3f, 13f, y + 1.3f, ink)
        c.drawLine(5.8f, y + 1.1f, 5.2f, y + .95f, ink)
        c.drawLine(18.2f, y + 1.1f, 18.8f, y + .95f, ink)

        // Lens glint sweeps into the cyan wave. At rest a small glass highlight
        // remains, deliberately away from the hat and mouth.
        val shine = p.glint.coerceIn(0f, 1f)
        if (shine > .005f) {
            glass.color = Color.argb((225f * opacity * alphaValue / 255f).roundToInt(), 242, 255, 255)
            val sweep = (shine * 2f - 1f).coerceIn(-1f, 1f)
            for (lens in lenses) {
                val save = c.save()
                val clip = Path().apply { addRoundRect(lens, 1.2f, 1.2f, Path.Direction.CW) }
                c.clipPath(clip)
                val base = lens.left + lens.width() * (if (shine >= .99f) .32f else .36f + .36f * sweep)
                val glimmer = Path().apply {
                    moveTo(base + .25f, lens.top - .4f)
                    lineTo(base + 1.15f, lens.top - .4f)
                    lineTo(base - .35f, lens.bottom + .4f)
                    lineTo(base - 1.25f, lens.bottom + .4f)
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
        val vertical = (1f - p.hat) * -4.2f + p.hatDrop
        c.translate(0f, vertical + p.nod * .22f)
        c.rotate(-3.8f * (1f - p.hat), 12f, 6.3f)
        val crown = Path().apply {
            moveTo(7.9f, 6.45f)
            cubicTo(8.65f, 4.5f, 8.45f, 1.35f, 9.3f, .5f)
            quadTo(12f, -.25f, 14.7f, .5f)
            cubicTo(15.55f, 1.35f, 15.35f, 4.5f, 16.1f, 6.45f)
            close()
        }
        glass.style = Paint.Style.FILL
        glass.color = Color.argb((218f * opacity * alphaValue / 255f).roundToInt(),
            Color.red(themeInk), Color.green(themeInk), Color.blue(themeInk))
        glass.colorFilter = suppliedFilter
        c.drawPath(crown, glass)
        ink(opacity = opacity, stroke = 1.2f)
        c.drawPath(crown, ink)
        // Clearly separated dark hat band keeps the crown legible at 31dp.
        ink(color = Color.rgb(23, 33, 41), opacity = opacity, stroke = 1.3f)
        c.drawLine(8.3f, 5.6f, 15.7f, 5.6f, ink)
        ink(opacity = opacity, stroke = 1.7f)
        val brim = Path().apply {
            moveTo(4.7f, 7.35f)
            quadTo(12f, 8.5f, 19.3f, 7.35f)
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
