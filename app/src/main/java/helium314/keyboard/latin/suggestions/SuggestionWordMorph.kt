// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.suggestions

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.drawable.Drawable
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.view.animation.PathInterpolator
import android.widget.TextView
import helium314.keyboard.latin.KeyboardMotionSpec

/**
 * Presentation-only coordinator. SuggestionStripLayoutHelper has already
 * published the latest words and their tap targets before morph() is called.
 */
internal class SuggestionWordMorph(private val owner: ViewGroup) {
    data class Frame(val labels: List<String?>, val centerConfidence: Boolean,
                     val centerPillWidth: Float, val visibleWords: List<SuggestionGlyphFrame?>)

    private val easing = PathInterpolator(.20f, 0f, .20f, 1f)
    private val active = ArrayList<MorphingSuggestionTextView>(3)
    private var animator: ValueAnimator? = null
    private var pill: SuggestionConfidencePillDrawable? = null
    private var pillView: TextView? = null
    private var pillEndingInactive = false
    private var pillFallbackBackground: Drawable? = null

    fun capture(words: List<TextView>, centerConfidence: Boolean): Frame {
        val canAnimateLetters = owner.isShown && KeyboardMotionSpec.enabled() &&
            KeyboardMotionSpec.isIos27(owner.context)
        val visible = words.map { word ->
            if (canAnimateLetters) (word as? MorphingSuggestionTextView)?.snapshot() else null
        }
        stopMotion()
        return Frame(words.map { it.text?.toString()?.takeIf { label -> label.isNotBlank() } },
            centerConfidence, words.getOrNull(1)?.let(::pillWidth) ?: 0f, visible)
    }

    fun morph(previous: Frame, words: List<TextView>, centerConfidence: Boolean,
              fillColor: Int, verticalInset: Float) {
        if (!KeyboardMotionSpec.enabled() || !owner.isShown || words.size < 3) return

        if (KeyboardMotionSpec.isIos27(owner.context)) {
            for (index in 0..2) {
                val word = words[index] as? MorphingSuggestionTextView ?: continue
                if (!word.isShown || word.text.isNullOrBlank() ||
                    previous.labels.getOrNull(index) == word.text.toString()) continue
                if (word.begin(previous.visibleWords.getOrNull(index))) active.add(word)
            }
        }

        val center = words[1]
        if ((centerConfidence || previous.centerConfidence) && center.isShown && center.width > 0) {
            pillFallbackBackground = if (centerConfidence) null else center.background
            val destination = pillWidth(center)
            val origin = if (previous.centerConfidence) previous.centerPillWidth else destination
            pill = SuggestionConfidencePillDrawable(fillColor, origin, destination,
                if (previous.centerConfidence) 1f else 0f,
                if (centerConfidence) 1f else 0f,
                verticalInset).also { drawable ->
                drawable.setProgress(0f)
                center.background = drawable
            }
            pillView = center
            pillEndingInactive = !centerConfidence
        }

        if (active.isEmpty() && pill == null) return
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = if (active.isNotEmpty()) SuggestionGlyphMotion.SETTLE_MS
                else KeyboardMotionSpec.WORD_CROSSFADE_MS
            interpolator = LinearInterpolator()
            addUpdateListener { frame ->
                val fraction = frame.animatedFraction
                active.forEach { it.advance(fraction) }
                pill?.setProgress(easing.getInterpolation(fraction))
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
        words.forEach { word ->
            word.animate().cancel()
            word.alpha = 1f
            word.translationX = 0f
            word.translationY = 0f
        }
    }

    private fun stopMotion() {
        val running = animator
        animator = null
        running?.removeAllListeners()
        running?.removeAllUpdateListeners()
        running?.cancel()
        active.forEach { it.end() }
        active.clear()
        pill?.setProgress(1f)
        if (pillEndingInactive) pillView?.background = pillFallbackBackground
        pill = null
        pillView = null
        pillEndingInactive = false
        pillFallbackBackground = null
    }

    private fun pillWidth(word: TextView): Float {
        val density = word.resources.displayMetrics.density
        return SuggestionConfidencePillGeometry.width(word.paint.measureText(word.text.toString()),
            word.width.toFloat(), density)
    }
}
