#!/usr/bin/env python3
"""Build-time regression checks: one mic, one private route, one complete APK."""
from pathlib import Path
import re
import unittest

ROOT = Path(__file__).resolve().parents[1]


class BundledOfflineMicContractTest(unittest.TestCase):
    def read(self, path: str) -> str:
        return (ROOT / path).read_text(encoding="utf-8")

    def test_every_mic_entrypoint_uses_offline_controller(self):
        footer = self.read("app/src/main/java/helium314/keyboard/latin/BoardFooterView.kt")
        self.assertIn("microphone.setOnClickListener {", footer)
        self.assertIn("ime.toggleBoardOfflineDictation()", footer)
        self.assertIn("ime.startBoardOfflineDictation()", footer)
        self.assertIn("ime.stopBoardOfflineDictation()", footer)
        self.assertNotIn("ime.toggleBoardDictation()", footer)

        ime = self.read("app/src/main/java/helium314/keyboard/latin/LatinIME.java")
        match = re.search(r"void toggleBoardDictation\(\) \{([^}]*)\}", ime, re.S)
        self.assertIsNotNone(match)
        self.assertIn("toggleBoardOfflineDictation();", match.group(1))
        self.assertNotIn("mInlineDictation.toggle", match.group(1))
        self.assertIn("if (KeyCode.VOICE_INPUT == event.getKeyCode())", ime)

    def test_cancel_animation_is_explicit_and_processing_has_no_visible_status(self):
        offline = self.read("app/src/main/java/helium314/keyboard/latin/OfflineBoardDictation.kt")
        footer = self.read("app/src/main/java/helium314/keyboard/latin/BoardFooterView.kt")
        ime = self.read("app/src/main/java/helium314/keyboard/latin/LatinIME.java")
        self.assertIn("Phase.DECODING -> cancelFromUser()", offline)
        self.assertIn("fun cancelFromUser() = finish(OfflineDictationExit.USER_CANCEL)", offline)
        self.assertIn("fun cancel() = finish(OfflineDictationExit.OTHER)", offline)
        self.assertIn("statusFooter?.clearDictationError()", offline)
        self.assertNotIn("showDictationStatus(R.string.board_voice_offline_processing)", offline)
        self.assertIn("FooterIndicatorPolicy.processingTint", footer)
        self.assertIn("fun showDictationDiscarded(wasProcessing:", footer)
        self.assertIn("FooterMicUnderlightView.Phase.RECORDING", footer)
        self.assertIn("FooterMicUnderlightView.Phase.PROCESSING", footer)
        self.assertIn("FooterMicMorphPolicy.DURATION_MS", footer)
        self.assertNotIn("microphone.background = micRing", footer)
        self.assertIn("micUnderlight.setPulse(phase)", footer)
        layout = self.read("app/src/main/res/layout/main_keyboard_frame.xml")
        self.assertIn("FooterMicDiscardMorphView", layout)
        self.assertIn("FooterMicUnderlightView", layout)
        self.assertIn('android:clipChildren="false"', layout)
        self.assertIn('android:enabled="false"', layout)
        self.assertIn("mOfflineDictation.cancelFromUser();", ime)
        # A normal tap during the dismissal morph interrupts it on DOWN and
        # starts a fresh dictation on the same UP. In incognito, a held mic
        # must instead enter the protected denial path, never auto-record.
        self.assertIn("micDiscardRestartTouchArmed = !isMicProtected()", footer)
        self.assertIn("if (micDiscardRestartTouchArmed)", footer)
        self.assertIn("if (protectedMicTouch ||", footer)
        self.assertLess(footer.index("if (protectedMicTouch ||"),
                        footer.index("if (FooterMicTapPolicy.isImmediate"))
        self.assertIn("clearMicDiscardFeedback()", footer)
        self.assertIn("view.performClick()", footer)

    def test_normal_apks_exclude_model_but_manual_release_can_bundle_it(self):
        prep = "bash tools/prepare-offline-voice-pack.sh"
        verify = "bash tools/verify-voice-model-packaging.sh"
        for path in (
            ".github/workflows/brrrboard-build-debug.yml",
            ".github/workflows/obtainium-release.yml",
        ):
            with self.subTest(path=path):
                workflow = self.read(path)
                self.assertIn(verify, workflow)
                self.assertNotIn("offline_model_bootstrap", workflow)
        debug = self.read(".github/workflows/brrrboard-build-debug.yml")
        release = self.read(".github/workflows/obtainium-release.yml")
        self.assertNotIn(prep, debug)
        self.assertIn(f'{verify} "$APK" unbundled', debug)
        self.assertIn('bundle_voice_model:', release)
        self.assertIn('default: false', release)
        self.assertIn("github.event_name == 'workflow_dispatch' && inputs.bundle_voice_model", release)
        self.assertIn(prep, release)
        self.assertIn(f'{verify} "$apk" "$BUNDLE_VOICE"', release)

        installer = self.read("app/src/main/java/helium314/keyboard/latin/voice/VoiceModelInstaller.kt")
        # No-build-weight updates must retain the pinned, already verified copy.
        self.assertLess(installer.index("if (isInstalled(context))"),
                        installer.index("val first = VoiceModelManifest.assets.first()"))

    def test_gtcrn_model_is_in_every_apk_with_private_fail_safe_processing(self):
        debug = self.read(".github/workflows/brrrboard-build-debug.yml")
        release = self.read(".github/workflows/obtainium-release.yml")
        for workflow in (debug, release):
            self.assertIn("bash tools/fetch-board-gtcrn.sh", workflow)
            self.assertIn("bash tools/verify-gtcrn-packaging.sh", workflow)
        fetch = self.read("tools/fetch-board-gtcrn.sh")
        verify = self.read("tools/verify-gtcrn-packaging.sh")
        parakeet_policy = self.read("tools/verify-voice-model-packaging.sh")
        self.assertIn("502ebfab64da7c4a9af78dcb9c6ceef1ebb01c73", fetch)
        self.assertIn("015454eef64bd4328e6ff4946c231d4a4a632e93", fetch)
        self.assertIn("015454eef64bd4328e6ff4946c231d4a4a632e93", verify)
        self.assertIn("assets/voice/gtcrn_simple.onnx", verify)
        self.assertIn("assets/licenses/gtcrn-mit.txt", verify)
        self.assertIn("grep -Fvx 'assets/voice/gtcrn_simple.onnx'", parakeet_policy)
        self.assertIn("grep -Eq '^assets/voice-model/'", parakeet_policy)
        license_text = self.read("app/src/main/assets/licenses/gtcrn-mit.txt")
        self.assertIn("MIT License", license_text)
        self.assertIn("Rong Xiaobin", license_text)

        enhancer = self.read("app/src/main/java/helium314/keyboard/latin/voice/PrivateGtcrnDenoiser.kt")
        recognizer = self.read("app/src/main/java/helium314/keyboard/latin/voice/OfflineVoiceEngine.kt")
        gate = self.read("app/src/main/java/helium314/keyboard/latin/voice/VoiceNoiseProfile.kt")
        dashboard = self.read("app/src/main/java/helium314/keyboard/settings/screens/BrainDashboardScreen.kt")
        mic = self.read("app/src/main/java/helium314/keyboard/latin/KeyboardPcmAudioSource.kt")
        self.assertIn("assetManager = context.assets", enhancer)
        # Emergency rollback: native denoiser is *not* initialized or called
        # from the live blue-mic path until ARM64 device tests prove stability.
        self.assertNotIn("PrivateGtcrnDenoiser(", recognizer)
        self.assertNotIn("enhanceIfNoisy(", recognizer)
        self.assertIn("shouldDenoise(samples: FloatArray)", gate)
        self.assertIn("GTCRN filtering paused", dashboard)
        self.assertNotIn("microphone.routedDevice", mic)
        self.assertNotIn("VoiceDenoiseMetrics.snapshot()", dashboard)
        self.assertNotIn("setPreferredDevice(", mic)
        for banned in ("http://", "https://", "HttpURLConnection", "Socket("):
            self.assertNotIn(banned, enhancer)
        # GTCRN is independent of the large optional Parakeet bootstrap.
        self.assertIn("default: false", release)

    def test_network_permissions_are_explicitly_removed(self):
        # Android's manifest merger can inherit a dependency's network permission.
        # The final signed-APK gate separately checks the merged permissions.
        import xml.etree.ElementTree as ET
        root = ET.fromstring(self.read("app/src/main/AndroidManifest.xml"))
        android_name = "{http://schemas.android.com/apk/res/android}name"
        tools_node = "{http://schemas.android.com/tools}node"
        forbidden = {"android.permission.INTERNET", "android.permission.ACCESS_NETWORK_STATE"}
        removed = {
            element.get(android_name)
            for element in root.findall("uses-permission")
            if element.get(tools_node) == "remove"
        }
        self.assertTrue(forbidden.issubset(removed))


if __name__ == "__main__":
    unittest.main()
