// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.suggestions

import android.graphics.drawable.Drawable
import android.util.TypedValue
import android.view.ViewGroup
import android.view.View
import android.widget.TextView
import helium314.keyboard.latin.KeyboardMotionSpec
import helium314.keyboard.latin.R
import helium314.keyboard.latin.utils.dpToPx

/**
 * Small compositor-only text morph. Actual suggestion views/tags/click targets
 * are updated synchronously; outgoing words are noninteractive, short-lived
 * render-only copies, never stored or logged outside this strip.
 */
internal class SuggestionWordMorph(private val owner: ViewGroup) {
    data class Previous(
        val index: Int, val parent: ViewGroup, val text: CharSequence,
        val left: Int, val top: Int, val width: Int, val height: Int,
        val color: Int, val textSize: Float, val textScale: Float,
        val gravity: Int, val typeface: android.graphics.Typeface?,
        val background: Drawable.ConstantState?, val alpha: Float,
    )

    private val ghosts = ArrayList<Pair<ViewGroup, TextView>>()
    private val ghostPool = ArrayList<TextView>(5)

    fun capture(words: List<TextView>): List<Previous> {
        cancel(words)
        return words.mapIndexedNotNull { index, word ->
            val parent = word.parent as? ViewGroup ?: return@mapIndexedNotNull null
            if (!parent.isShown || word.width <= 0 || word.height <= 0 ||
                word.text.isNullOrBlank()) return@mapIndexedNotNull null
            Previous(index, parent, word.text.toString(), word.left, word.top,
                word.width, word.height, word.currentTextColor, word.textSize,
                word.textScaleX, word.gravity, word.typeface,
                word.background?.constantState, word.alpha)
        }
    }

    fun morph(previous: List<Previous>, words: List<TextView>) {
        if (!KeyboardMotionSpec.enabled() || !owner.isShown) return
        for (old in previous) {
            val current = words.getOrNull(old.index) ?: continue
            val parent = current.parent as? ViewGroup ?: continue
            if (parent !== old.parent || !parent.isShown ||
                !KeyboardMotionSpec.shouldMorphWords(old.text.toString(), current.text.toString(),
                    true, true)) continue

            val ghost = (if (ghostPool.isEmpty())
                TextView(owner.context, null, R.attr.suggestionWordStyle)
            else ghostPool.removeAt(ghostPool.lastIndex)).apply {
                text = old.text
                setTextColor(old.color)
                setTextSize(TypedValue.COMPLEX_UNIT_PX, old.textSize)
                textScaleX = old.textScale
                gravity = old.gravity
                typeface = old.typeface
                includeFontPadding = false
                background = old.background?.newDrawable()?.mutate()
                isClickable = false
                importantForAccessibility = View.IMPORTANT_FOR_ACCESSIBILITY_NO
                alpha = old.alpha
                measure(View.MeasureSpec.makeMeasureSpec(old.width, View.MeasureSpec.EXACTLY),
                    View.MeasureSpec.makeMeasureSpec(old.height, View.MeasureSpec.EXACTLY))
                layout(old.left, old.top, old.left + old.width, old.top + old.height)
            }
            parent.overlay.add(ghost)
            ghosts.add(parent to ghost)

            current.animate().cancel()
            current.alpha = 0.18f
            current.translationY = 2.dpToPx(owner.resources).toFloat()
            current.animate().alpha(1f).translationY(0f)
                .setDuration(KeyboardMotionSpec.WORD_CROSSFADE_MS).start()
            ghost.animate().alpha(0f).translationY(-1.dpToPx(owner.resources).toFloat())
                .setDuration(KeyboardMotionSpec.WORD_CROSSFADE_MS)
                .withEndAction {
                    parent.overlay.remove(ghost)
                    ghosts.remove(parent to ghost)
                    recycle(ghost)
                }.start()
        }
    }

    fun cancel(words: List<TextView>) {
        val stale = ghosts.toList()
        ghosts.clear()
        stale.forEach { (parent, ghost) ->
            ghost.animate().cancel()
            parent.overlay.remove(ghost)
            recycle(ghost)
        }
        words.forEach {
            it.animate().cancel()
            it.alpha = 1f
            it.translationY = 0f
        }
    }

    private fun recycle(ghost: TextView) {
        // Do not retain typed suggestions after their transition has finished.
        ghost.text = ""
        ghost.background = null
        ghost.alpha = 1f
        ghost.translationY = 0f
        if (ghostPool.size < 5) ghostPool.add(ghost)
    }
}
