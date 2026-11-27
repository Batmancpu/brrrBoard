#!/usr/bin/env python3
"""Source-level smoke checks; Android compilation and signer validation run in CI."""
from pathlib import Path
import re
import xml.etree.ElementTree as ET

ROOT = Path(__file__).resolve().parents[1]
SETTINGS = ROOT / "app/src/main/java/helium314/keyboard/settings"
NAV = (SETTINGS / "SettingsNavHost.kt").read_text()
HOME = (SETTINGS / "screens/MainSettingsScreen.kt").read_text()
THEME = (SETTINGS / "SettingsBrandTheme.kt").read_text()
GLASS = (SETTINGS / "dialogs/FrostedGlassAdjustDialog.kt").read_text()
FOCUSED = (SETTINGS / "screens/FocusedSettingsScreens.kt").read_text()
SEARCH = (SETTINGS / "SettingsContainer.kt").read_text()
ABOUT = (SETTINGS / "screens/AboutScreen.kt").read_text()
RESOURCES = ROOT / "app/src/main/res/values/strings.xml"

ET.parse(RESOURCES)
for name in ("Keys", "Sound", "Clipboard", "Privacy", "Backup", "Voice"):
    assert re.search(rf'const val {name} = "', NAV), f"Missing destination: {name}"
    assert f"SettingsDestination.{name}) {{" in NAV, f"Missing navigation: {name}"
    assert f"onClick{name} = " in NAV, f"Missing home callback: {name}"
    assert f"onClick{name}: () -> Unit" in HOME, f"Missing home parameter: {name}"

assert "SettingsBrandTheme {" in (SETTINGS / "SettingsActivity.kt").read_text()
assert "dynamicLightColorScheme" not in THEME
assert "Color(0xFF2D60C8)" in THEME and "Color(0xFF89ACED)" in THEME
assert "if (!isImeReady)" in HOME and "isThisImeCurrent" in HOME
assert not re.search(r"(?m)^\s*QuickSetupCard\(", HOME), "Finished-setup card still invoked"
assert "brandIcon = true" in HOME and "mangoloads_brand_small" in HOME
assert "SettingsWithoutKey.SETUP_WIZARD" in ABOUT
assert "Settings.PREF_SECURE_APPS" in FOCUSED and "Settings.PREF_ENABLE_CLIPBOARD_HISTORY" in FOCUSED
ADAPTERS = (SETTINGS / "screens/FunctionalControlAdapters.kt").read_text()
assert "Settings.PREF_SOUND_ON" in ADAPTERS and "Settings.PREF_VIBRATE_ON" in ADAPTERS
assert "SettingsWithoutKey.BACKUP_RESTORE" in FOCUSED
for name in ("KeyboardHub", "InteractionsHub", "ToolsHub", "GlassEditor", "Size", "Correction", "Suggestions", "Writing", "TextTools", "Reset", "Diagnostics", "Compatibility"):
    assert f"composable(SettingsDestination.{name})" in NAV, name
assert "livePreviewHighlightStyle = selectedHighlightStyle" in GLASS
assert "KeyboardTheme.livePreviewHighlightStyle = null" in GLASS
assert "WorkspaceSettings.destination(key, originalRouteFor(key))" in SEARCH
assert "fun breadcrumbFor" in SEARCH and "fun routeFor" in SEARCH
assert "import helium314.keyboard.latin.R" in SEARCH, "Resource ID use requires generated R import"
assert "inJustDecodeBounds = true" in GLASS and "inSampleSize = sample" in GLASS
assert "if (previewTouched || isSaved) requestFinalFrostedSync()" in GLASS
assert "writeSnapshotToPrefs(initialSnapshot)" not in GLASS
SCREEN = (SETTINGS / "SearchScreen.kt").read_text()
assert "private val searchIndex by lazy" in SEARCH
assert "settingsSearchRank(" in SEARCH
assert "withContext(Dispatchers.Default) { filter(requested) }" in SCREEN
assert "query == requested" in SCREEN and "vaultDeadline?.value ?: 0L" in SCREEN
assert "if (useHaze)" in SCREEN and "imeInsets.getBottom(density) > 0" in SCREEN
assert "itemKey = { it.key }" in SCREEN
VISUAL = (SETTINGS / "SettingsVisuals.kt").read_text()
ROW = (SETTINGS / "preferences/Preference.kt").read_text()
SWITCH = (SETTINGS / "preferences/SettingsSwitch.kt").read_text()
SWITCH_PREF = (SETTINGS / "preferences/SwitchPreference.kt").read_text()
SLIDER = (SETTINGS / "preferences/SettingsSlider.kt").read_text()
CHOICE = (SETTINGS / "preferences/SettingsChoiceControls.kt").read_text()
TRYOUT = (SETTINGS / "screens/KeyboardTryoutCard.kt").read_text()
PICKER = (SETTINGS / "dialogs/ListPickerDialog.kt").read_text()
MULTI = (SETTINGS / "dialogs/MultiListPickerDialog.kt").read_text()
for source in (HOME, SCREEN, ROW, THEME, TRYOUT):
    assert "SettingsVisuals.groupRadius" in source, "Shared group geometry required"
assert "val controlTarget = 48.dp" in VISUAL
assert "val rowHeight = 52.dp" in VISUAL
assert "Brush.verticalGradient" not in HOME.split("private fun QuickSetupCard")[0]
assert 'listOf("QWERTYUIOP"' not in TRYOUT
assert "NO_PERSONALIZED_LEARNING" in TRYOUT and "NO_MICROPHONE" in TRYOUT
assert "Role.Switch" in ROW and "toggleState = value" in SWITCH_PREF
assert "interactionSource = interaction" in SWITCH_PREF and "onCheckedChange = null" in SWITCH_PREF
assert "Role.Switch" in SWITCH and "getTransitionAnimationScale" in SWITCH
assert "Modifier.size(width = 36.dp, height = 32.dp)" in SLIDER
assert "Slider(value = value" in SLIDER and "LayoutDirection.Rtl" in SLIDER
assert "getTransitionAnimationScale" in SLIDER and "steps = steps" in SLIDER
assert "selectableGroup()" in CHOICE and "Role.Tab" in CHOICE
assert "SettingsChoiceMark" in PICKER and "Role.RadioButton" in PICKER
assert "Role.Checkbox" in MULTI and "Checkbox(checked = item in selected" in MULTI
assert "SettingsSegments" in GLASS
print("Premium Settings source contract: PASS")
