// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import android.content.Context
import helium314.keyboard.latin.utils.prefs

/** Exactly one optional app-controlled suppression path per microphone session. */
internal enum class VoiceNoiseMode { OFF, ANDROID, GTCRN }

internal object VoiceNoiseChoice {
    private const val PREF_MODE = "brr_voice_noise_mode_v1"
    private const val PREF_LEGACY_PLATFORM_ENABLED = "brr_voice_platform_noise_suppression"

    fun selected(context: Context): VoiceNoiseMode {
        val preferences = context.prefs()
        val selectedMode = preferences.getString(PREF_MODE, null)
        if (selectedMode != null) return modeForSelection(selectedMode)
        // Older releases stored the Android effect as an explicit boolean. Keep
        // that choice across the upgrade; only installs with no saved choice use
        // the new brrClean default.
        return modeForLegacyChoice(
            preferences.contains(PREF_LEGACY_PLATFORM_ENABLED),
            preferences.getBoolean(PREF_LEGACY_PLATFORM_ENABLED, false),
        )
    }

    internal fun modeForSelection(selectedMode: String): VoiceNoiseMode = when (selectedMode) {
        VoiceNoiseMode.GTCRN.name -> VoiceNoiseMode.GTCRN
        VoiceNoiseMode.OFF.name -> VoiceNoiseMode.OFF
        VoiceNoiseMode.ANDROID.name -> VoiceNoiseMode.ANDROID
        else -> VoiceNoiseMode.GTCRN
    }

    internal fun modeForLegacyChoice(hasSavedChoice: Boolean, platformEnabled: Boolean): VoiceNoiseMode =
        if (!hasSavedChoice) VoiceNoiseMode.GTCRN
        else if (platformEnabled) VoiceNoiseMode.ANDROID else VoiceNoiseMode.OFF

    fun select(context: Context, mode: VoiceNoiseMode) {
        context.prefs().edit().putString(PREF_MODE, mode.name)
            .putBoolean("brr_voice_platform_noise_suppression", mode == VoiceNoiseMode.ANDROID).apply()
    }

    /** Everyday Tools exposes one clear brrClean on/off control; GTCRN is on by default. */
    fun isCleaningEnabled(context: Context): Boolean = selected(context) == VoiceNoiseMode.GTCRN

    fun setCleaningEnabled(context: Context, enabled: Boolean) =
        select(context, if (enabled) VoiceNoiseMode.GTCRN else VoiceNoiseMode.OFF)

    // Binder's 1 MiB transaction limit requires a conservative smaller bound.
    // Long dictation bypasses optional GTCRN and still transcribes the entire original.
    const val MAX_GTCRN_BYTES = 16_000 * 2 * 12

    fun eligible(bytes: Int): Boolean = bytes in 16_000..MAX_GTCRN_BYTES && bytes % 2 == 0
}
