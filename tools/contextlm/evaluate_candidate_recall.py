#!/usr/bin/env python3
"""Honest offline n-gram candidate recall study (NOT the native IME benchmark).

This deliberately does not pretend that held-out corpus counts are the same as
on-device typing accuracy, nor that a generated candidate is an actual accepted
word. The fully signed APK and Oppo metrics require separate measurement.
"""
from __future__ import annotations

import argparse
import collections
import json
import random
from pathlib import Path

import torch

from train_dfsmn import CausalDFSMN, encode, source_rows, split_name


def rows_by_source(corpus: Path):
    partitions = {name: {"tn": [], "mt": [], "hinge": []}
                  for name in ("train", "dev", "holdout")}
    seen = set()
    for row, tokens in source_rows(corpus):
        key = tuple(tokens)
        if key in seen:
            continue
        seen.add(key)
        partitions[split_name(row)][row.split("/", 1)[0]].append(tokens)
    return partitions


def index_ngrams(rows: list[list[int]]):
    table = collections.defaultdict(collections.Counter)
    global_counts = collections.Counter()
    for sentence in rows:
        for position in range(2, len(sentence)):
            target = sentence[position]
            if target <= 3:
                continue
            global_counts[target] += 1
            for length in (1, 2, 3, 4):
                if position < length:
                    continue
                prefix = tuple(sentence[position - length:position])
                table[prefix][target] += 1
    return table, global_counts


def offer(prefix, table, global_counts, max_count=12):
    # Modified Kneser–Ney smoothing is a production candidate; this is only a
    # transparent interpolation/recall baseline, no unproven smoothing claim.
    scored = collections.Counter()
    for n in (4, 3, 2, 1):
        if len(prefix) < n:
            continue
        matches = table.get(tuple(prefix[-n:]), collections.Counter())
        denominator = max(1, sum(matches.values()))
        for word, count in matches.most_common(24):
            scored[word] += (0.5 + 0.5 * n) * count / denominator
    total = max(1, sum(global_counts.values()))
    for word, count in global_counts.most_common(32):
        scored[word] += 0.1 * count / total
    choices = [word for word, score in scored.most_common(max_count)]
    ranking = [float(scored[word]) for word in choices]
    max_score = max(ranking) if ranking else 1
    return choices, [min(1., score / max_score) for score in ranking]


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--corpus-dir", required=True, type=Path)
    p.add_argument("--checkpoint", required=True, type=Path)
    p.add_argument("--vocab", required=True, type=Path)
    p.add_argument("--max-cases", type=int, default=1800)
    p.add_argument("--seed", type=int, default=2827)
    args = p.parse_args()
    torch.set_num_threads(min(4, torch.get_num_threads()))
    vocab = json.loads(args.vocab.read_text())
    partitions = rows_by_source(args.corpus_dir)
    training = [row for rows in partitions["train"].values() for row in rows]
    encoded = encode(training, vocab)
    index, counts = index_ngrams(encoded)
    model = CausalDFSMN(len(vocab))
    model.load_state_dict(torch.load(args.checkpoint, map_location="cpu", weights_only=True))
    model.eval()
    cases = []
    for label, rows in partitions["holdout"].items():
        for row in encode(rows, vocab):
            for position in range(3, len(row)):
                if row[position] <= 3:
                    continue
                context = ([vocab["<bos>"]] * 12 + row[:position])[-12:]
                cases.append((label, context, row[:position], row[position]))
    rng = random.Random(args.seed)
    rng.shuffle(cases)
    cases = cases[:args.max_cases]
    metrics = collections.defaultdict(lambda: collections.Counter())
    with torch.no_grad():
        for label, context, prefix, correct in cases:
            offered, native_scores = offer(prefix, index, counts)
            if not offered:
                continue
            data = metrics[label]
            data["cases"] += 1
            found = correct in offered
            data["candidate_recall_at_12"] += found
            data["ngram_recall_at_3"] += correct in offered[:3]
            data["ngram_top_1"] += correct == offered[0]
            # Score only generated words. In particular, do NOT insert the
            # held-out answer into model candidates as the synthetic train did.
            features = [[score, 1 / (rank + 1), 0., 0.]
                        for rank, score in enumerate(native_scores)]
            tokens = torch.tensor([context], dtype=torch.long)
            candidates = torch.tensor([offered], dtype=torch.long)
            candidate_features = torch.tensor([features], dtype=torch.float32)
            model_scores = model(tokens, candidates, candidate_features)[0]
            ranked = sorted(range(len(offered)), key=lambda i: -model_scores[i].item())
            predicted = [offered[i] for i in ranked]
            data["neural_recall_at_3"] += correct in predicted[:3]
            data["neural_top_1"] += correct == predicted[0]
    report = {}
    for label, counter in metrics.items():
        total = counter["cases"]
        report[label] = {"heldout_cases": total, **{
            k: round(value / total, 4) for k, value in counter.items() if k != "cases"}}
    print(json.dumps({"evidence": "CORPUS_NGRAM_PROXY_NOT_NATIVE_IME",
                      "quality_gate_passed": False,
                      "results": report}, indent=2, sort_keys=True))


if __name__ == "__main__":
    main()
