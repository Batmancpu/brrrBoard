#!/usr/bin/env python3
"""B16A synthetic-only regression tests for the existing offline seed builder.

These test deterministic data handling and holdout isolation; they do *not*
claim actual autocomplete accuracy, latency or Oppo phone acceptance.
"""
import tempfile
import unittest
from pathlib import Path

from prepare_brrpersonal import (
    MANUAL_RULE_FREQUENCY,
    build_seed_entries,
    find_columns,
    read_reviewed_social,
    read_rules,
)


class BrrPersonalSeedTests(unittest.TestCase):
    def test_pinned_source_headers_and_schema_variants(self):
        self.assertEqual(
            ("Sentence", "English translation"),
            find_columns(["Sentence", "English translation", "unused"]),
        )
        self.assertEqual(
            ("Code Mixed", "English"),
            find_columns(["Code Mixed", "English"]),
        )
        with self.assertRaises(ValueError):
            find_columns(["unknown", "more unknown"])

    def test_generator_determinism_and_frequency_caps(self):
        rows = [
            {"Sentence": "yaar kal", "English": "friend tomorrow"},
            {"Sentence": "yaar kal", "English": "friend tomorrow"},
            {"Sentence": "bhai yaar", "English": "bro friend"},
            {"Sentence": "bhai yaar", "English": "bro friend"},
            {"Sentence": "bhai yaar", "English": "bro friend"},
        ]
        rules = {"bhai": "😎", "slay": "💅"}
        forward = build_seed_entries(rows, "Sentence", "English", rules, max_entries=4)
        backward = build_seed_entries(reversed(rows), "Sentence", "English", rules, max_entries=4)
        self.assertEqual(forward, backward)
        entries, source_rows, candidate_count = forward
        self.assertEqual(5, source_rows)
        self.assertEqual(3, candidate_count)
        self.assertEqual((5, ""), entries["yaar"])
        self.assertEqual((MANUAL_RULE_FREQUENCY, "😎"), entries["bhai"])
        self.assertEqual((MANUAL_RULE_FREQUENCY, "💅"), entries["slay"])
        self.assertEqual(3, len(entries))

        capped, _, _ = build_seed_entries(
            [{"Sentence": "glow " * 300, "English": "friend"}],
            "Sentence", "English", {},
        )
        self.assertEqual((255, ""), capped["glow"])

    def test_english_function_words_and_translation_leakage_excluded(self):
        rows = [{"h": "the is okay okay mast mast", "e": "okay okay right good"}] * 2
        entries, _, _ = build_seed_entries(rows, "h", "e", {})
        self.assertEqual({"mast"}, set(entries))

    def test_training_rows_never_consume_holdout_terms(self):
        training = [{"h": "bhai bhai", "e": "brother brother"}]
        holdout = [{"h": "inventedholdout inventedholdout", "e": "sample sample"}]
        entries, _, _ = build_seed_entries(training, "h", "e", {})
        self.assertIn("bhai", entries)
        self.assertNotIn("inventedholdout", entries)
        self.assertEqual(1, len(holdout))  # intentionally never passed to builder

    def test_reviewed_rules_reject_duplicates_and_unbounded_strings(self):
        with tempfile.TemporaryDirectory(prefix="brr-seed-test-") as directory:
            path = Path(directory, "rules.tsv")
            path.write_text("yaar\t🫂\nslay\t💅\n", encoding="utf-8")
            self.assertEqual({"yaar": "🫂", "slay": "💅"}, read_rules(path))
            path.write_text("yaar\t🫂\nyaar\t😎\n", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "duplicate"):
                read_rules(path)
            path.write_text("contains space\t😎\n", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "invalid word"):
                read_rules(path)

    def test_reviewed_social_words_are_predictable_but_profanity_is_not(self):
        with tempfile.TemporaryDirectory(prefix="brr-social-test-") as directory:
            path = Path(directory, "social.tsv")
            path.write_text("kya\t105\nsolulu\t31\n", encoding="utf-8")
            reviewed = read_reviewed_social(path, {"wtf": "🤦"})
            entries, _, _ = build_seed_entries([], "h", "e", {"wtf": "🤦"},
                                                reviewed_social=reviewed)
            self.assertEqual((105, ""), entries["kya"])
            self.assertEqual((31, ""), entries["solulu"])
            self.assertEqual((MANUAL_RULE_FREQUENCY, "🤦"), entries["wtf"])
            path.write_text("wtf\t105\n", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "valid-only"):
                read_reviewed_social(path, {"wtf": "🤦"})
            path.write_text("solulu\t200\n", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "frequency"):
                read_reviewed_social(path, {})


if __name__ == "__main__":
    unittest.main()
