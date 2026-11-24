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

    @Test fun sustainedPauseEndsSpeechBatchBeforeHardLimit() {
        val speech = ByteArray(PrivateVoiceChunker.BYTES_PER_SECOND * 4) { i ->
            if (i % 2 == 0) 0x34 else 0x12
        }
        val pause = ByteArray(PrivateVoiceChunker.BYTES_PER_SECOND)
        val chunks = ArrayList<ByteArray>()
        val chunker = PrivateVoiceChunker()
        chunker.append(speech, speech.size, chunks::add)
        chunker.append(pause, pause.size, chunks::add)
        assertTrue("A 600 ms quiet tail should close a 4 s speech batch", chunks.isNotEmpty())
        assertTrue(chunks.first().size < PrivateVoiceChunker.MAX_BYTES)
        chunker.discard()
        chunks.forEach { it.fill(0) }
    }
    @Test fun continuousTwoMinuteRecordingEmitsRepeatedBatchesBeforeStop() {
        val chunker = PrivateVoiceChunker()
        val second = ByteArray(PrivateVoiceChunker.BYTES_PER_SECOND)
        for (index in second.indices step 2) {
            second[index] = 0x20
            second[index + 1] = 0x03
        }
        val sizes = mutableListOf<Int>()
        repeat(120) { chunker.append(second, second.size) { sizes += it.size } }
        org.junit.Assert.assertTrue(sizes.size >= 20)
        val beforeStop = sizes.size
        chunker.finish { sizes += it.size }
        org.junit.Assert.assertTrue(sizes.size >= beforeStop)
        org.junit.Assert.assertEquals(120L * second.size,
            sizes.sumOf { it.toLong() } - (sizes.size - 1L) * PrivateVoiceChunker.OVERLAP_BYTES)
    }


    @Test fun incompleteThoughtWaitsButNeverStopsCaptureOrLosesSamples() {
        val second = PrivateVoiceChunker.BYTES_PER_SECOND
        val pcm = ByteArray(second * 9) { i -> if (i % 2 == 0) 0x20 else 0x03 }
        pcm.fill(0, second * 3, second * 5)
        val chunks = mutableListOf<ByteArray>()
        var queries = 0
        val chunker = PrivateVoiceChunker(
            activity = { bytes, from, to -> (from until to).any { bytes[it] != 0.toByte() } },
            thoughtComplete = { _, _ -> ++queries >= 2 }
        )
        val first = second * 38 / 10
        chunker.append(pcm, first, chunks::add)
        assertTrue(queries >= 1)
        assertTrue(chunks.isEmpty())
        chunker.append(pcm.copyOfRange(first, pcm.size), pcm.size - first, chunks::add)
        chunker.finish(chunks::add)
        assertTrue(queries >= 2)
        assertTrue(chunks.size >= 2)
        assertTrue(chunks.all { it.size <= PrivateVoiceChunker.MAX_BYTES })
        val reconstructed = chunks.flatMapIndexed { i, b -> b.drop(if (i == 0) 0 else PrivateVoiceChunker.OVERLAP_BYTES) }.toByteArray()
        assertArrayEquals(pcm, reconstructed)
    }

    @Test fun missingNeuralModelsUseOriginalPauseGateAndPreservePcm() {
        val second = PrivateVoiceChunker.BYTES_PER_SECOND
        val pcm = ByteArray(second * 5) { i -> if (i < second * 4) 0x22 else 0 }
        val chunks = mutableListOf<ByteArray>()
        val chunker = PrivateVoiceChunker(activity = { _, _, _ -> null }, thoughtComplete = { _, _ -> null })
        chunker.append(pcm, pcm.size, chunks::add)
        assertTrue(chunks.isNotEmpty())
        assertTrue(chunks.first().size < PrivateVoiceChunker.MAX_BYTES)
        chunker.finish(chunks::add)
        assertArrayEquals(pcm, chunks.flatMapIndexed { i, b -> b.drop(if (i == 0) 0 else PrivateVoiceChunker.OVERLAP_BYTES) }.toByteArray())
    }
}
