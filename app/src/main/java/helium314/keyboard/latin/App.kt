// SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
package helium314.keyboard.latin

import android.app.Application
import android.os.Build
import android.os.StrictMode
import helium314.keyboard.keyboard.emoji.SupportedEmojis
import helium314.keyboard.keyboard.internal.GeminiService
import helium314.keyboard.latin.define.DebugFlags
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.FoldableUtils
import helium314.keyboard.latin.utils.LayoutUtilsCustom
import helium314.keyboard.latin.utils.Log
import helium314.keyboard.latin.utils.SafeCrashReport
import helium314.keyboard.latin.utils.SubtypeSettings
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.latin.utils.upgradeToolbarPrefs
import helium314.keyboard.latin.voice.VoiceModelInstaller
import helium314.keyboard.latin.voice.UltraVoiceModelManifest
import helium314.keyboard.latin.voice.PrivateVoiceCacheCleanup
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class App : Application() {
    override fun onCreate() {
        super.onCreate()
        SafeCrashReport.install(this)
        // Files written after this cold start may be active recordings. Never delete them.
        val voiceCleanupBoundary = System.currentTimeMillis()
        if (BuildConfig.DEBUG) {
            StrictMode.setVmPolicy(
                StrictMode.VmPolicy.Builder()
                    .detectLeakedClosableObjects()
                    .penaltyLog()
                    .build()
            )
        }
        DebugFlags.init(this)
        FoldableUtils.init(this)
        Settings.init(this)
        SubtypeSettings.init(this)

        val scope = CoroutineScope(Dispatchers.Default)
        scope.launch(Dispatchers.IO) {
            PrivateVoiceCacheCleanup.removeOrphans(this@App, voiceCleanupBoundary)
        }
        scope.launch { // do some uncritical work in background for faster startup
            SupportedEmojis.load(this@App)
            LayoutUtilsCustom.removeMissingLayouts(this@App)
            val packageInfo = packageManager.getPackageInfo(packageName, 0)
            @Suppress("DEPRECATION")
            Log.i(
                "startup", "Starting ${applicationInfo.processName} version ${packageInfo.versionName} (${
                    packageInfo.versionCode
                }) on Android ${Build.VERSION.RELEASE} (SDK ${Build.VERSION.SDK_INT})"
            )
        }

        // An optional manual bootstrap release carries ASR weights. Regular updates
        // reuse the verified private noBackup copy without packaging it again.
        // Only the main process may extract the large optional Ultra bootstrap.
        // Isolated denoiser processes must not race the same staging directory.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P && getProcessName() == packageName) {
            scope.launch(Dispatchers.IO) {
                VoiceModelInstaller.installBundledIfPresent(this@App, UltraVoiceModelManifest.pack)
            }
        }

        RichInputMethodManager.init(this)
        checkVersionUpgrade(this)
        // Upgrade in production too: this prunes legacy keys that now have dedicated
        // controls elsewhere, so a normal app update actually removes their old buttons.
        upgradeToolbarPrefs(prefs())
        transferOldPinnedClips(this) // todo: remove in a few months, maybe end 2026
        app = this
        Defaults.initDynamicDefaults(this)
        GeminiService.init(this)
    }

    companion object {
        // used so JniUtils can access application once
        private var app: App? = null
        fun getApp(): App? {
            val application = app
            app = null
            return application
        }
    }
}
