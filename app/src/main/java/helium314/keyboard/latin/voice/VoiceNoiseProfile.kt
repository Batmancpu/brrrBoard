// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import kotlin.math.sqrt

/**
 * Conservative, microphone-independent speech/noise gate.
 *
 * GTCRN is not universally better than raw speech for ASR. Apply it only when a
 * measurable noise floor and a distinct louder region coexist in the utterance.
 * Clean earbuds/quiet rooms keep their raw PCM. This is not a voice activity
 * detector or a claim about word-error-rate improvement.
 */
internal object VoiceNoiseProfile {
    private const val FRAME_SAMPLES = 320 // 20 ms at 16 kHz
    private const val MIN_FRAMES = 40 // 800 ms of audio
    private const val MIN_NOISE_RMS = 0.0063f // approximately -44 dBFS

    fun shouldDenoise(samples: FloatArray): Boolean {
        val frames = samples.size / FRAME_SAMPLES
        if (frames < MIN_FRAMES) return false
        val rms = FloatArray(frames)
        for (frame in 0 until frames) {
            var energy = 0.0
            val offset = frame * FRAME_SAMPLES
            for (i in 0 until FRAME_SAMPLES) {
                val sample = samples[offset + i]
                if (!sample.isFinite()) return false
                energy += sample.toDouble() * sample.toDouble()
            }
            rms[frame] = sqrt(energy / FRAME_SAMPLES).toFloat()
        }
        rms.sort()
        val noise = rms[(frames * 20) / 100]
        val voice = rms[(frames * 85) / 100]
        // A loud, nearly constant signal may simply be somebody speaking,
        // so require at least an 8 dB separation before touching the audio.
        return noise >= MIN_NOISE_RMS && voice >= noise * 2.5f
    }

    fun validEnhancement(original: FloatArray, enhanced: FloatArray): Boolean {
        if (enhanced === original || enhanced.size != original.size || enhanced.isEmpty()) return false
        var originalEnergy = 0.0
        var enhancedEnergy = 0.0
        for (index in original.indices) {
            val sample = enhanced[index]
            if (!sample.isFinite() || sample < -1.1f || sample > 1.1f) return false
            originalEnergy += original[index].toDouble() * original[index].toDouble()
            enhancedEnergy += sample.toDouble() * sample.toDouble()
        }
        // Reject a broken model that silently wipes audible speech.
        return originalEnergy <= 1e-9 || enhancedEnergy >= originalEnergy * 0.0025
    }
}
