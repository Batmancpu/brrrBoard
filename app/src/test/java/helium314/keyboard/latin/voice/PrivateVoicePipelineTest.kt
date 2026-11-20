// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivateVoicePipelineTest {
    @Test fun bufferReturnsOnlyRecordedPcmAndCannotBeReused() {
        val b = PrivatePcmBuffer(maxBytes = 8)
        b.append(byteArrayOf(1, 2, 3, 4))
        b.append(byteArrayOf(5, 6))
        assertArrayEquals(byteArrayOf(1, 2, 3, 4, 5, 6), b.drain())
        assertTrue(runCatching { b.append(byteArrayOf(7, 8)) }.isFailure)
    }

    @Test fun bufferBoundsLongSpeechAndWipesOnDiscard() {
        val b = PrivatePcmBuffer(maxBytes = 4)
        b.append(byteArrayOf(1, 2, 3, 4))
        assertTrue(runCatching { b.append(byteArrayOf(5, 6)) }.isFailure)
        b.discard()
        assertTrue(runCatching { b.drain() }.isFailure)
    }

    @Test fun silenceSplitsLongAudioButNeverLosesSampleCoverage() {
        val bytes = ByteArray(16_000 * 2 * 31)
        // Constant-amplitude speech except an 800-ms pause near 11 s and 22 s.
        for (i in bytes.indices step 2) {
            bytes[i] = 0x20
            bytes[i + 1] = 0x03
        }
        for (second in listOf(11, 22)) {
            bytes.fill(0, second * 16_000 * 2, (second * 16_000 + 12_800) * 2)
        }
        val windows = VoiceSegmenter.windows(bytes)
        assertTrue(windows.size >= 3)
        assertEquals(0, windows.first().startSample)
        assertEquals(bytes.size / 2, windows.last().endExclusive)
        assertTrue(windows.all { it.endExclusive - it.startSample <= 15 * 16_000 })
        for (i in 1 until windows.size) {
            assertTrue(windows[i].startSample <= windows[i - 1].endExclusive)
            assertTrue(windows[i].endExclusive > windows[i - 1].endExclusive)
        }
    }

    @Test fun shortUtteranceIsNotNeedlesslySegmented() {
        val p = ByteArray(16_000 * 2 * 7)
        assertEquals(listOf(VoiceSegmenter.Window(0, 7 * 16_000)), VoiceSegmenter.windows(p))
        assertFalse(VoiceSegmenter.windows(ByteArray(0)).isNotEmpty())
    }

    @Test fun repeatedBoundaryWordsAreMergedOnce() {
        assertEquals("Hello there, how are you today?", VoiceTranscriptJoiner.merge(
            listOf("Hello there, how are", "how are you today?")))
        assertEquals("One two three", VoiceTranscriptJoiner.merge(listOf("One two", "one two three")))
    }

    @Test fun serverAndOfflineNeverInsertTwiceEvenWithLateCallbacks() {
        val one = VoiceResultArbiter()
        assertTrue(one.acceptOffline())
        assertFalse(one.acceptServer())
        assertFalse(one.acceptOffline())
        assertEquals(VoiceResultArbiter.Winner.OFFLINE, one.winner)

        val two = VoiceResultArbiter()
        assertTrue(two.acceptServer())
        assertFalse(two.acceptOffline())
        assertEquals(VoiceResultArbiter.Winner.SERVER, two.winner)
    }

    @Test fun queuedDecodeCancellationImmediatelyWipesPrivatePcm() {
        val audio = byteArrayOf(1, 2, 3, 4)
        val request = VoiceDecodeRequest(audio)
        request.cancel()
        assertTrue(request.isCancelled)
        assertFalse(request.begin())
        assertArrayEquals(byteArrayOf(0, 0, 0, 0), audio)
    }

    @Test fun activeDecodeCancellationDefersWipeUntilNativeWindowFinishes() {
        val audio = byteArrayOf(1, 2, 3, 4)
        val request = VoiceDecodeRequest(audio)
        assertTrue(request.begin())
        request.cancel()
        assertTrue(request.isCancelled)
        // Never mutate the bytes concurrently with the native recognizer.
        assertArrayEquals(byteArrayOf(1, 2, 3, 4), audio)
        request.finish()
        assertArrayEquals(byteArrayOf(0, 0, 0, 0), audio)
    }

    @Test fun formattingOnlyChangesUnambiguousCasingWithoutRewritingSpeech() {
        assertEquals(
            "I updated brrBoard using GitHub and Obtainium on Android.",
            VoiceTranscriptFormatter.format(
                "I updated brrboard using github and obtainium on android.",
            ),
        )
        assertEquals(
            "The board meeting is about obtaining results, not github.com or a Kotlin-based project.",
            VoiceTranscriptFormatter.format(
                "The board meeting is about obtaining results, not github.com or a kotlin-based project.",
            ),
        )
        assertEquals("Android. GitHub! support@github.com",
            VoiceTranscriptFormatter.format("android. github! support@github.com"))
        assertEquals("UPSC NCERT ISRO ColorOS OPPO WhatsApp YouTube",
            VoiceTranscriptFormatter.format("upsc ncert isro coloros oppo whatsapp youtube"))
        assertEquals("upsc.gov.in ncert.nic.in support@oppo.com and tiffin",
            VoiceTranscriptFormatter.format("upsc.gov.in ncert.nic.in support@oppo.com and tiffin"))
    }

    @Test fun onlyOrphanedRedVoicePcmNamesCanBeRemoved() {
        assertTrue(PrivateVoiceCacheCleanup.isOrphanName(
            "brrboard-voice-123e4567-e89b-12d3-a456-426614174000.pcm"))
        assertFalse(PrivateVoiceCacheCleanup.isOrphanName("brrboard-voice-123.pcm"))
        assertFalse(PrivateVoiceCacheCleanup.isOrphanName("brrboard-voice-123e4567-e89b-12d3-a456-426614174000.txt"))
        assertFalse(PrivateVoiceCacheCleanup.isOrphanName("clipboard-image.png"))
        assertFalse(PrivateVoiceCacheCleanup.isOrphanName("../brrboard-voice-123e4567-e89b-12d3-a456-426614174000.pcm"))
    }
}
