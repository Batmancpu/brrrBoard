#!/usr/bin/env python3
"""Keep experimental ContextLM training honest about unavailable user history."""
from pathlib import Path
import re
import json
import hashlib
import unittest


ROOT = Path(__file__).resolve().parent.parent


class ContextLmTrainingContract(unittest.TestCase):
    def test_public_corpus_training_has_no_fabricated_personal_signals(self):
        source = (ROOT / "tools/contextlm/train_dfsmn.py").read_text(encoding="utf-8")
        self.assertIn("row = [[pseudo_native[i], 1. / (ranks[i] + 1), 0., 0.]", source)
        self.assertIn("Public corpora contain no consented per-user history", source)
        self.assertNotIn("rng.random() < (.29 if i == pos else .04)", source)
        self.assertNotIn("rng.random() < (.13 if i == pos else .02)", source)

    def test_proxy_evaluator_never_claims_native_quality_gate_pass(self):
        source = (ROOT / "tools/contextlm/evaluate_candidate_recall.py").read_text(encoding="utf-8")
        self.assertIn('"evidence": "CORPUS_NGRAM_PROXY_NOT_NATIVE_IME"', source)
        self.assertIn('"quality_gate_passed": False', source)

    def test_training_does_not_hide_true_target_from_candidate_proxy(self):
        source = (ROOT / "tools/contextlm/train_dfsmn.py").read_text(encoding="utf-8")
        candidate_builder = source.split("def minibatch(", 1)[1].split("def evaluate(", 1)[0]
        self.assertIn("native_proxy_candidates(", candidate_builder)
        self.assertNotIn("exclude_target=target", candidate_builder)

    def test_corpus_downloader_pins_revisions_and_full_hashes(self):
        source = (ROOT / "tools/contextlm/prepare_public_corpora.sh").read_text(encoding="utf-8")
        self.assertIn("0214cb358e59e36da60e01d3d59ddd24df897749", source)
        self.assertIn("e25a10382d9adb77f3a4f03b721fbd0a43c0de4c", source)
        hashes = re.findall(r"(?<![A-Za-z0-9])[a-f0-9]{64}(?![A-Za-z0-9])", source)
        self.assertGreaterEqual(len(hashes), 3)

    def test_experimental_model_is_separate_from_production_approval(self):
        source = (ROOT / "tools/contextlm/train_dfsmn.py").read_text(encoding="utf-8")
        self.assertIn('"status": "EXPERIMENTAL_DEVICE_TEST_ONLY"', source)
        self.assertIn('"quality_gate_passed": False', source)
        self.assertIn('"do_not_use_for_autocorrection": True', source)
        self.assertIn('"apply_only_to_manual_suggestions": True', source)
        runtime = (ROOT / "app/src/main/java/helium314/keyboard/latin/brain/BrContextLmInt8.java").read_text(encoding="utf-8")
        self.assertIn("loadExperimentalForTesting", runtime)
        self.assertIn("allowExperimental ? !experimental : !approved", runtime)

    def test_packaged_candidate_is_small_hash_checked_and_quality_gated(self):
        assets = ROOT / "app/src/main/assets/contextlm/experimental"
        manifest = json.loads((assets / "manifest.json").read_text(encoding="utf-8"))
        weights = (assets / "weights.int8").read_bytes()
        vocab = (assets / "vocab.json").read_bytes()
        self.assertEqual("EXPERIMENTAL_DEVICE_TEST_ONLY", manifest["status"])
        self.assertFalse(manifest["quality_gate_passed"])
        self.assertTrue(manifest["do_not_use_for_autocorrection"])
        self.assertTrue(manifest["apply_only_to_manual_suggestions"])
        self.assertEqual(manifest["weights_sha256"], hashlib.sha256(weights).hexdigest())
        self.assertEqual(manifest["vocab_sha256"], hashlib.sha256(vocab).hexdigest())
        self.assertLess(len(weights) + len(vocab) + (assets / "manifest.json").stat().st_size, 2_000_000)

    def test_live_suggestion_use_is_opt_in_private_and_never_autocorrects(self):
        suggest = (ROOT / "app/src/main/java/helium314/keyboard/latin/Suggest.kt").read_text(encoding="utf-8")
        self.assertIn("!hasAutoCorrection", suggest)
        self.assertIn("!currentSettings.mInputAttributes.mNoLearning", suggest)
        self.assertIn("mayUseEmojiContext", suggest)
        engine = (ROOT / "app/src/main/java/helium314/keyboard/latin/brain/BrContextLmExperiment.java").read_text(encoding="utf-8")
        self.assertIn("getBoolean(PREF_ENABLED, false)", engine)
        self.assertIn("MAX_CANDIDATES", engine)
        self.assertIn("ime.editorGeneration() != requestEditorGeneration", engine)
        self.assertNotIn("FileOutputStream", engine)


if __name__ == "__main__":
    unittest.main()
