// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.inputmethod.EditorInfo
import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode
import helium314.keyboard.latin.KeyboardMotionSpec
import helium314.keyboard.latin.common.Constants

/**
 * Shared-element handoff for exactly two real keys between typing and compact Emoji.
 * No layout modifications, text snapshots, scaling or whole-panel animation.
 */
object EmojiPanelKeyMorph {
    private const val NONE = Int.MIN_VALUE
    private const val DURATION = 150L

    // Pure geometry shared with unit tests. Native image dimensions never change.
    internal fun position(start: Float, end: Float, progress: Float): Float =
        start + (end - start) * progress.coerceIn(0f, 1f)

    class KeyImage(val code: Int, val bounds: Rect, val bitmap: Bitmap)
    class Snapshot(val search: KeyImage?, val delete: KeyImage?)
    private class Track(val from: KeyImage, val to: KeyImage)
    private class Session(val root: ViewGroup, val destination: MainKeyboardView) {
        var listener: ViewTreeObserver.OnPreDrawListener? = null
        var animator: ValueAnimator? = null
        var overlay: MovingKeys? = null
        var detach: View.OnAttachStateChangeListener? = null
    }
    private var current: Session? = null

    private fun searchCode(view: MainKeyboardView): Int {
        val keyboard = view.keyboard ?: return NONE
        return when {
            keyboard.mId.mElementId == KeyboardId.ELEMENT_EMOJI_BOTTOM_ROW -> KeyCode.EMOJI_SEARCH
            keyboard.mId.isAlphabetKeyboard() && keyboard.mId.imeAction() == EditorInfo.IME_ACTION_SEARCH -> Constants.CODE_ENTER
            else -> NONE
        }
    }

    private fun image(view: MainKeyboardView, code: Int): KeyImage? {
        if (code == NONE) return null
        val rect = Rect()
        if (!view.getKeyBoundsOnScreen(code, rect)) return null
        val bitmap = view.renderEmojiMorphKey(code) ?: return null
        return KeyImage(code, rect, bitmap)
    }

    @JvmStatic fun capture(view: MainKeyboardView?): Snapshot? {
        if (!KeyboardMotionSpec.enabled() || view == null || !view.isShown) return null
        val keyboard = view.keyboard ?: return null
        if (!keyboard.mId.isAlphabetKeyboard() && keyboard.mId.mElementId != KeyboardId.ELEMENT_EMOJI_BOTTOM_ROW) return null
        val search = image(view, searchCode(view))
        val delete = image(view, KeyCode.DELETE)
        return if (search == null && delete == null) null else Snapshot(search, delete)
    }

    @JvmStatic fun cancel() {
        val session = current ?: return
        current = null
        session.listener?.let { if (session.root.viewTreeObserver.isAlive) session.root.viewTreeObserver.removeOnPreDrawListener(it) }
        session.detach?.let { session.root.removeOnAttachStateChangeListener(it) }
        session.animator?.cancel()
        session.overlay?.let { session.root.overlay.remove(it) }
        session.destination.setEmojiMorphHidden(NONE, false)
    }

    @JvmStatic fun start(root: ViewGroup, source: Snapshot, destination: MainKeyboardView?) {
        cancel()
        if (destination == null || !root.isAttachedToWindow || !KeyboardMotionSpec.enabled()) return
        val keyboard = destination.keyboard ?: return
        if (!keyboard.mId.isAlphabetKeyboard() && keyboard.mId.mElementId != KeyboardId.ELEMENT_EMOJI_BOTTOM_ROW) return
        val session = Session(root, destination)
        current = session
        val code = searchCode(destination)
        // Hide before first draw, then measure after destination layout.
        destination.setEmojiMorphHidden(if (source.search != null) code else NONE, source.delete != null)
        val detach = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {}
            override fun onViewDetachedFromWindow(v: View) { if (current === session) cancel() }
        }
        session.detach = detach
        root.addOnAttachStateChangeListener(detach)
        val listener = object : ViewTreeObserver.OnPreDrawListener {
            override fun onPreDraw(): Boolean {
                if (current !== session) return true
                if (root.viewTreeObserver.isAlive) root.viewTreeObserver.removeOnPreDrawListener(this)
                session.listener = null
                val target = capture(destination)
                if (target == null || !root.isShown) { cancel(); return true }
                val tracks = listOfNotNull(
                    if (source.search != null && target.search != null) Track(source.search, target.search) else null,
                    if (source.delete != null && target.delete != null) Track(source.delete, target.delete) else null
                ).filter { it.from.bounds != it.to.bounds }
                if (tracks.isEmpty()) { cancel(); return true }
                destination.setEmojiMorphHidden(
                    tracks.firstOrNull { it.to.code != KeyCode.DELETE }?.to?.code ?: NONE,
                    tracks.any { it.to.code == KeyCode.DELETE })
                val loc = IntArray(2); root.getLocationOnScreen(loc)
                val drawable = MovingKeys(tracks, loc[0], loc[1])
                drawable.setBounds(0, 0, root.width, root.height)
                session.overlay = drawable
                root.overlay.add(drawable)
                val animator = ValueAnimator.ofFloat(0f, 1f)
                session.animator = animator
                animator.duration = DURATION
                animator.interpolator = KeyboardMotionSpec.easing(root.context)
                animator.addUpdateListener {
                    if (current !== session) return@addUpdateListener
                    if (!destination.isShown) { cancel(); return@addUpdateListener }
                    drawable.progress = it.animatedValue as Float
                    drawable.invalidateSelf()
                }
                animator.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        if (current === session) {
                            session.animator = null
                            cancel()
                        }
                    }
                })
                animator.start()
                return true
            }
        }
        session.listener = listener
        root.viewTreeObserver.addOnPreDrawListener(listener)
        root.invalidate()
    }

    /** Move native-sized key artwork only. If existing layouts differ in size,
     * crossfade their two unscaled artworks instead of stretching either key. */
    private class MovingKeys(val tracks: List<Track>, val rootX: Int, val rootY: Int) : Drawable() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        var progress = 0f
        override fun draw(canvas: Canvas) {
            val p = progress.coerceIn(0f, 1f)
            for (track in tracks) {
                val x = position(track.from.bounds.exactCenterX(), track.to.bounds.exactCenterX(), p) - rootX
                val y = position(track.from.bounds.exactCenterY(), track.to.bounds.exactCenterY(), p) - rootY
                val blend = ((p - .25f) / .5f).coerceIn(0f, 1f)
                drawNative(canvas, track.from.bitmap, x, y, 1f - blend)
                drawNative(canvas, track.to.bitmap, x, y, blend)
            }
        }
        private fun drawNative(canvas: Canvas, bitmap: Bitmap, x: Float, y: Float, opacity: Float) {
            if (opacity <= 0f) return
            paint.alpha = (opacity * 255f).toInt()
            canvas.drawBitmap(bitmap, x - bitmap.width / 2f, y - bitmap.height / 2f, paint)
        }
        override fun setAlpha(alpha: Int) {}
        override fun setColorFilter(colorFilter: ColorFilter?) {}
        @Deprecated("Deprecated in Android") override fun getOpacity() = PixelFormat.TRANSLUCENT
    }
}
