#!/usr/bin/env python3
"""Guardrails that keep the optional Ultra model out of ordinary updates."""
from pathlib import Path
import re
import subprocess
import unittest

ROOT = Path(__file__).resolve().parents[1]


class UltraVoicePackagingContractTest(unittest.TestCase):
    def read(self, path: str) -> str:
        return (ROOT / path).read_text(encoding="utf-8")

    def test_ultra_pack_remains_available_and_exactly_pinned(self):
        lock = self.read("tools/offline-ultra-voice-model.lock")
        entries = [line.split("|") for line in lock.splitlines()
                   if line and not line.startswith("#")]
        self.assertEqual(["encoder.onnx", "decoder.onnx", "joiner.onnx", "tokens.txt"],
                         [entry[1] for entry in entries])
        self.assertTrue(all(len(entry) == 4 and re.fullmatch(r"[a-f0-9]{64}", entry[3])
                            for entry in entries))
        self.assertEqual(670_576_835, sum(int(entry[2]) for entry in entries))
        fetch = self.read("tools/prepare-ultra-voice-pack.sh")
        self.assertIn("402e1190ce85b9793118ee223db21fb0d12f52db", fetch)
        self.assertIn("sha256sum -c", fetch)
        self.assertIn("curl -fSL", fetch)

    def test_no_ab_test_or_model_staging_job_exists(self):
        debug = self.read(".github/workflows/brrrboard-build-debug.yml")
        release = self.read(".github/workflows/obtainium-release.yml")
        manifest = self.read("app/src/main/AndroidManifest.xml")
        settings = self.read("app/src/main/java/helium314/keyboard/settings/screens/FocusedSettingsScreens.kt")
        for forbidden in ("ultra_ab_test_artifact", "sign-ultra-abtest-apk.sh",
                          "join-ultra-abtest-apk.sh", "split -b 300M",
                          "prepare-ultra-voice-pack.sh"):
            self.assertNotIn(forbidden, debug)
        self.assertNotIn("UltraVoiceTestService", manifest)
        self.assertNotIn("Compare voice models", settings)
        self.assertNotIn("VoiceNoiseLab", settings)
        self.assertIn("prepare-ultra-voice-pack.sh", release)
        self.assertIn("inputs.bundle_ultra_model", release)
        self.assertRegex(release, r"bundle_ultra_model:[\s\S]{0,180}default: false")
        self.assertRegex(release, r"if: steps\.release_scope\.outputs\.bundle_ultra == 'true'[\s\S]{0,120}prepare-ultra-voice-pack\.sh")
        push_scope = release.split('changed_files="$(git diff', 1)[1].split("          fi\n          if [[ \"$bundle_ultra\"", 1)[0]
        self.assertNotIn("bundle_ultra=true", push_scope)
        self.assertNotIn("offline-ultra-voice-model.lock", push_scope)

    def test_ultra_remains_keyboard_default_when_installed_and_normal_update_is_lightweight(self):
        choice = self.read("app/src/main/java/helium314/keyboard/latin/voice/VoiceModelChoice.kt")
        installer = self.read("app/src/main/java/helium314/keyboard/latin/voice/VoiceModelInstaller.kt")
        self.assertIn("available.firstOrNull { it.id == SELECTED_ULTRA }", choice)
        self.assertIn("if (isInstalled(context, pack))", installer)
        self.assertLess(installer.index("if (isInstalled(context, pack)"),
                        installer.index("hasBundled(appContext, pack)"))
        verify = self.read("tools/verify-voice-model-packaging.sh")
        self.assertIn("Verified no English ASR model weights", verify)

    def test_scripts_have_valid_bash_syntax(self):
        for path in ("tools/prepare-ultra-voice-pack.sh",
                     "tools/verify-bundled-ultra-voice-model.sh",
                     "tools/verify-voice-model-packaging.sh"):
            checked = subprocess.run(["bash", "-n", str(ROOT / path)],
                                     capture_output=True, text=True, check=False)
            self.assertEqual(0, checked.returncode, checked.stderr)


if __name__ == "__main__":
    unittest.main()
