// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

/** Joins overlapping ASR windows without repeating their matched boundary words. */
internal object VoiceTranscriptJoiner {
    fun merge(parts: List<String>, discontinuities: Set<Int> = emptySet()): String {
        var result = ""
        var allowOverlap = true
        for ((index, part) in parts.withIndex()) {
            if (index in discontinuities) allowOverlap = false
            val right = part.trim().split(Regex("\\s+")).filter { it.isNotBlank() }
            if (right.isEmpty()) continue
            if (result.isBlank()) {
                result = right.joinToString(" ")
                allowOverlap = true
                continue
            }
            val left = result.split(Regex("\\s+"))
            val max = if (allowOverlap) minOf(6, left.size, right.size) else 0
            val overlap = (max downTo 1).firstOrNull { count ->
                (0 until count).all { i ->
                    val leftWord = normalized(left[left.size - count + i])
                    leftWord.isNotEmpty() && leftWord == normalized(right[i])
                }
            } ?: 0
            result += if (overlap == right.size) "" else " " + right.drop(overlap).joinToString(" ")
            allowOverlap = true
        }
        return result.trim()
    }

    private fun normalized(token: String) = token.lowercase().trim { !it.isLetterOrDigit() }
}
