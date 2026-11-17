// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.clipboard

import helium314.keyboard.latin.ClipboardHistoryEntry
import helium314.keyboard.latin.ClipboardHistoryManager

/** Offline search, without caching clipboard text or leaking queries to host apps. */
internal object ClipboardSearchPolicy {
    fun matchingIds(query: String, entries: List<ClipboardHistoryEntry>): List<Long> {
        val q = query.trim()
        if (q.isEmpty()) return emptyList()
        return entries.asSequence()
            .filter { it.text.contains(q, ignoreCase = true) }
            .filter { ClipboardHistoryManager.decodeImageHistoryClip(it.text) == null }
            .sortedWith(compareByDescending<ClipboardHistoryEntry> { it.timeStamp }
                .thenByDescending { it.id })
            .map { it.id }
            .toList()
    }
}
