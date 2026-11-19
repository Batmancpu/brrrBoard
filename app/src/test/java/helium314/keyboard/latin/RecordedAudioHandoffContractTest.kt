// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.media.AudioFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RecordedAudioHandoffContractTest {
    @Test fun newReceiverVersionRequiredOldLiveStreamProbeIsNotEnough() {
        assertFalse(RecordedAudioWire.supports(null))
        assertFalse(RecordedAudioWire.supports(142_005L))
        assertFalse(RecordedAudioWire.supports(142_007L))
        assertFalse(RecordedAudioWire.supports(142_008L))
        assertTrue(RecordedAudioWire.supports(142_009L))
        assertTrue(RecordedAudioWire.supports(143_000L))
    }

    @Test fun exactRawPcm16FormatAndBoundedDuration() {
        assertEquals(16_000, RecordedPcmContract.RATE)
        assertEquals(1, RecordedPcmContract.CHANNELS)
        assertEquals(AudioFormat.ENCODING_PCM_16BIT, RecordedPcmContract.ENCODING)
        assertEquals(120_000L, RecordedPcmContract.MAX_MS)
        assertEquals(3_840_000L, RecordedPcmContract.MAX_BYTES)
        assertFalse(RecordedPcmContract.validLength(0))
        assertFalse(RecordedPcmContract.validLength(3))
        assertFalse(RecordedPcmContract.validLength(3_198))
        assertTrue(RecordedPcmContract.validLength(3_200))
        assertTrue(RecordedPcmContract.validLength(3_840_000))
        assertFalse(RecordedPcmContract.validLength(3_840_002))
    }

    @Test fun resultOnlyWireDoesNotRequireSpeechFrameworkOrEditorContent() {
        assertEquals(1, RecordedAudioWire.REQUEST)
        assertEquals(2, RecordedAudioWire.CANCEL)
        assertEquals(3, RecordedAudioWire.RESULT)
        assertEquals(4, RecordedAudioWire.ERROR)
        assertEquals("audio", RecordedAudioWire.AUDIO)
        assertEquals("text", RecordedAudioWire.TEXT)
        assertEquals("session", RecordedAudioWire.SESSION)
        assertEquals("com.mangoloads.speak.boardaudio", RecordedAudioWire.WAKE_AUTHORITY)
        assertEquals("prepare-recorded-audio", RecordedAudioWire.WAKE_METHOD)
        assertEquals("ready", RecordedAudioWire.WAKE_READY)
        assertTrue(RecordedAudioWire.SERVICE_CLASS.endsWith(".BoardRecordedAudioService"))
    }
}
