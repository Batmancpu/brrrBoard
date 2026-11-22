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

    def test_normal_apks_exclude_models_but_ab_test_app_bundles_both(self):
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
        self.assertIn(prep, debug)
        self.assertIn(f'{verify} "$APK" unbundled', debug)
        self.assertIn("ultra_ab_test_artifact:", debug)
        self.assertIn("needs: build", debug)
        self.assertIn("bash tools/prepare-ultra-voice-pack.sh", debug)
        signer = self.read("tools/sign-ultra-abtest-apk.sh")
        self.assertIn(f'{verify} "$output_apk" both', signer)
        self.assertIn("actions/upload-artifact@v4", debug)
        self.assertNotIn("gh release create", debug)
        self.assertIn('bundle_voice_model:', release)
        self.assertIn('bundle_ultra_model:', release)
        self.assertIn('default: false', release)
        self.assertIn("inputs.bundle_voice_model", release)
        self.assertIn("inputs.bundle_ultra_model", release)
        self.assertIn("tools/offline-ultra-voice-model.lock", release)
        self.assertIn(prep, release)
        self.assertIn("bash tools/prepare-ultra-voice-pack.sh", release)
        self.assertIn("BUNDLE_MODE", release)
        self.assertIn(f'{verify} "$apk" "$BUNDLE_MODE"', release)

        installer = self.read("app/src/main/java/helium314/keyboard/latin/voice/VoiceModelInstaller.kt")
        # No-build-weight updates must retain the pinned, already verified copy.
        self.assertLess(installer.index("if (isInstalled(context, pack))"),
                        installer.index("hasBundled(appContext, pack)"))

    def test_gtcrn_model_is_in_every_apk_with_private_fail_safe_processing(self):
        debug = self.read(".github/workflows/brrrboard-build-debug.yml")
        release = self.read(".github/workflows/obtainium-release.yml")
        for workflow in (debug, release):
            self.assertIn("bash tools/fetch-board-gtcrn.sh", workflow)
            self.assertIn("bash tools/verify-gtcrn-packaging.sh", workflow)
            self.assertIn("bash tools/fetch-board-brrclean.sh", workflow)
            self.assertIn("bash tools/verify-brrclean-packaging.sh", workflow)
        native_fetch = self.read("tools/fetch-board-brrclean.sh")
        native_verify = self.read("tools/verify-brrclean-packaging.sh")
        self.assertIn("2014cd0d06b2effa24b1ab1c95013f785f38dc80", native_fetch)
        self.assertIn("4db48ff819e5ac18f5ccad1dfdce0968dde1a209c0cbbf9f24ba9dc4d9f48ded", native_verify)
        self.assertIn("libbrrclean", native_verify)
        self.assertIn("Copyright (c) 2026 chabandou",
            self.read("app/src/main/assets/licenses/brrclean-poise-mit.txt"))
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
        voice_settings = self.read("app/src/main/java/helium314/keyboard/settings/screens/FocusedSettingsScreens.kt")
        mic = self.read("app/src/main/java/helium314/keyboard/latin/KeyboardPcmAudioSource.kt")
        self.assertIn("assetManager = context.assets", enhancer)
        # Native denoiser must still never initialize in LatinIME's shared
        # decoder process; optional GTCRN is now isolated in a child service.
        self.assertNotIn("PrivateGtcrnDenoiser(", recognizer)
        self.assertNotIn("enhanceIfNoisy(", recognizer)
        self.assertIn("shouldDenoise(samples: FloatArray)", gate)
        self.assertIn("Voice cleaning · brrClean", voice_settings)
        self.assertIn("VoiceNoiseChoice.isCleaningEnabled", voice_settings)
        self.assertIn("VoiceNoiseChoice.setCleaningEnabled", voice_settings)
        self.assertNotIn("brrClean native GTCRN (isolated process)", dashboard)
        self.assertNotIn("microphone.routedDevice", mic)
        # Both platform effect state and actual native success/fallback counters
        # are observable; only the service callback may count native successes.
        self.assertNotIn("voiceNoise.platformNoiseSuppression", dashboard)
        self.assertNotIn("voiceNoise.enhancedWindows", dashboard)
        self.assertIn("VoiceDenoiseMetrics.enhanced", self.read(
            "app/src/main/java/helium314/keyboard/latin/voice/BrrCleanVoiceBridge.kt"))
        # Only the phone's built-in mic may be preferred on the live keyboard.
        # BT communication routing stays in the optional lab; routedDevice
        # (the old ColorOS native crash suspect) remains forbidden.
        self.assertEqual(1, mic.count("setPreferredDevice("))
        for banned in ("http://", "https://", "HttpURLConnection", "Socket("):
            self.assertNotIn(banned, enhancer)
        # GTCRN is independent of the large optional Parakeet bootstrap.
        self.assertIn("default: false", release)

    def test_gtcrn_one_shot_is_private_explicit_and_process_separated(self):
        import xml.etree.ElementTree as ET
        root = ET.fromstring(self.read("app/src/main/AndroidManifest.xml"))
        android_name = "{http://schemas.android.com/apk/res/android}name"
        android_process = "{http://schemas.android.com/apk/res/android}process"
        android_exported = "{http://schemas.android.com/apk/res/android}exported"
        services = root.findall("application/service")
        child = next(s for s in services if s.get(android_name) ==
                     "helium314.keyboard.latin.voice.GtcrnProbeService")
        self.assertEqual(":brr_voice_probe", child.get(android_process))
        self.assertEqual("false", child.get(android_exported))

        report = self.read("app/src/main/java/helium314/keyboard/latin/voice/VoiceNativeCrashProbe.kt")
        child_code = self.read("app/src/main/java/helium314/keyboard/latin/voice/GtcrnProbeService.kt")
        dashboard = self.read("app/src/main/java/helium314/keyboard/settings/screens/BrainDashboardScreen.kt")
        checkup = self.read("app/src/main/java/helium314/keyboard/settings/screens/DeviceCheckupScreen.kt")
        self.assertIn("noBackupFilesDir", report)
        self.assertIn("getHistoricalProcessExitReasons", report)
        self.assertIn("AtomicFile", report)
        self.assertIn('":brr_voice_probe"', report)
        self.assertIn("Process.killProcess(Process.myPid())", child_code)
        self.assertIn("THREAD_PRIORITY_BACKGROUND", child_code)
        self.assertIn("MAX_PROBE_DURATION_MS", child_code)
        self.assertIn("MODE_IN_COMMUNICATION", report)
        self.assertIn("FloatArray(16_000)", child_code)
        self.assertIn("Seven checks · one report", checkup)
        self.assertNotIn("Run one private check", dashboard)
        self.assertNotIn("Copy safe result", dashboard)
        # Never ingest a microphone or dump/tombstone/stack raw payload.
        for bad in ("getTraceInputStream(", "getDescription(", "readAudio(", "MediaRecorder",
                    "AudioRecord(", "recording.wav", "HttpURLConnection", "Socket("):
            self.assertNotIn(bad, report)
            self.assertNotIn(bad, child_code)
        self.assertNotIn("GET_TASKS", self.read("app/src/main/AndroidManifest.xml"))

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

    def test_owner_armed_blue_voice_diagnostic_records_only_fixed_stage_codes(self):
        report = self.read("app/src/main/java/helium314/keyboard/latin/voice/VoiceSessionCrashTrail.kt")
        controller = self.read("app/src/main/java/helium314/keyboard/latin/OfflineBoardDictation.kt")
        model = self.read("app/src/main/java/helium314/keyboard/latin/voice/OfflineVoiceEngine.kt")
        dashboard = self.read("app/src/main/java/helium314/keyboard/settings/screens/BrainDashboardScreen.kt")
        self.assertIn('"voice-session-one-shot-v1.state"', report)
        self.assertIn("noBackupFilesDir", report)
        self.assertIn("getHistoricalProcessExitReasons", report)
        self.assertIn("AtomicFile", report)
        self.assertNotIn("VoiceSessionCrashTrail.arm(context)", dashboard)
        self.assertIn("VoiceSessionCrashTrail.begin(ime)", controller)
        self.assertIn('"ASR_LOADING"', model)
        self.assertIn('"ASR_READY"', model)
        self.assertIn('"ASR_DECODING"', model)
        self.assertIn("MAX_STEPS = 20", report)
        for bad in ("getTraceInputStream", "getDescription(", "getTextBeforeCursor(",
                    "AudioRecord(", "MediaRecorder", "HttpURLConnection", "Socket(",
                    "getStackTrace(", "android.util.Log"):
            self.assertNotIn(bad, report)

    def test_noise_suppression_is_local_and_never_calls_gtcrn_native_from_capture(self):
        source = self.read("app/src/main/java/helium314/keyboard/latin/KeyboardPcmAudioSource.kt")
        effect = self.read("app/src/main/java/helium314/keyboard/latin/voice/VoiceCaptureNoiseReduction.kt")
        dashboard = self.read("app/src/main/java/helium314/keyboard/settings/screens/BrainDashboardScreen.kt")
        self.assertIn("VOICE_RECOGNITION", source)
        self.assertIn("VoiceCaptureNoiseReduction.attach(context, microphone)", source)
        self.assertIn("suppression?.release()", source)
        self.assertIn("AudioDeviceInfo.TYPE_BUILTIN_MIC", source)
        self.assertIn("microphone.setPreferredDevice(phone)", source)
        self.assertIn("Android routed offline dictation away from phone mic", source)
        self.assertNotIn("AudioDeviceInfo.TYPE_BLUETOOTH_SCO", source)
        self.assertIn("NoiseSuppressor.isAvailable()", effect)
        self.assertIn("NoiseSuppressor.create(recorder.audioSessionId)", effect)
        self.assertIn("MODE_IN_COMMUNICATION", effect)
        voice_settings = self.read("app/src/main/java/helium314/keyboard/settings/screens/FocusedSettingsScreens.kt")
        self.assertIn("Voice cleaning · brrClean", voice_settings)
        self.assertNotIn("Current blue-mic engine:", dashboard)
        self.assertIn("brrClean", self.read("app/src/main/java/helium314/keyboard/settings/screens/VoiceNoiseLabScreen.kt"))
        self.assertNotIn("PrivateGtcrnDenoiser", source)
        self.assertNotIn("PrivateGtcrnDenoiser", effect)


if __name__ == "__main__":
    unittest.main()
