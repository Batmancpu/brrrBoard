// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import org.junit.Assert.*
import org.junit.Test

class VoiceFramePoolTest {
    @Test fun captureCopiesAreIndependentAndRecycledFramesAreWiped() {
        val pool = VoiceFramePool()
        val source = byteArrayOf(1, 2, 3, 4)
        val a = pool.copyFrom(source, 4)
        val b = pool.copyFrom(source, 4)
        source.fill(9)
        assertArrayEquals(byteArrayOf(1, 2, 3, 4), a)
        assertNotSame(a, b)
        pool.recycle(a)
        assertArrayEquals(ByteArray(4), a)
        val reused = pool.copyFrom(source, 4)
        assertSame(a, reused)
        assertArrayEquals(source, reused)
        assertArrayEquals(byteArrayOf(1, 2, 3, 4), b)
        pool.recycle(reused); pool.recycle(b); pool.close()
    }
    @Test fun capacityAndClosureDoNotDropOrRetainAudio() {
        val pool = VoiceFramePool(2, 8)
        val source = ByteArray(12) { 7 }
        val frames = List(5) { pool.copyFrom(source, 8) }
        frames.forEach { pool.recycle(it); assertArrayEquals(ByteArray(8), it) }
        assertEquals(2, pool.retainedCount())
        val large = pool.copyFrom(source, 12)
        pool.recycle(large)
        assertArrayEquals(ByteArray(12), large)
        val inFlight = pool.copyFrom(source, 8)
        pool.close()
        assertEquals(0, pool.retainedCount())
        assertArrayEquals(ByteArray(8) { 7 }, inFlight)
        pool.recycle(inFlight)
        assertArrayEquals(ByteArray(8), inFlight)
        assertEquals(0, pool.retainedCount())
    }
}
