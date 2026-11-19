// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import kotlin.math.abs

/**
 * Finds low-energy pauses and returns bounded PCM16 sample windows. Energy is used only to
 * pick a safe cut, not to decide which words to keep: every sample belongs to at least one
 * window. No model loading, allocations proportional to duration, or work on the UI thread.
 */
internal object VoiceSegmenter {
    private const val TARGET_SECONDS = 11
    private const val MAX_SECONDS = 15
    private const val MIN_SECONDS = 6
    private const val OVERLAP_MS = 250
    private const val STEP_MS = 100

    data class Window(val startSample: Int, val endExclusive: Int) {
        init { require(startSample >= 0 && endExclusive > startSample) }
    }

    fun windows(pcm16: ByteArray, sampleRate: Int = 16_000): List<Window> {
        require(sampleRate > 0 && pcm16.size % 2 == 0)
        val samples = pcm16.size / 2
        if (samples == 0) return emptyList()
        val max = sampleRate * MAX_SECONDS
        if (samples <= max) return listOf(Window(0, samples))

        val result = ArrayList<Window>()
        val overlap = sampleRate * OVERLAP_MS / 1_000
        val step = (sampleRate * STEP_MS / 1_000).coerceAtLeast(1)
        var start = 0
        while (start < samples) {
            val remaining = samples - start
            if (remaining <= max) {
                result += Window(start, samples)
                break
            }
            val earliest = start + sampleRate * MIN_SECONDS
            val latest = (start + max).coerceAtMost(samples)
            val target = start + sampleRate * TARGET_SECONDS
            var chosen = target
            var best = Double.POSITIVE_INFINITY
            var boundary = earliest
            while (boundary <= latest) {
                val energy = averageAbsAmplitude(pcm16, boundary, step)
                // A little preference for a cut near 11 s, never at the price of loud speech.
                val score = energy + (abs(boundary - target).toDouble() / sampleRate) * 12.0
                if (score < best) {
                    best = score
                    chosen = boundary
                }
                boundary += step
            }
            val end = chosen.coerceIn(earliest, latest)
            result += Window(start, end)
            start = (end - overlap).coerceAtLeast(start + 1)
        }
        return result
    }

    private fun averageAbsAmplitude(bytes: ByteArray, boundary: Int, step: Int): Double {
        val totalSamples = bytes.size / 2
        val from = (boundary - step / 2).coerceAtLeast(0)
        val limit = (boundary + step / 2).coerceAtMost(totalSamples)
        if (from == limit) return 0.0
        var sum = 0L
        for (index in from until limit) {
            val lo = bytes[index * 2].toInt() and 0xff
            val hi = bytes[index * 2 + 1].toInt()
            val sample = (hi shl 8) or lo
            sum += abs(sample).toLong()
        }
        return sum.toDouble() / (limit - from)
    }
}
