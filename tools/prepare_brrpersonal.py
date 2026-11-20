#!/usr/bin/env python3
"""Generate brrPersonal's compact decoder seed from pinned PHINC corpus counts.

The corpus is fetched at build time only. Android reads the generated TSV locally, then
builds an expandable binary dictionary with the app's existing native decoder.
"""
from __future__ import annotations

import csv
import io
import re
import sys
import time
import urllib.request
from collections import Counter
from pathlib import Path
from typing import Iterable

PHINC_REVISION = "44b5471df75e508b9d6fcdbcc93df04bb1e84056"
PHINC_URL = (
    "https://huggingface.co/datasets/LingoIITGN/PHINC/resolve/"
    + PHINC_REVISION + "/PHINC.csv"
)
MAX_TOTAL_ENTRIES = 6000
MIN_COUNT = 2
MANUAL_RULE_FREQUENCY = 96
TOKEN_RE = re.compile(r"[a-z]{2,24}")

# English function words already come from the selected English dictionary. Comparing counts
# against the paired translations also removes many English words common to both sides.
ENGLISH_FUNCTION_WORDS = set("""
a an and are as at be been being but by can could did do does doing down during each
few for from further had has have having he her hers herself him himself his how i if
in into is it its itself just me more most my myself no nor not of off on once only or
other our ours ourselves out over own same she should so some such than that the their
theirs them themselves then there these they this those through to too under until up
very was we were what when where which while who whom why will with would you your yours
""".split())

def normalize_header(value: str) -> str:
    return re.sub(r"[^a-z0-9]", "", value.casefold())

def find_columns(headers: list[str]) -> tuple[str, str]:
    normalized = [(header, normalize_header(header)) for header in headers]
    hinglish = next(
        (header for header, key in normalized
         if "hinglish" in key and "englishtranslation" not in key),
        None,
    )
    if hinglish is None:
        hinglish = next(
            (header for header, key in normalized
             if "codemixed" in key and "english" not in key),
            None,
        )
    if hinglish is None:
        # PHINC's pinned CSV calls its Romanized source sentence "Sentence".
        hinglish = next(
            (header for header, key in normalized if key == "sentence"),
            None,
        )
    english = next(
        (header for header, key in normalized
         if "english" in key and "hinglish" not in key),
        None,
    )
    if hinglish is None or english is None:
        raise ValueError("PHINC columns were not recognized: " + repr(headers))
    return hinglish, english

def download_csv() -> str:
    request = urllib.request.Request(
        PHINC_URL,
        headers={"User-Agent": "brrBoard-brrPersonal-build/1"},
    )
    last_error: Exception | None = None
    for attempt in range(3):
        try:
            with urllib.request.urlopen(request, timeout=60) as response:
                return response.read().decode("utf-8-sig")
        except Exception as error:
            last_error = error
            if attempt < 2:
                time.sleep(2 ** (attempt + 1))
    raise RuntimeError("Could not download pinned PHINC source") from last_error

def read_rules(path: Path) -> dict[str, str]:
    rules: dict[str, str] = {}
    with path.open("r", encoding="utf-8", newline="") as stream:
        for line_number, line in enumerate(stream, 1):
            if not line.strip() or line.lstrip().startswith("#"):
                continue
            fields = line.rstrip("\r\n").split("\t")
            if len(fields) != 2:
                raise ValueError(f"{path}:{line_number}: expected word<TAB>emoji")
            word, emoji = fields
            if not re.fullmatch(r"[a-z]{2,24}", word):
                raise ValueError(f"{path}:{line_number}: invalid word {word!r}")
            if not emoji:
                raise ValueError(f"{path}:{line_number}: emoji shortcut is required")
            if word in rules:
                raise ValueError(f"{path}:{line_number}: duplicate word {word!r}")
            rules[word] = emoji
    if not rules:
        raise ValueError("brrPersonal rules file is empty")
    return rules


def read_reviewed_social(path: Path, existing_rules: dict[str, str]) -> dict[str, int]:
    """Load safe authored Hinglish/social words, excluding valid-only/profanity rules."""
    words: dict[str, int] = {}
    with path.open("r", encoding="utf-8", newline="") as stream:
        for line_number, line in enumerate(stream, 1):
            if not line.strip() or line.lstrip().startswith("#"):
                continue
            fields = line.rstrip("\r\n").split("\t")
            if len(fields) != 2:
                raise ValueError(f"{path}:{line_number}: expected word<TAB>frequency")
            word, weight = fields
            if not re.fullmatch(r"[a-z]{2,24}", word):
                raise ValueError(f"{path}:{line_number}: invalid word {word!r}")
            if word in existing_rules:
                raise ValueError(f"{path}:{line_number}: valid-only word cannot be suggested: {word!r}")
            if word in words:
                raise ValueError(f"{path}:{line_number}: duplicate word {word!r}")
            try:
                frequency = int(weight)
            except ValueError as error:
                raise ValueError(f"{path}:{line_number}: invalid frequency {weight!r}") from error
            if frequency < 24 or frequency > 105:
                raise ValueError(f"{path}:{line_number}: frequency out of range")
            words[word] = frequency
    if not 1 <= len(words) <= 256:
        raise ValueError("reviewed social word count must be between 1 and 256")
    return words


def read_english_valid_only(path: Path, reserved: set[str]) -> dict[str, int]:
    """Authored spelling-valid tokens, deliberately excluded from proactive text."""
    result: dict[str, int] = {}
    with path.open("r", encoding="utf-8", newline="") as stream:
        for line_number, line in enumerate(stream, 1):
            if not line.strip() or line.lstrip().startswith("#"):
                continue
            fields = line.rstrip("\r\n").split("\t")
            if len(fields) != 2 or not re.fullmatch(r"[a-z]{2,24}", fields[0]):
                raise ValueError(f"{path}:{line_number}: malformed English validity token")
            word = fields[0]
            if word in reserved or word in result:
                raise ValueError(f"{path}:{line_number}: duplicate/conflicting valid-only token")
            try:
                frequency = int(fields[1])
            except ValueError as error:
                raise ValueError(f"{path}:{line_number}: invalid seed weight") from error
            if frequency < 24 or frequency > 64:
                raise ValueError(f"{path}:{line_number}: out-of-bound seed weight")
            result[word] = frequency
    if not 1 <= len(result) <= 64:
        raise ValueError("English validity list outside reviewed bounds")
    return result


def build_seed_entries(
    rows: Iterable[dict[str, str]],
    hinglish_column: str,
    english_column: str,
    rules: dict[str, str],
    max_entries: int = MAX_TOTAL_ENTRIES,
    reviewed_social: dict[str, int] | None = None,
    reviewed_english_valid_only: dict[str, int] | None = None,
) -> tuple[dict[str, tuple[int, str]], int, int]:
    """Pure, deterministic seed builder for disjoint synthetic quality fixtures.

    Preserve existing production selection and frequencies exactly. The caller
    supplies training rows only; this function never observes holdout examples
    or persists a sentence. Candidate count is recorded for provenance.
    """
    hinglish_counts: Counter[str] = Counter()
    english_counts: Counter[str] = Counter()
    row_count = 0
    for row in rows:
        row_count += 1
        hinglish_counts.update(TOKEN_RE.findall((row.get(hinglish_column) or "").casefold()))
        english_counts.update(TOKEN_RE.findall((row.get(english_column) or "").casefold()))

    candidates = [
        (word, count) for word, count in hinglish_counts.items()
        if count >= MIN_COUNT
        and count > english_counts[word]
        and word not in ENGLISH_FUNCTION_WORDS
    ]
    candidates.sort(key=lambda item: (-item[1], item[0]))
    reviewed_social = reviewed_social or {}
    reviewed_english_valid_only = reviewed_english_valid_only or {}
    if set(reviewed_social) & set(rules):
        raise ValueError("reviewed social words must not overlap valid-only rules")
    if set(reviewed_english_valid_only) & (set(rules) | set(reviewed_social)):
        raise ValueError("English valid-only words must not overlap proactive sources")
    corpus_limit = max(0, max_entries - len(rules) - len(reviewed_social)
                       - len(reviewed_english_valid_only))
    entries: dict[str, tuple[int, str]] = {
        word: (min(255, count), "") for word, count in candidates[:corpus_limit]
    }
    for word, frequency in reviewed_social.items():
        entries[word] = (max(frequency, entries.get(word, (0, ""))[0]), "")
    for word, frequency in reviewed_english_valid_only.items():
        # The app-side validity gate prevents silent/automatic replacement.
        entries[word] = (max(frequency, entries.get(word, (0, ""))[0]), "")
    for word, emoji in rules.items():
        corpus_frequency = entries.get(word, (0, ""))[0]
        entries[word] = (max(MANUAL_RULE_FREQUENCY, corpus_frequency), emoji)
    return entries, row_count, len(candidates)

def main() -> None:
    if len(sys.argv) not in (3, 4, 5):
        raise SystemExit("usage: prepare_brrpersonal.py OUTPUT_TSV RULES_TSV [SOCIAL_TSV] [ENGLISH_VALID_ONLY_TSV]")
    output_path = Path(sys.argv[1])
    rules_path = Path(sys.argv[2])
    rules = read_rules(rules_path)
    reviewed_social = read_reviewed_social(Path(sys.argv[3]), rules) if len(sys.argv) >= 4 else {}
    reviewed_english = read_english_valid_only(
        Path(sys.argv[4]), set(rules) | set(reviewed_social)) if len(sys.argv) == 5 else {}
    reader = csv.DictReader(io.StringIO(download_csv(), newline=""))
    if not reader.fieldnames:
        raise ValueError("Pinned PHINC CSV has no header")
    hinglish_column, english_column = find_columns(reader.fieldnames)

    entries, row_count, candidate_count = build_seed_entries(
        reader, hinglish_column, english_column, rules, reviewed_social=reviewed_social,
        reviewed_english_valid_only=reviewed_english)

    output_path.parent.mkdir(parents=True, exist_ok=True)
    with output_path.open("w", encoding="utf-8", newline="\n") as output:
        output.write("# brrPersonal schema 1; generated from PHINC and reviewed local rules\n")
        output.write(f"# PHINC revision: {PHINC_REVISION} (CC-BY-4.0)\n")
        output.write(f"# Reviewed 2026 social/Hinglish additions: {len(reviewed_social)}\n")
        output.write(f"# Authored English validity-only entries: {len(reviewed_english)}\n")
        output.write("# Columns: lowercase Latin token<TAB>native frequency<TAB>emoji shortcut\n")
        for word in sorted(entries, key=lambda item: (-entries[item][0], item)):
            frequency, emoji = entries[word]
            output.write(f"{word}\t{frequency}\t{emoji}\n")

    if row_count < 1000 or candidate_count < 100:
        raise ValueError(
            f"Unexpectedly small PHINC result: rows={row_count}, candidates={candidate_count}"
        )
    print(
        f"brrPersonal: {len(entries)} entries from {row_count} PHINC rows; "
        f"{len(rules)} valid-only shortcut rules, {len(reviewed_social)} safe social words, "
        f"{len(reviewed_english)} English validity-only words"
    )

if __name__ == "__main__":
    main()
