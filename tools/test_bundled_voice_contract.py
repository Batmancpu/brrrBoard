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
        self.assertIn("microphone.setOnClickListener { feedback(it); ime.toggleBoardOfflineDictation() }", footer)
        self.assertIn("ime.startBoardOfflineDictation()", footer)
        self.assertIn("ime.stopBoardOfflineDictation()", footer)
        self.assertNotIn("ime.toggleBoardDictation()", footer)

        ime = self.read("app/src/main/java/helium314/keyboard/latin/LatinIME.java")
        match = re.search(r"void toggleBoardDictation\(\) \{([^}]*)\}", ime, re.S)
        self.assertIsNotNone(match)
        self.assertIn("toggleBoardOfflineDictation();", match.group(1))
        self.assertNotIn("mInlineDictation.toggle", match.group(1))
        self.assertIn("if (KeyCode.VOICE_INPUT == event.getKeyCode())", ime)

    def test_both_ci_apks_embed_verified_weights(self):
        prep = "bash tools/prepare-offline-voice-pack.sh"
        verify = "bash tools/verify-bundled-voice-model.sh"
        for path in (
            ".github/workflows/brrrboard-build-debug.yml",
            ".github/workflows/obtainium-release.yml",
        ):
            with self.subTest(path=path):
                workflow = self.read(path)
                self.assertIn(prep, workflow)
                self.assertIn(verify, workflow)
                self.assertNotIn("offline_model_bootstrap", workflow)

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
