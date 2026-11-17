#!/usr/bin/env python3
"""B16B offline comparison for *supplied* synthetic next-word candidate rows.

It does not call the IME decoder, inspect user input or claim phone latency.
There is deliberately no telemetry or background collection path. Candidate
rows must be independently produced by an authorized test harness before any
runtime ranking experiment can be evaluated with this tool.
"""
from __future__ import annotations

import argparse
import json
from dataclasses import dataclass
from pathlib import Path


@dataclass(frozen=True)
class Case:
    case_id: str
    split: str
    category: str
    expected: str
    baseline: tuple[str, ...]
    proposed: tuple[str, ...]
    protected: bool


def read_cases(path: Path) -> list[Case]:
    records: list[Case] = []
    seen: set[str] = set()
    for line_number, line in enumerate(path.read_text(encoding="utf-8").splitlines(), 1):
        if not line.strip() or line.lstrip().startswith("#"):
            continue
        row = json.loads(line)
        required = {"id", "origin", "split", "category", "expected", "baseline", "proposed", "protected"}
        if set(row) != required:
            raise ValueError(f"line {line_number}: unexpected/missing fields; no raw context may be recorded")
        if row["origin"] != "authored_synthetic":
            raise ValueError(f"line {line_number}: only reviewed synthetic fixtures are permitted")
        if row["split"] not in {"train", "development", "holdout"}:
            raise ValueError(f"line {line_number}: invalid split")
        if row["category"] not in {"english", "indian_english", "hinglish", "proper_name", "url_email", "punctuation"}:
            raise ValueError(f"line {line_number}: invalid category")
        if not isinstance(row["id"], str) or not row["id"] or row["id"] in seen:
            raise ValueError(f"line {line_number}: duplicate/empty fixture id")
        seen.add(row["id"])
        if not isinstance(row["expected"], str) or not 1 <= len(row["expected"]) <= 48:
            raise ValueError(f"line {line_number}: invalid expected token")
        for key in ("baseline", "proposed"):
            words = row[key]
            if not isinstance(words, list) or len(words) > 3 or not all(
                isinstance(word, str) and 1 <= len(word) <= 48 for word in words
            ) or len(words) != len(set(words)):
                raise ValueError(f"line {line_number}: {key} must contain at most three distinct short tokens")
        if not isinstance(row["protected"], bool):
            raise ValueError(f"line {line_number}: protected must be boolean")
        if row["category"] in {"proper_name", "url_email"} and not row["protected"]:
            raise ValueError(f"line {line_number}: fragile categories must be protected")
        records.append(Case(row["id"], row["split"], row["category"], row["expected"],
                            tuple(row["baseline"]), tuple(row["proposed"]), row["protected"]))
    if not records:
        raise ValueError("No synthetic test rows")
    return records


def evaluate(cases: list[Case], split: str = "holdout") -> dict[str, int | bool | str]:
    holdout = [c for c in cases if c.split == split]
    if not holdout:
        raise ValueError(f"No cases in {split} split")
    base_top1 = sum(c.baseline[:1] == (c.expected,) for c in holdout)
    new_top1 = sum(c.proposed[:1] == (c.expected,) for c in holdout)
    base_top3 = sum(c.expected in c.baseline[:3] for c in holdout)
    new_top3 = sum(c.expected in c.proposed[:3] for c in holdout)
    lost_top3 = sum(c.expected in c.baseline[:3] and c.expected not in c.proposed[:3]
                    for c in holdout)
    protected_changes = sum(c.protected and c.baseline != c.proposed for c in holdout)
    return {
        "evidence": "authored_synthetic_candidates_only_not_decoder_results",
        "cases": len(holdout),
        "baseline_top1": base_top1,
        "proposed_top1": new_top1,
        "baseline_top3": base_top3,
        "proposed_top3": new_top3,
        "lost_existing_top3": lost_top3,
        "protected_candidate_changes": protected_changes,
        "offline_comparison_pass": (
            new_top1 >= base_top1 and new_top3 >= base_top3
            and lost_top3 == 0 and protected_changes == 0
        ),
        # This comparison alone can never license a production ranking change:
        # actual decoder output and device-side latency are independent gates.
        "production_quality_verified": False,
    }


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("fixture", type=Path, help="authored synthetic JSONL, no editor text")
    args = parser.parse_args()
    print(json.dumps(evaluate(read_cases(args.fixture)), indent=2, sort_keys=True))


if __name__ == "__main__":
    main()
