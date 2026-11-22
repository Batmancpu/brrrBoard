#!/usr/bin/env python3
"""Matched offline Parakeet WER/latency comparison on one local WAV manifest.

Manifest: JSONL rows with ``audio`` (16 kHz mono WAV), ``reference`` and an
optional coarse ``cohort``. The script runs both models on identical clips,
uses one normalizer and reports only aggregate metrics. It never prints audio,
references, or hypotheses. Install sherpa-onnx and soundfile in an isolated
Python environment; model files and evaluation audio stay outside the repo.
"""
from __future__ import annotations

import argparse
import hashlib
import json
import platform
import random
import re
import statistics
import time
import unicodedata
from collections import defaultdict
from pathlib import Path


def normalize(text: str) -> list[str]:
    text = unicodedata.normalize("NFKC", text).casefold()
    # Match the official sherpa FLEURS policy: case/punctuation stripped.
    return re.findall(r"[a-z0-9]+(?:'[a-z0-9]+)?", text)


def distance(reference: list[str], hypothesis: list[str]) -> int:
    previous = list(range(len(hypothesis) + 1))
    for row, ref in enumerate(reference, 1):
        current = [row]
        for col, hyp in enumerate(hypothesis, 1):
            current.append(min(current[-1] + 1, previous[col] + 1,
                               previous[col - 1] + (ref != hyp)))
        previous = current
    return previous[-1]


def make_recognizer(model_dir: Path, threads: int):
    try:
        import sherpa_onnx
    except ImportError as error:
        raise SystemExit("Install sherpa-onnx in the benchmark environment") from error
    required = [model_dir / name for name in
                ("encoder.onnx", "decoder.onnx", "joiner.onnx", "tokens.txt")]
    missing = [str(path) for path in required if not path.is_file()]
    if missing:
        raise SystemExit("Missing model files: " + ", ".join(missing))
    return sherpa_onnx.OfflineRecognizer.from_transducer(
        encoder=str(required[0]), decoder=str(required[1]),
        joiner=str(required[2]), tokens=str(required[3]),
        num_threads=threads, sample_rate=16000, feature_dim=80,
        decoding_method="greedy_search", provider="cpu", model_type="nemo_transducer",
    )


def fingerprint(model_dir: Path):
    manifest = {}
    for name in ("encoder.onnx", "decoder.onnx", "joiner.onnx", "tokens.txt"):
        path = model_dir / name
        digest = hashlib.sha256()
        with path.open("rb") as stream:
            for block in iter(lambda: stream.read(1024 * 1024), b""):
                digest.update(block)
        manifest[name] = {"bytes": path.stat().st_size, "sha256": digest.hexdigest()}
    return manifest


def load_pcm(path: Path):
    try:
        import soundfile as sf
        import numpy as np
    except ImportError as error:
        raise SystemExit("Install soundfile and numpy in the benchmark environment") from error
    samples, sample_rate = sf.read(str(path), dtype="float32", always_2d=True)
    if sample_rate != 16000 or samples.shape[1] != 1:
        raise ValueError(f"Expected 16 kHz mono WAV, got {sample_rate} Hz/{samples.shape[1]} channels")
    return np.ascontiguousarray(samples[:, 0]), len(samples) / sample_rate


def decode(recognizer, samples):
    stream = recognizer.create_stream()
    try:
        stream.accept_waveform(16000, samples)
        recognizer.decode_stream(stream)
        return stream.result.text
    finally:
        stream = None


def summarize(records):
    errors = sum(item["errors"] for item in records)
    words = sum(item["words"] for item in records)
    latencies = [item["seconds"] for item in records]
    duration = sum(item["audio_seconds"] for item in records)
    return {
        "clips": len(records), "reference_words": words,
        "wer": round(errors / max(1, words), 5),
        "audio_hours": round(duration / 3600, 3),
        "decode_seconds_p50": round(statistics.median(latencies), 3) if latencies else None,
        "decode_seconds_p95": round(sorted(latencies)[max(0, int(.95 * len(latencies)) - 1)], 3)
            if latencies else None,
        "real_time_factor": round(sum(latencies) / max(1e-9, duration), 4),
    }


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--manifest", type=Path, required=True)
    parser.add_argument("--model-110m", type=Path, required=True)
    parser.add_argument("--model-ultra", type=Path, required=True)
    parser.add_argument("--threads", type=int, default=2)
    parser.add_argument("--bootstrap", type=int, default=2000)
    parser.add_argument("--max-clips", type=int, default=0,
                        help="reproducible random exploratory subset; 0 evaluates every manifest row")
    parser.add_argument("--seed", type=int, default=2827)
    args = parser.parse_args()
    if args.threads < 1 or args.threads > 8 or args.bootstrap < 100:
        parser.error("threads must be 1..8 and bootstrap must be at least 100")

    rows = [json.loads(line) for line in args.manifest.read_text(encoding="utf-8").splitlines()
            if line.strip()]
    if not rows:
        parser.error("manifest contains no audio rows")
    if args.max_clips < 0:
        parser.error("max-clips must be zero or greater")
    if args.max_clips and args.max_clips < len(rows):
        rows = random.Random(args.seed).sample(rows, args.max_clips)
    clips = []
    for row in rows:
        audio = Path(row["audio"]).resolve()
        if not audio.is_file():
            raise SystemExit(f"Audio file missing: {audio}")
        samples, seconds = load_pcm(audio)
        reference = normalize(row["reference"])
        if not reference:
            continue
        clips.append((samples, seconds, reference, str(row.get("cohort", "all"))))
    if not clips:
        parser.error("manifest has no usable references")

    results = defaultdict(list)
    by_model = {"110m": [], "ultra": []}
    for label, model_dir in (("110m", args.model_110m), ("ultra", args.model_ultra)):
        recognizer = make_recognizer(model_dir, args.threads)
        for samples, seconds, reference, cohort in clips:
            start = time.perf_counter()
            hypothesis = normalize(decode(recognizer, samples))
            elapsed = time.perf_counter() - start
            errors = distance(reference, hypothesis)
            record = {"errors": errors, "words": len(reference), "seconds": elapsed,
                      "audio_seconds": seconds}
            if cohort != "all":
                results[(label, cohort)].append(record)
            by_model[label].append(record)
            results[(label, "all")].append(record)
        del recognizer

    # Paired bootstrap over utterances; each draw keeps the same audio/reference
    # for both models and reports the relative WER change with percentile bounds.
    rng = random.Random(args.seed)
    baseline, candidate = by_model["110m"], by_model["ultra"]
    deltas = []
    relative = []
    for _ in range(args.bootstrap):
        selected = [rng.randrange(len(clips)) for _ in clips]
        base_words = sum(baseline[index]["words"] for index in selected)
        base_errors = sum(baseline[index]["errors"] for index in selected)
        ultra_errors = sum(candidate[index]["errors"] for index in selected)
        base_wer = base_errors / max(1, base_words)
        ultra_wer = ultra_errors / max(1, base_words)
        deltas.append(ultra_wer - base_wer)
        relative.append((ultra_wer - base_wer) / max(1e-9, base_wer))
    deltas.sort()
    relative.sort()
    report_ci = {
        "ultra_minus_110m_wer_95pct": [round(deltas[int(.025 * len(deltas))], 5),
                                       round(deltas[min(len(deltas)-1, int(.975 * len(deltas)))], 5)],
        "relative_wer_change_95pct": [round(relative[int(.025 * len(relative))], 4),
                                       round(relative[min(len(relative)-1, int(.975 * len(relative)))], 4)],
        "negative_relative_change_means_ultra_improved": True,
    }

    try:
        import sherpa_onnx
        runtime_version = sherpa_onnx.__version__
    except ImportError:
        runtime_version = "unavailable"
    report = {"evaluation": "matched_local_public_audio_same_normalizer_greedy_cpu",
              "clips": len(clips), "threads": args.threads, "seed": args.seed,
              "runtime_version": runtime_version,
              "host": {"system": platform.system(), "machine": platform.machine(),
                       "processor": platform.processor()},
              "model_files": {"110m": fingerprint(args.model_110m),
                              "ultra": fingerprint(args.model_ultra)},
              "paired_bootstrap": report_ci,
              "results": {f"{model}/{cohort}": summarize(records)
                          for (model, cohort), records in sorted(results.items())}}
    print(json.dumps(report, indent=2, sort_keys=True))


if __name__ == "__main__":
    main()
