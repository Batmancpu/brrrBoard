// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

/** Session-local, exact-size scratch frames. Never retains nonzero PCM or limits queued speech. */
internal class VoiceFramePool(
    private val capacity: Int = 4,
    private val maxFrameBytes: Int = 16_384,
) {
    private val idle = ArrayList<ByteArray>(capacity)
    private var closed = false

    fun copyFrom(source: ByteArray, count: Int): ByteArray {
        require(count in 0..source.size)
        val frame = synchronized(this) {
            val index = if (closed) -1 else idle.indexOfFirst { it.size == count }
            if (index < 0) ByteArray(count) else idle.removeAt(index)
        }
        source.copyInto(frame, 0, 0, count)
        return frame
    }

    fun recycle(frame: ByteArray) {
        frame.fill(0)
        synchronized(this) {
            if (!closed && frame.size <= maxFrameBytes && idle.size < capacity) idle.add(frame)
        }
    }

    @Synchronized fun close() {
        closed = true
        idle.forEach { it.fill(0) }
        idle.clear()
    }
    @Synchronized internal fun retainedCount(): Int = idle.size
}
