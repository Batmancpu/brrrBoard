// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.suggestions

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.res.ColorStateList
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.PixelFormat
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.text.Spanned
import android.text.TextPaint
import android.text.style.CharacterStyle
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.widget.TextView
import helium314.keyboard.keyboard.MorphFrameBudget
import helium314.keyboard.latin.KeyboardMotionSpec

/**
 * Clean-room QuickType presentation renderer. One drawable, one clock, three hard clips.
 * Native TextViews retain current text, accessibility, layout and input routing.
 * Only their ink is suppressed during a supported visual transition; no cloned Views,
 * bitmap capture, cross-slot movement, model calls or per-frame layout requests.
 */
internal class QuickTypeMotion(private val owner: ViewGroup, private val dividers: List<View>) {
    data class Ink(val character: String, val x: Float, val y: Float,
                   val opacity: Float, val paint: TextPaint, val color: Int)
    data class Slot(val label: String, val clip: Rect, val ink: List<Ink>)
    data class Frame(val slots: List<Slot?>, val confidence: Float, val dividerAlpha: List<Float>)
    private data class Track(val from: Ink, val to: Ink, val duration: Long) {
        fun at(elapsed: Float): Ink {
            val p = QuickTypeMotionPolicy.progress(elapsed, duration)
            return to.copy(x = QuickTypeMotionPolicy.interpolate(from.x, to.x, p),
                y = QuickTypeMotionPolicy.interpolate(from.y, to.y, p),
                opacity = QuickTypeMotionPolicy.interpolate(from.opacity, to.opacity, p))
        }
    }
    private data class MotionSlot(val index: Int, val target: Slot, val tracks: List<Track>)
    private data class NativeInk(val view: TextView, val colors: ColorStateList)
    private data class Divider(val view: View, val from: Float, val to: Float, val finalVisibility: Int)

    private var animator: ValueAnimator? = null
    private var artwork: Drawable? = null
    private var motion = emptyList<MotionSlot>()
    private val hidden = ArrayList<NativeInk>(3)
    private val dividerMotion = ArrayList<Divider>(2)
    private var pill: SuggestionConfidencePillDrawable? = null
    private var pillView: TextView? = null
    private var finalBackground: Drawable? = null
    private var elapsed = 0f

    fun capture(words: List<TextView>, centerConfidence: Boolean): Frame? {
        if (!owner.isShown || !KeyboardMotionSpec.isIos27(owner.context) ||
            !KeyboardMotionSpec.optionalMotion(owner.context)) {
            stop()
            return null
        }
        val frame = Frame((0..2).map { index ->
            val active = motion.firstOrNull { it.index == index }
            if (active == null) words.getOrNull(index)?.let(::snapshot)
            else active.target.copy(ink = active.tracks.map { it.at(elapsed) }
                .filter { it.opacity > .035f }
                .sortedByDescending { it.opacity }.take(QuickTypeMotionPolicy.MAX_LABEL)
                .sortedBy { it.x })
        }, pill?.currentOpacity() ?: if (centerConfidence) 1f else 0f,
            dividers.map { if (it.visibility == View.VISIBLE) it.alpha else 0f })
        stop()
        return frame
    }

    fun morph(previous: Frame, words: List<TextView>, centerConfidence: Boolean,
              fillColor: Int, verticalInset: Float, chips: Boolean) {
        if (chips || !KeyboardMotionSpec.isIos27(owner.context) ||
            !KeyboardMotionSpec.optionalMotion(owner.context) || !owner.isShown ||
            words.size < 3 || owner.width <= 0 || owner.height <= 0) return
        val plans = ArrayList<MotionSlot>(3)
        for (index in 0..2) {
            val view = words[index]
            val before = previous.slots.getOrNull(index) ?: continue
            val after = snapshot(view) ?: continue
            if (before.clip != after.clip) continue // geometry/panel changes snap natively
            if (before.label == after.label && sameInk(before, after)) continue
            val tracks = makeTracks(before, after)
            if (tracks.isEmpty()) continue
            plans.add(MotionSlot(index, after, tracks))
            hidden.add(NativeInk(view, view.textColors))
            view.setTextColor(Color.TRANSPARENT)
        }
        motion = plans
        val center = words[1]
        val targetOpacity = if (centerConfidence) 1f else 0f
        if (center.isShown && center.width > 0 && previous.confidence != targetOpacity) {
            val width = SuggestionConfidencePillGeometry.fixedWidth(
                center.width.toFloat(), center.resources.displayMetrics.density)
            finalBackground = center.background
            pill = SuggestionConfidencePillDrawable(fillColor, width, width,
                previous.confidence, targetOpacity, verticalInset).also {
                it.setProgress(0f)
                center.background = it
            }
            pillView = center
        }
        dividers.forEachIndexed { index, view ->
            if (view.parent !== center.parent) return@forEachIndexed
            val target = if (view.visibility == View.VISIBLE) 1f else 0f
            val start = previous.dividerAlpha.getOrElse(index) { target }
            if (start != target) {
                dividerMotion.add(Divider(view, start, target, view.visibility))
                view.alpha = start
                view.visibility = View.VISIBLE
            }
        }
        if (motion.isEmpty() && pill == null && dividerMotion.isEmpty()) return
        elapsed = 0f
        if (motion.isNotEmpty()) {
            artwork = object : Drawable() {
                override fun draw(canvas: Canvas) {
                    for (slot in motion) {
                        val save = canvas.save()
                        canvas.clipRect(slot.target.clip)
                        for (track in slot.tracks) {
                            // No per-frame layout or glyph-list allocation.
                            val p = QuickTypeMotionPolicy.progress(elapsed, track.duration)
                            val a = QuickTypeMotionPolicy.interpolate(track.from.opacity, track.to.opacity, p)
                            if (a <= .005f) continue
                            val ink = track.to
                            ink.paint.color = ink.color
                            ink.paint.alpha = (Color.alpha(ink.color) * a).toInt().coerceIn(0, 255)
                            canvas.drawText(ink.character,
                                QuickTypeMotionPolicy.interpolate(track.from.x, ink.x, p),
                                QuickTypeMotionPolicy.interpolate(track.from.y, ink.y, p), ink.paint)
                        }
                        canvas.restoreToCount(save)
                    }
                }
                override fun setAlpha(alpha: Int) = Unit
                override fun setColorFilter(colorFilter: ColorFilter?) = Unit
                @Suppress("DEPRECATION")
                override fun getOpacity() = PixelFormat.TRANSLUCENT
            }.also {
                it.setBounds(0, 0, owner.width, owner.height)
                owner.overlay.add(it)
            }
        }
        val duration = maxOf(if (pill != null || dividerMotion.isNotEmpty())
            QuickTypeMotionPolicy.CHROME_MS else 0L,
            motion.maxOfOrNull { slot -> slot.tracks.maxOf { it.duration } } ?: 0L)
        val budget = MorphFrameBudget(owner.display?.refreshRate ?: 60f)
        animator = ValueAnimator.ofFloat(0f, duration.toFloat()).apply {
            this.duration = duration
            interpolator = LinearInterpolator()
            addUpdateListener { value ->
                if (!owner.isShown || !center.isShown || !KeyboardMotionSpec.enabled() ||
                    budget.shouldSnap(System.nanoTime())) {
                    stop()
                    return@addUpdateListener
                }
                elapsed = value.animatedValue as Float
                val chrome = QuickTypeMotionPolicy.progress(elapsed, QuickTypeMotionPolicy.CHROME_MS)
                pill?.setProgress(chrome)
                dividerMotion.forEach { it.view.alpha =
                    QuickTypeMotionPolicy.interpolate(it.from, it.to, chrome) }
                artwork?.invalidateSelf()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (animator === animation) stop()
                }
            })
        }
        animator?.start()
    }

    fun cancel(@Suppress("UNUSED_PARAMETER") words: List<TextView>) = stop()

    private fun stop() {
        val running = animator
        animator = null
        running?.removeAllUpdateListeners()
        running?.removeAllListeners()
        running?.cancel()
        artwork?.let { owner.overlay.remove(it) }
        artwork = null
        hidden.forEach { it.view.setTextColor(it.colors) }
        hidden.clear()
        dividerMotion.forEach {
            it.view.alpha = 1f
            it.view.visibility = it.finalVisibility
        }
        dividerMotion.clear()
        pillView?.background = finalBackground
        pillView = null
        finalBackground = null
        pill = null
        motion = emptyList()
        elapsed = 0f
    }

    private fun snapshot(view: TextView): Slot? {
        val text = view.text ?: ""
        val label = text.toString()
        if (!view.isShown || view.width <= 0 || view.height <= 0 ||
            view.layoutDirection == View.LAYOUT_DIRECTION_RTL ||
            !QuickTypeMotionPolicy.supports(label)) return null
        val location = IntArray(2)
        val origin = IntArray(2)
        view.getLocationOnScreen(location)
        owner.getLocationOnScreen(origin)
        val left = location[0] - origin[0]
        val top = location[1] - origin[1]
        val clip = Rect(left, top, left + view.width, top + view.height)
        if (label.isEmpty()) return Slot(label, clip, emptyList())
        val paint = TextPaint(view.paint)
        paint.color = view.currentTextColor
        paint.textAlign = Paint.Align.LEFT
        if (text is Spanned) {
            for (style in text.getSpans(0, text.length, CharacterStyle::class.java)) {
                // Native fallback for per-range styling; whole-word bold/underline is safe.
                if (text.getSpanStart(style) != 0 || text.getSpanEnd(style) != text.length) return null
                style.updateDrawState(paint)
            }
        }
        val contentLeft = left + view.compoundPaddingLeft
        val contentRight = left + view.width - view.compoundPaddingRight
        val contentTop = top + view.compoundPaddingTop
        val contentBottom = top + view.height - view.compoundPaddingBottom
        val width = paint.measureText(label)
        val x = (contentLeft + contentRight - width) / 2f
        val metrics = paint.fontMetrics
        val baseline = (contentTop + contentBottom - metrics.ascent - metrics.descent) / 2f
        val color = paint.color
        val glyphs = label.indices.map { i ->
            Ink(label.substring(i, i + 1), x + paint.measureText(label, 0, i),
                baseline, 1f, paint, color)
        }
        return Slot(label, clip, glyphs)
    }

    private fun sameInk(before: Slot, after: Slot): Boolean =
        before.ink.size == after.ink.size && before.ink.zip(after.ink).all { (a, b) ->
            a.character == b.character && a.opacity >= .995f &&
                kotlin.math.abs(a.x - b.x) < .5f && a.color == b.color
        }

    private fun makeTracks(before: Slot, after: Slot): List<Track> {
        val old = before.ink
        val next = after.ink
        val matches = QuickTypeMotionPolicy.matches(old.joinToString("") { it.character }, after.label)
        val duration = if (matches.isEmpty()) QuickTypeMotionPolicy.REPLACEMENT_MS
            else QuickTypeMotionPolicy.GLYPH_MS
        val byNew = matches.associate { it.second to it.first }
        val used = matches.map { it.first }.toHashSet()
        val result = ArrayList<Track>(old.size + next.size)
        // Draw disappearing ink first, then matched and arriving glyphs above it.
        old.indices.filterNot { it in used }.forEach { i ->
            val ink = old[i]
            result.add(Track(ink, ink.copy(opacity = 0f), duration))
        }
        next.forEachIndexed { i, ink ->
            val from = byNew[i]?.let { old[it] } ?: ink.copy(opacity = 0f)
            result.add(Track(from, ink, duration))
        }
        return result
    }
}
