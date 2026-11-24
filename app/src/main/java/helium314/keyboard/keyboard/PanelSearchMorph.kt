// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode
import helium314.keyboard.latin.InputView
import helium314.keyboard.latin.KeyboardMotionSpec
import helium314.keyboard.latin.common.ColorType
import helium314.keyboard.latin.settings.Settings
import java.util.WeakHashMap

/** One lightweight overlay shape, never a screenshot of a search field or user content. */
object PanelSearchMorph {
    private class Run(val root: ViewGroup, val drawable: Capsule, val animator: ValueAnimator)
    private val pending = WeakHashMap<View, ViewTreeObserver.OnPreDrawListener>()
    private val running = WeakHashMap<View, Run>()
    private val reversePending = WeakHashMap<MainKeyboardView, ViewTreeObserver.OnPreDrawListener>()
    private val reverseRunning = WeakHashMap<MainKeyboardView, Run>()

    @JvmStatic fun bounds(view: View?): Rect? {
        if (view == null || !view.isShown || view.width <= 0 || view.height <= 0) return null
        val location = IntArray(2); view.getLocationOnScreen(location)
        return Rect(location[0], location[1], location[0] + view.width, location[1] + view.height)
    }

    private fun rootOf(view: View): ViewGroup? {
        var ancestor = view.parent
        while (ancestor is View && ancestor !is InputView) ancestor = ancestor.parent
        return (ancestor as? ViewGroup) ?: (view.rootView as? ViewGroup)
    }

    @JvmStatic fun cancelReverse(keyboard: MainKeyboardView?) {
        if (keyboard == null) return
        reversePending.remove(keyboard)?.let { if (keyboard.viewTreeObserver.isAlive) keyboard.viewTreeObserver.removeOnPreDrawListener(it) }
        reverseRunning.remove(keyboard)?.let { run -> run.animator.cancel(); run.root.overlay.remove(run.drawable) }
        keyboard.setPanelSearchMorphHidden(false)
    }

    @JvmStatic fun exit(keyboard: MainKeyboardView?, from: Rect?) {
        if (keyboard == null) return
        cancelReverse(keyboard)
        if (from == null || from.isEmpty || !KeyboardMotionSpec.enabled()) return
        keyboard.setPanelSearchMorphHidden(true)
        val listener = object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (reversePending[keyboard] !== this) return true
                reversePending.remove(keyboard)
                if (keyboard.viewTreeObserver.isAlive) keyboard.viewTreeObserver.removeOnPreDrawListener(this)
                val root = rootOf(keyboard)
                val key = origin(keyboard)
                if (root == null || key == null) { cancelReverse(keyboard); return true }
                val loc = IntArray(2); root.getLocationOnScreen(loc)
                val start = RectF(from).apply { offset(-loc[0].toFloat(), -loc[1].toFloat()) }
                val end = RectF(key).apply { offset(-loc[0].toFloat(), -loc[1].toFloat()) }
                val colors = Settings.getValues().mColors
                val accent = colors.ogFrostKeyAccent != 0
                val fill = colors.get(if (accent) ColorType.OG_FROST_ACCENT_BACKGROUND else ColorType.SPECIAL_KEY_BACKGROUND)
                val ink = if (accent) colors.ogFrostKeyAccentForeground else colors.get(ColorType.KEY_ICON)
                val capsule = Capsule(end, start, fill, ink, keyboard.resources.displayMetrics.density, true)
                capsule.progress = 1f
                capsule.setBounds(0, 0, root.width, root.height)
                val animator = ValueAnimator.ofFloat(1f, 0f)
                val run = Run(root, capsule, animator)
                reverseRunning[keyboard] = run
                root.overlay.add(capsule)
                animator.duration = 135L
                animator.interpolator = KeyboardMotionSpec.easing(keyboard.context)
                animator.addUpdateListener {
                    if (reverseRunning[keyboard] !== run) return@addUpdateListener
                    capsule.progress = it.animatedValue as Float
                    capsule.invalidateSelf()
                }
                animator.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        if (reverseRunning[keyboard] !== run) return
                        reverseRunning.remove(keyboard); root.overlay.remove(capsule)
                        keyboard.setPanelSearchMorphHidden(false)
                    }
                })
                animator.start()
                return true
            }
        }
        reversePending[keyboard] = listener
        keyboard.viewTreeObserver.addOnPreDrawListener(listener)
    }
    @JvmStatic fun origin(keyboard: MainKeyboardView?): Rect? {
        val bounds = Rect()
        return if (keyboard?.getKeyBoundsOnScreen(KeyCode.EMOJI_SEARCH, bounds) == true) bounds else null
    }
    @JvmStatic fun cancel(view: View?) {
        if (view == null) return
        pending.remove(view)?.let { if (view.viewTreeObserver.isAlive) view.viewTreeObserver.removeOnPreDrawListener(it) }
        running.remove(view)?.let { run -> run.animator.cancel(); run.root.overlay.remove(run.drawable) }
        view.alpha = 1f
    }
    @JvmStatic fun enter(view: View?, from: Rect?) {
        if (view == null) return
        cancel(view)
        if (from == null || from.isEmpty || !KeyboardMotionSpec.enabled()) return
        view.alpha = 0f
        val listener = object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (pending[view] !== this) return true
                pending.remove(view)
                if (view.viewTreeObserver.isAlive) view.viewTreeObserver.removeOnPreDrawListener(this)
                val root = rootOf(view)
                if (root == null || !view.isShown || view.width <= 0 || view.height <= 0) { cancel(view); return true }
                val rootLocation = IntArray(2); root.getLocationOnScreen(rootLocation)
                val dest = IntArray(2); view.getLocationOnScreen(dest)
                val start = RectF(from).apply { offset(-rootLocation[0].toFloat(), -rootLocation[1].toFloat()) }
                val end = RectF((dest[0]-rootLocation[0]).toFloat(), (dest[1]-rootLocation[1]).toFloat(),
                    (dest[0]-rootLocation[0]+view.width).toFloat(), (dest[1]-rootLocation[1]+view.height).toFloat())
                val colors = Settings.getValues().mColors
                val accent = colors.ogFrostKeyAccent != 0
                val fill = colors.get(if (accent) ColorType.OG_FROST_ACCENT_BACKGROUND else ColorType.SPECIAL_KEY_BACKGROUND)
                val ink = if (accent) colors.ogFrostKeyAccentForeground else colors.get(ColorType.KEY_ICON)
                val capsule = Capsule(start, end, fill, ink, view.resources.displayMetrics.density)
                capsule.setBounds(0, 0, root.width, root.height)
                val animator = ValueAnimator.ofFloat(0f, 1f)
                val run = Run(root, capsule, animator)
                running[view] = run
                root.overlay.add(capsule)
                animator.duration = 135L
                animator.interpolator = KeyboardMotionSpec.easing(view.context)
                animator.addUpdateListener {
                    if (running[view] !== run) return@addUpdateListener
                    val p = it.animatedValue as Float
                    capsule.progress = p
                    view.alpha = ((p - .73f) / .27f).coerceIn(0f, 1f)
                    capsule.invalidateSelf()
                }
                animator.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        if (running[view] !== run) return
                        running.remove(view)
                        root.overlay.remove(capsule)
                        view.alpha = 1f
                    }
                })
                animator.start()
                return true
            }
        }
        pending[view] = listener
        view.viewTreeObserver.addOnPreDrawListener(listener)
    }

    private class Capsule(val start: RectF, val end: RectF, val fill: Int, val ink: Int, val density: Float, val reverse: Boolean = false) : Drawable() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
        private val rect = RectF()
        var progress = 0f
        override fun draw(canvas: Canvas) {
            val p = progress.coerceIn(0f, 1f)
            fun mix(a: Float, b: Float) = a + (b - a) * p
            rect.set(mix(start.left, end.left), mix(start.top, end.top), mix(start.right, end.right), mix(start.bottom, end.bottom))
            val opacity = if (reverse) 1f else (1f - ((p - .73f) / .27f).coerceIn(0f, 1f))
            val radius = minOf(rect.height()/2f, mix(8f*density, end.height()/2f))
            paint.style = Paint.Style.FILL
            paint.color = fill; paint.alpha = (Color.alpha(fill)*opacity).toInt()
            canvas.drawRoundRect(rect, radius, radius, paint)
            paint.style = Paint.Style.STROKE
            paint.strokeWidth = 1.8f*density; paint.strokeCap = Paint.Cap.ROUND
            paint.color = ink; paint.alpha = (Color.alpha(ink)*opacity).toInt()
            val x = mix(start.centerX(), end.left + 20f*density)
            val y = rect.centerY(); val r = minOf(5f*density, rect.height()*.18f)
            canvas.drawCircle(x-r*.25f, y-r*.25f, r, paint)
            canvas.drawLine(x+r*.45f, y+r*.45f, x+r*1.25f, y+r*1.25f, paint)
        }
        override fun setAlpha(alpha: Int) {}
        override fun setColorFilter(colorFilter: ColorFilter?) {}
        @Deprecated("Deprecated in Android") override fun getOpacity() = PixelFormat.TRANSLUCENT
    }
}
