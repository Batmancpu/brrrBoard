// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import android.content.Context
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.padding
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import helium314.keyboard.latin.common.Constants.Separators
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import helium314.keyboard.keyboard.KeyboardSwitcher
import helium314.keyboard.latin.R
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.MAX_PINNED_TOOLBAR_KEYS
import helium314.keyboard.latin.utils.ToolbarKey
import helium314.keyboard.latin.utils.Log
import helium314.keyboard.latin.utils.ToolbarMode
import helium314.keyboard.latin.utils.isAvailableInToolbarUi
import helium314.keyboard.latin.utils.getActivity
import helium314.keyboard.latin.utils.getStringResourceOrName
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.settings.SearchSettingsScreen
import helium314.keyboard.settings.Setting
import helium314.keyboard.settings.SettingsActivity
import helium314.keyboard.latin.utils.Theme
import helium314.keyboard.settings.dialogs.ToolbarKeysCustomizer
import helium314.keyboard.settings.initPreview
import helium314.keyboard.settings.preferences.ListPreference
import helium314.keyboard.settings.preferences.Preference
import helium314.keyboard.settings.preferences.ReorderSwitchPreference
import helium314.keyboard.settings.preferences.SwitchPreference
import helium314.keyboard.latin.utils.previewDark

@Composable
fun ToolbarScreen(
    onClickBack: () -> Unit,
) {
    val prefs = LocalContext.current.prefs()
    val b = (LocalContext.current.getActivity() as? SettingsActivity)?.prefChanged?.collectAsState()
    if ((b?.value ?: 0) < 0)
        Log.v("irrelevant", "stupid way to trigger recomposition on preference change")
    val toolbarMode = Settings.readToolbarMode(prefs)
    val customToolbarCodesVisible = toolbarMode != ToolbarMode.HIDDEN
        || !prefs.getBoolean(Settings.PREF_TOOLBAR_HIDING_GLOBAL, Defaults.PREF_TOOLBAR_HIDING_GLOBAL)
    val items = listOf(
        Settings.PREF_TOOLBAR_MODE,
        if (toolbarMode == ToolbarMode.HIDDEN) Settings.PREF_TOOLBAR_HIDING_GLOBAL else null,
        if (toolbarMode != ToolbarMode.HIDDEN) Settings.PREF_TOOLBAR_SWIPE_DOWN_TO_HIDE else null,
        if (toolbarMode != ToolbarMode.HIDDEN) Settings.PREF_TOOLBAR_KEYS else null,
        when (toolbarMode) {
            ToolbarMode.EXPANDABLE, ToolbarMode.TOOLBAR_KEYS -> Settings.PREF_PINNED_TOOLBAR_KEYS
            else -> null
        },
        if (toolbarMode != ToolbarMode.HIDDEN) Settings.PREF_PERSISTENT_TOOLBAR_KEY else null,
        if (customToolbarCodesVisible) Settings.PREF_TOOLBAR_CUSTOM_KEY_CODES else null,
        if (toolbarMode != ToolbarMode.HIDDEN) Settings.PREF_VARIABLE_TOOLBAR_DIRECTION else null,
    )
    SearchSettingsScreen(
        onClickBack = onClickBack,
        title = stringResource(R.string.settings_screen_toolbar),
        settings = items,
        header = {
            if (toolbarMode != ToolbarMode.HIDDEN) {
                val persistent = prefs.getString(Settings.PREF_PERSISTENT_TOOLBAR_KEY, Defaults.PREF_PERSISTENT_TOOLBAR_KEY)
                fun enabled(key: String, fallback: String) = prefs.getString(key, fallback).orEmpty().split(Separators.ENTRY).mapNotNull { entry ->
                    val pair = entry.split(Separators.KV)
                    if (pair.last() == "true") runCatching { ToolbarKey.valueOf(pair.first()) }.getOrNull()?.takeIf { it.isAvailableInToolbarUi() } else null
                }.distinct()
                Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Toolbar order", style = MaterialTheme.typography.titleMedium)
                    enabled(Settings.PREF_TOOLBAR_KEYS, Defaults.PREF_TOOLBAR_KEYS).chunked(4).forEach { row ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) { row.forEach { key -> Text(key.name.lowercase().getStringResourceOrName("", LocalContext.current), Modifier.weight(1f), style = MaterialTheme.typography.bodySmall) } }
                    }
                    if (toolbarMode == ToolbarMode.EXPANDABLE || toolbarMode == ToolbarMode.TOOLBAR_KEYS) {
                        Text("Pinned row", style = MaterialTheme.typography.titleMedium)
                        Text(enabled(Settings.PREF_PINNED_TOOLBAR_KEYS, Defaults.PREF_PINNED_TOOLBAR_KEYS).filterNot { it.name == persistent }.take(MAX_PINNED_TOOLBAR_KEYS).joinToString(" · ") { it.name.lowercase().replace("_", " ") }.ifEmpty { "No pinned keys" })
                    }
                    Text("Use the toolbar and pinned keys editors below to reorder and enable actions.", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    )
}

fun createToolbarSettings(context: Context) = listOf(
    Setting(context, Settings.PREF_TOOLBAR_MODE, R.string.toolbar_mode) { setting ->
        val ctx = LocalContext.current
        val items =
            ToolbarMode.entries.map { it.name.lowercase().getStringResourceOrName("toolbar_mode_", ctx) to it.name }
        ListPreference(
            setting,
            items,
            Defaults.PREF_TOOLBAR_MODE
        ) {
            KeyboardSwitcher.getInstance().setThemeNeedsReload()
        }
    },
    Setting(context, Settings.PREF_TOOLBAR_HIDING_GLOBAL, R.string.toolbar_hiding_global) {
        SwitchPreference(it, Defaults.PREF_TOOLBAR_HIDING_GLOBAL) {
            KeyboardSwitcher.getInstance().setThemeNeedsReload()
        }
    },
    Setting(context, Settings.PREF_TOOLBAR_SWIPE_DOWN_TO_HIDE, R.string.toolbar_swipe_down_to_hide, R.string.toolbar_swipe_down_to_hide_summary) {
        SwitchPreference(it, Defaults.PREF_TOOLBAR_SWIPE_DOWN_TO_HIDE)
    },
    Setting(context, Settings.PREF_TOOLBAR_KEYS, R.string.toolbar_keys) {
        ReorderSwitchPreference(it, Defaults.PREF_TOOLBAR_KEYS)
    },
    Setting(context, Settings.PREF_PINNED_TOOLBAR_KEYS, R.string.pinned_toolbar_keys) {
        ReorderSwitchPreference(it, Defaults.PREF_PINNED_TOOLBAR_KEYS, maxChecked = MAX_PINNED_TOOLBAR_KEYS)
    },
    Setting(context, Settings.PREF_PERSISTENT_TOOLBAR_KEY, R.string.persistent_toolbar_key) { setting ->
        val ctx = LocalContext.current
        ListPreference(
            setting,
            ToolbarKey.entries
                .filter { it.isAvailableInToolbarUi() }
                .map { it.name.lowercase().getStringResourceOrName("", ctx) to it.name },
            Defaults.PREF_PERSISTENT_TOOLBAR_KEY
        )
    },
    Setting(context, Settings.PREF_TOOLBAR_CUSTOM_KEY_CODES, R.string.customize_toolbar_key_codes) {
        var showDialog by rememberSaveable { mutableStateOf(false) }
        Preference(
            name = it.title,
            onClick = { showDialog = true },
        )
        if (showDialog)
            ToolbarKeysCustomizer(
                key = it.key,
                onDismissRequest = { showDialog = false }
            )
    },
    Setting(context, Settings.PREF_VARIABLE_TOOLBAR_DIRECTION,
        R.string.var_toolbar_direction, R.string.var_toolbar_direction_summary)
    {
        SwitchPreference(it, Defaults.PREF_VARIABLE_TOOLBAR_DIRECTION)
    }
)

@Preview
@Composable
private fun Preview() {
    initPreview(LocalContext.current)
    Theme(previewDark) {
        Surface {
            ToolbarScreen { }
        }
    }
}
