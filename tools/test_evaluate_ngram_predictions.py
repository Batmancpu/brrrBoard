#!/usr/bin/env python3
"""B16B harness correctness only, no fabricated native-decoder measurements."""
import json
import tempfile
import unittest
from pathlib import Path

from evaluate_ngram_predictions import Case, evaluate, read_cases


def case(name: str, expected: str, baseline: tuple[str, ...], proposed: tuple[str, ...],
         category: str = "english", protected: bool = False, split: str = "holdout") -> Case:
    return Case(name, split, category, expected, baseline, proposed, protected)


class NgramComparisonTests(unittest.TestCase):
    def test_gains_count_only_when_ground_truth_is_in_top_three(self):
        rows = [
            case("a", "tomorrow", ("today", "now", "tomorrow"),
                 ("tomorrow", "today", "now")),
            case("b", "yaar", ("friend", "buddy"), ("friend", "yaar", "buddy"), "hinglish"),
            case("c", "Riya", ("Riya", "Rhea"), ("Riya", "Rhea"), "proper_name", True),
            case("d", "gmail.com", ("gmail.com",), ("gmail.com",), "url_email", True),
        ]
        report = evaluate(rows)
        self.assertEqual(4, report["cases"])
        self.assertEqual(2, report["baseline_top1"])
        self.assertEqual(3, report["proposed_top1"])
        self.assertEqual(3, report["baseline_top3"])
        self.assertEqual(4, report["proposed_top3"])
        self.assertTrue(report["offline_comparison_pass"])
        self.assertFalse(report["production_quality_verified"])

    def test_one_lost_existing_candidate_fails_even_if_total_recall_gains(self):
        rows = [
            case("a", "tomorrow", ("tomorrow", "later"), ("later", "soon")),
            case("b", "yaar", ("friend", "buddy"), ("yaar", "friend"), "hinglish"),
        ]
        report = evaluate(rows)
        self.assertEqual(1, report["lost_existing_top3"])
        self.assertFalse(report["offline_comparison_pass"])

    def test_protected_name_or_url_cannot_be_reordered(self):
        report = evaluate([case("protected", "Riya", ("Riya", "Rhea"),
                                ("Rhea", "Riya"), "proper_name", True)])
        self.assertEqual(1, report["protected_candidate_changes"])
        self.assertFalse(report["offline_comparison_pass"])

    def test_holdout_is_disjoint_from_development_metrics(self):
        rows = [
            case("dev", "cat", ("dog",), ("cat",), split="development"),
            case("hold", "dog", ("dog",), ("dog",)),
        ]
        self.assertEqual(1, evaluate(rows)["cases"])
        self.assertEqual(1, evaluate(rows, "development")["cases"])
        with self.assertRaisesRegex(ValueError, "No cases"):
            evaluate(rows, "train")

    def test_loader_refuses_real_context_fields_duplicate_ids_and_unprotected_names(self):
        row = {"id": "hold1", "origin": "authored_synthetic", "split": "holdout",
               "category": "proper_name", "expected": "Riya", "baseline": ["Riya"],
               "proposed": ["Riya"], "protected": True}
        with tempfile.TemporaryDirectory(prefix="brr-ngram-test-") as directory:
            path = Path(directory, "synthetic.jsonl")
            path.write_text(json.dumps(row) + "\n", encoding="utf-8")
            self.assertEqual(1, len(read_cases(path)))
            bad = dict(row, raw_text="A user's sentence")
            path.write_text(json.dumps(bad) + "\n", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "no raw context"):
                read_cases(path)
            path.write_text(json.dumps(row) + "\n" + json.dumps(row) + "\n", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "duplicate"):
                read_cases(path)
            path.write_text(json.dumps(dict(row, protected=False)) + "\n", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "protected"):
                read_cases(path)
            path.write_text(json.dumps(dict(row, origin="recorded_user_input")) + "\n", encoding="utf-8")
            with self.assertRaisesRegex(ValueError, "synthetic"):
                read_cases(path)


if __name__ == "__main__":
    unittest.main()
