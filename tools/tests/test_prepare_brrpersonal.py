"""Deterministic, offline B15 regression checks for the pinned brrPersonal generator."""

import csv
import importlib.util
import io
import itertools
import sys
import tempfile
import unittest
from pathlib import Path
from unittest.mock import patch


GENERATOR_PATH = Path(__file__).resolve().parents[1] / "prepare_brrpersonal.py"
spec = importlib.util.spec_from_file_location("prepare_brrpersonal", GENERATOR_PATH)
generator = importlib.util.module_from_spec(spec)
spec.loader.exec_module(generator)


class BrrPersonalGeneratorTest(unittest.TestCase):
    def test_pinned_revision_and_column_aliases(self):
        self.assertEqual(len(generator.PHINC_REVISION), 40)
        self.assertEqual(
            generator.find_columns(["Sentence", "English Translation"]),
            ("Sentence", "English Translation"),
        )
        self.assertEqual(
            generator.find_columns(["Hinglish", "English translation"]),
            ("Hinglish", "English translation"),
        )

    def test_repeated_synthetic_input_is_deterministic_and_bounded(self):
        # Author-created tokens, never user typing or the PHINC training corpus.
        words = ["".join(letters) for letters in itertools.islice(
            itertools.product("bcdfghjklmnpqrstvwxyz", repeat=2), 110
        )]
        stream = io.StringIO()
        writer = csv.writer(stream)
        writer.writerow(["Sentence", "English Translation"])
        for _ in range(1001):
            writer.writerow([" ".join(words), "translation"])
        synthetic_csv = stream.getvalue()

        with tempfile.TemporaryDirectory(prefix="brrpersonal-test-") as temp:
            root = Path(temp)
            rules_path = root / "rules.tsv"
            rules_path.write_text("zz\t😀\n", encoding="utf-8")
            first = root / "first.tsv"
            second = root / "second.tsv"
            with patch.object(generator, "download_csv", return_value=synthetic_csv):
                with patch.object(sys, "argv", ["prepare_brrpersonal.py", str(first), str(rules_path)]):
                    generator.main()
                with patch.object(sys, "argv", ["prepare_brrpersonal.py", str(second), str(rules_path)]):
                    generator.main()

            data = first.read_text(encoding="utf-8")
            self.assertEqual(data, second.read_text(encoding="utf-8"))
            entries = [line.split("\t") for line in data.splitlines() if not line.startswith("#")]
            self.assertLessEqual(len(entries), generator.MAX_TOTAL_ENTRIES)
            self.assertIn(["zz", "96", "😀"], entries)
            self.assertTrue(all(len(fields) == 3 for fields in entries))
            self.assertTrue(all(1 <= int(fields[1]) <= 255 for fields in entries))


if __name__ == "__main__":
    unittest.main()
