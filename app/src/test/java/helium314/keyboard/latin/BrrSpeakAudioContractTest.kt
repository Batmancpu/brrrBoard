// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.content.ComponentName
import android.media.AudioFormat
import android.os.ParcelFileDescriptor
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.util.Locale

@RunWith(RobolectricTestRunner::class)
class BrrSpeakAudioContractTest {
    private val brrSpeak = ComponentName(BRRSPEAK_PACKAGE, BRRSPEAK_RECOGNITION_SERVICE_CLASS)
    private val original = ComponentName("net.devemperor.dictate", BRRSPEAK_RECOGNITION_SERVICE_CLASS)

    @Test fun exactForkComponentOnlyNeverFallsBackToPurchasedDictate() {
        assertEquals(brrSpeak, resolveBrrSpeakService(listOf(original, brrSpeak)))
        assertNull(resolveBrrSpeakService(listOf(original)))
        assertNull(resolveBrrSpeakService(emptyList()))
        assertNull(resolveBrrSpeakService(listOf(
            ComponentName(BRRSPEAK_PACKAGE, "com.mangoloads.speak.UnverifiedRecognizer"),
        )))
    }

    @Test fun oldReceiverVersionCannotAccidentallyOpenItsOwnMicrophone() {
        assertFalse(supportsBrrSpeakAudioSource(null))
        assertFalse(supportsBrrSpeakAudioSource(142_004L))
        assertTrue(supportsBrrSpeakAudioSource(142_005L))
        assertTrue(supportsBrrSpeakAudioSource(143_000L))
    }

    @Test fun intentPassesLivePipeDescriptorWithExactPcm16Format() {
        val pipe = ParcelFileDescriptor.createPipe()
        try {
            val intent = createBrrSpeakAudioIntent(Locale.forLanguageTag("en-IN"), pipe[0])
            assertEquals(RecognizerIntent.ACTION_RECOGNIZE_SPEECH, intent.action)
            assertTrue(intent.hasExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE))
            assertEquals(1, intent.getIntExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_CHANNEL_COUNT, -1))
            assertEquals(AudioFormat.ENCODING_PCM_16BIT,
                intent.getIntExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_ENCODING, -1))
            assertEquals(16_000, intent.getIntExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_SAMPLING_RATE, -1))
            assertEquals("en-IN", intent.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE))
            assertEquals(3, intent.getIntExtra(RecognizerIntent.EXTRA_MAX_RESULTS, -1))
        } finally {
            pipe.forEach { it.close() }
        }
    }

    @Test fun hindiKeyboardLocaleRequestsEnglishWithoutOpeningAnyActivity() {
        val pipe = ParcelFileDescriptor.createPipe()
        try {
            val intent = createBrrSpeakAudioIntent(Locale.forLanguageTag("hi-IN"), pipe[0])
            assertEquals("en-IN", intent.getStringExtra(RecognizerIntent.EXTRA_LANGUAGE))
            assertNull(intent.component)
        } finally {
            pipe.forEach { it.close() }
        }
    }

    @Test fun providerPermissionErrorIsNotMisreportedAsBoardPermissionDenial() {
        assertEquals(R.string.inline_voice_provider_audio_failure,
            brrSpeakFailureMessage(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS, true))
        assertEquals(R.string.inline_voice_permission,
            brrSpeakFailureMessage(SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS, false))
        assertEquals(R.string.inline_voice_no_speech,
            brrSpeakFailureMessage(SpeechRecognizer.ERROR_NO_MATCH, true))
    }

    @Test fun idleDoubleTapWindowAndActiveStopAreDistinct() {
        assertFalse(FooterMicTapPolicy.isImmediate(false, false))
        assertTrue(FooterMicTapPolicy.isImmediate(true, false))
        assertTrue(FooterMicTapPolicy.isImmediate(false, true))
    }
}
