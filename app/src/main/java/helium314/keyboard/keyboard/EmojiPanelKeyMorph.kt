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
    private const val MAX_GEOMETRY_RETARGETS = 2
    internal fun canRetargetGeometry(count: Int): Boolean = count < MAX_GEOMETRY_RETARGETS
    internal fun position(start: Float, end: Float, progress: Float): Float =
        start + (end - start) * progress.coerceIn(0f, 1f)
    internal fun moves(fromX: Float, fromY: Float, toX: Float, toY: Float): Boolean =
        fromX != toX || fromY != toY

    // The first layout can resize the bottom glass and request a second traversal.
    // Retain the source across at most two pending traversals, never across a new editor.
    internal fun deferTargetCapture(cold: Boolean, pendingLayout: Boolean, targetReady: Boolean,
            retries: Int): Boolean = retries < 2 && ((cold && retries == 0) || pendingLayout || !targetReady)

    class KeyImage(val code: Int, val bounds: RectF, val artwork: Bitmap,
                   val velocity: RectF? = null, val composite: Boolean = false)
    class Snapshot(val keys: List<KeyImage>)
    private class Track(val from: KeyImage, val to: KeyImage)
    private class Session(val root: ViewGroup, val destination: MainKeyboardView, val source: Snapshot,
                          val geometryRetargets: Int) {
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
        val pairs = source.distinct().filter { it in target }.map { it to it }.toMutableList()
        val fromReturn = source.firstOrNull { FrostAccentRoles.isReturnRole(it) && pairs.none { pair -> pair.first == it } }
        val toReturn = target.firstOrNull { FrostAccentRoles.isReturnRole(it) && pairs.none { pair -> pair.second == it } }
        val search = KeyCode.EMOJI_SEARCH
        if (fromReturn != null && toReturn != null)
            pairs.add(fromReturn to toReturn)
        else if (fromReturn != null && target.none(FrostAccentRoles::isReturnRole) &&
            search in target && pairs.none { it.second == search })
            pairs.add(fromReturn to search)
        else if (toReturn != null && source.none(FrostAccentRoles::isReturnRole) &&
            search in source && pairs.none { it.first == search })
            pairs.add(search to toReturn)
        return pairs
    }

    private fun supported(view: MainKeyboardView): Boolean {
        val id = view.keyboard?.mId ?: return false
        return PanelMotionPolicy.supportsArtwork(id.isAlphaOrSymbolKeyboard,
            id.mElementId == KeyboardId.ELEMENT_EMOJI_BOTTOM_ROW,
            id.mElementId == KeyboardId.ELEMENT_CLIPBOARD_BOTTOM_ROW)
    }

    @JvmStatic fun capture(root: ViewGroup?, view: MainKeyboardView?): Snapshot? =
        captureNative(root,view,emptySet())

    private fun captureNative(root: ViewGroup?, view: MainKeyboardView?, skipCodes: Set<Int>): Snapshot? {
        if (root == null || view == null || !view.isShown || !supported(view) ||
            !KeyboardMotionSpec.decorativeMotion(root.context)) return null
        val origin = IntArray(2)
        root.getLocationOnScreen(origin)
        val keys = listOf(Constants.CODE_ENTER, KeyCode.SHIFT_ENTER, KeyCode.EMOJI_SEARCH, KeyCode.DELETE, Constants.CODE_SPACE).mapNotNull { code ->
            if (code in skipCodes) return@mapNotNull null
            val bounds = Rect()
            if (!view.getEmojiMorphArtworkBoundsOnScreen(code,bounds)) return@mapNotNull null
            bounds.offset(-origin[0],-origin[1])
            val artwork = view.renderEmojiMorphKey(code) ?: return@mapNotNull null
            KeyImage(code,RectF(bounds),artwork)
        }
        return if (keys.isEmpty()) null else Snapshot(keys)
    }

    @JvmStatic fun capturePresented(root: ViewGroup?, view: MainKeyboardView?): Snapshot? {
        if (root == null || view == null || !supported(view) ||
            !KeyboardMotionSpec.decorativeMotion(root.context)) return null
        val session = current?.takeIf { it.root === root && it.destination === view }
        val overlay = session?.overlay
        if (overlay != null) {
            // Overlay-owned keys are already visible; do not render their unused
            // native equivalents before sampling the exact presented artwork.
            TransitionDiagnostics.event(TransitionDiagnostics.Event.MORPH_INTERRUPTED)
            val visible = overlay.presentedAll()
            val native = captureNative(root,view,overlay.ownedCodes)
            return Snapshot(visible + (native?.keys ?: emptyList()))
        }
        val native = capture(root,view)
        if (session == null) return native
        if (native == null) return session.source
        val pairs = pairedCodes(session.source.keys.map { it.code },native.keys.map { it.code })
        return Snapshot(native.keys.map { key ->
            val from = pairs.firstOrNull { it.second == key.code }?.first
                ?.let { code -> session.source.keys.firstOrNull { it.code == code } }
            if (from == null) key else KeyImage(key.code,RectF(from.bounds),from.artwork,from.velocity,from.composite || from.code != key.code)
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

    @JvmStatic fun start(root: ViewGroup, source: Snapshot, destination: MainKeyboardView?) =
        startInternal(root,source,destination,0)

    private fun retargetGeometry(session: Session) {
        if (current !== session) return
        if (!canRetargetGeometry(session.geometryRetargets)) {
            TransitionDiagnostics.event(TransitionDiagnostics.Event.MORPH_LAYOUT_FALLBACK)
            cancel()
            return
        }
        val sampled = capturePresented(session.root,session.destination)
        if (sampled != null && session.destination.isShown) {
            TransitionDiagnostics.event(TransitionDiagnostics.Event.MORPH_GEOMETRY_RETARGET)
            startInternal(session.root,sampled,session.destination,session.geometryRetargets+1)
        } else cancel()
    }

    private fun startInternal(root: ViewGroup, source: Snapshot, destination: MainKeyboardView?,
                              geometryRetargets: Int) {
        cancel()
        if (destination == null || !root.isAttachedToWindow || !supported(destination) ||
            !KeyboardMotionSpec.decorativeMotion(root.context)) return
        val cold = !destination.isLaidOut || destination.width <= 0 || destination.height <= 0
        val session = Session(root,destination,source,geometryRetargets)
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
                if (!root.isShown || !destination.isShown || !KeyboardMotionSpec.motionFrameAllowed(root.context)) {
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
                if (target == null) {
                    TransitionDiagnostics.event(TransitionDiagnostics.Event.MORPH_LAYOUT_FALLBACK)
                    cancel(); return true
                }
                val tracks = pairedCodes(source.keys.map { it.code },target.keys.map { it.code }).mapNotNull { (a,b) ->
                    val from = source.keys.first { it.code == a }
                    val to = target.keys.first { it.code == b }
                    if (a == b && from.bounds == to.bounds && !from.composite) null else Track(from,to)
                }
                if (tracks.isEmpty()) { cancel(); return true }
                val drawable = MovingKeys(tracks, KeyboardMotionSpec.easing(root.context))
                drawable.setBounds(0,0,root.width,root.height)
                session.overlay = drawable
                root.overlay.add(drawable)
                TransitionDiagnostics.event(TransitionDiagnostics.Event.MORPH_OVERLAY)
                destination.setEmojiMorphHiddenCodes(tracks.map { it.to.code }.toIntArray())
                val layout = View.OnLayoutChangeListener { _,l,t,r,b,ol,ot,orr,ob ->
                    if (current === session && (l!=ol || t!=ot || r!=orr || b!=ob)) {
                        retargetGeometry(session)
                    }
                }
                session.layout = layout
                destination.addOnLayoutChangeListener(layout)
                var keyboard = destination.keyboard
                val animator = ValueAnimator.ofFloat(0f,1f)
                val budget = MorphFrameBudget(root.display?.refreshRate ?: 60f)
                session.animator = animator
                animator.duration = DURATION
                animator.interpolator = android.view.animation.LinearInterpolator()
                animator.addUpdateListener {
                    if (current !== session) return@addUpdateListener
                    if (!destination.isShown || !supported(destination) || !KeyboardMotionSpec.motionFrameAllowed(root.context)) {
                        cancel(); return@addUpdateListener
                    }
                    if (budget.shouldSnap(System.nanoTime())) {
                        TransitionDiagnostics.event(TransitionDiagnostics.Event.MORPH_BUDGET_FALLBACK)
                        cancel(); return@addUpdateListener
                    }
                    if (destination.keyboard !== keyboard) {
                        val origin = IntArray(2)
                        root.getLocationOnScreen(origin)
                        val bounds = Rect()
                        val same = tracks.all { track ->
                            destination.getEmojiMorphArtworkBoundsOnScreen(track.to.code,bounds) &&
                                run { bounds.offset(-origin[0],-origin[1]); RectF(bounds) == track.to.bounds }
                        }
                        if (!same) {
                            retargetGeometry(session)
                            return@addUpdateListener
                        }
                        keyboard = destination.keyboard
                    }
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

    private class MovingKeys(val tracks: List<Track>,
                             val ease: android.animation.TimeInterpolator?) : Drawable() {
        private val paint = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG)
        private val add = PorterDuffXfermode(PorterDuff.Mode.ADD)
        private val rect = RectF()
        var progress = 0f
        private var drawnProgress = 0f
        private var previousProgress = 0f
        private var drawnAt = 0L
        private var previousAt = 0L
        private fun eased(p: Float): Float = ease?.getInterpolation(p.coerceIn(0f,1f))
            ?: MotionContinuity.smooth(p)
        private fun bounds(track: Track, p: Float, out: RectF) {
            val a = track.from.bounds; val b = track.to.bounds
            val v = track.from.velocity
            fun axis(from: Float, to: Float, velocity: Float): Float =
                if (v == null) position(from,to,eased(p))
                else MotionContinuity.position(from,to,velocity,p,DURATION)
            out.set(axis(a.left,b.left,v?.left ?: 0f),axis(a.top,b.top,v?.top ?: 0f),
                axis(a.right,b.right,v?.right ?: 0f),axis(a.bottom,b.bottom,v?.bottom ?: 0f))
            if (out.right <= out.left) out.right = out.left + 1f
            if (out.bottom <= out.top) out.bottom = out.top + 1f
        }
        private fun drawTrack(canvas: Canvas, track: Track, p: Float, area: RectF) {
            paint.alpha = 255
            paint.xfermode = null
            if (p <= 0f) {
                canvas.drawBitmap(track.from.artwork,null,area,paint)
            } else if (p >= 1f || (track.from.code == track.to.code && !track.from.composite)) {
                canvas.drawBitmap(track.to.artwork,null,area,paint)
            } else {
                val layer = canvas.saveLayer(area,null)
                val alpha = Math.round(255*eased(p))
                paint.alpha = 255-alpha
                canvas.drawBitmap(track.from.artwork,null,area,paint)
                paint.alpha = alpha
                paint.xfermode = add
                canvas.drawBitmap(track.to.artwork,null,area,paint)
                paint.alpha = 255
                paint.xfermode = null
                canvas.restoreToCount(layer)
            }
        }
        private fun presented(track: Track): KeyImage {
            val visible = RectF()
            bounds(track,drawnProgress,visible)
            val velocity = RectF()
            if (previousAt > 0L && drawnAt > previousAt) {
                val prior = RectF()
                bounds(track,previousProgress,prior)
                val rate = 1_000_000_000f / (drawnAt-previousAt).toFloat()
                velocity.set((visible.left-prior.left)*rate,(visible.top-prior.top)*rate,
                    (visible.right-prior.right)*rate,(visible.bottom-prior.bottom)*rate)
            }
            val composite = (track.from.code != track.to.code || track.from.composite) &&
                drawnProgress > 0f && drawnProgress < 1f
            val artwork = if (composite) {
                val w = visible.width().toInt().coerceAtLeast(1)
                val h = visible.height().toInt().coerceAtLeast(1)
                if (w.toLong()*h <= 1_000_000L) {
                    Bitmap.createBitmap(w,h,Bitmap.Config.ARGB_8888).also {
                        it.density = Bitmap.DENSITY_NONE
                        drawTrack(Canvas(it),track,drawnProgress,RectF(0f,0f,w.toFloat(),h.toFloat()))
                    }
                } else track.to.artwork
            } else if (drawnProgress <= 0f) track.from.artwork else track.to.artwork
            return KeyImage(track.to.code,visible,artwork,velocity,composite ||
                (drawnProgress <= 0f && (track.from.composite || track.from.code != track.to.code)))
        }
        val ownedCodes: Set<Int> = tracks.map { it.to.code }.toSet()
        fun presentedAll(): List<KeyImage> = tracks.map(::presented)
        override fun draw(canvas: Canvas) {
            val p = progress.coerceIn(0f,1f)
            if (p != drawnProgress || drawnAt == 0L) {
                previousProgress = drawnProgress
                previousAt = drawnAt
                drawnProgress = p
                drawnAt = System.nanoTime()
            }
            for (track in tracks) {
                bounds(track,p,rect)
                drawTrack(canvas,track,p,rect)
            }
        }
        override fun setAlpha(alpha: Int) {}
        override fun setColorFilter(colorFilter: ColorFilter?) {}
        @Deprecated("Deprecated in Android") override fun getOpacity() = PixelFormat.TRANSLUCENT
    }
}
