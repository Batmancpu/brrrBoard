// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

/**
 * Cooperative cancellation for one private recording. A queued job can dispose of its
 * PCM immediately; an active native decode finishes its current window before wiping
 * the same bytes. Cancellation never wipes a buffer while the decoder is reading it.
 */
internal class VoiceDecodeRequest(private val pcm16: ByteArray) {
    private val lock = Any()
    private var started = false
    private var completed = false

    @Volatile var isCancelled = false
        private set

    fun begin(): Boolean = synchronized(lock) {
        if (isCancelled || completed) false else {
            started = true
            true
        }
    }

    fun cancel() = synchronized(lock) {
        isCancelled = true
        if (!started && !completed) {
            pcm16.fill(0)
            completed = true
        }
    }

    fun finish() = synchronized(lock) {
        pcm16.fill(0)
        started = false
        completed = true
    }
}
