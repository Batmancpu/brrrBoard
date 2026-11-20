// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VoiceNoiseProfileTest {
    @Test fun cleanMicPreservesRawSpeech() {
        val samples = FloatArray(32_000) { index ->
            if (index < 16_000) 0.0005f else 0.1f
        }
        assertFalse(VoiceNoiseProfile.shouldDenoise(samples))
    }

    @Test fun measurableNoiseWithSpeechCanBeEnhanced() {
        val samples = FloatArray(32_000) { index ->
            if (index % 16_000 < 9_600) 0.012f else 0.10f
        }
        assertTrue(VoiceNoiseProfile.shouldDenoise(samples))
    }

    @Test fun constantLoudAudioAndShortAudioBypass() {
        assertFalse(VoiceNoiseProfile.shouldDenoise(FloatArray(32_000) { 0.07f }))
        assertFalse(VoiceNoiseProfile.shouldDenoise(FloatArray(1_600) { 0.12f }))
        assertFalse(VoiceNoiseProfile.shouldDenoise(FloatArray(32_000)))
    }

    @Test fun incorrectOrDestroyedEnhancementFallsBackToRaw() {
        val clean = FloatArray(16_000) { 0.04f }
        assertFalse(VoiceNoiseProfile.validEnhancement(clean, clean))
        assertFalse(VoiceNoiseProfile.validEnhancement(clean, FloatArray(10)))
        assertFalse(VoiceNoiseProfile.validEnhancement(clean, FloatArray(16_000)))
        assertFalse(VoiceNoiseProfile.validEnhancement(clean, FloatArray(16_000) { Float.NaN }))
        assertFalse(VoiceNoiseProfile.validEnhancement(clean, FloatArray(16_000) { 2f }))
        assertTrue(VoiceNoiseProfile.validEnhancement(clean, FloatArray(16_000) { 0.02f }))
    }

    @Test fun invalidMicrophoneSamplesNeverEnterDenoiser() {
        val samples = FloatArray(32_000) { if (it == 123) Float.NaN else 0.03f }
        assertFalse(VoiceNoiseProfile.shouldDenoise(samples))
    }
}
