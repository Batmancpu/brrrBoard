#!/usr/bin/env python3
"""Dev-tuned conservative fusion; heldout results are only an offline proxy.

The native Android dictionary is not represented by these public-corpus n-grams.
Never use this report alone to approve a bundled ContextLM or claim owner-device gain.
"""
from __future__ import annotations

import argparse
import collections
import json
import random
from pathlib import Path

import torch

from evaluate_candidate_recall import index_ngrams, offer, rows_by_source
from train_dfsmn import CausalDFSMN, encode


def generate_cases(partitions, vocab, table, counts, label, seed, cap):
    items = []
    for family, rows in partitions[label].items():
        for row in encode(rows, vocab):
            for position in range(3, len(row)):
                if row[position] <= 3:
                    continue
                native, scores = offer(row[:position], table, counts)
                if not native:
                    continue
                context = ([vocab["<bos>"]] * 12 + row[:position])[-12:]
                items.append((family, context, native, scores, row[position]))
    random.Random(seed).shuffle(items)
    return items[:cap]


def scored_cases(cases, model):
    result = []
    model.eval()
    with torch.no_grad():
        for family, context, native, native_scores, target in cases:
            features = [[score, 1. / (rank + 1), 0., 0.]
                        for rank, score in enumerate(native_scores)]
            output = model(torch.tensor([context]), torch.tensor([native]),
                           torch.tensor([features]))[0].tolist()
            low, high = min(output), max(output)
            neural = [(v - low) / (high - low) if high > low else 0. for v in output]
            result.append((family, native, native_scores, neural, target))
    return result


def metrics(cases, neural_fraction):
    tally = collections.defaultdict(collections.Counter)
    for family, native, native_scores, neural_scores, target in cases:
        combined = [(1. - neural_fraction) * n + neural_fraction * m
                    for n, m in zip(native_scores, neural_scores)]
        chosen = sorted(range(len(native)), key=lambda i: -combined[i])[:3]
        tally[family]["cases"] += 1
        tally[family]["recall_at_3"] += target in [native[i] for i in chosen]
    per_family = {family: round(row["recall_at_3"] / row["cases"], 4)
                  for family, row in tally.items()}
    weighted = sum(row["recall_at_3"] for row in tally.values()) / max(
        1, sum(row["cases"] for row in tally.values()))
    return round(weighted, 5), per_family


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--corpus-dir", required=True, type=Path)
    parser.add_argument("--checkpoint", required=True, type=Path)
    parser.add_argument("--vocab", required=True, type=Path)
    parser.add_argument("--seed", default=2827, type=int)
    parser.add_argument("--max-cases", default=1600, type=int)
    args = parser.parse_args()
    torch.set_num_threads(min(4, torch.get_num_threads()))
    vocab = json.loads(args.vocab.read_text(encoding="utf-8"))
    parts = rows_by_source(args.corpus_dir)
    train = encode([row for family in parts["train"].values() for row in family], vocab)
    table, counts = index_ngrams(train)
    network = CausalDFSMN(len(vocab))
    network.load_state_dict(torch.load(args.checkpoint, map_location="cpu", weights_only=True))
    dev = scored_cases(generate_cases(parts, vocab, table, counts, "dev", args.seed,
                                      args.max_cases), network)
    grid = [0., 0.1, 0.2, 0.3, 0.4, 0.5, 0.6, 0.8, 1.]
    dev_comparison = {str(weight): metrics(dev, weight) for weight in grid}
    # Tie goes to the smaller neural fraction: no unverified reranking is safer.
    selected = max(grid, key=lambda weight: (dev_comparison[str(weight)][0], -weight))
    holdout = scored_cases(generate_cases(parts, vocab, table, counts, "holdout",
                                          args.seed + 1, args.max_cases), network)
    baseline = metrics(holdout, 0.)
    fused = metrics(holdout, selected)
    print(json.dumps({"evidence": "DEV_TUNED_PUBLIC_NGRAM_PROXY_NOT_NATIVE_IME",
                      "selected_neural_fraction": selected, "development": dev_comparison,
                      "holdout_native_proxy": baseline,
                      "holdout_fusion": fused,
                      "quality_gate_passed": False,
                      "native_IME_accuracy_measured": False,
                      "owner_phone_latency_measured": False}, indent=2, sort_keys=True))


if __name__ == "__main__":
    main()
