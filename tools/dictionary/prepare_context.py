#!/usr/bin/env python3
# SPDX-License-Identifier: GPL-3.0-only
"""Compile bounded ContextLM tables and report public next-word replay."""
import argparse
import ast
import collections
import csv
import hashlib
import itertools
import json
import re
import tarfile
from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]
ASSETS = ROOT / "app/src/main/assets"
INPUT_HASHES = {
    "tatoeba": "7b14f124ad50c2e221d1bbf26451f6d0d86d34e9f9a1490340203264e4d7b8cd",
    "comi": "9a9bf88ced2cd94925549cce0b3f361eb4ae1f70b1d4abf9fbeec22deae1a2cc",
    "hinge": "dcfaf37ca9d7528e27da7be6b0cfffafb8bbe7825fe4da45077b96236cd9c9c3",
    "phinc": "325ba383cd6d73e0c3128885e2a79133e612ec03d18c8b55506d0b4b12ec68ed",
    "US": "bd950ef4b57655120eee65cee62a5d216a63f721d9a8bb759ce2022437840443",
    "GB": "642ff3e5727e81d3a1ebdeddd9226dd7c17166bcd52f8f6e39f34b44be6faabd",
}
WORD = re.compile(r"[a-z]{1,24}(?:['-][a-z]{1,12}){0,2}\Z")
SENTENCE = re.compile(r"""[A-Za-z' ,"()\-]+[.!?]?\Z""")
MAX_ROWS = 20_000
DELIMITER = "brr-context-1"
COUNTS_HEADER = "brr-context-counts-1"
AUTHORED = {
    "en": [
        "vibe coding", "rage bait", "brain rot", "cold brew", "use a passkey",
        "please unmute", "avoid doomscrolling", "spot a deepfake",
        "okay let's", "think it's", "hope we'll", "think we're",
        "i think it's", "i think we're", "i hope we'll",
        "they were", "very well", "felt ill", "its colour", "wooden shell", "of its own",
        "two lakh", "ten lakh", "five crore", "one crore", "please prepone", "did you prepone",
    ],
    "mix": [
        "kal milte hain", "bahut accha", "bahut achha", "bahut achha hai", "haan bhai",
        "nahi yaar", "kya baat hai", "aap kaise hain", "phir milte hain",
        "mujhe pata nahi", "aaj milte hain", "kal subah milte hain", "haan theek hai",
        "haan please send", "aaj meeting hai", "kal call karna", "bahut badhiya yaar",
    ],
}


def sha(path):
    with path.open("rb") as stream:
        return hashlib.file_digest(stream, "sha256").hexdigest()


def table(name):
    return (line.split("\t") for line in (ASSETS / name).read_text().splitlines()
            if line and not line.startswith("#"))


def native_table(path):
    predictions, words = {}, set()
    for line in path.read_text().splitlines():
        surface, weight, *next_words = line.split("\t")
        word = surface.lower()
        if WORD.fullmatch(word) and (surface == word or surface.startswith("I'") or surface == "I"):
            if int(weight) > 0:
                words.add(word)
            predictions[word] = [w.lower() for w in next_words]
    return predictions, words


def tokenize(text):
    text = text.replace("’", "'").strip()
    if not SENTENCE.fullmatch(text):
        return ()
    tokens = tuple(value.lower() for value in re.sub(r'[,"()]', " ", text.rstrip(".!?")).split())
    return tokens if 3 <= len(tokens) <= 24 and all(WORD.fullmatch(w) for w in tokens) else ()


def read_tatoeba(path):
    with tarfile.open(path, "r:bz2") as archive:
        for raw in archive.extractfile("sentences_CC0.csv"):
            _, language, line, *_ = raw.decode("utf-8").split("\t")
            if language == "eng":
                yield line


def read_phinc(path):
    with path.open(encoding="utf-8-sig", newline="") as stream:
        yield from (row["Sentence"] for row in csv.DictReader(stream))


def read_comi(path):
    with path.open(encoding="utf-8-sig", newline="") as stream:
        yield from (row["Sentences"] for row in csv.DictReader(stream))


def read_hinge(path):
    with path.open(encoding="utf-8-sig", newline="") as stream:
        for row in csv.DictReader(stream):
            variants = ast.literal_eval(row["Human-generated Hinglish"])
            if isinstance(variants, list):
                yield from variants


def split_rows(lines):
    """Keep exact and near-duplicate groups on the same side of the holdout."""
    seen = set()
    for text in lines:
        tokens = tokenize(text)
        if not tokens or tokens in seen:
            continue
        seen.add(tokens)
        # Related rows with a shared beginning and ending stay together. This
        # deliberately undercounts several nearly identical short sentences.
        group = " ".join((*tokens[:2], tokens[-1])).encode()
        partition = int.from_bytes(hashlib.sha256(group).digest()[:4], "big") % 10
        yield tokens, partition


def windows(tokens):
    padded = ("^", *tokens)
    for pos in range(1, len(padded)):
        yield padded[max(0, pos - 3):pos], padded[pos]


def score(count, total, context_size):
    # Zero is reserved for individually authored phrases.
    return 255 if count == 0 else min(
        254, count * 170 // total + (count.bit_length() - 1) * 6 + context_size * 8)


def quantize(rows):
    return {context: [(word, score(count, total, len(context))) for word, count, total in values]
            for context, values in rows.items()}


def write_model(output, lanes):
    with output.open("w", encoding="ascii", newline="\n") as stream:
        stream.write(DELIMITER + "\n")
        for lane, rows in lanes.items():
            for key, targets in sorted(quantize(rows).items()):
                fields = [lane, " ".join(key)]
                for target, weight in targets:
                    fields.extend((target, str(weight)))
                stream.write("\t".join(fields) + "\n")


def write_counts(output, lanes):
    with output.open("w", encoding="ascii", newline="\n") as stream:
        stream.write(COUNTS_HEADER + "\n")
        for lane, rows in lanes.items():
            for key, targets in sorted(rows.items()):
                fields = [lane, " ".join(key)]
                for target, count, total in targets:
                    fields.extend((target, str(count), str(total)))
                stream.write("\t".join(fields) + "\n")


def read_counts(path):
    if path.stat().st_size > 1024 * 1024:
        raise ValueError("Context count snapshot exceeds capacity")
    lanes = {"en": {}, "mix": {}}
    with path.open(encoding="ascii") as stream:
        if next(stream, "").rstrip() != COUNTS_HEADER:
            raise ValueError("Incompatible context count snapshot")
        for row in stream:
            lane, key, *fields = row.rstrip("\n").split("\t")
            context = tuple(key.split(" "))
            if (lane not in lanes or context in lanes[lane] or not 1 <= len(context) <= 3
                    or not 3 <= len(fields) <= 9 or len(fields) % 3):
                raise ValueError("Invalid context count row")
            values = []
            for target, raw_count, raw_total in zip(fields[::3], fields[1::3], fields[2::3]):
                count, total = int(raw_count), int(raw_total)
                if not WORD.fullmatch(target) or not (0 <= count <= total and bool(count) == bool(total)):
                    raise ValueError("Invalid context count")
                values.append((target, count, total))
            lanes[lane][context] = values
    if not all(lanes.values()) or sum(map(len, lanes.values())) > MAX_ROWS:
        raise ValueError("Context count table exceeds row limit or omits a lane")
    return lanes


def build(lines, vocabulary, starters, authored, row_cap):
    counts = collections.defaultdict(collections.Counter)
    development, holdout = [], []
    for tokens, partition in split_rows(lines):
        if not starters or starters.intersection(tokens):
            if partition in (0, 1):
                (holdout if partition == 0 else development).append(tokens)
                continue
            for context, target in windows(tokens):
                if target in vocabulary:
                    for size in range(1, len(context) + 1):
                        key = context[-size:]
                        if all(w == "^" or w in vocabulary for w in key):
                            counts[key][target] += 1
    rows = {}
    for context, targets in counts.items():
        total = sum(targets.values())
        picked = []
        for target, count in sorted(targets.items(), key=lambda pair: (-pair[1], pair[0]))[:3]:
            minimum = (2 if len(context) > 1 else 3) if starters else (3 if len(context) > 1 else 6)
            if count < minimum or count * 18 < total:
                continue
            picked.append((target, count, total))
        if picked:
            rows[context] = picked
    rows = dict(sorted(rows.items(), key=lambda item: (
        -score(*item[1][0][1:], len(item[0])), -sum(counts[item[0]].values()), item[0]))[:row_cap])
    authored_rows = collections.defaultdict(list)
    for phrase in authored:
        context, target = phrase.split()[:-1], phrase.split()[-1]
        authored_rows[tuple(context)].append((target, 0, 0))
    rows.update({key: values + [pair for pair in rows.get(key, ()) if pair[0] not in
                                {v[0] for v in values}][:3 - len(values)]
                 for key, values in authored_rows.items()})
    return rows, development, holdout


def lookup(rows, context):
    for size in range(len(context), 0, -1):
        if context[-size:] in rows:
            return rows[context[-size:]]
    return []


def replay(rows, native, holdout, floor=110, second_floor=105):
    count = top1_base = top1_new = top3_base = top3_new = 0
    for tokens in holdout:
        for context, target in windows(tokens):
            if context[-1] == "^":
                continue
            # Native-only baseline of the shipped main; no fabricated targets.
            base = native.get(context[-1], [])
            matches = lookup(rows, context)
            chosen = [word for word, score in matches if score >= floor][:1]
            if chosen and second_floor:
                chosen.extend([word for word, score in matches if word not in chosen and score >= second_floor][:1])
            proposal = chosen + [word for word in base if word not in chosen]
            top1_base += target in base[:1]
            top1_new += target in proposal[:1]
            top3_base += target in base[:3]
            top3_new += target in proposal[:3]
            count += 1
    return dict(sentences=len(holdout), contexts=count, top1_base=top1_base, top1_new=top1_new,
                top3_base=top3_base, top3_new=top3_new)


def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--output", required=True, type=Path)
    for name in ("tatoeba", "phinc", "comi", "hinge", "us", "gb", "counts", "from-counts"):
        parser.add_argument("--" + name, type=Path)
    args = parser.parse_args()
    if args.from_counts:
        write_model(args.output, read_counts(args.from_counts))
        return
    if not all((args.tatoeba, args.phinc, args.comi, args.hinge, args.us, args.gb, args.counts)):
        parser.error("full replay requires the pinned corpora, both native dumps and a count output")
    for key, path in (("tatoeba", args.tatoeba), ("phinc", args.phinc),
                      ("comi", args.comi), ("hinge", args.hinge),
                      *((locale, ASSETS / "dicts" / f"main_en-{locale}.dict") for locale in ("US", "GB"))):
        if sha(path) != INPUT_HASHES[key]:
            raise SystemExit(f"Pinned {key} input differs")
    us, us_words = native_table(args.us)
    gb, gb_words = native_table(args.gb)
    block = {row[0] for filename in ("brrPersonal_rules.tsv", "brrPersonal_english_valid_only.tsv")
             for row in table(filename)}
    english = (us_words & gb_words) - block
    english |= {word for lane, word, _, policy in table("brrPersonal_modern.tsv")
                if lane == "en" and policy == "prefix"}
    authored_roman = {row[0] for row in table("brrPersonal_social.tsv")} | {
        word for lane, word, _, policy in table("brrPersonal_modern.tsv") if lane == "mix" and policy == "prefix"}
    roman_markers = authored_roman - (us_words | gb_words)
    mixed_vocab = (english | authored_roman) - block
    en_rows, en_dev, en_holdout = build(read_tatoeba(args.tatoeba), english, set(), AUTHORED["en"], 15_000)
    mixed_lines = itertools.chain(read_phinc(args.phinc), read_comi(args.comi), read_hinge(args.hinge))
    mix_rows, mix_dev, mix_holdout = build(mixed_lines, mixed_vocab, roman_markers, AUTHORED["mix"], 4000)
    if len(en_rows) + len(mix_rows) > MAX_ROWS:
        raise SystemExit("Context index exceeded row bound")
    lanes = {"en": en_rows, "mix": mix_rows}
    write_counts(args.counts, lanes)
    write_model(args.output, lanes)
    en_rows, mix_rows = quantize(en_rows), quantize(mix_rows)
    print(json.dumps({
        "inputs": INPUT_HASHES, "output_sha256": sha(args.output), "counts_sha256": sha(args.counts),
        "rows": {"en": len(en_rows), "mix": len(mix_rows)},
        "development_grid": {f"{first}/{second}": [
            replay(en_rows, us, en_dev, first, second),
            replay(mix_rows, us, mix_dev, first, second),
        ] for first, second in ((126, 0), (110, 105), (100, 82), (92, 80), (84, 72))},
        "replay": {"US": replay(en_rows, us, en_holdout),
                   "GB": replay(en_rows, gb, en_holdout),
                   "mixed_vs_US_stock": replay(mix_rows, us, mix_holdout)},
    }, indent=2, sort_keys=True))


if __name__ == "__main__":
    main()
