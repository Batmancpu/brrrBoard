#!/usr/bin/env python3
"""Keep experimental ContextLM training honest about unavailable user history."""
from pathlib import Path
import re
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

    def test_corpus_downloader_pins_revisions_and_full_hashes(self):
        source = (ROOT / "tools/contextlm/prepare_public_corpora.sh").read_text(encoding="utf-8")
        self.assertIn("0214cb358e59e36da60e01d3d59ddd24df897749", source)
        self.assertIn("e25a10382d9adb77f3a4f03b721fbd0a43c0de4c", source)
        hashes = re.findall(r"(?<![A-Za-z0-9])[a-f0-9]{64}(?![A-Za-z0-9])", source)
        self.assertGreaterEqual(len(hashes), 3)


if __name__ == "__main__":
    unittest.main()
