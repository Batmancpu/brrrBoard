// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import helium314.keyboard.latin.utils.SafeCrashReport
import helium314.keyboard.settings.SearchSettingsScreen
import helium314.keyboard.settings.SettingsDestination

@Composable
internal fun TypingSettingsHub(onClickBack: () -> Unit, navigate: (String) -> Unit) {
    SearchSettingsScreen(onClickBack, "Typing", emptyList()) {
        Md3ePreferenceGroup("Everyday typing") {
            Md3ePreference("spellcheck", "Correction & suggestions", "Autocorrect, predictions & spacing",
                { navigate(SettingsDestination.TextCorrection) }, isFirst = true)
            Md3ePreference("gesture", "Swipe typing", "Gesture input & trails",
                { navigate(SettingsDestination.GestureTyping) })
            Md3ePreference("language", "Languages & layouts", "Enabled languages & input layouts",
                { navigate(SettingsDestination.Languages) }, isLast = true)
        }
        Md3ePreferenceGroup("Make it yours") {
            Md3ePreference("keyboard", "Keys & interactions", "Number row, long press & shortcuts",
                { navigate(SettingsDestination.Keys) }, isFirst = true)
            Md3ePreference("view_quilt", "Layout variants", "Alternate layouts for each language",
                { navigate(SettingsDestination.Layouts) })
            Md3ePreference("dashboard", "Toolbar", "Shortcuts & suggestion bar",
                { navigate(SettingsDestination.Toolbar) }, isLast = true)
        }
    }
}
@Composable
internal fun AdvancedSettingsHub(onClickBack: () -> Unit, navigate: (String) -> Unit) {
    val context = LocalContext.current
    SearchSettingsScreen(onClickBack, "Advanced", emptyList()) {
        Md3ePreferenceGroup("Fine tuning") {
            Md3ePreference("tune", "Detailed preferences", "All fine-grained typing options",
                { navigate(SettingsDestination.Preferences) }, isFirst = true)
            Md3ePreference("dictionary", "Dictionaries & learning", "Language resources and personal words",
                { navigate(SettingsDestination.Dictionaries) })
            Md3ePreference("settings", "Compatibility & diagnostics", "Device-specific behaviour",
                { navigate(SettingsDestination.Advanced) }, isLast = true)
        }
        Md3ePreferenceGroup("Maintenance") {
            Md3ePreference("backup", "Backup & restore", "Export, import or reset settings",
                { navigate(SettingsDestination.Backup) }, isFirst = true)
            Md3ePreference("bug_report", "Copy safe crash report", "Code frames and crash type only",
                {
                    context.getSystemService(ClipboardManager::class.java)?.setPrimaryClip(
                        ClipData.newPlainText("brrBoard safe crash report", SafeCrashReport.copyText(context)))
                    Toast.makeText(context, "Safe report copied", Toast.LENGTH_SHORT).show()
                }, isLast = true)
        }
    }
}
