// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import android.content.Context
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import helium314.keyboard.latin.BuildConfig
import helium314.keyboard.latin.VisualPowerPolicy
import helium314.keyboard.settings.SearchSettingsScreen
import helium314.keyboard.settings.SettingsWithoutKey

@Composable
internal fun CustomAdditionsScreen(onClickBack: () -> Unit) {
    SearchSettingsScreen(onClickBack = onClickBack, title = "Custom",
        settings = if (BuildConfig.BUILD_TYPE == "nouserlib") listOf(SettingsWithoutKey.CUSTOM_EMOJI_FONT)
        else listOf(SettingsWithoutKey.CUSTOM_EMOJI_FONT, SettingsWithoutKey.LOAD_GESTURE_LIB))
}

@Composable
internal fun BatteryCompatibilityPrompt() {
    val ctx = LocalContext.current
    val owner = LocalLifecycleOwner.current
    val prefs = remember(ctx) { ctx.getSharedPreferences("compatibility_setup", Context.MODE_PRIVATE) }
    var dismissed by remember { mutableStateOf(prefs.getBoolean("battery_prompt_dismissed", false)) }
    var restricted by remember { mutableStateOf(VisualPowerPolicy.isBatteryRestricted(ctx)) }
    DisposableEffect(owner, ctx) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) restricted = VisualPowerPolicy.isBatteryRestricted(ctx)
        }
        owner.lifecycle.addObserver(observer)
        onDispose { owner.lifecycle.removeObserver(observer) }
    }
    if (restricted && !dismissed) {
        Md3ePreferenceGroup("Keyboard reliability") {
            Md3ePreference("settings", "Battery optimization", "Allow unrestricted battery use in Android settings",
                { VisualPowerPolicy.openBatterySettings(ctx) }, isFirst = true)
            Md3ePreference("settings", "Not now", "You can find battery optimization in Settings search",
                { dismissed = true; prefs.edit().putBoolean("battery_prompt_dismissed", true).apply() }, isLast = true)
        }
    }
}
