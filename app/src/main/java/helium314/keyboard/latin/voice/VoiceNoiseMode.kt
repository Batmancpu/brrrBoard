// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import android.content.Context
import helium314.keyboard.latin.utils.prefs

/** Exactly one optional app-controlled suppression path per microphone session. */
internal enum class VoiceNoiseMode { OFF, ANDROID, GTCRN }

internal object VoiceNoiseChoice {
    private const val PREF_MODE = "brr_voice_noise_mode_v1"

    fun selected(context: Context): VoiceNoiseMode = when (context.prefs().getString(PREF_MODE, null)) {
        VoiceNoiseMode.GTCRN.name -> VoiceNoiseMode.GTCRN
        VoiceNoiseMode.OFF.name -> VoiceNoiseMode.OFF
        VoiceNoiseMode.ANDROID.name -> VoiceNoiseMode.ANDROID
        // Preserve the prior Android-effect OFF preference when migrating.
        else -> if (context.prefs().getBoolean("brr_voice_platform_noise_suppression", true))
            VoiceNoiseMode.ANDROID else VoiceNoiseMode.OFF
    }

    fun select(context: Context, mode: VoiceNoiseMode) {
        context.prefs().edit().putString(PREF_MODE, mode.name)
            .putBoolean("brr_voice_platform_noise_suppression", mode == VoiceNoiseMode.ANDROID).apply()
    }

    // Binder's 1 MiB transaction limit requires a conservative smaller bound.
    // Long dictation bypasses optional GTCRN and still transcribes the entire original.
    const val MAX_GTCRN_BYTES = 16_000 * 2 * 12

    fun eligible(bytes: Int): Boolean = bytes in 16_000..MAX_GTCRN_BYTES && bytes % 2 == 0
}
