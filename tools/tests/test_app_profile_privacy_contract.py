"""Guard the session-local app-profile rejection counter at privacy boundaries."""

from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[2]
LATIN_IME = ROOT / "app/src/main/java/helium314/keyboard/latin/LatinIME.java"
PROFILE_STORE = ROOT / "app/src/main/java/helium314/keyboard/latin/settings/AppProfileStore.kt"
PROFILE_WINDOW = ROOT / "app/src/main/java/helium314/keyboard/latin/settings/AppProfileRejectionWindow.kt"


class AppProfilePrivacyContractTest(unittest.TestCase):
    def test_ineligible_settings_clear_session_local_rejection_evidence(self):
        ime = LATIN_IME.read_text()
        load_settings = ime.partition("private void loadSettings()")[2].partition(
            "private void refreshPersonalizationDictionarySession"
        )[0]
        self.assertIn("if (!mayUseTransientCorrectionMemory(currentSettingsValues))", load_settings)
        self.assertIn("AppProfileStore.clearTransientRejectionEvidence()", load_settings)

        input_view = ime.partition("void onStartInputViewInternal(")[2].partition(
            "void onFinishInputViewInternal("
        )[0]
        settings_read = input_view.rindex("currentSettingsValues = mSettings.getCurrent()")
        privacy_clear = input_view.index("AppProfileStore.clearTransientRejectionEvidence()", settings_read)
        self.assertGreater(privacy_clear, settings_read)
        self.assertIn("if (!mayUseTransientCorrectionMemory(currentSettingsValues))",
                      input_view[settings_read:privacy_clear])

    def test_clear_only_drops_bounded_content_free_counter(self):
        store = PROFILE_STORE.read_text()
        window = PROFILE_WINDOW.read_text()
        self.assertIn("fun clearTransientRejectionEvidence() { rejectionWindow.clear() }", store)
        self.assertIn("private var count = 0", window)
        self.assertIn("fun clear() { epoch = Long.MIN_VALUE; started = 0; count = 0 }", window)
        self.assertNotIn("String", window)


if __name__ == "__main__":
    unittest.main()
