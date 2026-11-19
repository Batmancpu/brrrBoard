// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.suggestions

import android.view.ViewGroup
import android.view.animation.PathInterpolator
import android.widget.TextView
import helium314.keyboard.latin.KeyboardMotionSpec
import helium314.keyboard.latin.utils.dpToPx

/**
 * One visible glyph layer per suggestion. In particular, do not copy the old
 * TextView over the new one: both strings become legible at the same time and
 * create the doubled, shadow-like letter shapes reported on the phone.
 *
 * Words that survive a prediction update are visually carried to their next
 * slot; newly replaced words settle into their existing target hitbox.
 */
internal class SuggestionWordMorph(private val owner: ViewGroup) {
    data class Previous(
        val parent: ViewGroup, val text: String,
        val left: Int, val top: Int,
    )

    private val ease = PathInterpolator(.20f, 0f, .20f, 1f)
    private var generation = 0

    fun capture(words: List<TextView>): List<Previous> {
        cancel(words)
        return words.mapNotNull { word ->
            val parent = word.parent as? ViewGroup ?: return@mapNotNull null
            if (!parent.isShown || word.width <= 0 || word.text.isNullOrBlank())
                return@mapNotNull null
            Previous(parent, word.text.toString(), word.left, word.top)
        }
    }

    fun morph(previous: List<Previous>, words: List<TextView>) {
        if (!KeyboardMotionSpec.enabled() || !owner.isShown || previous.isEmpty()) return
        val token = ++generation
        // The strip recycles and re-lays out its word cells in updateKeys().
        // Wait for that layout before calculating a real old-to-new movement.
        owner.post {
            if (token != generation || !owner.isShown) return@post
            val carried = HashSet<Int>()
            words.forEach { current ->
                val parent = current.parent as? ViewGroup ?: return@forEach
                if (!parent.isShown || current.width <= 0 || current.text.isNullOrBlank())
                    return@forEach
                val match = previous.indices.firstOrNull { index ->
                    index !in carried && previous[index].parent === parent &&
                        previous[index].text == current.text.toString()
                }
                current.animate().cancel()
                if (match != null) {
                    carried.add(match)
                    val old = previous[match]
                    val dx = (old.left - current.left).toFloat()
                    val dy = (old.top - current.top).toFloat()
                    if (dx != 0f || dy != 0f) {
                        current.translationX = dx
                        current.translationY = dy
                        current.animate().translationX(0f).translationY(0f)
                            .setInterpolator(ease).setDuration(KeyboardMotionSpec.WORD_CROSSFADE_MS)
                            .start()
                    }
                } else {
                    // Only one text rendering exists. A slight settling motion
                    // avoids a visual cut without overprinting outgoing glyphs.
                    current.alpha = .88f
                    current.translationY = 1.dpToPx(owner.resources).toFloat()
                    current.animate().alpha(1f).translationY(0f)
                        .setInterpolator(ease).setDuration(KeyboardMotionSpec.WORD_CROSSFADE_MS)
                        .start()
                }
            }
        }
    }

    fun cancel(words: List<TextView>) {
        ++generation
        words.forEach {
            it.animate().cancel()
            it.alpha = 1f
            it.translationX = 0f
            it.translationY = 0f
        }
    }
}
