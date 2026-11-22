#!/usr/bin/env python3
"""Guardrails for the opt-in, offline Parakeet Ultra A/B test package."""
from pathlib import Path
import re
import subprocess
import unittest


ROOT = Path(__file__).resolve().parents[1]


class UltraVoicePackagingContractTest(unittest.TestCase):
    def read(self, path: str) -> str:
        return (ROOT / path).read_text()

    def test_ultra_pack_is_exactly_pinned_and_bounded(self):
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

    def test_large_pack_is_debug_artifact_only_and_never_released(self):
        release = self.read(".github/workflows/obtainium-release.yml")
        debug = self.read(".github/workflows/brrrboard-build-debug.yml")
        self.assertIn("github.ref_name == 'feature/parakeet-ultra-brain-abtest'", debug)
        self.assertIn("bash tools/prepare-ultra-voice-pack.sh", debug)
        self.assertIn("bash tools/prepare-offline-voice-pack.sh", debug)
        self.assertIn("needs: build", debug)
        self.assertIn("sign-ultra-abtest-apk.sh", debug)
        self.assertIn('verify-voice-model-packaging.sh "$output_apk" both',
                      self.read("tools/sign-ultra-abtest-apk.sh"))
        self.assertIn("actions/upload-artifact@v4", debug)
        self.assertIn("split -b 300M", debug)
        self.assertIn('cp \"$source_apk\" \"$artifacts/brrrBoard-ultra-ab-test.apk\"', debug)
        self.assertLess(debug.index("name: Prepare full APK and bounded download pieces"),
                        debug.index("name: Upload signed A/B test APK artifact only"))
        self.assertIn("part-3-of-3", debug)
        self.assertIn("brrrBoard-ultra-apk-${{ github.sha }}-part-1-of-3", debug)
        self.assertIn("brrrBoard-ultra-apk-${{ github.sha }}-part-2-of-3", debug)
        self.assertIn("brrrBoard-ultra-apk-${{ github.sha }}-part-3-of-3", debug)
        self.assertIn("bash tools/join-ultra-abtest-apk.sh", self.read("docs/BR_BRAIN_PARAKEET_AB_TEST_2026_11_22.md"))
        self.assertNotIn("gh release create", debug)
        self.assertNotIn("bundle_ultra_test_model", release)
        self.assertNotIn("prepare-ultra-voice-pack.sh", release)

    def test_model_comparison_is_same_take_offline_and_not_default(self):
        lab = self.read("app/src/main/java/helium314/keyboard/latin/voice/VoiceNoiseLab.kt")
        screen = self.read("app/src/main/java/helium314/keyboard/settings/screens/VoiceNoiseLabScreen.kt")
        service = self.read("app/src/main/java/helium314/keyboard/latin/voice/UltraVoiceTestService.kt")
        engine = self.read("app/src/main/java/helium314/keyboard/latin/voice/OfflineVoiceEngine.kt")
        self.assertIn("clips[key]?.copyOf()", lab)
        self.assertIn("decodeCurrentModel(pcm", lab)
        self.assertIn("decodeUltraModel(pcm", lab)
        self.assertIn("Compare both on same audio", screen)
        self.assertIn("installBundledIfPresent(context, UltraVoiceModelManifest.pack)", screen)
        self.assertIn('android:process=":brr_voice_ultra_test"', self.read("app/src/main/AndroidManifest.xml"))
        self.assertIn('modelType = "nemo_transducer"', engine)
        self.assertIn("peakPssKb", service)
        manifest = self.read("app/src/main/AndroidManifest.xml")
        self.assertIn('android.permission.INTERNET" tools:node="remove"', manifest)


    def test_chunk_joiner_checks_hash_before_creating_output(self):
        import hashlib
        import tempfile

        with tempfile.TemporaryDirectory() as tmp:
            root = Path(tmp)
            parts = root / "parts"
            parts.mkdir()
            values = [b"part-one", b"part-two", b"part-three"]
            for suffix, value in zip(("00", "01", "02"), values):
                (parts / f"brrrBoard-ultra-ab-test.apk.part-{suffix}").write_bytes(value)
            expected = hashlib.sha256(b"".join(values)).hexdigest()
            output = root / "reassembled.apk"
            command = ["bash", str(ROOT / "tools/join-ultra-abtest-apk.sh"),
                       str(parts), str(output), expected]
            checked = subprocess.run(command, capture_output=True, text=True, check=False)
            self.assertEqual(0, checked.returncode, checked.stderr)
            self.assertEqual(b"".join(values), output.read_bytes())

            output.unlink()
            command[-1] = "0" * 64
            rejected = subprocess.run(command, capture_output=True, text=True, check=False)
            self.assertNotEqual(0, rejected.returncode)
            self.assertFalse(output.exists())

    def test_scripts_have_valid_bash_syntax(self):
        for path in ("tools/prepare-ultra-voice-pack.sh",
                     "tools/prepare-offline-voice-pack.sh",
                     "tools/sign-ultra-abtest-apk.sh",
                     "tools/join-ultra-abtest-apk.sh",
                     "tools/verify-bundled-ultra-voice-model.sh",
                     "tools/verify-voice-model-packaging.sh"):
            checked = subprocess.run(["bash", "-n", str(ROOT / path)],
                                     capture_output=True, text=True, check=False)
            self.assertEqual(0, checked.returncode, checked.stderr)


if __name__ == "__main__":
    unittest.main()
