# Adaptive offline voice batching (2026-11-24)

## Research and decisions
- Keep native brrClean GTCRN: existing weights 211,220 bytes; upstream GTCRN corrected configuration about 48.2k parameters and 33 MMAC/s. No matched device WER evidence establishes that replacing it with RNNoise, DeepFilterNet3 or another denoiser improves this keyboard. See https://github.com/Xiaobin-Rong/gtcrn .
- Silero VAD v6.2.3: MIT, 2,327,524 bytes, pinned commit 5cd7945676eb32225748052e2e6a0580e4686a08, SHA-256 1a153a22f4509e292a94e67d6f9b85e8deb25b4988682b7e174c65279d8788e3. See https://github.com/snakers4/silero-vad and https://github.com/snakers4/silero-vad/discussions/817 .
- Smart Turn v3.2 CPU INT8: BSD-2-Clause, 8,679,182 bytes, pinned model revision f766f81, SHA-256 2bb026316b14a660486a75b1733cd3fbab8c2fd0314dc9af7be49f8cca967e4f. See https://huggingface.co/pipecat-ai/smart-turn-v3 . Published multilingual turn prediction benchmark (not dictation WER): 92.63% overall, English 94.26%, Hindi 90.11%. These results are not measured on Reno10.
- Model asset increase: 11,006,706 bytes before compression. Reuse the sherpa ONNX runtime; native C API adapter and matching build headers; no second runtime. Preserve installed transcription weights and existing signing.

## Integration
Dedicated session-local voice worker owns the models, their state and audio buffers. Neural speech probability replaces the amplitude gate when available. Smart Turn can defer a quiet-boundary flush, but never ends recording or sends a message. Six-second hard batch cap, 320 ms overlap, cancellation and byte conservation retained. No destructive silence trimming, to avoid deleting weak speech; retain the legacy amplitude gate if the neural model fails. Default-on toggle in Offline voice settings. Feature extraction adapts MIT Marmalade WhisperFeatures (blob 437b863cb8fb18fee5be78162030325f0dcfeda2), with attribution to upstream Daily and Hugging Face. Model binaries and build headers downloaded only by build tooling and SHA-256 checked. Device inference entirely offline.

## Validation
Host CPU model graph smoke test, JUnit chunk continuity/fallback/semantic delay, existing full debug tests, and signed release packaging verification are required. Candidate pending CI and device acceptance; do not infer phone latency or actual WER improvement from host tests. Test English and Hindi, faint speech, noisy fan/traffic, natural pauses, a two-minute session, cancellation, and voice settings off/on on Reno10. Compare paired WER and latency before changing denoising or aggressively removing silence.
