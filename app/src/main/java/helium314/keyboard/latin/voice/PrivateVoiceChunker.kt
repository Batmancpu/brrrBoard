// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import kotlin.math.abs

/**
 * Recorder-thread-only, bounded 16 kHz PCM16 chunker. Closes a batch after a
 * sustained quiet tail or at the quietest 100 ms boundary in the 5–6 second
 * safety window, and keeps 320 ms of real overlap.
 * No speech is thrown away, no audio is logged and the input frame is never retained.
 */
internal class PrivateVoiceChunker {
    private val pcm = ByteArray(MAX_BYTES)
    private var used = 0
    private var emitted = false
    private var closed = false
    private var totalBytes = 0L
    private var scanBytes = 0
    private var quietBytes = 0
    private var speechSeen = false

    @Synchronized
    fun append(source: ByteArray, count: Int, emit: (ByteArray) -> Unit) {
        if (closed) return
        require(count >= 0 && count <= source.size && count % 2 == 0)
        check(totalBytes + count <= MAX_CAPTURE_BYTES) { "Voice duration limit reached" }
        totalBytes += count
        var position = 0
        while (position < count) {
            val bytes = minOf(count - position, MAX_BYTES - used)
            System.arraycopy(source, position, pcm, used, bytes)
            used += bytes
            position += bytes
            if (used == MAX_BYTES) {
                flushWindow(emit)
            } else if (used - scanBytes >= ENERGY_WINDOW_BYTES) {
                scanBytes = used
                val energy = averageAbsAmplitude(used - ENERGY_WINDOW_BYTES, used)
                if (energy >= SPEECH_ENERGY_THRESHOLD) {
                    speechSeen = true
                    quietBytes = 0
                } else if (speechSeen) {
                    quietBytes += ENERGY_WINDOW_BYTES
                    if (used >= MIN_EARLY_CHUNK_BYTES && quietBytes >= END_SILENCE_BYTES) {
                        // A pause ends the utterance sooner than the hard 6 s cap.
                        flushWindow(used)
                        quietBytes = 0
                        speechSeen = false
                    }
                }
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
        System.arraycopy(pcm, from, pcm, 0, remaining)
        pcm.fill(0, remaining, used)
        used = remaining
        scanBytes = used
        emitted = true
        emit(chunk)
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
        var best = TARGET_BYTES
        var bestScore = Double.POSITIVE_INFINITY
        var at = TARGET_BYTES
        while (at <= MAX_BYTES) {
            val from = (at - ENERGY_WINDOW_BYTES / 2).coerceAtLeast(0)
            val to = (at + ENERGY_WINDOW_BYTES / 2).coerceAtMost(used)
            var energy = 0L
            for (index in from until to step 2) {
                val sample = ((pcm[index + 1].toInt() shl 8) or
                    (pcm[index].toInt() and 255)).toShort().toInt()
                energy += abs(sample).toLong()
            }
            val average = energy.toDouble() / ((to - from) / 2).coerceAtLeast(1)
            val score = average + (at - TARGET_BYTES).toDouble() / BYTES_PER_SECOND * 8.0
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
        const val MAX_SECONDS = 6
        const val OVERLAP_MS = 320
        private const val END_SILENCE_MS = 600
        private const val MIN_EARLY_CHUNK_SECONDS = 3
        private const val SPEECH_ENERGY_THRESHOLD = 180.0
        private const val END_SILENCE_BYTES = BYTES_PER_SECOND * END_SILENCE_MS / 1000
        private const val MIN_EARLY_CHUNK_BYTES = BYTES_PER_SECOND * MIN_EARLY_CHUNK_SECONDS
        const val TARGET_BYTES = BYTES_PER_SECOND * TARGET_SECONDS
        const val MAX_BYTES = BYTES_PER_SECOND * MAX_SECONDS
        const val OVERLAP_BYTES = BYTES_PER_SECOND * OVERLAP_MS / 1000
        val MAX_CAPTURE_BYTES: Long = BYTES_PER_SECOND.toLong() * PrivatePcmBuffer.MAX_SECONDS
        private const val ENERGY_WINDOW_BYTES = BYTES_PER_SECOND / 10
    }
}
