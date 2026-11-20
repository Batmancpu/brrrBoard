// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import android.content.Context
import android.media.AudioManager
import android.media.AudioRecord
import android.media.audiofx.NoiseSuppressor
import helium314.keyboard.latin.utils.prefs

/**
 * Uses the phone's already-installed microphone processing, if offered by its
 * audio HAL. Never loads the GTCRN JNI model that exited while initializing
 * on the owner's Oppo; never opens a second microphone or runs a neural worker.
 *
 * Some Android devices pre-process VOICE_RECOGNITION audio or do not expose a
 * NoiseSuppressor. The effect is optional and always releases with AudioRecord.
 */
internal object VoiceCaptureNoiseReduction {
    private const val PREF_ENABLED = "brr_voice_platform_noise_suppression"

    fun enabled(context: Context): Boolean = context.prefs().getBoolean(PREF_ENABLED, true)

    fun setEnabled(context: Context, enabled: Boolean) {
        context.prefs().edit().putBoolean(PREF_ENABLED, enabled).apply()
    }

    fun callIsActive(context: Context): Boolean = runCatching {
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audio.mode == AudioManager.MODE_IN_CALL || audio.mode == AudioManager.MODE_IN_COMMUNICATION
    }.getOrDefault(true) // Fail closed if Android cannot disclose the mode.

    /** Return an effect only if actually enabled. Caller must release it. */
    fun attach(context: Context, recorder: AudioRecord): NoiseSuppressor? {
        if (!enabled(context)) {
            VoiceDenoiseMetrics.platformState("OFF")
            return null
        }
        if (callIsActive(context)) {
            VoiceDenoiseMetrics.platformState("CALL_BLOCKED")
            return null
        }
        var created: NoiseSuppressor? = null
        return runCatching {
            if (!NoiseSuppressor.isAvailable()) return@runCatching null
            val effect = NoiseSuppressor.create(recorder.audioSessionId) ?: return@runCatching null
            created = effect
            if (effect.setEnabled(true) != android.media.audiofx.AudioEffect.SUCCESS || !effect.enabled) {
                return@runCatching null
            }
            effect
        }.getOrNull().also { effect ->
            if (effect == null) runCatching { created?.release() }
            VoiceDenoiseMetrics.platformState(if (effect == null) "UNAVAILABLE" else "ACTIVE")
        }
    }
}
