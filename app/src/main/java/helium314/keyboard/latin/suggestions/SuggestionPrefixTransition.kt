// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.suggestions

/**
 * Identifies the stable, already-familiar prefix of two Latin QuickType labels.
 * Motion is presentation-only: callers must publish the new word and tap target first.
 */
internal object SuggestionPrefixTransition {
    /** UTF-16 end offset of the shared prefix, or zero when native shaping should be used. */
    fun stablePrefixEnd(previous: String?, next: String?): Int {
        if (previous.isNullOrBlank() || next.isNullOrBlank() || previous == next ||
            !isSimpleLatin(previous) || !isSimpleLatin(next)) return 0
        var index = 0
        val limit = minOf(previous.length, next.length)
        while (index < limit && previous[index].lowercaseChar() == next[index].lowercaseChar()) {
            index++
        }
        // A one-letter anchor is meaningful for QuickType (tree -> they); an isolated
        // coincidental suffix is never treated as continuity.
        return index
    }

    private fun isSimpleLatin(value: String): Boolean = value.length <= 32 &&
        value.all { it in 'a'..'z' || it in 'A'..'Z' || it == '\'' || it == '-' }
}
