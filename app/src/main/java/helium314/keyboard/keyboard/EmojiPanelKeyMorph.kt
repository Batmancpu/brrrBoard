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

    class KeyImage(val code: Int, val bounds: Rect,
        val centerX: Float = bounds.exactCenterX(), val centerY: Float = bounds.exactCenterY()) {
        fun at(x: Float, y: Float) = KeyImage(code, bounds, x, y)
    }
    class Snapshot(val search: KeyImage?, val delete: KeyImage?)
    private class Track(val from: KeyImage, val to: KeyImage, val artwork: Bitmap)
    private class Session(val root: ViewGroup, val destination: MainKeyboardView,
        val source: Snapshot) {
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

    private fun geometry(view: MainKeyboardView, code: Int, rootX: Int, rootY: Int): KeyImage? {
        if (code == NONE) return null
        val rect = Rect()
        if (!view.getKeyBoundsOnScreen(code, rect)) return null
        rect.offset(-rootX, -rootY)
        return KeyImage(code, rect)
    }

    /** Capture only source geometry. The destination is the single visual owner. */
    @JvmStatic fun capture(root: ViewGroup?, view: MainKeyboardView?): Snapshot? {
        if (!KeyboardMotionSpec.enabled() || root == null || view == null || !view.isShown) return null
        val keyboard = view.keyboard ?: return null
        if (!keyboard.mId.isAlphabetKeyboard() && keyboard.mId.mElementId != KeyboardId.ELEMENT_EMOJI_BOTTOM_ROW) return null
        val origin = IntArray(2)
        root.getLocationOnScreen(origin)
        val search = geometry(view, searchCode(view), origin[0], origin[1])
        val delete = geometry(view, KeyCode.DELETE, origin[0], origin[1])
        return if (search == null && delete == null) null else Snapshot(search, delete)
    }

    /**
     * Capture the last actually drawn overlay, not the logical destination.
     * A rapid return before its first draw starts from the previous source.
     * The caller cancels the old owner immediately after sampling in the same
     * UI transaction, so its cleanup cannot cause a positional jump.
     */
    @JvmStatic fun capturePresented(root: ViewGroup?, view: MainKeyboardView?): Snapshot? {
        val native = capture(root, view) ?: return null
        val session = current?.takeIf { it.root === root && it.destination === view }
            ?: return native
        fun presented(key: KeyImage?, source: KeyImage?): KeyImage? {
            if (key == null) return null
            val overlay = session.overlay
            if (overlay != null && overlay.hasDrawn) {
                return overlay.presented(key) ?: key
            }
            return if (source != null) key.at(source.centerX, source.centerY) else key
        }
        return Snapshot(presented(native.search, session.source.search),
            presented(native.delete, session.source.delete))
    }

    internal fun moves(fromX: Float, fromY: Float, toX: Float, toY: Float): Boolean =
        fromX != toX || fromY != toY

    private fun track(from: KeyImage?, to: KeyImage?, destination: MainKeyboardView): Track? {
        if (from == null || to == null || !moves(from.centerX, from.centerY,
                to.centerX, to.centerY)) return null
        val artwork = destination.renderEmojiMorphKey(to.code) ?: return null
        return Track(from, to, artwork)
    }

    @JvmStatic fun overlayActive(): Boolean = current?.overlay != null

    @JvmStatic fun cancel() {
        val session = current ?: return
        helium314.keyboard.latin.utils.TransitionDiagnostics.event(
            helium314.keyboard.latin.utils.TransitionDiagnostics.Event.MORPH_RELEASE)
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
        val session = Session(root, destination, source)
        current = session
        // Leave real controls visible until a replacement overlay exists.
        // A canceled or unlaid-out transition must never leave an empty key.
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
                val target = capture(root, destination)
                if (target == null || !root.isShown) { cancel(); return true }
                val tracks = listOfNotNull(
                    track(source.search, target.search, destination),
                    track(source.delete, target.delete, destination)
                )
                if (tracks.isEmpty()) { cancel(); return true }
                val drawable = MovingKeys(tracks)
                drawable.setBounds(0, 0, root.width, root.height)
                session.overlay = drawable
                root.overlay.add(drawable)
                helium314.keyboard.latin.utils.TransitionDiagnostics.event(
                    helium314.keyboard.latin.utils.TransitionDiagnostics.Event.MORPH_OVERLAY)
                // Both ownership changes happen before the same draw.
                destination.setEmojiMorphHidden(
                    tracks.firstOrNull { it.to.code != KeyCode.DELETE }?.to?.code ?: NONE,
                    tracks.any { it.to.code == KeyCode.DELETE })
                val animator = ValueAnimator.ofFloat(0f, 1f)
                val budget = MorphFrameBudget(root.display?.refreshRate ?: 60f)
                session.animator = animator
                animator.duration = DURATION
                animator.interpolator = KeyboardMotionSpec.easing(root.context)
                animator.addUpdateListener {
                    if (current !== session) return@addUpdateListener
                    if (!destination.isShown || !KeyboardMotionSpec.enabled()
                        || budget.shouldSnap(System.nanoTime())) { cancel(); return@addUpdateListener }
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

    /** Move one unscaled destination artwork at full original opacity.
     * Two overlapping translucent bitmaps cause an opacity dip or double ink
     * during the old crossfade. Root-local coordinates also follow IME resize. */
    private class MovingKeys(val tracks: List<Track>) : Drawable() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        var progress = 0f
        var hasDrawn = false
            private set
        private var drawnProgress = 0f
        fun presented(native: KeyImage): KeyImage? {
            val track = tracks.firstOrNull { it.to.code == native.code } ?: return null
            return native.at(position(track.from.centerX, track.to.centerX, drawnProgress),
                position(track.from.centerY, track.to.centerY, drawnProgress))
        }
        override fun draw(canvas: Canvas) {
            val p = progress.coerceIn(0f, 1f)
            drawnProgress = p
            hasDrawn = true
            for (track in tracks) {
                val x = position(track.from.centerX, track.to.centerX, p)
                val y = position(track.from.centerY, track.to.centerY, p)
                val bitmap = track.artwork
                canvas.drawBitmap(bitmap, x - bitmap.width / 2f, y - bitmap.height / 2f, paint)
            }
        }
        override fun setAlpha(alpha: Int) {}
        override fun setColorFilter(colorFilter: ColorFilter?) {}
        @Deprecated("Deprecated in Android") override fun getOpacity() = PixelFormat.TRANSLUCENT
    }
}
