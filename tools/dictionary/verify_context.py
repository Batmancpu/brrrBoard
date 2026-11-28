#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-only
"""Verify the bundled lexicon, ContextLM and fixed host replay."""
import argparse
import hashlib
import json
import zipfile
from pathlib import Path

from prepare_context import ASSETS, DELIMITER, INPUT_HASHES, MAX_ROWS, WORD

HERE = Path(__file__).resolve().parent
LEXICON = "brrPersonal_modern.tsv"


def verify_assets(read):
    report = json.loads((HERE / "replay_report.json").read_text())
    model = read("contextlm/production.tsv")
    if hashlib.sha256(model).hexdigest() != report["output_sha256"]:
        raise ValueError("ContextLM output fingerprint differs from replay")
    lines = model.decode("ascii").splitlines()
    if not lines or lines[0] != DELIMITER or not 1 < len(lines) <= MAX_ROWS + 1 or len(model) > 768 * 1024:
        raise ValueError("ContextLM version or size differs")
    stats = {"en": 0, "mix": 0}
    seen = set()
    for line in lines[1:]:
        lane, context, *scored = line.split("\t")
        tokens = context.split(" ")
        key = (lane, context)
        if (lane not in stats or key in seen or not 1 <= len(tokens) <= 3
                or not all(WORD.fullmatch(word) or (i == 0 and word == "^") for i, word in enumerate(tokens))):
            raise ValueError("Invalid context row")
        if not 2 <= len(scored) <= 6 or len(scored) % 2 or len(set(scored[::2])) * 2 != len(scored):
            raise ValueError("Invalid context candidates")
        if any(not WORD.fullmatch(word) or not 1 <= int(weight) <= 255
               for word, weight in zip(scored[::2], scored[1::2])):
            raise ValueError("Invalid context scoring")
        seen.add(key)
        stats[lane] += 1
    if stats != report["rows"] or report["inputs"] != INPUT_HASHES:
        raise ValueError("ContextLM source digests or row counts differ")
    for locale, replay in report["replay"].items():
        required_gain = 3 if locale in ("US", "GB") else 0
        if (replay["top1_new"] < replay["top1_base"]
                or (replay["top3_new"] - replay["top3_base"]) * 100 < required_gain * replay["contexts"]):
            raise ValueError(f"{locale} next-word replay failed")
    lexical = set()
    for line in read(LEXICON).decode("ascii").splitlines():
        if not line or line.startswith("#"):
            continue
        lane, word, weight, policy = line.split("\t")
        if (lane not in stats or not WORD.fullmatch(word) or not 1 <= int(weight) <= 105
                or policy not in ("prefix", "valid") or word in lexical):
            raise ValueError("Invalid reviewed term")
        lexical.add(word)
    lexical.update(row.split("\t")[0] for row in read("brrPersonal_social.tsv").decode().splitlines()
                   if row and not row.startswith("#"))
    if not {"prepone", "lakh", "crore", "bhai", "haan", "hain", "unmute", "deepfake",
            "doomscrolling", "unfollow", "ngl", "rizz", "delulu"}.issubset(lexical):
        raise ValueError("Missing reviewed terms")
    license_note = read("licenses/br-offline-context.txt")
    if b"CC BY 4.0" not in license_note or b"Tatoeba CC0" not in license_note:
        raise ValueError("Missing public-corpus attribution")
    for locale in ("US", "GB"):
        if hashlib.sha256(read(f"dicts/main_en-{locale}.dict")).hexdigest() != INPUT_HASHES[locale]:
            raise ValueError(f"Bundled {locale} stock dictionary differs")
    return stats


def verify_taps(path):
    policies = {
        row.split("\t")[1]: row.split("\t")[3]
        for row in (ASSETS / LEXICON).read_text().splitlines()
        if row and not row.startswith("#")
    }
    target_before = target_after = targets = controls = 0

    def entries(data):
        return [(word, int(score)) for word, score, _ in
                (item.rsplit(":", 2) for item in data.split("|") if item)]

    for row in path.read_text().splitlines():
        _, typed, expected, baseline, supplemental = row.split("\t")
        base = entries(baseline)
        merged = dict(reversed(base))
        for word, score in entries(supplemental):
            if policies.get(word) == "prefix" and len(typed) >= 2 and word.startswith(typed):
                merged[word] = max(score, merged.get(word, -2**31))
        old = [word for word, _ in base]
        new = sorted(merged, key=lambda word: (-merged[word], len(word), word))
        if policies.get(expected) == "prefix" or ("'" in expected and "'" not in typed):
            targets += 1
            target_before += expected in old[:3]
            target_after += expected in new[:3]
        else:
            controls += 1
            if old[:1] != new[:1]:
                raise ValueError("Fixed common-word/dialect top-1 changed")
    if targets < 20 or controls < 15 or (target_after - target_before) * 10 < targets:
        raise ValueError("Fixed native tap recall did not gain 10 percentage points")
    return dict(targets=targets, baseline=target_before, overlay=target_after, controls=controls)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--apk", type=Path)
    parser.add_argument("--native-taps", type=Path, nargs="+")
    args = parser.parse_args()
    if args.apk:
        with zipfile.ZipFile(args.apk) as apk:
            stats = verify_assets(lambda path: apk.read(f"assets/{path}"))
    else:
        stats = verify_assets(lambda path: (ASSETS / path).read_bytes())
    print(json.dumps(dict(rows=stats, taps=[verify_taps(path) for path in args.native_taps or []])))


if __name__ == "__main__":
    main()
