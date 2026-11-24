#!/usr/bin/env python3
"""Release guard: lightweight APKs allow only named auxiliary models, never ASR weights."""
from pathlib import Path
import subprocess
import tempfile
import unittest
from zipfile import ZipFile


ROOT = Path(__file__).resolve().parents[1]
CHECK = ROOT / "tools/verify-voice-model-packaging.sh"


class VoiceModelPackagingTest(unittest.TestCase):
    def check_apk(self, entries: dict[str, bytes], expected_code: int) -> None:
        with tempfile.TemporaryDirectory() as directory:
            apk = Path(directory) / "example.apk"
            with ZipFile(apk, "w") as archive:
                for name, data in entries.items():
                    archive.writestr(name, data)
            proc = subprocess.run(["bash", str(CHECK), str(apk), "unbundled"],
                                  capture_output=True, text=True, check=False)
            self.assertEqual(expected_code, proc.returncode, proc.stderr)

    def test_only_runtime_and_attribution_are_allowed(self):
        self.check_apk({"assets/voice-model-attribution.txt": b"attribution",
                        "lib/arm64-v8a/libsherpa-onnx-jni.so": b"native-runtime"}, 0)

    def test_only_exact_auxiliary_model_paths_are_allowed(self):
        self.check_apk({
            "assets/voice/gtcrn_simple.onnx": b"test",
            "assets/voice/silero_vad_v6_2_3.onnx": b"test",
            "assets/voice/smart-turn-v3.2-cpu.onnx": b"test",
        }, 0)
        self.check_apk({"assets/voice/smart-turn-v3.2-cpu-copy.onnx": b"test"}, 1)
        self.check_apk({"assets/other/silero_vad_v6_2_3.onnx": b"test"}, 1)

    def test_accidental_weight_file_is_rejected(self):
        self.check_apk({"assets/voice-model/parakeet-tdt-110m-en/encoder.onnx": b"weights"}, 1)

    def test_model_weights_outside_expected_directory_are_rejected(self):
        self.check_apk({"assets/another-model/encoder.onnx": b"weights"}, 1)


if __name__ == "__main__":
    unittest.main()
