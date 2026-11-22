// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import android.content.Context
import helium314.keyboard.latin.utils.prefs

/** Selects only verified on-device packs; default to Ultra when it is installed. */
internal object VoiceModelChoice {
    const val PREFERENCE_KEY = "brr_active_offline_voice_model_v1"
    private const val SELECTED_110M = VoiceModelManifest.ID
    private const val SELECTED_ULTRA = UltraVoiceModelManifest.ID

    fun installed(context: Context): List<VoiceModelPack> = buildList(2) {
        if (VoiceModelInstaller.isInstalled(context, UltraVoiceModelManifest.pack))
            add(UltraVoiceModelManifest.pack)
        if (VoiceModelInstaller.isInstalled(context, VoiceModelManifest.pack))
            add(VoiceModelManifest.pack)
    }

    /** Saved choice wins while installed. A missing pack falls back safely to the other. */
    fun active(context: Context): VoiceModelPack? {
        val available = installed(context)
        val requested = context.prefs().getString(PREFERENCE_KEY, null)
        return when (requested) {
            SELECTED_ULTRA -> available.firstOrNull { it.id == SELECTED_ULTRA }
                ?: available.firstOrNull { it.id == SELECTED_110M }
            SELECTED_110M -> available.firstOrNull { it.id == SELECTED_110M }
                ?: available.firstOrNull { it.id == SELECTED_ULTRA }
            else -> available.firstOrNull { it.id == SELECTED_ULTRA }
                ?: available.firstOrNull { it.id == SELECTED_110M }
        }
    }

    fun select(context: Context, pack: VoiceModelPack): Boolean {
        if (!VoiceModelInstaller.isInstalled(context, pack)) return false
        if (pack.id != SELECTED_110M && pack.id != SELECTED_ULTRA) return false
        context.prefs().edit().putString(PREFERENCE_KEY, pack.id).apply()
        return true
    }
}
