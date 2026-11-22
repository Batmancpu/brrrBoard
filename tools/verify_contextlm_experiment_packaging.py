#!/usr/bin/env python3
"""Verify that an APK contains only the bounded, opt-in ContextLM test bundle."""
from __future__ import annotations

import hashlib
import json
import sys
import zipfile
from pathlib import Path


PREFIX = "assets/contextlm/experimental/"
EXPECTED = {
    PREFIX + "manifest.json",
    PREFIX + "vocab.json",
    PREFIX + "weights.int8",
    "assets/licenses/contextlm-training-data.txt",
}
MAX_MODEL_BUNDLE_BYTES = 2_000_000


def verify(apk: Path) -> dict[str, int | str]:
    with zipfile.ZipFile(apk) as archive:
        names = set(archive.namelist())
        missing = EXPECTED - names
        if missing:
            raise ValueError(f"missing ContextLM experiment assets: {sorted(missing)}")
        model_paths = {name for name in names
                       if name.startswith(PREFIX) and not name.endswith("/")}
        if model_paths != EXPECTED - {"assets/licenses/contextlm-training-data.txt"}:
            raise ValueError("unexpected file in ContextLM experiment asset directory")
        manifest_bytes = archive.read(PREFIX + "manifest.json")
        vocab_bytes = archive.read(PREFIX + "vocab.json")
        weights = archive.read(PREFIX + "weights.int8")
        manifest = json.loads(manifest_bytes)
        if manifest.get("status") != "EXPERIMENTAL_DEVICE_TEST_ONLY":
            raise ValueError("ContextLM test manifest is not experimental")
        if manifest.get("quality_gate_passed") is not False:
            raise ValueError("ContextLM quality gate must remain explicitly failed")
        if manifest.get("do_not_use_for_autocorrection") is not True:
            raise ValueError("ContextLM must not be used for automatic correction")
        if manifest.get("apply_only_to_manual_suggestions") is not True:
            raise ValueError("ContextLM must remain limited to manual suggestions")
        if hashlib.sha256(vocab_bytes).hexdigest() != manifest.get("vocab_sha256"):
            raise ValueError("ContextLM vocabulary hash mismatch")
        if hashlib.sha256(weights).hexdigest() != manifest.get("weights_sha256"):
            raise ValueError("ContextLM weight hash mismatch")
        total = len(manifest_bytes) + len(vocab_bytes) + len(weights)
        if total >= MAX_MODEL_BUNDLE_BYTES:
            raise ValueError(f"ContextLM test assets exceed size bound: {total}")
        if not archive.read("assets/licenses/contextlm-training-data.txt"):
            raise ValueError("ContextLM dataset attribution is empty")
        return {
            "weights_bytes": len(weights),
            "vocab_bytes": len(vocab_bytes),
            "total_bytes": total,
            "status": manifest["status"],
        }


if __name__ == "__main__":
    if len(sys.argv) != 2:
        raise SystemExit("usage: verify_contextlm_experiment_packaging.py <apk>")
    print(json.dumps(verify(Path(sys.argv[1])), sort_keys=True))
