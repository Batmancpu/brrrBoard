// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import java.util.Locale
import kotlin.math.sqrt

/** Non-persistent measurements for the explicitly initiated BR Brain voice test. */
internal object VoiceLabScore {
    data class Signal(val levelPercent: Int, val clippingPercent: Double)

    fun signal(pcm16: ByteArray, bytes: Int = pcm16.size): Signal {
        require(bytes in 0..pcm16.size && bytes % 2 == 0)
        if (bytes == 0) return Signal(0, 0.0)
        var sum = 0.0
        var clipped = 0
        for (i in 0 until bytes step 2) {
            val sample = ((pcm16[i + 1].toInt() shl 8) or
                (pcm16[i].toInt() and 0xff)).toShort().toInt()
            sum += sample.toDouble() * sample
            if (sample >= 32_600 || sample <= -32_600) clipped++
        }
        val frames = bytes / 2
        return Signal(
            (sqrt(sum / frames) * 100.0 / 32_768.0).toInt().coerceIn(0, 100),
            clipped * 100.0 / frames,
        )
    }

    /** Percent word error rate; may exceed 100% with additional recognized words. */
    fun wordErrorPercent(reference: String, recognized: String): Int? {
        val expected = words(reference)
        if (expected.isEmpty()) return null
        val actual = words(recognized)
        var previous = IntArray(actual.size + 1) { it }
        var current = IntArray(actual.size + 1)
        for (i in expected.indices) {
            current[0] = i + 1
            for (j in actual.indices) {
                val substitution = if (expected[i] == actual[j]) 0 else 1
                current[j + 1] = minOf(
                    previous[j + 1] + 1,
                    current[j] + 1,
                    previous[j] + substitution,
                )
            }
            val swap = previous
            previous = current
            current = swap
        }
        return (100.0 * previous[actual.size] / expected.size).toInt()
    }

    private fun words(text: String): List<String> =
        text.lowercase(Locale.ROOT).split(Regex("[^\\p{L}\\p{N}]+"))
            .filter(String::isNotBlank)
}
