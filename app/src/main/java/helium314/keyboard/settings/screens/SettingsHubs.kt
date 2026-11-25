// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import androidx.compose.runtime.Composable
import helium314.keyboard.settings.SearchSettingsScreen
import helium314.keyboard.settings.SettingsDestination as D

private data class WorkspaceEntry(val icon: String, val title: String, val summary: String, val route: String)

@Composable
private fun Workspace(title: String, onClickBack: () -> Unit, navigate: (String) -> Unit, vararg entries: WorkspaceEntry) {
    SearchSettingsScreen(onClickBack, title, emptyList()) {
        Md3ePreferenceGroup(title) {
            entries.forEachIndexed { index, entry ->
                Md3ePreference(entry.icon, entry.title, entry.summary, { navigate(entry.route) },
                    isFirst = index == 0, isLast = index == entries.lastIndex)
            }
        }
    }
}

@Composable
internal fun KeyboardSettingsHub(onClickBack: () -> Unit, navigate: (String) -> Unit) = Workspace(
    "Keyboard", onClickBack, navigate,
    WorkspaceEntry("palette", "Glass Studio", "Surface, keycaps and accent", D.GlassEditor),
    WorkspaceEntry("palette", "Appearance & accent", "Theme options and background images", D.Glass),
    WorkspaceEntry("straighten", "Size & positioning", "Height, padding, split and bottom row", D.Size),
    WorkspaceEntry("keyboard", "Keys & symbols", "Number row, hints, popups and keys", D.KeySymbols),
    WorkspaceEntry("language", "Languages & layouts", "Enabled languages and input layouts", D.Languages),
    WorkspaceEntry("view_quilt", "Layout variants", "Alternate layouts", D.Layouts),
    WorkspaceEntry("text_fields", "Lettering & fonts", "Keyboard typography", D.Lettering),
)

@Composable
internal fun TypingSettingsHub(onClickBack: () -> Unit, navigate: (String) -> Unit) = Workspace(
    "Typing & intelligence", onClickBack, navigate,
    WorkspaceEntry("spellcheck", "Autocorrection", "Correction strength and precise controls", D.Correction),
    WorkspaceEntry("auto_awesome", "Predictions & suggestions", "Suggestion visibility and behaviour", D.Suggestions),
    WorkspaceEntry("text_fields", "Writing conventions", "Capitalization and automatic spacing", D.Writing),
    WorkspaceEntry("gesture", "Swipe typing", "Gesture input and trails", D.GestureTyping),
    WorkspaceEntry("psychology", "BR Brain", "Local intelligence and adaptive touch", D.BrainDashboard),
    WorkspaceEntry("dictionary", "Personal dictionary", "Manage your words", D.PersonalDictionaries),
)

@Composable
internal fun InteractionsSettingsHub(onClickBack: () -> Unit, navigate: (String) -> Unit) = Workspace(
    "Interactions", onClickBack, navigate,
    WorkspaceEntry("gesture", "Gesture assignments", "Spacebar, delete, long press and editing", D.GestureAssignments),
    WorkspaceEntry("graphic_eq", "Sound & haptics", "Keypress feedback and system defaults", D.Sound),
    WorkspaceEntry("animation", "Motion", "Existing keyboard motion profiles", D.Motion),
)

@Composable
internal fun ToolsSettingsHub(onClickBack: () -> Unit, navigate: (String) -> Unit) = Workspace(
    "Tools", onClickBack, navigate,
    WorkspaceEntry("dashboard", "Toolbar", "Order, pinned tools and visibility", D.Toolbar),
    WorkspaceEntry("content_paste", "Clipboard", "History, screenshots and retention", D.Clipboard),
    WorkspaceEntry("emoji_emotions", "Emoji", "Fonts, skin tones and search", D.Emoji),
    WorkspaceEntry("edit", "Text tools", "Templates, literal replacements and context tools", D.TextTools),
)

@Composable
internal fun AdvancedSettingsHub(onClickBack: () -> Unit, navigate: (String) -> Unit) = Workspace(
    "Manage brrBoard", onClickBack, navigate,
    WorkspaceEntry("update", "Updates & version history", "Published app versions", D.VersionHistory),
    WorkspaceEntry("backup", "Backup & restore", "Export and import settings", D.Backup),
    WorkspaceEntry("dictionary", "Resources & dictionaries", "Installed language resources", D.Dictionaries),
    WorkspaceEntry("bug_report", "Troubleshooting", "Privacy-safe diagnostic reports", D.Diagnostics),
    WorkspaceEntry("settings", "Compatibility", "Device-specific options", D.Compatibility),
    WorkspaceEntry("restart_alt", "Reset settings", "Review destructive actions", D.Reset),
    WorkspaceEntry("info", "About & licences", "brrBoard, setup and legal information", D.About),
)
