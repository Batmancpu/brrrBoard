// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivateVoiceChunkerTest {
    @Test fun twoMinutesKeepEveryOriginalSampleWithOnlyKnownOverlap() {
        val audio = ByteArray(PrivateVoiceChunker.BYTES_PER_SECOND * 125) { i ->
            // Incompressible repeating PCM16 bytes with no intentional silence.
            (i * 31 + i / 13).toByte()
        }
        val chunks = ArrayList<ByteArray>()
        val chunker = PrivateVoiceChunker()
        for (pos in audio.indices step 4096) {
            val count = minOf(4096, audio.size - pos)
            chunker.append(audio.copyOfRange(pos, pos + count), count, chunks::add)
        }
        chunker.finish(chunks::add)
        assertTrue(chunks.size >= 20)
        assertTrue(chunks.all { it.size <= PrivateVoiceChunker.MAX_BYTES })
        assertTrue(chunks.dropLast(1).all { it.size >= PrivateVoiceChunker.TARGET_BYTES })

        val merged = ByteArray(audio.size)
        var cursor = 0
        chunks.forEachIndexed { index, chunk ->
            val start = if (index == 0) 0 else PrivateVoiceChunker.OVERLAP_BYTES
            val count = chunk.size - start
            assertTrue(count >= 0)
            assertTrue(cursor + count <= merged.size)
            System.arraycopy(chunk, start, merged, cursor, count)
            cursor += count
        }
        assertEquals(audio.size, cursor)
        assertArrayEquals(audio, merged)
    }

    @Test fun shortAudioFlushesOnceAndCancellationEmitsNothing() {
        val short = ByteArray(PrivateVoiceChunker.BYTES_PER_SECOND) { 7 }
        val received = ArrayList<ByteArray>()
        val chunker = PrivateVoiceChunker()
        chunker.append(short, short.size, received::add)
        assertTrue(received.isEmpty())
        chunker.finish(received::add)
        assertEquals(1, received.size)
        assertArrayEquals(short, received.single())
        chunker.finish(received::add)
        assertEquals(1, received.size)

        val cancelled = PrivateVoiceChunker()
        cancelled.append(short, short.size, received::add)
        cancelled.discard()
        cancelled.finish(received::add)
        assertEquals(1, received.size)
    }
}
