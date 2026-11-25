// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.latin.utils.getActivity
import helium314.keyboard.settings.dialogs.ListPickerDialog
import helium314.keyboard.settings.preferences.Preference

internal enum class CorrectionPreset(val label: String, val threshold: Float?) {
    OFF("Off", null), GENTLE("Gentle", .185f), BALANCED("Balanced", .067f), STRONG("Strong", -1f), CUSTOM("Custom", null);
    companion object {
        fun read(enabled: Boolean, threshold: Float, more: Boolean): CorrectionPreset =
            if (!enabled) OFF else if (more) CUSTOM else entries.firstOrNull { it.threshold == threshold && it.threshold != null } ?: CUSTOM
    }
}

@Composable
internal fun CorrectionPresetControl(onCustom: () -> Unit) {
    val prefs = LocalContext.current.prefs()
    val revision by (LocalContext.current.getActivity() as helium314.keyboard.settings.SettingsActivity).prefChanged.collectAsState()
    @Suppress("UNUSED_VARIABLE") val observedRevision = revision
    val current = CorrectionPreset.read(prefs.getBoolean(Settings.PREF_AUTO_CORRECTION, Defaults.PREF_AUTO_CORRECTION),
        prefs.getFloat(Settings.PREF_AUTO_CORRECT_THRESHOLD, Defaults.PREF_AUTO_CORRECT_THRESHOLD),
        prefs.getBoolean(Settings.PREF_MORE_AUTO_CORRECTION, Defaults.PREF_MORE_AUTO_CORRECTION))
    var open by remember { mutableStateOf(false) }
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Preference(name = "Correction strength", description = current.label, onClick = { open = true })
        Text("Individual correction options below remain available. Off preserves your previous threshold.")
    }
    if (open) ListPickerDialog(onDismissRequest = { open = false }, items = CorrectionPreset.entries,
        selectedItem = current, title = { Text("Correction strength") },
        getItemName = { it.label }, onItemSelected = { preset ->
            if (preset == CorrectionPreset.CUSTOM) onCustom() else {
                val edit = prefs.edit().putBoolean(Settings.PREF_AUTO_CORRECTION, preset != CorrectionPreset.OFF)
                preset.threshold?.let { edit.putFloat(Settings.PREF_AUTO_CORRECT_THRESHOLD, it).putBoolean(Settings.PREF_MORE_AUTO_CORRECTION, false) }
                edit.apply()
            }
        })
}

internal enum class SuggestionVisibility(val label: String) {
    OFF("Off"), CONTEXTUAL("Contextual"), ALWAYS("Always");
    companion object {
        fun read(show: Boolean, always: Boolean) = if (!show) OFF else if (always) ALWAYS else CONTEXTUAL
    }
}

@Composable
internal fun SuggestionVisibilityControl() {
    val prefs = LocalContext.current.prefs()
    val revision by (LocalContext.current.getActivity() as helium314.keyboard.settings.SettingsActivity).prefChanged.collectAsState()
    @Suppress("UNUSED_VARIABLE") val observedRevision = revision
    val current = SuggestionVisibility.read(
        prefs.getBoolean(Settings.PREF_SHOW_SUGGESTIONS, Defaults.PREF_SHOW_SUGGESTIONS),
        prefs.getBoolean(Settings.PREF_ALWAYS_SHOW_SUGGESTIONS, Defaults.PREF_ALWAYS_SHOW_SUGGESTIONS))
    var open by remember { mutableStateOf(false) }
    Column(Modifier.padding(horizontal = 16.dp, vertical = 8.dp)) {
        Preference(name = "Suggestion visibility", description = current.label, onClick = { open = true })
    }
    if (open) ListPickerDialog(onDismissRequest = { open = false }, items = SuggestionVisibility.entries,
        selectedItem = current, title = { Text("Suggestion visibility") }, getItemName = { it.label },
        onItemSelected = { mode ->
            val edit = prefs.edit().putBoolean(Settings.PREF_SHOW_SUGGESTIONS, mode != SuggestionVisibility.OFF)
            if (mode != SuggestionVisibility.OFF) edit.putBoolean(Settings.PREF_ALWAYS_SHOW_SUGGESTIONS, mode == SuggestionVisibility.ALWAYS)
            edit.apply()
        })
}
