"""Privacy and crash-regression guard for production offline voice processing."""

from pathlib import Path
import unittest

ROOT = Path(__file__).resolve().parent.parent
SOURCE = ROOT / "app/src/main/java/helium314/keyboard"


class OfflineVoiceContractTest(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.production = (SOURCE / "latin/KeyboardPcmAudioSource.kt").read_text()
        cls.effects = (SOURCE / "latin/voice/VoiceCaptureNoiseReduction.kt").read_text()
        cls.bridge = (SOURCE / "latin/voice/BrrCleanVoiceBridge.kt").read_text()
        cls.child = (SOURCE / "latin/voice/BrrCleanVoiceService.kt").read_text()
        cls.native = (ROOT / "app/src/main/jni/brrclean/brrclean_jni.cpp").read_text()
        cls.dictation = (SOURCE / "latin/OfflineBoardDictation.kt").read_text()
        cls.pipeline = (SOURCE / "latin/voice/PrivateVoiceStreamingPipeline.kt").read_text()
        cls.manifest = (ROOT / "app/src/main/AndroidManifest.xml").read_text()
        cls.settings = (SOURCE / "settings/screens/FocusedSettingsScreens.kt").read_text()

    def test_normal_voice_uses_builtin_mic_and_optional_platform_effect(self):
        self.assertNotIn(".routedDevice", self.production)
        self.assertNotIn("getRoutedDevice(", self.production)
        self.assertNotIn("PrivateGtcrnDenoiser", self.production)
        self.assertIn("VoiceCaptureNoiseReduction.attach(context, microphone)", self.production)
        self.assertIn("VoiceNoiseChoice.isCleaningEnabled", self.settings)
        self.assertIn("VoiceNoiseChoice.setCleaningEnabled", self.settings)
        self.assertIn("NoiseSuppressor.isAvailable()", self.effects)
        self.assertIn("NoiseSuppressor.create(recorder.audioSessionId)", self.effects)

    def test_brrclean_isolated_fails_back_to_original_pcm(self):
        self.assertIn('android:process=":brr_clean_denoise"', self.manifest)
        self.assertIn('android:exported="false"', self.manifest)
        self.assertIn("PrivateVoiceStreamingPipeline", self.dictation)
        self.assertIn("onPcmFrame = { bytes, count -> pipeline.append(bytes, count) }", self.dictation)
        self.assertIn("streaming?.cancel()", self.dictation)
        self.assertIn("streaming?.finishCapture()", self.dictation)
        self.assertIn("VoiceNoiseChoice.eligible", self.pipeline)
        self.assertIn("BrrCleanVoiceBridge(context)", self.pipeline)
        self.assertIn("decode(accepted ?: raw)", self.pipeline)
        self.assertIn("activeDecode?.cancel()", self.pipeline)
        self.assertIn("VoiceTranscriptJoiner.merge(recognized)", self.pipeline)
        self.assertIn("PARENT_TIMEOUT_MS", self.bridge)
        self.assertIn("BrrCleanWire.CANCEL", self.bridge)
        self.assertIn("BrrCleanWire.CANCEL", self.child)
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

    def test_removed_model_ab_test_has_no_live_app_entrypoint(self):
        self.assertFalse((SOURCE / "latin/voice/VoiceNoiseLab.kt").exists())
        self.assertFalse((SOURCE / "settings/screens/VoiceNoiseLabScreen.kt").exists())
        self.assertFalse((SOURCE / "latin/voice/UltraVoiceTestService.kt").exists())
        self.assertNotIn("VoiceNoiseLabScreen", self.manifest)
        self.assertNotIn("VoiceNoiseLab", self.settings)
        self.assertNotIn("Compare voice models", self.settings)


if __name__ == "__main__":
    unittest.main()
