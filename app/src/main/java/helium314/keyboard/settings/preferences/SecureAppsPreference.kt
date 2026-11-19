// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.preferences

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.edit
import helium314.keyboard.latin.AppsManager
import helium314.keyboard.latin.R
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.settings.Setting
import helium314.keyboard.settings.dialogs.MultiListPickerDialog
import helium314.keyboard.settings.dialogs.TextInputDialog

/** A local chooser of launcher apps; package IDs are stored, never screen labels. */
@Composable
fun SecureAppsPreference(setting: Setting) {
    val context = LocalContext.current
    val prefs = context.prefs()
    var selected by remember { mutableStateOf(prefs.getStringSet(Settings.PREF_SECURE_APPS, emptySet()).orEmpty().toSet()) }
    var showing by remember { mutableStateOf(false) }
    var addingPackage by remember { mutableStateOf(false) }
    val installed = remember { AppsManager(context).getPackagesAndNames().toMap() }
    val options = remember(installed, selected) {
        (installed.keys + selected).distinct()
            .sortedWith(compareBy(String.CASE_INSENSITIVE_ORDER) { installed[it] ?: it })
    }
    Preference(
        name = setting.title,
        description = if (selected.isEmpty()) setting.description
            else context.resources.getQuantityString(R.plurals.board_secure_apps_count, selected.size, selected.size),
        onClick = { showing = true },
    )
    Preference(
        name = stringResource(R.string.board_secure_apps_add_package),
        description = stringResource(R.string.board_secure_apps_add_package_summary),
        onClick = { addingPackage = true },
    )
    if (showing) {
        MultiListPickerDialog(
            onDismissRequest = { showing = false },
            items = options,
            initialSelection = selected.toList(),
            title = { Text(stringResource(R.string.board_secure_apps_title)) },
            getItemName = { pkg -> installed[pkg]?.let { "$it ($pkg)" } ?: pkg },
            onConfirmed = { chosen ->
                selected = chosen.toSet()
                // Existing learned history is untouched. Only future input from
                // these exact apps is excluded from ALL learning and persistence.
                prefs.edit { putStringSet(Settings.PREF_SECURE_APPS, selected.toSet()) }
                showing = false
            },
        )
    }
    if (addingPackage) {
        TextInputDialog(
            onDismissRequest = { addingPackage = false },
            title = { Text(stringResource(R.string.board_secure_apps_add_package)) },
            checkTextValid = { text ->
                text.matches(Regex("[A-Za-z][A-Za-z0-9_]*(\\.[A-Za-z][A-Za-z0-9_]*)+"))
            },
            onConfirmed = { pkg ->
                selected = selected + pkg.trim()
                prefs.edit { putStringSet(Settings.PREF_SECURE_APPS, selected.toSet()) }
                addingPackage = false
            },
        )
    }
}
