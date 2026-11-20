"""Static privacy and crash-regression guard for the opt-in BR Brain microphone lab."""

from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / "app/src/main/java/helium314/keyboard"


class VoiceNoiseLabContractTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.lab = (SOURCE / "latin/voice/VoiceNoiseLab.kt").read_text()
        cls.ui = (SOURCE / "settings/screens/VoiceNoiseLabScreen.kt").read_text()
        cls.production = (SOURCE / "latin/KeyboardPcmAudioSource.kt").read_text()
        cls.effects = (SOURCE / "latin/voice/VoiceCaptureNoiseReduction.kt").read_text()

    def test_live_keyboard_does_not_reintroduce_suspected_route_probe(self):
        self.assertNotIn(".routedDevice", self.production)
        self.assertNotIn("getRoutedDevice(", self.production)
        self.assertNotIn("PrivateGtcrnDenoiser", self.production)
        self.assertIn("VoiceCaptureNoiseReduction.attach(context, microphone)", self.production)

    def test_private_and_bounded_lab(self):
        self.assertIn("MAX_SECONDS = 12", self.lab)
        self.assertIn("VoiceCaptureNoiseReduction.callIsActive", self.lab)
        self.assertIn("clips.values.forEach { it.fill(0) }", self.lab)
        self.assertIn("DisposableEffect(lab) { onDispose { lab.close() } }", self.ui)
        self.assertIn("AudioManager.GET_DEVICES_INPUTS", self.lab)
        self.assertIn("activeRecordingConfigurations", self.lab)
        self.assertNotIn("getRoutedDevice(", self.lab)
        for forbidden in ["FileOutputStream", "FileInputStream", "SharedPreferences",
                          "HttpURLConnection", "Socket(", "Log.", "ClipboardManager"]:
            self.assertNotIn(forbidden, self.lab)

    def test_lab_toggle_is_independent_of_production_default(self):
        self.assertIn("getBoolean(PREF_ENABLED, true)", self.effects)
        self.assertIn("attachForLab", self.effects)
        self.assertNotIn("setEnabled(context,", self.lab)
        self.assertIn("VoiceLabScore.wordErrorPercent", self.lab)


if __name__ == "__main__":
    unittest.main()
