// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

/** Joins overlapping ASR windows without repeating their matched boundary words. */
internal object VoiceTranscriptJoiner {
    fun merge(parts: List<String>): String {
        var result = ""
        for (part in parts) {
            val right = part.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            if (right.isEmpty()) continue
            if (result.isBlank()) {
                result = right.joinToString(" ")
                continue
            }
            val left = result.split(Regex("\\s+"))
            val max = minOf(6, left.size, right.size)
            val overlap = (max downTo 1).firstOrNull { count ->
                (0 until count).all { i ->
                    val leftWord = normalized(left[left.size - count + i])
                    leftWord.isNotEmpty() && leftWord == normalized(right[i])
                }
            } ?: 0
            result += if (overlap == right.size) "" else " " + right.drop(overlap).joinToString(" ")
        }
        return result.trim()
    }

    private fun normalized(token: String) = token.lowercase().trim { !it.isLetterOrDigit() }
}
