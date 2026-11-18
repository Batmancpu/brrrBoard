// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.clipboard

/**
 * Bounded view-only window around the FIRST case-insensitive search hit.
 * Never changes the original clipboard entry used when the card is pasted.
 * The selected source indices are mapped into the visible snippet so long
 * copied passages do not hide their matching words behind an ellipsis.
 */
internal object ClipboardSearchPreview {
    internal data class Window(val text: String, val matchStart: Int, val matchEnd: Int) {
        val hasMatch: Boolean get() = matchStart >= 0 && matchEnd > matchStart
    }

    fun window(source: String, query: String, maxChars: Int = 136): Window {
        if (source.isEmpty() || maxChars <= 0) return Window("", -1, -1)
        val term = query.trim()
        val match = if (term.isEmpty()) -1 else source.indexOf(term, ignoreCase = true)
        if (match < 0) return slice(source, 0, maxChars, -1, -1)

        // Keep a short lead-in for meaning, then allocate most of the card to
        // text AFTER the match. Exact match text is always the first visible
        // portion, even when it occurs thousands of characters into a clip.
        val lead = (maxChars / 5).coerceAtMost(28)
        val start = (match - lead).coerceAtLeast(0)
        val end = (start + maxChars).coerceAtMost(source.length)
        return slice(source, start, end - start,
            match, (match + term.length).coerceAtMost(source.length))
    }

    private fun slice(source: String, start: Int, requestedLength: Int,
            matchStart: Int, matchEnd: Int): Window {
        var from = start.coerceIn(0, source.length)
        var to = (from + requestedLength).coerceAtMost(source.length)
        // Do not split a Unicode surrogate pair at the preview boundary.
        if (from in 1 until source.length && Character.isLowSurrogate(source[from])) from--
        if (to in 1 until source.length && Character.isHighSurrogate(source[to - 1])) to--
        if (to < from) to = from
        val preceding = if (from > 0) "…" else ""
        val trailing = if (to < source.length) "…" else ""
        val content = source.substring(from, to).replace('\r', ' ').replace('\n', ' ')
        val visible = preceding + content + trailing
        if (matchStart < from || matchStart >= to) return Window(visible, -1, -1)
        val begin = preceding.length + matchStart - from
        val end = preceding.length + (matchEnd.coerceAtMost(to) - from)
        return Window(visible, begin, end)
    }
}
