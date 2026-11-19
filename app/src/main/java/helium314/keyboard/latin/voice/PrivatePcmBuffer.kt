// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import java.io.ByteArrayOutputStream
import java.util.Arrays

/**
 * Microphone PCM is held only in RAM. No WAV, cache file, clipboard item or transcript history
 * is created by this buffer. The source owns its capture thread and copies frames immediately.
 *
 * The returned byte array also contains private audio: its caller MUST wipe it after decoding.
 */
internal class PrivatePcmBuffer(
    private val maxBytes: Int = SAMPLE_RATE * CHANNELS * BYTES_PER_SAMPLE * MAX_SECONDS,
) {
    private class WipingStream : ByteArrayOutputStream(8_192) {
        fun take(): ByteArray = buf.copyOf(count).also { reset() }

        override fun reset() {
            Arrays.fill(buf, 0.toByte())
            super.reset()
        }
    }

    private val stream = WipingStream()
    @Volatile private var closed = false

    @Synchronized
    fun append(source: ByteArray, offset: Int = 0, length: Int = source.size) {
        check(!closed) { "Voice buffer has been closed" }
        require(length >= 0 && length % BYTES_PER_SAMPLE == 0) { "PCM16 frames must be aligned" }
        require(offset >= 0 && offset <= source.size - length)
        check(stream.size() <= maxBytes - length) { "Private voice buffer reached its duration cap" }
        stream.write(source, offset, length)
    }

    @Synchronized
    fun drain(): ByteArray {
        check(!closed)
        closed = true
        return stream.take()
    }

    @Synchronized
    fun discard() {
        closed = true
        stream.reset()
    }

    @get:Synchronized
    val capturedMilliseconds: Long
        get() = stream.size().toLong() * 1_000 / (SAMPLE_RATE * CHANNELS * BYTES_PER_SAMPLE)

    companion object {
        const val SAMPLE_RATE = 16_000
        const val CHANNELS = 1
        const val BYTES_PER_SAMPLE = 2
        const val MAX_SECONDS = 170
    }
}
