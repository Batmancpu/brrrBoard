#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-only
"""No network, Gradle or private data needed to verify the release ledger."""

import json
from pathlib import Path
import unittest

from generate_version_history import generate, release_notes, validate_notes


ROOT = Path(__file__).resolve().parents[1]
NOTES = json.loads((ROOT / "docs/releases/notes.json").read_text(encoding="utf-8"))
VERIFIED = json.loads((ROOT / "docs/releases/legacy_verified.json").read_text(encoding="utf-8"))
COMMIT = "a" * 40


class VersionHistoryTest(unittest.TestCase):
    def test_previously_published_versions_have_verified_order_and_times(self):
        data = generate(NOTES, VERIFIED, [], "1.0.187", "2026-11-20T10:20:30Z", COMMIT)
        self.assertEqual(["1.0.187", "1.0.186", "1.0.183"],
                         [record["version"] for record in data["releases"]])
        self.assertEqual("release_cut", data["releases"][0]["timeSource"])
        self.assertEqual("github_published", data["releases"][1]["timeSource"])

    def test_new_signed_release_is_recovered_from_its_structured_notes(self):
        first = generate(NOTES, VERIFIED, [], "1.0.187", "2026-11-20T10:20:30Z", COMMIT)
        payload = release_notes(first["releases"][0])
        observed = [{"tag_name": "v1.0.187", "published_at": "2026-11-20T10:24:33Z",
                     "target_commitish": COMMIT, "body": payload}]
        next_data = generate(NOTES, VERIFIED, [observed],
                             "1.0.188", "2026-11-20T11:01:02Z", "b" * 40)
        prior = next_data["releases"][1]
        self.assertEqual("1.0.187", prior["version"])
        self.assertEqual("2026-11-20T10:24:33Z", prior["releaseTimeUtc"])
        self.assertEqual("github_published", prior["timeSource"])
        self.assertEqual(NOTES["changes"], prior["changes"])

    def test_debug_or_unverified_release_never_enters_catalog(self):
        fake = [
            {"tag_name": "v2.5.8-debug.abc", "published_at": "2026-11-20T11:00:00Z",
             "target_commitish": COMMIT, "body": "unverified"},
            {"tag_name": "v1.0.999", "published_at": "2026-11-20T11:00:00Z",
             "target_commitish": COMMIT, "body": "no structured change notes"},
        ]
        self.assertEqual([], generate(NOTES, {"releases": []}, fake)["releases"])

    def test_history_is_bounded_to_latest_ten_signed_releases(self):
        entries = []
        for version in range(180, 195):
            notes = release_notes(generate(NOTES, {"releases": []}, [],
                f"1.0.{version}", f"2026-11-{(version % 20) + 1:02d}T10:20:30Z", COMMIT)["releases"][0])
            entries.append({"tag_name": f"v1.0.{version}",
                "published_at": f"2026-11-{(version % 20) + 1:02d}T10:20:30Z",
                "target_commitish": COMMIT, "body": notes})
        result = generate(NOTES, {"releases": []}, entries)
        self.assertEqual(10, len(result["releases"]))
        self.assertEqual([f"1.0.{v}" for v in range(194, 184, -1)],
                         [entry["version"] for entry in result["releases"]])

    def test_public_notes_remove_personal_metadata_in_new_and_older_releases(self):
        private = {"schema": 1, "summary": "Checked on Oppo Reno10 5G",
                   "changes": [{"title": "Reference device testing",
                                "detail": "Tested with Nirvana Ion; /home/tester/private.log and person@example.invalid."}]}
        new = generate(private, {"releases": []}, [], "1.0.187",
                       "2026-11-20T10:20:30Z", COMMIT)
        row = new["releases"][0]
        self.assertEqual("Checked on reference test device", row["summary"])
        self.assertIn("Tested with reference test device", row["changes"][0]["detail"])
        marker = {"schema": 1, "version": "1.0.187", "commit": COMMIT,
                  "summary": private["summary"], "changes": private["changes"]}
        observed = [{"tag_name": "v1.0.187", "published_at": "2026-11-20T10:24:33Z",
                     "target_commitish": COMMIT,
                     "body": "<!-- brrboard-version-history-v1\n" + json.dumps(marker) + "\n-->"}]
        old = generate(NOTES, {"releases": []}, observed)["releases"][0]
        self.assertEqual(row["summary"], old["summary"])
        self.assertEqual(row["changes"], old["changes"])
        self.assertEqual(COMMIT, old["commit"])
        self.assertEqual("github_published", old["timeSource"])
        for output in (json.dumps(new), json.dumps(old), release_notes(row)):
            for identifier in ("Oppo", "Reno10", "Nirvana Ion", "/home/tester", "person@example.invalid"):
                self.assertNotIn(identifier, output)

    def test_invalid_notes_and_timestamps_fail_closed(self):
        with self.assertRaises(ValueError):
            validate_notes({"schema": 1, "summary": "new", "changes": []})
        with self.assertRaises(ValueError):
            generate(NOTES, VERIFIED, [], "1.0.187", "2026-02-30T10:00:00Z", COMMIT)


if __name__ == "__main__":
    unittest.main()
