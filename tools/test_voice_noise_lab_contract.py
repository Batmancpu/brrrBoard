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
        cls.choice = (SOURCE / "latin/voice/VoiceNoiseMode.kt").read_text()
        cls.bridge = (SOURCE / "latin/voice/BrrCleanVoiceBridge.kt").read_text()
        cls.child = (SOURCE / "latin/voice/BrrCleanVoiceService.kt").read_text()
        cls.native = (ROOT / "app/src/main/jni/brrclean/brrclean_jni.cpp").read_text()
        cls.dictation = (SOURCE / "latin/OfflineBoardDictation.kt").read_text()

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
        self.assertIn("VoiceNoiseChoice.selected(context) == VoiceNoiseMode.ANDROID", self.effects)
        self.assertIn("attachForLab", self.effects)
        self.assertNotIn("setEnabled(context,", self.lab)
        self.assertIn("VoiceLabScore.wordErrorPercent", self.lab)

    def test_one_take_two_engines_and_fail_closed_silent_bluetooth(self):
        self.assertIn("VoiceNoiseChoice.select(context, mode)", self.ui)
        self.assertIn("VoiceNoiseChoice.selected(context)", self.ui)
        self.assertIn("Original and brrClean use identical captured audio", self.ui)
        self.assertIn("enhancedClips[key]", self.lab)
        self.assertIn("Handler(Looper.getMainLooper()).postDelayed", self.lab)
        self.assertIn("if (!audible)", self.lab)
        self.assertIn("activeRouteDoesNotMatch", self.lab)
        self.assertIn("isClientSilenced", self.lab)
        self.assertIn("check(device != null)", self.lab)
        self.assertIn("VoiceNoiseMode.ANDROID", self.choice)
        self.assertIn("VoiceNoiseMode.GTCRN", self.choice)
        self.assertIn("VoiceNoiseMode.OFF", self.choice)
        self.assertIn('"✓ OFF"', self.ui)
        self.assertIn("val enhance: Boolean get() = mode == VoiceNoiseMode.GTCRN", self.lab)
        self.assertIn("if (key.enhance) Handler(Looper.getMainLooper()).postDelayed", self.lab)
        self.assertIn("if (take.key.enhance) OutlinedButton", self.ui)
        self.assertIn("setCommunicationDevice(communication)", self.lab)
        self.assertIn("clearCommunicationDevice()", self.lab)
        self.assertIn("MODE_IN_COMMUNICATION", self.lab)
        self.assertIn("VOICE_COMMUNICATION", self.lab)

    def test_gtcrn_child_and_raw_dictation_fallback(self):
        self.assertIn('android:process=":brr_clean_denoise"',
            (ROOT / "app/src/main/AndroidManifest.xml").read_text())
        self.assertIn('android:exported="false"',
            (ROOT / "app/src/main/AndroidManifest.xml").read_text())
        self.assertIn("VoiceNoiseChoice.eligible", self.dictation)
        self.assertIn("decodeCaptured(accepted ?: original", self.dictation)
        self.assertIn("denoiseBridge?.cancel()", self.dictation)
        self.assertIn("PARENT_TIMEOUT_MS", self.bridge)
        self.assertIn("pending?.fill(0)", self.bridge)
        self.assertIn("Process.killProcess(Process.myPid())", self.child)
        self.assertIn("THREAD_PRIORITY_BACKGROUND", self.child)
        self.assertIn('System.loadLibrary("brrclean")',
            (SOURCE / "latin/voice/BrrCleanNative.kt").read_text())
        self.assertIn("gtcrn_run(model.get(), mix, clean)", self.native)
        self.assertIn("stft.computeSTFT(frame, real, imag)", self.native)
        self.assertNotIn("BrrCleanNative", self.production)
        for source in (self.bridge, self.child):
            for forbidden in ("HttpURLConnection", "Socket(", "FileOutputStream", "AudioRecord("):
                self.assertNotIn(forbidden, source)


if __name__ == "__main__":
    unittest.main()
