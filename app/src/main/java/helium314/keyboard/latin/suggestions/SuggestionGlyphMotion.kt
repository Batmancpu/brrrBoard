// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.suggestions

import kotlin.math.abs

/** Positions of the one word actually visible in a suggestion slot. */
internal data class SuggestionGlyphFrame(val word: String, val positions: FloatArray)

/** The current suggestion is completely visible from the very first frame. */
internal data class SuggestionGlyphPlan(
    val word: String,
    val positions: FloatArray,
    val startOffsets: FloatArray,
    val startScales: FloatArray,
)

/**
 * Bounded, linear-time presentation policy. This never influences prediction,
 * word text, accessibility, or tap handling.
 *
 * Match stable prefix/suffix letters, move them a few pixels, and gently settle
 * inserted letters at full opacity. No LCS, old-word ghost, or delayed reveal.
 */
internal object SuggestionGlyphMotion {
    const val MAX_ANIMATED_CHARACTERS = 18
    const val SETTLE_MS = 112L
    private const val MIN_VISIBLE_MOVEMENT = .35f
    private const val NEW_LETTER_SCALE = .97f

    fun supported(word: String): Boolean = word.length in 1..MAX_ANIMATED_CHARACTERS &&
        word.all { it in 'a'..'z' || it in 'A'..'Z' || it == '\'' || it == '-' }

    fun plan(previous: SuggestionGlyphFrame?, next: String,
             nextPositions: FloatArray, maxShiftPx: Float): SuggestionGlyphPlan? {
        if (previous == null || previous.word == next ||
            !supported(previous.word) || !supported(next) ||
            previous.positions.size != previous.word.length ||
            nextPositions.size != next.length || maxShiftPx <= 0f || !maxShiftPx.isFinite() ||
            !previous.positions.all { it.isFinite() } || !nextPositions.all { it.isFinite() }) {
            return null
        }

        val original = previous.word
        var prefix = 0
        while (prefix < original.length && prefix < next.length &&
            original[prefix] == next[prefix]) prefix++

        var suffix = 0
        while (suffix < original.length - prefix && suffix < next.length - prefix &&
            original[original.lastIndex - suffix] == next[next.lastIndex - suffix]) suffix++

        // Unrelated candidates replace instantly, using native TextView ink.
        if (prefix + suffix < 2) return null

        val offsets = FloatArray(next.length)
        val scales = FloatArray(next.length) { NEW_LETTER_SCALE }
        var hasMotion = false

        fun retain(nextIndex: Int, oldIndex: Int) {
            offsets[nextIndex] = (previous.positions[oldIndex] - nextPositions[nextIndex])
                .coerceIn(-maxShiftPx, maxShiftPx)
            scales[nextIndex] = 1f
            if (abs(offsets[nextIndex]) > MIN_VISIBLE_MOVEMENT) hasMotion = true
        }

        for (index in 0 until prefix) retain(index, index)
        for (offset in 0 until suffix) {
            retain(next.lastIndex - offset, original.lastIndex - offset)
        }

        if (prefix + suffix < next.length) hasMotion = true
        return if (hasMotion) SuggestionGlyphPlan(next, nextPositions.copyOf(), offsets, scales)
            else null
    }

    fun eased(progress: Float): Float {
        val remaining = 1f - progress.coerceIn(0f, 1f)
        return 1f - remaining * remaining * remaining
    }

    fun x(plan: SuggestionGlyphPlan, index: Int, progress: Float): Float =
        plan.positions[index] + plan.startOffsets[index] * (1f - progress)

    fun scale(plan: SuggestionGlyphPlan, index: Int, progress: Float): Float =
        plan.startScales[index] + (1f - plan.startScales[index]) * progress
}
