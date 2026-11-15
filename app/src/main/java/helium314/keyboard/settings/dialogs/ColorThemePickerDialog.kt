// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.dialogs

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.core.content.edit
import helium314.keyboard.keyboard.KeyboardSwitcher
import helium314.keyboard.keyboard.KeyboardTheme
import helium314.keyboard.latin.R
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.getStringResourceOrName
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.settings.Setting

@Composable
fun ColorThemePickerDialog(onDismissRequest: () -> Unit, setting: Setting) {
    val context = LocalContext.current
    val prefs = context.prefs()
    val selected = KeyboardTheme.supportedTheme(prefs.getString(setting.key, null))
    AlertDialog(
        onDismissRequest = onDismissRequest,
        title = { Text(setting.title) },
        text = {
            Column {
                KeyboardTheme.getAvailableDefaultColors().forEach { theme ->
                    val select = {
                        prefs.edit {
                            if (theme == KeyboardTheme.THEME_FROSTED_GLASS) {
                                putString(Settings.PREF_THEME_COLORS, theme)
                                putString(Settings.PREF_THEME_COLORS_NIGHT, theme)
                            } else {
                                putString(setting.key, theme)
                            }
                        }
                        KeyboardSwitcher.getInstance().setThemeNeedsReload()
                        onDismissRequest()
                    }
                    Row(
                        modifier = Modifier.fillMaxWidth().clickable(onClick = select),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        RadioButton(selected = selected == theme, onClick = select)
                        Text(theme.getStringResourceOrName("theme_name_", context))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismissRequest) { Text(stringResource(R.string.dialog_close)) }
        },
    )
}
