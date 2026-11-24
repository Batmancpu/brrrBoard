// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import kotlin.math.abs

/**
 * Capture-worker-only, bounded 16 kHz PCM16 chunker. Semantic completion
 * takes priority over the old six-second cut, up to a latched safety budget.
 * At the ceiling choose a low-energy/neural-safe boundary and keep 320 ms
 * of real overlap. A shorter budget applies only to the next batch.
 * No speech is thrown away, no audio is logged and the input frame is never retained.
 */
internal class PrivateVoiceChunker(
    private val activity: ((ByteArray, Int, Int) -> Boolean?)? = null,
    private val thoughtComplete: ((ByteArray, Int) -> Boolean?)? = null,
    private val boundaryPenalty: ((Int, Int) -> Double)? = null,
    private val capSeconds: () -> Int = { LEGACY_MAX_SECONDS },
) {
    private val pcm = ByteArray(MAX_BYTES)
    private var used = 0
    private var emitted = false
    private var closed = false
    private var totalBytes = 0L
    private var scanBytes = 0
    private var quietBytes = 0
    private var speechSeen = false
    private var nextTurnCheck = END_SILENCE_BYTES
    private var limitBytes = 0

    private fun budgetBytes(): Int = capSeconds().coerceIn(LEGACY_MAX_SECONDS, MAX_SECONDS) * BYTES_PER_SECOND

    @Synchronized
    fun append(source: ByteArray, count: Int, emit: (ByteArray) -> Unit) {
        if (closed) return
        require(count >= 0 && count <= source.size && count % 2 == 0)
        check(totalBytes + count <= MAX_CAPTURE_BYTES) { "Voice duration limit reached" }
        totalBytes += count
        if (limitBytes == 0) limitBytes = budgetBytes()
        var position = 0
        while (position < count) {
            // Consume at most the next analysis window. An early endpoint can
            // then lower the next budget even if a caller supplies a huge frame.
            val bytes = minOf(count - position, limitBytes - used,
                ENERGY_WINDOW_BYTES - (used - scanBytes))
            check(bytes > 0)
            System.arraycopy(source, position, pcm, used, bytes)
            used += bytes
            position += bytes
            if (used - scanBytes >= ENERGY_WINDOW_BYTES) {
                val windowEnd = scanBytes + ENERGY_WINDOW_BYTES
                val speech = activity?.invoke(pcm, scanBytes, windowEnd)
                    ?: (averageAbsAmplitude(scanBytes, windowEnd) >= SPEECH_ENERGY_THRESHOLD)
                scanBytes = windowEnd
                if (speech) {
                    speechSeen = true
                    quietBytes = 0
                    nextTurnCheck = END_SILENCE_BYTES
                } else if (speechSeen) {
                    quietBytes += ENERGY_WINDOW_BYTES
                    val semanticBudget = limitBytes > LEGACY_MAX_BYTES
                    val minimum = if (semanticBudget) MIN_SEMANTIC_CHUNK_BYTES else MIN_EARLY_CHUNK_BYTES
                    val maxPause = if (semanticBudget) SEMANTIC_MAX_PAUSE_BYTES else MAX_PAUSE_BYTES
                    if (scanBytes >= minimum && quietBytes >= nextTurnCheck) {
                        // A hesitation is not a complete thought. The long-mode
                        // pause timeout and safety ceiling are independent.
                        val completed = if (quietBytes < maxPause)
                            thoughtComplete?.invoke(pcm, scanBytes) else true
                        if (completed != false) {
                            flushWindow(scanBytes, emit)
                            quietBytes = 0
                            speechSeen = false
                            nextTurnCheck = END_SILENCE_BYTES
                        } else {
                            nextTurnCheck = quietBytes + TURN_RECHECK_BYTES
                        }
                    }
                }
            }
            if (used == limitBytes) {
                flushWindow(emit)
                quietBytes = 0
                nextTurnCheck = END_SILENCE_BYTES
            }
        }
    }

    @Synchronized
    fun finish(emit: (ByteArray) -> Unit) {
        if (closed) return
        closed = true
        // A final all-overlap suffix would reproduce words already sent.
        if (used > 0 && (!emitted || used > OVERLAP_BYTES)) emit(pcm.copyOf(used))
        pcm.fill(0)
        used = 0
    }

    @Synchronized
    fun discard() {
        closed = true
        pcm.fill(0)
        used = 0
    }

    private fun flushWindow(emit: (ByteArray) -> Unit) = flushWindow(quietBoundary(), emit)

    private fun flushWindow(boundary: Int, emit: (ByteArray) -> Unit) {
        val chunk = pcm.copyOf(boundary)
        // Move any post-boundary audio and the previous 320 ms to the next
        // window. The downstream transcript joiner removes duplicated words.
        val from = boundary - OVERLAP_BYTES
        val remaining = used - from
        val scanned = scanBytes - from
        System.arraycopy(pcm, from, pcm, 0, remaining)
        pcm.fill(0, remaining, used)
        used = remaining
        // Neither rescore old overlap nor skip unscanned post-boundary samples.
        scanBytes = scanned.coerceIn(OVERLAP_BYTES, used)
        emitted = true
        emit(chunk)
        limitBytes = budgetBytes()
    }

    private fun averageAbsAmplitude(from: Int, to: Int): Double {
        var energy = 0L
        var count = 0
        for (index in from.coerceAtLeast(0) until to.coerceAtMost(used) step 2) {
            val sample = ((pcm[index + 1].toInt() shl 8) or
                (pcm[index].toInt() and 255)).toShort().toInt()
            energy += abs(sample).toLong()
            count++
        }
        return if (count == 0) 0.0 else energy.toDouble() / count
    }

    private fun quietBoundary(): Int {
        val earliest = limitBytes - (if (limitBytes > 8 * BYTES_PER_SECOND) 2 else 1) * BYTES_PER_SECOND
        val preferred = if (limitBytes == LEGACY_MAX_BYTES) TARGET_BYTES else limitBytes
        var best = preferred
        var bestScore = Double.POSITIVE_INFINITY
        var at = earliest
        while (at <= limitBytes) {
            val from = (at - ENERGY_WINDOW_BYTES / 2).coerceAtLeast(0)
            val to = (at + ENERGY_WINDOW_BYTES / 2).coerceAtMost(used)
            var energy = 0L
            for (index in from until to step 2) {
                val sample = ((pcm[index + 1].toInt() shl 8) or
                    (pcm[index].toInt() and 255)).toShort().toInt()
                energy += abs(sample).toLong()
            }
            val average = energy.toDouble() / ((to - from) / 2).coerceAtLeast(1)
            val score = average + abs(at - preferred).toDouble() / BYTES_PER_SECOND * 8.0 +
                (boundaryPenalty?.invoke(from, to) ?: 0.0)
            if (score < bestScore) {
                best = at
                bestScore = score
            }
            at += ENERGY_WINDOW_BYTES
        }
        return best
    }

    companion object {
        const val SAMPLE_RATE = 16_000
        const val BYTES_PER_SECOND = SAMPLE_RATE * 2
        const val TARGET_SECONDS = 5
        const val LEGACY_MAX_SECONDS = 6
        const val MAX_SECONDS = 12
        const val OVERLAP_MS = 320
        private const val END_SILENCE_MS = 600
        private const val MIN_EARLY_CHUNK_SECONDS = 3
        private const val SPEECH_ENERGY_THRESHOLD = 180.0
        private const val END_SILENCE_BYTES = BYTES_PER_SECOND * END_SILENCE_MS / 1000
        private const val TURN_RECHECK_BYTES = BYTES_PER_SECOND * 400 / 1000
        private const val MAX_PAUSE_BYTES = BYTES_PER_SECOND * 1400 / 1000
        private const val SEMANTIC_MAX_PAUSE_BYTES = BYTES_PER_SECOND * 2200 / 1000
        private const val MIN_SEMANTIC_CHUNK_BYTES = BYTES_PER_SECOND * 1200 / 1000
        private const val MIN_EARLY_CHUNK_BYTES = BYTES_PER_SECOND * MIN_EARLY_CHUNK_SECONDS
        const val TARGET_BYTES = BYTES_PER_SECOND * TARGET_SECONDS
        const val LEGACY_MAX_BYTES = BYTES_PER_SECOND * LEGACY_MAX_SECONDS
        const val MAX_BYTES = BYTES_PER_SECOND * MAX_SECONDS
        const val OVERLAP_BYTES = BYTES_PER_SECOND * OVERLAP_MS / 1000
        val MAX_CAPTURE_BYTES: Long = BYTES_PER_SECOND.toLong() * PrivatePcmBuffer.MAX_SECONDS
        private const val ENERGY_WINDOW_BYTES = BYTES_PER_SECOND / 10
    }
}
