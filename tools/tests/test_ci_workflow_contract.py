"""Protect the one-test-suite, two-workflow release trust boundary."""

import json
from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[2]
WORKFLOWS = ROOT / ".github" / "workflows"
DEBUG = WORKFLOWS / "brrrboard-build-debug.yml"
RELEASE = WORKFLOWS / "obtainium-release.yml"


class CiWorkflowContractTest(unittest.TestCase):
    def test_only_live_main_branch_workflows_remain(self):
        self.assertEqual(
            {path.name for path in WORKFLOWS.glob("*.yml")},
            {DEBUG.name, RELEASE.name},
        )
        self.assertNotIn("brrrboard-audit-*", DEBUG.read_text())

    def test_full_android_unit_suite_runs_in_debug_once(self):
        debug = DEBUG.read_text()
        release = RELEASE.read_text()
        invocation = "./gradlew --no-daemon --stacktrace testRunTests"
        self.assertEqual(debug.count(invocation), 1)
        self.assertNotIn(invocation, release)
        self.assertLess(debug.index("name: Run unit tests"), debug.index("name: Publish GitHub release"))
        self.assertIn("if: steps.unit_tests.outcome == 'failure'", debug)

    def test_release_notes_repair_triggers_both_workflows(self):
        debug = DEBUG.read_text()
        release = RELEASE.read_text()
        self.assertIn("Require per-release change notes", debug)
        self.assertIn("- 'docs/releases/notes.json'", release)
        notes = json.loads((ROOT / "docs" / "releases" / "notes.json").read_text())
        self.assertEqual(notes["schema"], 1)
        self.assertTrue(notes["summary"].strip())
        self.assertTrue(notes["changes"])

    def test_original_signer_requires_matching_successful_debug_gate(self):
        release = RELEASE.read_text()
        self.assertIn('head_sha=$GITHUB_SHA', release)
        self.assertIn('.github/workflows/brrrboard-build-debug.yml', release)
        self.assertIn('"$observed_sha" == "$GITHUB_SHA"', release)
        self.assertIn('"$status" == "completed"', release)
        self.assertIn('"$conclusion" == "success"', release)
        self.assertIn('name: Sign and verify APK', release)
        self.assertLess(
            release.index("name: Require same-commit debug CI success"),
            release.index("name: Sign and verify APK"),
        )
        self.assertIn('if: github.ref == \'refs/heads/main\'', release)


if __name__ == "__main__":
    unittest.main()
