#!/usr/bin/env python3
"""Offline-only BR ContextLM experiment; never accepts private keyboard history.

This experimental training program does not authorize shipping a model. Training
corpora need an independently reviewed redistribution/provenance manifest, native
IME paired benchmarks, Oppo latency, and signing/size gates before APK inclusion.
"""
from __future__ import annotations

import argparse
import ast
import collections
import csv
import hashlib
import json
import math
import random
import re
from pathlib import Path

import torch
import torch.nn as nn
import torch.nn.functional as F


TOKEN = re.compile(r"[a-z]+(?:'[a-z]+)?", re.IGNORECASE)
FORBIDDEN = re.compile(r"https?://|www\.|\b[\w.]+@[\w.]+\b|@[a-z0-9_]{2,}", re.I)
SPECIAL = ("<pad>", "<unk>", "<bos>", "<eos>")
SOURCE_REVISIONS = {
    "COMI-LINGUA": "0214cb358e59e36da60e01d3d59ddd24df897749",
    "HinGE": "e25a10382d9adb77f3a4f03b721fbd0a43c0de4c",
}


def tokenize(text: str) -> list[str]:
    if FORBIDDEN.search(text) or len(text) > 1000:
        return []
    result = [word.lower() for word in TOKEN.findall(text)]
    return result if 3 <= len(result) <= 64 else []


def source_rows(root: Path):
    """Yield a group identifier and publicly licensed text, never the phone's data."""
    sources = (
        ("COMI_TN_train.csv", "tn"),
        ("COMI_MT_train.csv", "mt"),
        ("HinGE.csv", "hinge"),
    )
    for filename, family in sources:
        path = root / filename
        if not path.is_file():
            raise FileNotFoundError(f"required corpus absent: {filename}")
        with path.open(encoding="utf-8-sig", newline="") as stream:
            for number, record in enumerate(csv.DictReader(stream), 1):
                if family == "tn":
                    samples = (record.get("Sentences", ""),
                               record.get("Annotated by: Annotator 1", ""))
                elif family == "mt":
                    samples = (record.get("Annotator_1_RH_translation", ""),
                               record.get("Annotator_1_en_translation", ""))
                else:
                    raw = record.get("Human-generated Hinglish", "")
                    try:
                        generated = ast.literal_eval(raw)
                    except (ValueError, SyntaxError):
                        generated = []
                    if not isinstance(generated, list):
                        generated = []
                    samples = tuple(x for x in generated[:3] if isinstance(x, str)) + (
                        record.get("English", ""),)
                for sample in samples:
                    tokens = tokenize(sample)
                    if tokens:
                        yield f"{family}/{number}", tokens


def split_name(group_id: str) -> str:
    # All human/algorithmic variants of a source row stay in the same partition.
    bucket = int.from_bytes(hashlib.sha256(group_id.encode()).digest()[:4], "big") % 100
    return "holdout" if bucket < 10 else ("dev" if bucket < 15 else "train")


def build_records(root: Path):
    partitions = {"train": [], "dev": [], "holdout": []}
    seen = set()
    for group, tokens in source_rows(root):
        signature = tuple(tokens)
        if signature in seen:
            continue
        seen.add(signature)
        partitions[split_name(group)].append(tokens)
    return partitions


def vocabulary(rows: list[list[str]], size: int):
    counts = collections.Counter(word for words in rows for word in words)
    words = SPECIAL + tuple(w for w, n in counts.most_common(size * 2)
                            if n >= 2 and w not in SPECIAL)[:size - len(SPECIAL)]
    return {word: index for index, word in enumerate(words)}


def encode(rows, vocab):
    unk = vocab["<unk>"]
    bos = vocab["<bos>"]
    eos = vocab["<eos>"]
    return [[bos] + [vocab.get(word, unk) for word in row] + [eos] for row in rows]


def examples(rows: list[list[int]], context_length: int, limit: int, seed: int):
    pairs = []
    for row in rows:
        for target in range(2, len(row)):
            if row[target] <= 3:
                continue  # A word-level candidate decoder cannot generate UNK/structural tokens.
            # Sliding context is fixed width with BOS padding; only committed tokens count.
            context = ([row[0]] * context_length + row[:target])[-context_length:]
            pairs.append((context, row[target]))
    rng = random.Random(seed)
    rng.shuffle(pairs)
    return pairs[:limit] if limit else pairs


def build_native_proxy(rows):
    """N-gram candidate offers from training data only; not actual IME output."""
    table = collections.defaultdict(collections.Counter)
    counts = collections.Counter()
    for row in rows:
        for position in range(2, len(row)):
            word = row[position]
            if word <= 3:
                continue
            counts[word] += 1
            for size in (1, 2, 3, 4):
                if position >= size:
                    table[tuple(row[position - size:position])][word] += 1
    return table, counts


def native_proxy_candidates(context, table, counts, candidate_count, exclude_target=None):
    """Use actual ambiguous next-word neighbors rather than easy random negatives."""
    weighted = collections.Counter()
    for size in (4, 3, 2, 1):
        prefix = tuple(context[-size:])
        if len(prefix) != size:
            continue
        matches = table.get(prefix, {})
        total = max(1, sum(matches.values()) - int(exclude_target in matches))
        for word, frequency in sorted(matches.items(), key=lambda x: -x[1])[:32]:
            adjusted = frequency - int(word == exclude_target)
            if adjusted > 0:
                weighted[word] += (0.5 + 0.5 * size) * adjusted / total
    total = max(1, sum(counts.values()) - int(exclude_target in counts))
    for word, frequency in counts.most_common(48):
        adjusted = frequency - int(word == exclude_target)
        if adjusted > 0:
            weighted[word] += 0.1 * adjusted / total
    offered = [word for word, score in weighted.most_common(candidate_count)]
    highest = max((weighted[word] for word in offered), default=1.)
    return offered, [min(1., weighted[word] / highest) for word in offered]


class CausalDFSMN(nn.Module):
    """Trainable, bounded causal memory network with a candidate-personal head."""

    def __init__(self, vocab_size: int, width: int = 128, layers: int = 5, taps: int = 4):
        super().__init__()
        self.embedding = nn.Embedding(vocab_size, width)
        self.projections = nn.ModuleList(nn.Linear(width, width) for _ in range(layers))
        self.memories = nn.Parameter(torch.zeros(layers, taps, width))
        self.personal_base = nn.Parameter(torch.tensor([0.2, 0.1, 0.5, 0.3]))
        self.personal_gate = nn.Linear(width, 4)
        self.context_width = width
        nn.init.normal_(self.embedding.weight, std=0.025)
        nn.init.normal_(self.memories, std=0.02)

    def forward(self, token_ids, candidates, candidate_features):
        state = self.embedding(token_ids)
        for layer, projection in enumerate(self.projections):
            memory = torch.zeros_like(state)
            for lag in range(1, self.memories.shape[1] + 1):
                memory[:, lag:, :] += state[:, :-lag, :] * self.memories[layer, lag - 1]
            state = torch.tanh(projection(state) + memory)
        latest = state[:, -1, :]
        candidate_words = self.embedding(candidates)
        lexical_score = torch.bmm(candidate_words, latest.unsqueeze(2)).squeeze(2)
        lexical_score = lexical_score / math.sqrt(self.context_width)
        personal_weight = self.personal_base + torch.tanh(self.personal_gate(latest))
        personal_score = torch.sum(candidate_features * personal_weight[:, None, :], dim=2)
        return lexical_score + personal_score


def minibatch(items, unigram, candidate_count, seed, native_proxy=None):
    rng = random.Random(seed)
    token_ids = []
    candidate_rows = []
    features = []
    for context, target in items:
        if native_proxy is None:
            offered = [target]
            native_values = []
        else:
            offered, native_values = native_proxy_candidates(
                context, native_proxy[0], native_proxy[1], candidate_count)
            if target not in offered:
                # Only training uses this forced target: production evaluation must never
                # inject the answer when the decoder failed candidate recall.
                offered = offered[:candidate_count - 1] + [target]
                native_values = native_values[:candidate_count - 1] + [.005]
        while len(offered) < candidate_count:
            word = rng.choice(unigram)
            if word not in offered:
                offered.append(word)
                native_values.append(.01)
        if native_proxy is None:
            rng.shuffle(offered)
        pos = offered.index(target)
        pseudo_native = native_values if native_proxy is not None else [
            rng.uniform(0.03, 0.93) for _ in offered]
        if native_proxy is None and rng.random() < 0.55:
            pseudo_native[pos] = min(1., pseudo_native[pos] + .21)
        native_order = sorted(range(candidate_count), key=lambda i: -pseudo_native[i])
        ranks = [0] * candidate_count
        for rank, index in enumerate(native_order):
            ranks[index] = rank
        # Public corpora contain no consented per-user history. Training with
        # fabricated history/personal signals creates a train/inference mismatch
        # because production must pass zeros for these features. Keep those slots
        # zero; the separate encrypted personalization adapter may use them only
        # after it has genuine, opt-in signals and its own holdout evaluation.
        row = [[pseudo_native[i], 1. / (ranks[i] + 1), 0., 0.]
               for i in range(candidate_count)]
        token_ids.append(context)
        candidate_rows.append(offered)
        features.append(row)
    # Deliberately return target positions; synthetic personal features are not real accuracy.
    return (torch.tensor(token_ids, dtype=torch.long),
            torch.tensor(candidate_rows, dtype=torch.long),
            torch.tensor(features, dtype=torch.float32),
            torch.tensor([row.index(target) for row, (_, target) in zip(candidate_rows, items)],
                         dtype=torch.long))


def evaluate(model, pairs, unigram, candidate_count, batch_size, seed, native_proxy=None):
    model.eval()
    correct = 0
    native_correct = 0
    personal_ablation_correct = 0
    total = 0
    with torch.no_grad():
        for offset in range(0, len(pairs), batch_size):
            tokens, candidates, feats, targets = minibatch(
                pairs[offset:offset + batch_size], unigram, candidate_count,
                seed + offset, native_proxy)
            scores = model(tokens, candidates, feats)
            zero_features = torch.zeros_like(feats)
            without_personal = model(tokens, candidates, zero_features)
            correct += (scores.argmax(1) == targets).sum().item()
            native_correct += (feats[:, :, 0].argmax(1) == targets).sum().item()
            personal_ablation_correct += (without_personal.argmax(1) == targets).sum().item()
            total += len(targets)
    return {"synthetic_candidates": total,
            "trained_candidate_top1": round(correct / max(1, total), 4),
            "synthetic_native_proxy_top1": round(native_correct / max(1, total), 4),
            "without_personal_features_top1": round(personal_ablation_correct / max(1, total), 4),
            "native_IME_accuracy_measured": False,
            "Oppo_latency_measured": False}


def export_weights(model, output: Path):
    """Deterministic INT8 test artifact; not installable until Android runtime verifies it."""
    params = []
    with (output / "weights.int8").open("wb") as weights:
        for name, tensor in sorted(model.state_dict().items()):
            values = tensor.detach().cpu().contiguous().float()
            scale = max(float(values.abs().max().item()) / 127, 1e-9)
            quantized = torch.clamp(torch.round(values / scale), -127, 127).to(torch.int8)
            encoded = quantized.numpy().tobytes()
            params.append({"name": name, "shape": list(values.shape), "scale": scale,
                           "offset": weights.tell(), "bytes": len(encoded)})
            weights.write(encoded)
    return params


def sha256(path: Path):
    digest = hashlib.sha256()
    with path.open("rb") as f:
        for block in iter(lambda: f.read(1024 * 1024), b""):
            digest.update(block)
    return digest.hexdigest()


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--corpus-dir", required=True, type=Path)
    p.add_argument("--output", required=True, type=Path)
    p.add_argument("--epochs", type=int, default=1)
    p.add_argument("--max-examples", type=int, default=80000)
    p.add_argument("--vocab", type=int, default=8192)
    p.add_argument("--batch-size", type=int, default=128)
    p.add_argument("--candidates", type=int, default=16)
    p.add_argument("--seed", type=int, default=2827)
    args = p.parse_args()
    if not 2 <= args.candidates <= 64 or args.epochs < 1 or args.vocab < 100:
        p.error("invalid bounded training settings")
    torch.manual_seed(args.seed)
    torch.set_num_threads(min(6, torch.get_num_threads()))
    partitions = build_records(args.corpus_dir)
    vocab = vocabulary(partitions["train"], args.vocab)
    train = examples(encode(partitions["train"], vocab), 12, args.max_examples, args.seed)
    dev = examples(encode(partitions["dev"], vocab), 12, 1500, args.seed + 1)
    frequency = collections.Counter(target for _, target in train)
    sampling = [word for word, _ in frequency.most_common(512) if word > 3]
    native_proxy = build_native_proxy(encode(partitions["train"], vocab))
    model = CausalDFSMN(len(vocab))
    optimizer = torch.optim.AdamW(model.parameters(), lr=0.001, weight_decay=0.01)
    print(json.dumps({"train_pairs": len(train), "dev_pairs": len(dev),
                      "vocab": len(vocab), "model_params": sum(p.numel() for p in model.parameters()),
                      "data_partition_rows": {k: len(v) for k, v in partitions.items()}}, sort_keys=True), flush=True)
    for epoch in range(args.epochs):
        model.train()
        order = list(range(len(train)))
        random.Random(args.seed + epoch).shuffle(order)
        mean_loss = 0.
        n = 0
        for start in range(0, len(order), args.batch_size):
            chosen = [train[i] for i in order[start:start + args.batch_size]]
            tokens, candidates, features, targets = minibatch(
                chosen, sampling, args.candidates, args.seed + epoch * 1000000 + start,
                native_proxy)
            optimizer.zero_grad(set_to_none=True)
            loss = F.cross_entropy(model(tokens, candidates, features), targets)
            loss.backward()
            nn.utils.clip_grad_norm_(model.parameters(), 1.)
            optimizer.step()
            mean_loss += loss.item() * len(chosen)
            n += len(chosen)
        report = evaluate(model, dev, sampling, args.candidates, args.batch_size,
                          args.seed + epoch * 1111, native_proxy)
        print(json.dumps({"epoch": epoch + 1, "loss": mean_loss / max(1, n),
                          "development": report}), flush=True)
        # A long CPU training run should be recoverable if interrupted.
        args.output.mkdir(parents=True, exist_ok=True)
        torch.save(model.state_dict(), args.output / f"epoch_{epoch + 1}.pt")
    args.output.mkdir(parents=True, exist_ok=True)
    weights = export_weights(model, args.output)
    vocab_path = args.output / "vocab.json"
    vocab_path.write_text(json.dumps(vocab, ensure_ascii=False, separators=(",", ":")), encoding="utf-8")
    manifest = {"status": "UNVALIDATED_TRAINING_EXPERIMENT_NOT_FOR_APK",
                "architecture": "causal_dfsmn_5x128_taps4_candidate_head",
                "sources": SOURCE_REVISIONS,
                "sources_sha256": {path.name: sha256(path) for path in args.corpus_dir.glob("*.csv")},
                "parameters": weights, "vocab_count": len(vocab),
                "weights_sha256": sha256(args.output / "weights.int8"),
                "vocab_sha256": sha256(vocab_path),
                "trained_candidate_head_synthetic_not_native_ground_truth": True,
                "do_not_ship_until_accuracy_latency_licensing_size_tests_pass": True}
    (args.output / "manifest.json").write_text(json.dumps(manifest, indent=2), encoding="utf-8")
    torch.save(model.state_dict(), args.output / "train_checkpoint.pt")
    print(json.dumps({"int8_bytes": (args.output / "weights.int8").stat().st_size,
                      "result": "EXPERIMENTAL_ONLY"}), flush=True)


if __name__ == "__main__":
    main()
