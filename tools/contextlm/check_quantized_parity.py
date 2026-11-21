#!/usr/bin/env python3
"""Check exported INT8 storage against the trained float scorer before Android use."""
from __future__ import annotations

import argparse
import hashlib
import json
from pathlib import Path

import numpy as np
import torch

from train_dfsmn import CausalDFSMN


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--output", type=Path, required=True)
    p.add_argument("--samples", type=int, default=512)
    args = p.parse_args()
    torch.set_num_threads(min(4, torch.get_num_threads()))
    manifest = json.loads((args.output / "manifest.json").read_text())
    vocab = json.loads((args.output / "vocab.json").read_text())
    binary = (args.output / "weights.int8").read_bytes()
    assert hashlib.sha256(binary).hexdigest() == manifest["weights_sha256"]
    assert hashlib.sha256((args.output / "vocab.json").read_bytes()).hexdigest() == manifest["vocab_sha256"]
    model = CausalDFSMN(len(vocab))
    model.load_state_dict(torch.load(args.output / "train_checkpoint.pt", map_location="cpu",
                                     weights_only=True))
    approximate = CausalDFSMN(len(vocab))
    tensors = {}
    for item in manifest["parameters"]:
        data = np.frombuffer(binary, dtype=np.int8, count=item["bytes"], offset=item["offset"])
        array = (data.astype(np.float32) * item["scale"]).reshape(item["shape"])
        tensors[item["name"]] = torch.from_numpy(array.copy())
    approximate.load_state_dict(tensors)
    model.eval()
    approximate.eval()
    generator = torch.Generator().manual_seed(87)
    matches = 0
    worst = 0.0
    with torch.no_grad():
        for offset in range(0, args.samples, 64):
            count = min(64, args.samples - offset)
            contexts = torch.randint(0, len(vocab), (count, 12), generator=generator)
            candidates = torch.randint(0, len(vocab), (count, 12), generator=generator)
            features = torch.rand((count, 12, 4), generator=generator)
            float_scores = model(contexts, candidates, features)
            int8_scores = approximate(contexts, candidates, features)
            matches += (float_scores.argmax(1) == int8_scores.argmax(1)).sum().item()
            worst = max(worst, float((float_scores - int8_scores).abs().max().item()))
    output = {"int8_storage_bytes": len(binary), "samples": args.samples,
              "top1_agreement": matches / args.samples, "worst_score_delta": round(worst, 5),
              "pass_reference_parity": matches / args.samples >= .99,
              "Android_device_performance_tested": False}
    print(json.dumps(output, indent=2))
    if not output["pass_reference_parity"]:
        raise SystemExit("Failed reference parity; do not ship these assets")


if __name__ == "__main__":
    main()
