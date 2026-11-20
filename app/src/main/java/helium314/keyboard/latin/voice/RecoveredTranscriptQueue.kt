// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

/** Only successful recognition whose original editor destination became unsafe. */
data class RecoveredTranscript(val id: Long, val timestamp: Long, val text: String)

/** Bounded oldest-eviction policy; no ordinary successful voice text is added. */
internal class RecoveredTranscriptQueue(
    private val maxEntries: Int = 5,
    private val retentionMs: Long = 24L * 60L * 60L * 1000L,
) {
    private val entries = mutableListOf<RecoveredTranscript>()
    private var nextId = 1L

    fun add(text: String, now: Long): RecoveredTranscript? {
        if (text.isBlank() || text.length > MAX_TEXT_CHARS) return null
        prune(now)
        val item = RecoveredTranscript(nextId++, now, text)
        entries.add(0, item)
        while (entries.size > maxEntries) entries.removeAt(entries.lastIndex)
        return item
    }

    fun restore(items: List<RecoveredTranscript>, now: Long) {
        entries.clear()
        entries.addAll(items.filter { it.text.isNotBlank() && it.text.length <= MAX_TEXT_CHARS }
            .sortedWith(compareByDescending<RecoveredTranscript> { it.timestamp }.thenByDescending { it.id })
            .take(maxEntries))
        nextId = (entries.maxOfOrNull { it.id } ?: 0L) + 1L
        prune(now)
    }

    fun all(now: Long): List<RecoveredTranscript> {
        prune(now)
        return entries.toList()
    }

    fun get(id: Long, now: Long): RecoveredTranscript? = all(now).firstOrNull { it.id == id }

    fun remove(id: Long): Boolean = entries.removeAll { it.id == id }

    fun clear() = entries.clear()

    private fun prune(now: Long) {
        entries.removeAll { it.timestamp > now || now - it.timestamp > retentionMs }
    }

    companion object { const val MAX_TEXT_CHARS = 12_000 }
}
