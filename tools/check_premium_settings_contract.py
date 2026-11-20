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
assert "Settings.PREF_SOUND_ON" in FOCUSED and "SettingsWithoutKey.BACKUP_RESTORE" in FOCUSED
assert "fun breadcrumbFor" in SEARCH and "fun routeFor" in SEARCH
assert "inJustDecodeBounds = true" in GLASS and "inSampleSize = sample" in GLASS
assert "if (previewTouched || isSaved) requestFinalFrostedSync()" in GLASS
assert "writeSnapshotToPrefs(initialSnapshot)" not in GLASS
print("Premium Settings source contract: PASS")
