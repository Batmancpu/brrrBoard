// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.utils

import android.app.ActivityManager
import android.content.Context
import android.os.Build
import android.os.Debug
import android.view.WindowManager
import helium314.keyboard.latin.BuildConfig
import helium314.keyboard.latin.FrostedGlassHelper
import helium314.keyboard.latin.VisualPowerPolicy
import helium314.keyboard.latin.KeyboardMotionSpec
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.voice.VoiceModelInstaller
import java.io.File

/** Fixed schema, no raw logcat, editor text, paths, identifiers, audio or arbitrary exception messages. */
internal object CompatibilityDiagnostics {
    fun report(context: Context): String = buildString {
        appendLine("--- detailed component health ---")
        appendLine("source_revision=${BuildConfig.SOURCE_REVISION} abi=${Build.SUPPORTED_ABIS.firstOrNull() ?: "unknown"}")
        val battery = VisualPowerPolicy.battery(context)
        appendLine("battery_percent=${battery?.percent ?: -1} charging=${battery?.charging} system_saver=${VisualPowerPolicy.systemSaver(context)} app_battery_restricted=${VisualPowerPolicy.isBatteryRestricted(context)} optional_effects_suppressed=${VisualPowerPolicy.suppressOptional(context)}")
        val blur = if (Build.VERSION.SDK_INT >= 31) runCatching {
            context.getSystemService(WindowManager::class.java)?.isCrossWindowBlurEnabled
        }.getOrNull() else false
        val override = context.prefs().getString(Settings.PREF_BLUR_RENDER_OVERRIDE, "auto")
        appendLine("cross_window_blur=$blur renderer=${FrostedGlassHelper.lastRenderer} override=${override?.takeIf { it in setOf("auto","force_native","force_samsung","force_solid") } ?: "unknown"}")
        appendLine("animators_enabled=${KeyboardMotionSpec.enabled()} decorative_motion=${KeyboardMotionSpec.decorativeMotion(context)}")
        appendLine("comma_period=${context.prefs().getBoolean(Settings.PREF_COMPAT_COMMA_PERIOD,false)}")
        val am = context.getSystemService(ActivityManager::class.java)
        val memory = ActivityManager.MemoryInfo()
        if (am != null) {
            am.getMemoryInfo(memory)
            appendLine("memory_class_mb=${am.memoryClass} low_ram=${am.isLowRamDevice} available_mib=${memory.availMem / 1048576} low_memory=${memory.lowMemory}")
        }
        appendLine("own_process_pss_kib=${Debug.getPss()} heap_used_bytes=${Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory()} private_free_mib=${context.filesDir.usableSpace / 1048576}")
        val library = File(DeviceProtectedUtils.getFilesDir(context), JniUtils.JNI_LIB_IMPORT_FILE_NAME)
        val font = Settings.getCustomEmojiFontFile(context)
        appendLine("gesture_library_file=${library.isFile} gesture_library_bytes=${if (library.isFile) library.length() else 0} gesture_decoder_available=${JniUtils.sHaveGestureLib}")
        appendLine("custom_emoji_font=${font.isFile} custom_emoji_font_bytes=${if(font.isFile) font.length() else 0}")
        appendLine("voice_model_install_state=${VoiceModelInstaller.installState} percent=${VoiceModelInstaller.installPercent}")
        appendLine("--- own-package historical exit codes (no descriptions or traces) ---")
        if (Build.VERSION.SDK_INT >= 30 && am != null) {
            runCatching {
                am.getHistoricalProcessExitReasons(context.packageName, 0, 12).forEach {
                    appendLine("exit_timestamp_ms=${it.timestamp} reason=${it.reason} status=${it.status} importance=${it.importance} pss_kib=${it.pss} rss_kib=${it.rss}")
                }
            }.onFailure { appendLine("exit_history_unavailable") }
        } else appendLine("exit_history_unsupported")
    }
}
