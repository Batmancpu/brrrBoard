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
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode
import helium314.keyboard.latin.KeyboardMotionSpec
import helium314.keyboard.latin.common.Constants
import helium314.keyboard.latin.utils.TransitionDiagnostics

/** One visual owner for shared public controls, with native layout and touch targets unchanged. */
object EmojiPanelKeyMorph {
    private const val DURATION = 180L
    internal fun position(start: Float, end: Float, progress: Float): Float =
        start + (end - start) * progress.coerceIn(0f, 1f)
    internal fun moves(fromX: Float, fromY: Float, toX: Float, toY: Float): Boolean =
        fromX != toX || fromY != toY

    // The first layout can resize the bottom glass and request a second traversal.
    // Retain the source across at most two pending traversals, never across a new editor.
    internal fun deferTargetCapture(cold: Boolean, pendingLayout: Boolean, targetReady: Boolean,
            retries: Int): Boolean = retries < 2 && ((cold && retries == 0) || pendingLayout || !targetReady)

    class KeyImage(val code: Int, val bounds: RectF, val artwork: Bitmap)
    class Snapshot(val keys: List<KeyImage>)
    private class Track(val from: KeyImage, val to: KeyImage)
    private class Session(val root: ViewGroup, val destination: MainKeyboardView, val source: Snapshot) {
        var listener: ViewTreeObserver.OnPreDrawListener? = null
        var animator: ValueAnimator? = null
        var overlay: MovingKeys? = null
        var detach: View.OnAttachStateChangeListener? = null
        var layout: View.OnLayoutChangeListener? = null
    }
    private var current: Session? = null

    // Actual shared codes always win. Only if a Return key disappears or appears
    // does it hand off to/from the compact panel Search role.
    internal fun pairedCodes(source: List<Int>, target: List<Int>): List<Pair<Int,Int>> {
        val pairs = source.filter { it in target }.map { it to it }.toMutableList()
        val fromReturn = source.firstOrNull(FrostAccentRoles::isReturnRole)
        val toReturn = target.firstOrNull(FrostAccentRoles::isReturnRole)
        val search = KeyCode.EMOJI_SEARCH
        if (fromReturn != null && toReturn != null && fromReturn != toReturn)
            pairs.add(fromReturn to toReturn)
        else if (fromReturn != null && toReturn == null && search in target && search !in source)
            pairs.add(fromReturn to search)
        else if (fromReturn == null && toReturn != null && search in source && search !in target)
            pairs.add(search to toReturn)
        return pairs
    }

    private fun supported(view: MainKeyboardView): Boolean {
        val id = view.keyboard?.mId ?: return false
        return id.isAlphaOrSymbolKeyboard || id.mElementId == KeyboardId.ELEMENT_EMOJI_BOTTOM_ROW
    }

    @JvmStatic fun capture(root: ViewGroup?, view: MainKeyboardView?): Snapshot? {
        if (!KeyboardMotionSpec.enabled() || root == null || view == null || !view.isShown || !supported(view)) return null
        val origin = IntArray(2)
        root.getLocationOnScreen(origin)
        val keys = listOf(Constants.CODE_ENTER, KeyCode.SHIFT_ENTER, KeyCode.EMOJI_SEARCH, KeyCode.DELETE, Constants.CODE_SPACE).mapNotNull { code ->
            val bounds = Rect()
            if (!view.getEmojiMorphArtworkBoundsOnScreen(code,bounds)) return@mapNotNull null
            bounds.offset(-origin[0],-origin[1])
            val artwork = view.renderEmojiMorphKey(code) ?: return@mapNotNull null
            KeyImage(code,RectF(bounds),artwork)
        }
        return if (keys.isEmpty()) null else Snapshot(keys)
    }

    @JvmStatic fun capturePresented(root: ViewGroup?, view: MainKeyboardView?): Snapshot? {
        val native = capture(root,view) ?: return null
        val session = current?.takeIf { it.root === root && it.destination === view } ?: return native
        val overlay = session.overlay
        if (overlay != null) return Snapshot(native.keys.map { overlay.presented(it) ?: it })
        val pairs = pairedCodes(session.source.keys.map { it.code },native.keys.map { it.code })
        return Snapshot(native.keys.map { key ->
            val from = pairs.firstOrNull { it.second == key.code }?.first
                ?.let { code -> session.source.keys.firstOrNull { it.code == code } }
            if (from == null) key else KeyImage(key.code,RectF(from.bounds),from.artwork)
        })
    }

    @JvmStatic fun overlayActive(): Boolean = current?.overlay != null

    @JvmStatic fun cancel() {
        val session = current ?: return
        TransitionDiagnostics.event(TransitionDiagnostics.Event.MORPH_RELEASE)
        current = null
        session.listener?.let { if (session.root.viewTreeObserver.isAlive) session.root.viewTreeObserver.removeOnPreDrawListener(it) }
        session.detach?.let { session.root.removeOnAttachStateChangeListener(it) }
        session.layout?.let { session.destination.removeOnLayoutChangeListener(it) }
        session.animator?.cancel()
        session.destination.setEmojiMorphHiddenCodes(intArrayOf())
        session.overlay?.let { session.root.overlay.remove(it) }
        session.root.invalidate()
    }

    @JvmStatic fun start(root: ViewGroup, source: Snapshot, destination: MainKeyboardView?) {
        cancel()
        if (destination == null || !root.isAttachedToWindow || !KeyboardMotionSpec.enabled() || !supported(destination)) return
        val cold = !destination.isLaidOut || destination.width <= 0 || destination.height <= 0
        val session = Session(root,destination,source)
        current = session
        val detach = object : View.OnAttachStateChangeListener {
            override fun onViewAttachedToWindow(v: View) {}
            override fun onViewDetachedFromWindow(v: View) { if (current === session) cancel() }
        }
        session.detach = detach
        root.addOnAttachStateChangeListener(detach)
        val listener = object : ViewTreeObserver.OnPreDrawListener {
            private var retries = 0
            override fun onPreDraw(): Boolean {
                if (current !== session) return true
                if (!root.isShown || !destination.isShown || !KeyboardMotionSpec.enabled()) {
                    cancel(); return true
                }
                val ready = destination.isLaidOut && destination.width > 0 && destination.height > 0
                val pending = root.isLayoutRequested || destination.isLayoutRequested
                if (deferTargetCapture(cold, pending, ready, retries)) {
                    retries++
                    // Do not present an unmorphed destination before the overlay owns its keys.
                    root.postInvalidateOnAnimation()
                    return false
                }
                val target = capture(root,destination)
                if (target == null && deferTargetCapture(false, false, false, retries)) {
                    retries++
                    root.postInvalidateOnAnimation()
                    return false
                }
                if (root.viewTreeObserver.isAlive) root.viewTreeObserver.removeOnPreDrawListener(this)
                session.listener = null
                if (target == null) { cancel(); return true }
                val tracks = pairedCodes(source.keys.map { it.code },target.keys.map { it.code }).mapNotNull { (a,b) ->
                    val from = source.keys.first { it.code == a }
                    val to = target.keys.first { it.code == b }
                    if (a == b && from.bounds == to.bounds) null else Track(from,to)
                }
                if (tracks.isEmpty()) { cancel(); return true }
                val drawable = MovingKeys(tracks)
                drawable.setBounds(0,0,root.width,root.height)
                session.overlay = drawable
                root.overlay.add(drawable)
                TransitionDiagnostics.event(TransitionDiagnostics.Event.MORPH_OVERLAY)
                destination.setEmojiMorphHiddenCodes(tracks.map { it.to.code }.toIntArray())
                val layout = View.OnLayoutChangeListener { _,l,t,r,b,ol,ot,orr,ob ->
                    if (current === session && (l!=ol || t!=ot || r!=orr || b!=ob)) cancel()
                }
                session.layout = layout
                destination.addOnLayoutChangeListener(layout)
                val keyboard = destination.keyboard
                val animator = ValueAnimator.ofFloat(0f,1f)
                val budget = MorphFrameBudget(root.display?.refreshRate ?: 60f)
                session.animator = animator
                animator.duration = DURATION
                animator.interpolator = KeyboardMotionSpec.easing(root.context)
                animator.addUpdateListener {
                    if (current !== session) return@addUpdateListener
                    if (!destination.isShown || destination.keyboard !== keyboard || !KeyboardMotionSpec.enabled()
                        || budget.shouldSnap(System.nanoTime())) { cancel(); return@addUpdateListener }
                    drawable.progress = it.animatedValue as Float
                    drawable.invalidateSelf()
                }
                animator.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationEnd(animation: Animator) {
                        if (current === session) { session.animator = null; cancel() }
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

    private class MovingKeys(val tracks: List<Track>) : Drawable() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        private val add = PorterDuffXfermode(PorterDuff.Mode.ADD)
        private val rect = RectF()
        var progress = 0f
        private var drawnProgress = 0f

        private fun bounds(track: Track, p: Float, out: RectF) {
            val a = track.from.bounds; val b = track.to.bounds
            out.set(position(a.left,b.left,p),position(a.top,b.top,p),
                position(a.right,b.right,p),position(a.bottom,b.bottom,p))
        }

        fun presented(native: KeyImage): KeyImage? {
            val track = tracks.firstOrNull { it.to.code == native.code } ?: return null
            val visible = RectF()
            bounds(track,drawnProgress,visible)
            // Sample geometry and the dominant artwork, not the logical destination position.
            return KeyImage(native.code,visible,if (drawnProgress < .5f) track.from.artwork else track.to.artwork)
        }

        override fun draw(canvas: Canvas) {
            val p = progress.coerceIn(0f,1f)
            drawnProgress = p
            for (track in tracks) {
                bounds(track,p,rect)
                paint.alpha = 255
                paint.xfermode = null
                if (p <= 0f) {
                    canvas.drawBitmap(track.from.artwork,null,rect,paint)
                } else if (p >= 1f || track.from.code == track.to.code) {
                    // Same-role controls have one artwork, never two translucent keycaps.
                    canvas.drawBitmap(track.to.artwork,null,rect,paint)
                } else {
                    // Isolated additive interpolation of premultiplied pixels,
                    // then composite once onto the glass. No source-over alpha dip.
                    val layer = canvas.saveLayer(rect,null)
                    val alpha = Math.round(255*p)
                    paint.alpha = 255-alpha
                    canvas.drawBitmap(track.from.artwork,null,rect,paint)
                    paint.alpha = alpha
                    paint.xfermode = add
                    canvas.drawBitmap(track.to.artwork,null,rect,paint)
                    paint.alpha = 255
                    paint.xfermode = null
                    canvas.restoreToCount(layer)
                }
            }
        }
        override fun setAlpha(alpha: Int) {}
        override fun setColorFilter(colorFilter: ColorFilter?) {}
        @Deprecated("Deprecated in Android") override fun getOpacity() = PixelFormat.TRANSLUCENT
    }
}
