// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.suggestions

import java.text.BreakIterator
import java.util.Locale

/** Logical-character range newly introduced by a suggestion replacement. */
internal object SuggestionGlyphDiff {
    data class Range(val start: Int, val endExclusive: Int)

    /**
     * Keep matching leading and trailing graphemes fully visible. Only the
     * changed portion of the *new* word needs to settle, never the old word.
     * Offsets are UTF-16 indices suitable for Android Spannable spans.
     */
    fun changedRange(old: CharSequence?, next: CharSequence?): Range? {
        val before = clusters(old?.toString().orEmpty())
        val after = clusters(next?.toString().orEmpty())
        if (after.isEmpty() || before == after) return null
        var prefix = 0
        while (prefix < before.size && prefix < after.size &&
            before[prefix] == after[prefix]) prefix++
        var suffix = 0
        while (suffix < before.size - prefix && suffix < after.size - prefix &&
            before[before.lastIndex - suffix] == after[after.lastIndex - suffix]) suffix++
        val start = after.take(prefix).sumOf { it.length }
        val end = after.take(after.size - suffix).sumOf { it.length }
        return if (start >= end) null else Range(start, end)
    }

    private fun clusters(value: String): List<String> {
        if (value.isEmpty()) return emptyList()
        val iterator = BreakIterator.getCharacterInstance(Locale.ROOT)
        iterator.setText(value)
        val result = ArrayList<String>(value.length)
        var from = iterator.first()
        while (true) {
            val to = iterator.next()
            if (to == BreakIterator.DONE) break
            result.add(value.substring(from, to))
            from = to
        }
        return result
    }
}
