// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.suggestions

/** Positions of the one word actually visible in a suggestion slot. */
internal data class SuggestionGlyphFrame(val word: String, val positions: FloatArray)

/** One transition between actual visible word states, never used for word insertion. */
internal data class SuggestionGlyphPlan(
    val word: String,
    val positions: FloatArray,
    val startOffsets: FloatArray,
    val startScales: FloatArray,
    val startAlphas: FloatArray,
    val outgoingWord: String,
    val outgoingPositions: FloatArray,
    val outgoingVisible: BooleanArray,
    val isRelated: Boolean,
)

/**
 * Bounded, linear-in-word-length presentation policy. This never influences prediction,
 * word text, accessibility, or tap handling.
 *
 * Preserve a common prefix, including a SINGLE typed letter. The remaining
 * letters shrink/fade/move to their new positions. Unrelated words get a whole
 * word replacement instead of misleading LCS matches between random letters.
 * The fixed tap target always contains the newest word.
 */
internal object SuggestionGlyphMotion {
    const val MAX_ANIMATED_CHARACTERS = 18
    // The existing iOS-profile motion budget; Apple does not publish its
    // private QuickType per-frame easing or timing as a public API.
    const val SETTLE_MS = 164L
    private const val NEW_LETTER_SCALE = .80f
    private const val NEW_LETTER_ALPHA = .58f
    private const val OUTGOING_ALPHA = .90f

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
        var commonPrefix = 0
        while (commonPrefix < minOf(oldSize, newSize)
            && original[commonPrefix].equals(next[commonPrefix], ignoreCase = true)) commonPrefix++
        var commonSuffix = 0
        while (commonSuffix < minOf(oldSize, newSize) - commonPrefix
            && original[oldSize - 1 - commonSuffix].equals(
                next[newSize - 1 - commonSuffix], ignoreCase = true)) commonSuffix++
        // A coincidental shared suffix alone isn't semantic continuity.
        val related = commonPrefix > 0
        val retainedOld = BooleanArray(oldSize)
        val oldIndexForNew = IntArray(newSize) { -1 }
        if (related) {
            for (i in 0 until commonPrefix) {
                oldIndexForNew[i] = i
                retainedOld[i] = true
            }
            for (i in 0 until commonSuffix) {
                val oldIndex = oldSize - 1 - i
                val newIndex = newSize - 1 - i
                oldIndexForNew[newIndex] = oldIndex
                retainedOld[oldIndex] = true
            }
        }

        val offsets = FloatArray(next.length)
        val scales = FloatArray(next.length) { NEW_LETTER_SCALE }
        val alphas = FloatArray(next.length) { NEW_LETTER_ALPHA }
        for (index in next.indices) {
            val oldIndex = oldIndexForNew[index]
            if (oldIndex >= 0) {
                offsets[index] = (previous.positions[oldIndex] - nextPositions[index])
                    .coerceIn(-maxShiftPx, maxShiftPx)
                scales[index] = 1f
                alphas[index] = 1f
            } else {
                offsets[index] = if (related) maxShiftPx * .7f else -maxShiftPx * .55f
            }
        }
        return SuggestionGlyphPlan(next, nextPositions.copyOf(), offsets, scales, alphas,
            original, previous.positions.copyOf(), BooleanArray(oldSize) { !retainedOld[it] }, related)
    }

    fun eased(progress: Float): Float {
        val remaining = 1f - progress.coerceIn(0f, 1f)
        return 1f - remaining * remaining * remaining
    }

    fun x(plan: SuggestionGlyphPlan, index: Int, progress: Float): Float =
        plan.positions[index] + plan.startOffsets[index] * (1f - progress)

    fun scale(plan: SuggestionGlyphPlan, index: Int, progress: Float): Float =
        plan.startScales[index] + (1f - plan.startScales[index]) * progress

    fun incomingAlpha(plan: SuggestionGlyphPlan, index: Int, progress: Float): Float =
        plan.startAlphas[index] + (1f - plan.startAlphas[index]) * progress

    fun outgoingAlpha(plan: SuggestionGlyphPlan, index: Int, progress: Float): Float {
        val remaining = 1f - progress.coerceIn(0f, 1f)
        return if (plan.outgoingVisible[index]) OUTGOING_ALPHA * remaining else 0f
    }

    fun outgoingOffset(plan: SuggestionGlyphPlan, index: Int, progress: Float,
                       maxShiftPx: Float): Float =
        if (!plan.outgoingVisible[index]) 0f
        else -maxShiftPx * .6f * progress.coerceIn(0f, 1f)
}
