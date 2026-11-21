// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.suggestions

/** Positions of the one word actually visible in a suggestion slot. */
internal data class SuggestionGlyphFrame(val word: String, val positions: FloatArray)

/** The new suggestion is fully readable on frame zero; obsolete glyphs are decoration. */
internal data class SuggestionGlyphPlan(
    val word: String,
    val positions: FloatArray,
    val startOffsets: FloatArray,
    val startScales: FloatArray,
    val outgoingWord: String,
    val outgoingPositions: FloatArray,
    val outgoingVisible: BooleanArray,
)

/**
 * Bounded, quadratic-in-word-length presentation policy. This never influences prediction,
 * word text, accessibility, or tap handling.
 *
 * Match any retained letters, even one, while EVERY changed word gets a
 * bounded transition. Unmatched old ink fades away as new ink remains readable
 * from frame zero. The fixed tap target always contains the newest word.
 */
internal object SuggestionGlyphMotion {
    const val MAX_ANIMATED_CHARACTERS = 18
    // The existing iOS-profile motion budget; Apple does not publish its
    // private QuickType per-frame easing or timing as a public API.
    const val SETTLE_MS = 164L
    private const val NEW_LETTER_SCALE = .88f
    private const val OUTGOING_ALPHA = .38f

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
        val oldSize = original.length
        val newSize = next.length
        // At most 19x19 integers, allocated on replacement, never per frame.
        val lcs = Array(oldSize + 1) { IntArray(newSize + 1) }
        for (i in oldSize - 1 downTo 0) for (j in newSize - 1 downTo 0) {
            lcs[i][j] = if (original[i] == next[j]) 1 + lcs[i + 1][j + 1]
                else maxOf(lcs[i + 1][j], lcs[i][j + 1])
        }
        val retainedOld = BooleanArray(oldSize)
        val oldIndexForNew = IntArray(newSize) { -1 }
        var i = 0
        var j = 0
        while (i < oldSize && j < newSize) {
            if (original[i] == next[j]) {
                oldIndexForNew[j] = i
                retainedOld[i] = true
                i++
                j++
            } else if (lcs[i + 1][j] >= lcs[i][j + 1]) i++ else j++
        }

        val offsets = FloatArray(next.length)
        val scales = FloatArray(next.length) { NEW_LETTER_SCALE }
        for (index in next.indices) {
            val oldIndex = oldIndexForNew[index]
            if (oldIndex >= 0) {
                offsets[index] = (previous.positions[oldIndex] - nextPositions[index])
                    .coerceIn(-maxShiftPx, maxShiftPx)
                scales[index] = 1f
            } else {
                // Every new letter has motion, including an unrelated word
                // replacing an unrelated word and the very first typed letter.
                val fromSide = if (index <= (next.length - 1) / 2) -1f else 1f
                offsets[index] = fromSide * (maxShiftPx * .55f)
            }
        }
        return SuggestionGlyphPlan(next, nextPositions.copyOf(), offsets, scales,
            original, previous.positions.copyOf(), BooleanArray(oldSize) { !retainedOld[it] })
    }

    fun eased(progress: Float): Float {
        val remaining = 1f - progress.coerceIn(0f, 1f)
        return 1f - remaining * remaining * remaining
    }

    fun x(plan: SuggestionGlyphPlan, index: Int, progress: Float): Float =
        plan.positions[index] + plan.startOffsets[index] * (1f - progress)

    fun scale(plan: SuggestionGlyphPlan, index: Int, progress: Float): Float =
        plan.startScales[index] + (1f - plan.startScales[index]) * progress

    fun outgoingAlpha(plan: SuggestionGlyphPlan, index: Int, progress: Float): Float {
        val remaining = 1f - progress.coerceIn(0f, 1f)
        return if (plan.outgoingVisible[index]) OUTGOING_ALPHA * remaining * remaining else 0f
    }
}
