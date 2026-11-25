// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

/** A single full final transcript, with a fixed monotonic 60-second lifetime. */
internal class VoiceRecoveryWindow {
    private var item: RecoveredTranscript? = null
    private var nextId = 1L

    fun add(text: String, now: Long): RecoveredTranscript? {
        if (text.isBlank() || text.length > MAX_TEXT_CHARS) return null
        return RecoveredTranscript(nextId++, now, text).also { item = it }
    }

    fun all(now: Long): List<RecoveredTranscript> {
        val current = item ?: return emptyList()
        if (now - current.timestamp !in 0L until RETENTION_MS) {
            clear()
            return emptyList()
        }
        return listOf(current)
    }

    fun remove(id: Long): Boolean {
        if (item?.id != id) return false
        clear()
        return true
    }

    fun clear() { item = null }

    companion object {
        const val RETENTION_MS = 60_000L
        const val MAX_TEXT_CHARS = 262_144
    }
}
