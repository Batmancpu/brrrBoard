// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.suggestions

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.drawable.Drawable
import android.text.Spannable
import android.text.SpannableStringBuilder
import android.text.Spanned
import android.text.TextPaint
import android.text.style.CharacterStyle
import android.text.style.UpdateAppearance
import android.view.ViewGroup
import android.view.animation.PathInterpolator
import android.view.animation.LinearInterpolator
import android.widget.TextView
import helium314.keyboard.latin.KeyboardMotionSpec
import kotlin.math.min

/**
 * Slot-local prediction motion. The actual TextViews, words, tags and click
 * targets update synchronously. A changed word never visits a neighbouring
 * cell, and no outgoing text layer is drawn.
 *
 * One clock drives only the new graphemes and the center capsule. Rapid typing
 * cancels the old clock before capturing the next settled text.
 */
internal class SuggestionWordMorph(private val owner: ViewGroup) {
    data class Frame(val labels: List<String?>, val centerConfidence: Boolean,
                     val centerPillWidth: Float,
                     val visibleGlyphs: List<List<SuggestionGlyphSnapshot>>)

    private class SettlingInkSpan(var progress: Float, private val iosMotion: Boolean) : CharacterStyle(), UpdateAppearance {
        override fun updateDrawState(paint: TextPaint) {
            // iOS-inspired: settle the actual glyph ink horizontally, WITHOUT
            // opacity ghosts. Previous Default retains the shipped subtle fade.
            if (iosMotion) {
                paint.alpha = (paint.alpha * (.10f + .90f * progress)).toInt()
                paint.textScaleX *= .94f + .06f * progress
            } else {
                paint.alpha = (paint.alpha * (.74f + .26f * progress)).toInt()
            }
        }
    }

    private data class ActiveInk(val view: TextView, val span: SettlingInkSpan)

    private val easing = PathInterpolator(.20f, 0f, .20f, 1f)
    private val activeInk = ArrayList<ActiveInk>(3)
    private val activeMotion = ArrayList<MorphingSuggestionTextView>(3)
    private var animator: ValueAnimator? = null
    private var pill: SuggestionConfidencePillDrawable? = null
    private var pillView: TextView? = null
    private var pillEndingInactive = false
    private var pillFallbackBackground: Drawable? = null

    fun capture(words: List<TextView>, centerConfidence: Boolean): Frame {
        // Read the *currently drawn* glyph positions before ending an interrupted
        // animation. A faster prediction can retarget smoothly from that frame.
        val canAnimate = owner.isShown && KeyboardMotionSpec.enabled() &&
            KeyboardMotionSpec.isIos27(owner.context)
        val visible = words.map { view ->
            if (canAnimate) (view as? MorphingSuggestionTextView)?.snapshot().orEmpty()
            else emptyList()
        }
        stopMotion()
        val width = words.getOrNull(1)?.let { pillWidth(it) } ?: 0f
        return Frame(words.map { it.text?.toString()?.takeIf { text -> text.isNotBlank() } },
            centerConfidence, width, visible)
    }

    fun morph(previous: Frame, words: List<TextView>, centerConfidence: Boolean,
              fillColor: Int, verticalInset: Float) {
        if (!KeyboardMotionSpec.enabled() || !owner.isShown || words.size < 3) return

        for (index in 0 until min(3, words.size)) {
            val view = words[index]
            if (!view.isShown || view.text.isNullOrBlank()) continue
            val next = view.text.toString()
            if (previous.labels.getOrNull(index) == next) continue
            if (KeyboardMotionSpec.isIos27(owner.context) && view is MorphingSuggestionTextView &&
                view.begin(previous.visibleGlyphs.getOrNull(index).orEmpty(), next)) {
                activeMotion.add(view)
                continue
            }
            val range = SuggestionGlyphDiff.changedRange(previous.labels.getOrNull(index), view.text)
                ?: continue
            val span = SettlingInkSpan(0f, KeyboardMotionSpec.isIos27(owner.context))
            val scale = view.textScaleX
            val styled = SpannableStringBuilder(view.text)
            styled.setSpan(span, range.start, range.endExclusive, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
            view.setText(styled, TextView.BufferType.SPANNABLE)
            view.textScaleX = scale
            activeInk.add(ActiveInk(view, span))
        }

        val center = words[1]
        if ((centerConfidence || previous.centerConfidence) && center.isShown && center.width > 0) {
            pillFallbackBackground = if (centerConfidence) null else center.background
            val targetWidth = pillWidth(center)
            val originWidth = if (previous.centerConfidence) previous.centerPillWidth else targetWidth
            pill = SuggestionConfidencePillDrawable(
                fillColor, originWidth, targetWidth,
                if (previous.centerConfidence) 1f else 0f,
                if (centerConfidence) 1f else 0f,
                verticalInset,
            ).also { drawable ->
                drawable.setProgress(0f)
                center.background = drawable
            }
            pillView = center
            pillEndingInactive = !centerConfidence
        }

        if (activeInk.isEmpty() && activeMotion.isEmpty() && pill == null) return
        val iosMotion = KeyboardMotionSpec.isIos27(owner.context)
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = if (iosMotion) KeyboardMotionSpec.IOS_SUGGESTION_SETTLE_MS
                else KeyboardMotionSpec.WORD_CROSSFADE_MS
            interpolator = if (iosMotion) LinearInterpolator() else easing
            addUpdateListener { update ->
                val fraction = update.animatedValue as Float
                val progress = if (iosMotion)
                    SuggestionInkTiming.reveal(fraction * duration.toFloat())
                else fraction
                activeInk.forEach { ink ->
                    ink.span.progress = progress
                    ink.view.invalidate()
                }
                activeMotion.forEach { it.advance(fraction) }
                pill?.setProgress(if (iosMotion) easing.getInterpolation(fraction) else fraction)
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (animator === animation) stopMotion()
                }
            })
            start()
        }
    }

    fun cancel(words: List<TextView>) {
        stopMotion()
        words.forEach { view ->
            view.animate().cancel()
            view.alpha = 1f
            view.translationX = 0f
            view.translationY = 0f
        }
    }

    private fun stopMotion() {
        val running = animator
        animator = null
        running?.removeAllListeners()
        running?.cancel()
        activeInk.forEach { ink ->
            (ink.view.text as? Spannable)?.removeSpan(ink.span)
            ink.view.invalidate()
        }
        activeInk.clear()
        activeMotion.forEach { it.end() }
        activeMotion.clear()
        pill?.setProgress(1f)
        if (pillEndingInactive) pillView?.background = pillFallbackBackground
        pill = null
        pillView = null
        pillEndingInactive = false
        pillFallbackBackground = null
    }

    private fun pillWidth(view: TextView): Float {
        val density = view.resources.displayMetrics.density
        val measuredText = view.paint.measureText(view.text.toString())
        return SuggestionConfidencePillGeometry.width(measuredText, view.width.toFloat(), density)
    }
}
