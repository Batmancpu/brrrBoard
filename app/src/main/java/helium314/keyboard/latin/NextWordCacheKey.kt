// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

/** Immutable copy of transient n-gram context plus every lookup-affecting scope. */
internal data class NextWordCacheKey(
    val context: List<PreviousWord>,
    val localeTag: String,
    val keyboardIdHash: Int,
    val inputStyle: Int,
    val blockPotentiallyOffensive: Boolean,
    val spaceAwareGesture: Boolean,
    val editorGeneration: Long,
) {
    internal data class PreviousWord(val text: String?, val isBeginningOfSentence: Boolean)

    companion object {
        fun from(context: NgramContext, localeTag: String, keyboardIdHash: Int,
                 inputStyle: Int, blockPotentiallyOffensive: Boolean,
                 spaceAwareGesture: Boolean, editorGeneration: Long = -1L): NextWordCacheKey = NextWordCacheKey(
            (1..context.prevWordCount).map { index ->
                PreviousWord(context.getNthPrevWord(index)?.toString(),
                    context.isNthPrevWordBeginningOfSentence(index))
            },
            localeTag, keyboardIdHash, inputStyle,
            blockPotentiallyOffensive, spaceAwareGesture, editorGeneration,
        )
    }
}
