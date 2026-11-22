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
    # Reservoir-sample from the complete development/holdout partition. The
    # earlier implementation collected every token position and only then
    # shuffled/truncated, which wasted memory and made a capped calibration
    # needlessly expensive on constrained hosts.
    rng = random.Random(seed)
    items = []
    seen = 0
    for family, rows in partitions[label].items():
        for row in encode(rows, vocab):
            for position in range(3, len(row)):
                if row[position] <= 3:
                    continue
                native, scores = offer(row[:position], table, counts)
                if not native:
                    continue
                context = ([vocab["<bos>"]] * 12 + row[:position])[-12:]
                item = (family, context, native, scores, row[position])
                seen += 1
                if cap <= 0 or len(items) < cap:
                    items.append(item)
                else:
                    replacement = rng.randrange(seen)
                    if replacement < cap:
                        items[replacement] = item
    return items


def scored_cases(cases, model):
    result = []
    model.eval()
    with torch.no_grad():
        # Batch cases rather than making one tiny PyTorch/ORT-shaped call per
        # utterance. Candidate counts vary, so pad with <pad> and ignore those
        # tail scores after inference. This keeps the offline sweep fast enough
        # to run routinely and does not change the candidate ranking semantics.
        for offset in range(0, len(cases), 64):
            batch = cases[offset:offset + 64]
            width = max(len(item[2]) for item in batch)
            token_rows = []
            candidate_rows = []
            feature_rows = []
            for _, context, native, native_scores, _ in batch:
                token_rows.append(context)
                candidate_rows.append(native + [0] * (width - len(native)))
                feature_rows.append(
                    [[score, 1. / (rank + 1), 0., 0.]
                     for rank, score in enumerate(native_scores)]
                    + [[0., 0., 0., 0.]] * (width - len(native)))
            outputs = model(torch.tensor(token_rows, dtype=torch.long),
                            torch.tensor(candidate_rows, dtype=torch.long),
                            torch.tensor(feature_rows, dtype=torch.float32)).tolist()
            for item, output in zip(batch, outputs):
                family, _, native, native_scores, target = item
                output = output[:len(native)]
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
