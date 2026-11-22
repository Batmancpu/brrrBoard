// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.suggestions

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.graphics.Color
import android.graphics.Rect
import android.util.TypedValue
import android.view.View
import android.view.ViewGroup
import android.view.animation.LinearInterpolator
import android.view.animation.PathInterpolator
import android.widget.TextView
import helium314.keyboard.latin.KeyboardMotionSpec
import helium314.keyboard.latin.R
import helium314.keyboard.keyboard.KeyboardTypeface
import kotlin.math.max

/**
 * Presentation-only transition for the three native suggestion slots.
 *
 * The decoder publishes current words, labels, and tap targets synchronously. This class draws
 * outgoing labels in a non-interactive ViewGroupOverlay while the real native TextViews fade and
 * settle in. It does no glyph matching, bitmap capture, dictionary access, or model work. Up to
 * two outgoing labels per slot are retained when fast typing retargets an in-flight transition.
 */
internal class SuggestionWordMorph(private val owner: ViewGroup) {
    data class Frame(
        val labels: List<String?>,
        val labelAlpha: List<Float>,
        val labelTranslationY: List<Float>,
        val labelScale: List<Float>,
        val labelTextScaleX: List<Float>,
        val labelColor: List<Int>,
        val centerPillWidth: Float,
        val centerPillOpacity: Float,
    )

    private data class Incoming(val view: TextView, val delayMs: Long, val startAlpha: Float)
    private data class Outgoing(
        val slot: Int,
        val view: TextView,
        var startAlpha: Float,
        var startTranslationY: Float,
        var startScale: Float,
        var delayMs: Long,
        val stablePrefixClipStart: Int,
    )

    private val easing = PathInterpolator(.20f, 0f, .20f, 1f)
    private val incoming = ArrayList<Incoming>(3)
    private val outgoing = ArrayList<Outgoing>(6)
    private val overlayPool = ArrayList<TextView>(6)
    private var animator: ValueAnimator? = null
    private var pill: SuggestionConfidencePillDrawable? = null
    private var pillView: TextView? = null
    private var pillEndingInactive = false
    private var pillFallbackBackground: android.graphics.drawable.Drawable? = null

    fun capture(words: List<TextView>, centerConfidence: Boolean): Frame {
        val frame = Frame(
            labels = words.map { it.text?.toString()?.takeIf(String::isNotBlank) },
            labelAlpha = words.map { it.alpha },
            labelTranslationY = words.map { it.translationY },
            labelScale = words.map { it.scaleX },
            labelTextScaleX = words.map { it.textScaleX },
            labelColor = words.map { it.currentTextColor },
            centerPillWidth = pill?.currentWidth() ?: words.getOrNull(1)?.let(::pillWidth) ?: 0f,
            centerPillOpacity = pill?.currentOpacity() ?: if (centerConfidence) 1f else 0f,
        )
        freezeRunningTransition(words)
        // Layout helpers rebind the center background on every update. The frame above retains
        // the live pill geometry, so the previous drawable can be discarded safely here.
        pill = null
        pillView = null
        pillEndingInactive = false
        pillFallbackBackground = null
        return frame
    }

    fun morph(
        previous: Frame,
        words: List<TextView>,
        centerConfidence: Boolean,
        fillColor: Int,
        verticalInset: Float,
        chips: Boolean,
    ) {
        if (!KeyboardMotionSpec.enabled() || !KeyboardMotionSpec.isIos27(owner.context)
            || !owner.isShown || words.size < 3) {
            cancel(words)
            return
        }

        for (index in words.indices) {
            val word = words[index]
            val label = word.text?.toString().orEmpty()
            val oldLabel = previous.labels.getOrNull(index)
            if (!word.isShown || label.isBlank() || oldLabel == label) continue

            val sideDelay = if (index == 1) 0L else KeyboardMotionSpec.SUGGESTION_SIDE_STAGGER_MS
            val stablePrefixEnd = SuggestionPrefixTransition.stablePrefixEnd(oldLabel, label)
            if (!oldLabel.isNullOrBlank()) {
                val oldAlpha = previous.labelAlpha.getOrNull(index) ?: 1f
                if (oldAlpha > .015f) {
                    addOutgoing(
                        index = index,
                        source = word,
                        text = oldLabel,
                        alpha = oldAlpha,
                        translationY = previous.labelTranslationY.getOrNull(index) ?: 0f,
                        scale = previous.labelScale.getOrNull(index) ?: 1f,
                        textScaleX = previous.labelTextScaleX.getOrNull(index) ?: word.textScaleX,
                        color = previous.labelColor.getOrNull(index) ?: word.currentTextColor,
                        delayMs = sideDelay,
                        stablePrefixEnd = stablePrefixEnd,
                    )
                }
            }

            // Keep the newly published label stationary and immediately tappable. For related
            // words the old overlay clips away everything before the shared prefix, so the
            // stable prefix never fades or jumps; only the predicted suffix contracts away to
            // reveal the new suffix underneath (for example, tree -> they). Unrelated words and
            // first appearances retain the bounded whole-word settle.
            if (stablePrefixEnd > 0) {
                resetTransform(word)
                word.clipBounds = null
            } else {
                val startAlpha = if (oldLabel.isNullOrBlank())
                    KeyboardMotionSpec.SUGGESTION_START_ALPHA else 0f
                word.alpha = startAlpha
                word.translationY = KeyboardMotionSpec.SUGGESTION_START_OFFSET_DP *
                    word.resources.displayMetrics.density
                word.scaleX = KeyboardMotionSpec.SUGGESTION_START_SCALE
                word.scaleY = KeyboardMotionSpec.SUGGESTION_START_SCALE
                word.clipBounds = null
                incoming.add(Incoming(word, sideDelay, startAlpha))
            }
        }

        val center = words[1]
        if (!chips && (centerConfidence || previous.centerPillOpacity > 0f)
            && center.isShown && center.width > 0) {
            pillFallbackBackground = if (centerConfidence) null else center.background
            val destination = pillWidth(center)
            val origin = if (previous.centerPillOpacity > 0f) previous.centerPillWidth else destination
            pill = SuggestionConfidencePillDrawable(
                fillColor, origin, destination, previous.centerPillOpacity,
                if (centerConfidence) 1f else 0f, verticalInset,
            ).also { drawable ->
                drawable.setProgress(0f)
                center.background = drawable
            }
            pillView = center
            pillEndingInactive = !centerConfidence
        }

        if (incoming.isEmpty() && outgoing.isEmpty() && pill == null) return
        val longestDelay = max(
            incoming.maxOfOrNull { it.delayMs } ?: 0L,
            outgoing.maxOfOrNull { it.delayMs } ?: 0L,
        )
        animator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = KeyboardMotionSpec.WORD_CROSSFADE_MS + longestDelay
            interpolator = LinearInterpolator()
            addUpdateListener { frame ->
                val elapsedMs = frame.animatedFraction *
                    (KeyboardMotionSpec.WORD_CROSSFADE_MS + longestDelay)
                incoming.forEach { slot ->
                    val local = ((elapsedMs - slot.delayMs) / KeyboardMotionSpec.WORD_CROSSFADE_MS)
                        .coerceIn(0f, 1f)
                    val eased = easing.getInterpolation(local)
                    val view = slot.view
                    view.alpha = slot.startAlpha + (1f - slot.startAlpha) * eased
                    view.translationY = KeyboardMotionSpec.SUGGESTION_START_OFFSET_DP *
                        view.resources.displayMetrics.density * (1f - eased)
                    val scale = KeyboardMotionSpec.SUGGESTION_START_SCALE +
                        (1f - KeyboardMotionSpec.SUGGESTION_START_SCALE) * eased
                    view.scaleX = scale
                    view.scaleY = scale
                }
                outgoing.forEach { slot ->
                    val local = ((elapsedMs - slot.delayMs) / KeyboardMotionSpec.WORD_CROSSFADE_MS)
                        .coerceIn(0f, 1f)
                    val eased = easing.getInterpolation(local)
                    if (slot.stablePrefixClipStart > 0) {
                        val width = slot.view.width
                        val left = slot.stablePrefixClipStart.coerceIn(0, width)
                        val right = (left + (width - left) * (1f - eased)).toInt()
                            .coerceIn(left, width)
                        slot.view.clipBounds = Rect(left, 0, right, slot.view.height)
                        slot.view.alpha = slot.startAlpha * (1f - eased * .28f)
                    } else {
                        slot.view.alpha = slot.startAlpha * (1f - eased)
                        val lift = viewDensity(slot.view) * 1.1f * eased
                        slot.view.translationY = slot.startTranslationY - lift
                        val scale = slot.startScale + (.995f - slot.startScale) * eased
                        slot.view.scaleX = scale
                        slot.view.scaleY = scale
                    }
                }
                pill?.setProgress(easing.getInterpolation(frame.animatedFraction))
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (animator === animation) finish(words)
                }
            })
            start()
        }
    }

    fun cancel(words: List<TextView>) {
        freezeRunningTransition(words)
        clearOutgoing()
        pill?.setProgress(1f)
        if (pillEndingInactive) pillView?.background = pillFallbackBackground
        clearPill()
    }

    private fun freezeRunningTransition(words: List<TextView>) {
        val running = animator
        animator = null
        running?.removeAllListeners()
        running?.removeAllUpdateListeners()
        running?.cancel()
        incoming.forEach { slot -> resetTransform(slot.view) }
        incoming.clear()
        words.forEach(::resetTransform)
        // Preserve the currently visible outgoing text at its sampled opacity. A fast next
        // keystroke can then retarget it smoothly instead of making the prior word blink out.
        outgoing.forEach { slot ->
            slot.startAlpha = slot.view.alpha
            slot.startTranslationY = slot.view.translationY
            slot.startScale = slot.view.scaleX
            slot.delayMs = 0L
        }
    }

    private fun addOutgoing(
        index: Int,
        source: TextView,
        text: String,
        alpha: Float,
        translationY: Float,
        scale: Float,
        textScaleX: Float,
        color: Int,
        delayMs: Long,
        stablePrefixEnd: Int,
    ) {
        val sameSlot = outgoing.filter { it.slot == index }
        if (sameSlot.size >= MAX_OUTGOING_PER_SLOT) {
            val oldest = sameSlot.minByOrNull { it.startAlpha }
            if (oldest != null) removeOutgoing(oldest)
        }
        val ghost = if (overlayPool.isEmpty()) {
            TextView(owner.context, null, R.attr.suggestionWordStyle).also {
                KeyboardTypeface.applyToTextView(it)
            }
        } else overlayPool.removeAt(overlayPool.lastIndex)
        ghost.text = text
        ghost.setTextColor(color)
        ghost.setTextSize(TypedValue.COMPLEX_UNIT_PX, source.textSize)
        ghost.typeface = source.typeface
        ghost.letterSpacing = source.letterSpacing
        ghost.textScaleX = textScaleX
        ghost.gravity = source.gravity
        ghost.includeFontPadding = source.includeFontPadding
        ghost.textAlignment = source.textAlignment
        ghost.textDirection = source.textDirection
        ghost.maxLines = source.maxLines
        ghost.ellipsize = source.ellipsize
        ghost.setPadding(source.paddingLeft, source.paddingTop, source.paddingRight, source.paddingBottom)
        ghost.setBackgroundColor(Color.TRANSPARENT)
        ghost.isClickable = false
        ghost.isFocusable = false
        ghost.importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO_HIDE_DESCENDANTS
        ghost.alpha = alpha.coerceIn(0f, 1f)
        ghost.translationY = translationY
        ghost.scaleX = scale
        ghost.scaleY = scale
        positionOverlayAt(ghost, source)
        // TextView's own Layout gives the shaped horizontal position of the prefix boundary.
        // The supported transition policy is deliberately limited to simple LTR Latin labels;
        // other scripts keep the native whole-word transition.
        val prefixClipStart = if (stablePrefixEnd > 0 && ghost.layout != null) {
            (ghost.paddingLeft + ghost.layout.getPrimaryHorizontal(stablePrefixEnd))
                .toInt().coerceIn(0, ghost.width)
        } else 0
        if (prefixClipStart > 0) {
            ghost.clipBounds = Rect(prefixClipStart, 0, ghost.width, ghost.height)
        }
        owner.overlay.add(ghost)
        outgoing.add(Outgoing(index, ghost, ghost.alpha, translationY, scale, delayMs,
            prefixClipStart))
    }

    private fun positionOverlayAt(overlay: TextView, source: TextView) {
        val sourceLocation = IntArray(2)
        val ownerLocation = IntArray(2)
        source.getLocationOnScreen(sourceLocation)
        owner.getLocationOnScreen(ownerLocation)
        val width = source.width.coerceAtLeast(1)
        val height = source.height.coerceAtLeast(1)
        val exactWidth = View.MeasureSpec.makeMeasureSpec(width, View.MeasureSpec.EXACTLY)
        val exactHeight = View.MeasureSpec.makeMeasureSpec(height, View.MeasureSpec.EXACTLY)
        overlay.measure(exactWidth, exactHeight)
        val left = sourceLocation[0] - ownerLocation[0]
        val top = sourceLocation[1] - ownerLocation[1]
        overlay.layout(left, top, left + width, top + height)
    }

    private fun finish(words: List<TextView>) {
        animator = null
        incoming.forEach { resetTransform(it.view) }
        incoming.clear()
        clearOutgoing()
        pill?.setProgress(1f)
        if (pillEndingInactive) pillView?.background = pillFallbackBackground
        clearPill()
        words.forEach(::resetTransform)
    }

    private fun clearOutgoing() {
        outgoing.toList().forEach(::removeOutgoing)
    }

    private fun removeOutgoing(slot: Outgoing) {
        owner.overlay.remove(slot.view)
        outgoing.remove(slot)
        resetTransform(slot.view)
        slot.view.text = ""
        if (overlayPool.size < MAX_OVERLAY_POOL) overlayPool.add(slot.view)
    }

    private fun clearPill() {
        pill = null
        pillView = null
        pillEndingInactive = false
        pillFallbackBackground = null
    }

    private fun pillWidth(word: TextView): Float =
        SuggestionConfidencePillGeometry.width(
            word.paint.measureText(word.text.toString()),
            word.width.toFloat(),
            word.resources.displayMetrics.density,
        )

    private fun viewDensity(view: View): Float = view.resources.displayMetrics.density

    private fun resetTransform(view: View) {
        view.alpha = 1f
        view.translationX = 0f
        view.translationY = 0f
        view.scaleX = 1f
        view.scaleY = 1f
        view.clipBounds = null
    }

    private companion object {
        const val MAX_OUTGOING_PER_SLOT = 2
        const val MAX_OVERLAY_POOL = 6
    }
}
