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

/** Normal smile icon with a small, matching gold privacy shield in incognito mode. */
internal class FooterIncognitoEmojiDrawable(context: Context) : Drawable() {
    private val iconPx = (34f * context.resources.displayMetrics.density).roundToInt()
    private val ink = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.STROKE
        strokeJoin = Paint.Join.ROUND
        strokeCap = Paint.Cap.ROUND
    }
    private val fill = Paint(Paint.ANTI_ALIAS_FLAG)
    private val gold = FooterPrivacyPalette.GOLD
    private var themeInk = Color.WHITE
    private var alphaValue = 255
    private var suppliedFilter: ColorFilter? = null
    private var pose = FooterIncognitoMotion.Frame()
    private var wantedIncognito = false
    private var animator: ValueAnimator? = null
    private var frameListener: ((FooterIncognitoMotion.Frame) -> Unit)? = null
    private var denialFraction = 0f

    override fun getIntrinsicWidth(): Int = iconPx
    override fun getIntrinsicHeight(): Int = iconPx
    override fun getOpacity(): Int = PixelFormat.TRANSLUCENT

    fun setThemeInk(color: Int) {
        if (themeInk != color) { themeInk = color; invalidateSelf() }
    }

    /** The microphone badge is driven by this same frame and clock. */
    fun setFrameListener(listener: ((FooterIncognitoMotion.Frame) -> Unit)?) {
        frameListener = listener
        listener?.invoke(pose)
    }

    fun setDenialFraction(amount: Float) {
        denialFraction = amount.coerceIn(0f, 1f)
        invalidateSelf()
    }

    private fun publishPose() {
        frameListener?.invoke(pose)
        invalidateSelf()
    }

    fun setIncognito(enabled: Boolean, animated: Boolean) {
        if (enabled == wantedIncognito && animator?.isRunning == true) return
        if (enabled == wantedIncognito && pose == FooterIncognitoMotion.settled(enabled)) return
        val from = pose
        val oldAnimator = animator
        animator = null
        oldAnimator?.cancel()
        wantedIncognito = enabled
        if (!animated || !KeyboardMotionSpec.enabled()) {
            pose = FooterIncognitoMotion.settled(enabled)
            publishPose()
            return
        }
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = if (enabled) FooterIncognitoMotion.ENTER_MS else FooterIncognitoMotion.EXIT_MS
            interpolator = LinearInterpolator()
            addUpdateListener {
                pose = FooterIncognitoMotion.at(from, enabled, it.animatedValue as Float)
                publishPose()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (animator === animation) {
                        animator = null
                        pose = FooterIncognitoMotion.settled(enabled)
                        publishPose()
                    }
                }
            })
            start()
        }
    }

    fun stopAnimation() {
        val oldAnimator = animator
        animator = null
        oldAnimator?.cancel()
        pose = FooterIncognitoMotion.settled(wantedIncognito)
        denialFraction = 0f
        publishPose()
    }

    override fun draw(canvas: Canvas) {
        if (bounds.isEmpty) return
        val saved = canvas.save()
        canvas.translate(bounds.left.toFloat(), bounds.top.toFloat())
        canvas.scale(bounds.width() / 24f, bounds.height() / 24f)
        drawSmile(canvas, pose)
        if (pose.shield > .001f) drawPrivacyBadge(canvas, pose)
        canvas.restoreToCount(saved)
    }

    private fun drawSmile(canvas: Canvas, frame: FooterIncognitoMotion.Frame) {
        val tint = if (denialFraction > .01f) ColorUtils.blendARGB(
            themeInk, 0xFFFF7378.toInt(), .25f * denialFraction
        ) else themeInk
        val alpha = (255f * alphaValue / 255f).roundToInt()
        fill.style = Paint.Style.FILL
        fill.color = ColorUtils.setAlphaComponent(tint, alpha)
        fill.colorFilter = suppliedFilter
        // Transparent cutouts show the existing glass, not an assumed background.
        canvas.drawPath(Path().apply {
            fillType = Path.FillType.EVEN_ODD
            addCircle(12f, 12f, 9.5f, Path.Direction.CW)
            addCircle(8.4f, 10f, 1.05f, Path.Direction.CW)
            addCircle(15.6f, 10f, 1.05f, Path.Direction.CW)
            moveTo(6f, 12.8f)
            quadTo(12f, 15.1f, 18f, 12.8f)
            quadTo(16.8f, 19.2f, 12f, 19.2f)
            quadTo(7.2f, 19.2f, 6f, 12.8f)
            close()
        }, fill)
        canvas.drawPath(Path().apply {
            moveTo(7.5f, 14.2f)
            quadTo(12f, 16.4f, 16.5f, 14.2f)
            lineTo(15.9f, 15.6f)
            quadTo(12f, 17.2f, 8.1f, 15.6f)
            close()
        }, fill)
        if (frame.glow > .001f) {
            fill.style = Paint.Style.STROKE
            fill.strokeWidth = .75f
            fill.color = ColorUtils.setAlphaComponent(FooterPrivacyPalette.HIGHLIGHT,
                (70f * frame.glow * alphaValue / 255f).roundToInt())
            canvas.drawCircle(12f, 12f, 10.1f, fill)
        }
    }

    private fun drawPrivacyBadge(canvas: Canvas, frame: FooterIncognitoMotion.Frame) {
        val opacity = frame.shield.coerceIn(0f, 1f)
        val checkpoint = canvas.save()
        canvas.translate(18.6f, 5.0f)
        canvas.scale(.72f + .28f * opacity, .72f + .28f * opacity, 2.6f, 2.9f)
        val shield = Path().apply {
            moveTo(2.65f, .15f)
            cubicTo(1.9f, .62f, .95f, .83f, .3f, .96f)
            lineTo(.36f, 2.63f)
            cubicTo(.46f, 3.66f, 1.63f, 4.72f, 2.65f, 5.35f)
            cubicTo(3.67f, 4.72f, 4.84f, 3.66f, 4.94f, 2.63f)
            lineTo(5f, .96f)
            cubicTo(4.35f, .83f, 3.4f, .62f, 2.65f, .15f)
            close()
        }
        fill.style = Paint.Style.FILL
        fill.color = ColorUtils.setAlphaComponent(Color.rgb(20, 35, 53),
            (232f * opacity * alphaValue / 255f).roundToInt())
        fill.colorFilter = null
        canvas.drawPath(shield, fill)
        ink.style = Paint.Style.STROKE
        ink.strokeWidth = .64f
        ink.color = ColorUtils.setAlphaComponent(
            if (denialFraction > .01f) 0xFFFF8E8E.toInt() else gold,
            (255f * opacity * alphaValue / 255f).roundToInt())
        ink.colorFilter = null
        canvas.drawPath(shield, ink)
        ink.strokeWidth = .48f
        canvas.drawArc(RectF(1.8f, 2.05f, 3.5f, 3.75f), 180f, 180f, false, ink)
        fill.style = Paint.Style.FILL
        fill.color = ink.color
        canvas.drawRoundRect(RectF(1.55f, 2.8f, 3.75f, 4.15f), .35f, .35f, fill)
        canvas.restoreToCount(checkpoint)
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
