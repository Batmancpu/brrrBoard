# Capture-first performance pass — 2026-11-24

Owner requested no model-load wait before speaking, ordered RAM audio, and a wider app efficiency review. Base 704e742250c2c323cfe4c016e325275bf43ce8ee. Latest prior signed release v1.0.260; neural endpoint and speech-protection changes are pending CI.

## Source findings
- BoardFooterView used onSingleTapConfirmed for idle mic taps, imposing Android's double-tap timeout before starting a session. A truly immediate single tap and a double tap that never opens the microphone are mutually exclusive on the same control.
- OfflineBoardDictation previously enqueued ASR warm-up before starting AudioRecord. Native model inference already runs off the UI thread, but CPU/storage contention can delay mic initialization. Hardware/permission/routing setup cannot be removed. Model pack metadata checks remain fast-path preconditions (file sizes and verified marker, not full model hashing).
- Recorder used the full hardware buffer as its read size. Separate 40ms reads retain hardware buffer capacity.
- Streaming pipeline already uses ordered worker tasks and bounded capture duration; brrClean then ASR are serial per batch. Isolated denoise process presently reinitializes weights/VAD per request. ASR warm-up refreshes idle lease. No claim of measured latency gain.

## Implemented
Immediate idle tap on UP, disable GestureDetector double tap listener by default; optional legacy shortcut restores delay. Keep hold-to-talk, TalkBack and protected mic restrictions. Starting/recording/decoding can stop immediately. Prioritize AudioRecord worker; copy every captured PCM frame, then load Silero on capture worker, enqueue Parakeet warm on its own worker, prepare Smart Turn; no model load on audio thread. All frames queue in order during preparation and wipe after use/cancel. 16k mono PCM16 uses 32,000 bytes/sec (6s = 192,000 bytes, 170s = 5.44 MB before copies/overlap). Record only elapsed time from validated controller request to first PCM in volatile metrics. Keep call checks, selected model validation, original signing, no Internet, installed model and privacy safeguards.

## Next efficiency work (measurement gates first)
1. Device measure request-to-first-PCM, first partial, per-stage real-time factor, queue high-water, peak PSS, thermal/battery for 2-minute dictation. A/B paired audio against prior signed release: word error rate, deletion rate and noise conditions. Target steady-state processing below capture duration, not a promised number.
2. Reuse isolated denoiser process/model per session with bounded lifetime, explicit cancellation, verified request IDs and timeout/crash fallback; do not remove isolation. Avoid concurrent child cleanup race. Pipeline clean batch N+1 during ASR N using at most one prefetched batch and ordered delivery; measure peak RSS first.
3. Reduce redundant VAD calls and PCM/FloatArray copies, use bounded pool only after byte conservation and cancellation tests. Avoid unsafe destructive silence removal or routine double ASR.
4. Audit UI hot paths: glass RenderNode invalidation/blur, keyboard layout recomputation, panel inflation, suggestion/native scoring and dictionary loading, clipboard/emoji indexing, settings list and startup work. Perfetto/frame metrics and cold/warm benchmarks before changing hot paths. Prioritize avoiding main-thread I/O and repeated allocation over cosmetic changes. Do not warm models without an explicit mic action.
5. Retain ASR 90s idle unload for now; test memory-pressure callback and release policy, never keep Ultra resident indefinitely.
6. Run exact SHA debug tests and signed workflow, physical Oppo test: rapid taps during STARTING, hold/release/cancel, legacy double-tap with no microphone, switch editors, protected mode, first syllables, cold/warm 120 seconds. No physical validation yet.
