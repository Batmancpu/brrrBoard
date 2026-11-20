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

    def test_invalid_notes_and_timestamps_fail_closed(self):
        with self.assertRaises(ValueError):
            validate_notes({"schema": 1, "summary": "new", "changes": []})
        with self.assertRaises(ValueError):
            generate(NOTES, VERIFIED, [], "1.0.187", "2026-02-30T10:00:00Z", COMMIT)


if __name__ == "__main__":
    unittest.main()
