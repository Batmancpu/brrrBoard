
## 2026-11-21 — GO software delivery verified: signed v1.0.217

The owner authorized the saved next-GO plan. The focused migration fix and voice documentation update shipped from `7910d96aeba5b2c45cadfaab6502218eaf31be0f` as original-signed **v1.0.217**. Debug workflow **44897236271** and Obtainium workflow **44897236284** both completed successfully on that exact SHA. Published APK: https://github.com/Batmancpu/brrrBoard/releases/download/v1.0.217/brrBoard.apk — **64,969,203 bytes**, SHA-256 `348309bdfbece47fd6d8270c3378306ac4ef4bc5cad660f36b2b45523f6b43a4`. Release is non-prerelease and package/certificate and pinned voice packaging gates passed. The release change preserves legacy Android NoiseSuppressor ON/OFF preferences across upgrade while new installs default to brrClean, adds regression cases, and corrects README voice mode/streaming claims. Focused Python voice packaging/privacy contracts passed (8 + 5); JSON and `git diff --check` passed. Local Gradle could not download 8.14.4 through the runtime proxy, so the exact-SHA Android workflows were used.

Current main is a documentation checkpoint after the signed source SHA. Existing QuickType/native strip improvements are present from earlier signed releases; the neural ContextLM remains unshipped because independent held-out candidate quality was worse than native. Oppo visual, mic/WER and install/update acceptance remain device checks, not claimed passed. The build is ready for the owner’s Oppo evaluation.


## 2026-11-21 — owner GO: preserve legacy voice noise choices, exact-SHA release pending

Owner's **Go** authorizes the saved [next GO master plan](docs/NEXT_GO_MASTER_PLAN_2026_11_21.md). Fresh main baseline is `34d154560e0a49bc7b52c21ac421fc5bb71288b8`; latest signed release remains v1.0.216 (`f3d1c207`). The first source change fixes a confirmed migration defect: the pre-mode boolean was read with a default that sent saved Android NoiseSuppressor users to brrClean. An absent old key now selects the new-install brrClean default; a present `true` maps to Android and present `false` maps to OFF; current explicit mode wins. Added focused policy regression cases and corrected README voice claims to match current brrClean streaming source. ContextLM remains unshipped because untouched holdout quality failed. Local Gradle cannot download 8.14.4 through this runtime proxy; exact-SHA GitHub debug/full Android and original-signer CI are pending. This is not yet compiled, tested, signed, or Oppo-accepted. Preserve the model, no-network permission, package and certificate.

# brrClean always-on offline streaming dictation — 21 Nov 2026

## Owner acceptance and new instructions

Owner tested brrClean successfully without earbuds, and explicitly wants it
to become the normal blue microphone's default. They want excellent sound
from the phone's own microphones, best-effort access to multi-mic hardware,
noise suppression for daily household/traffic sounds, and 2+ minute dictated
utterances processed *during* speech rather than waiting for the recording to
end. Exact user request: if GitHub Actions is idle, implement and poll the
workflows to success; hand off a real original-signed Obtainium APK. Latest
signed predecessor: **v1.0.212**, `f96964dc`, with successful exact-SHA debug
`44874929118` and original-signer `44874929134` before this change.

## Architecture

- `VoiceNoiseChoice`: brand-new installs default to brrClean/GTCRN. Existing
  explicit OFF or Android preference remains authoritative. A migrated legacy
  effect-OFF preference still maps to OFF; do not forcibly re-enable a user's
  explicitly disabled microphone processing.
- The *normal blue mic* requests `TYPE_BUILTIN_MIC` with `setPreferredDevice`
  from Android's own input list. No Bluetooth SCO/LE communication activation
  in regular keyboard dictation; Bluetooth remains separately selectable only
  in the user-invoked BR Brain laboratory. Capture remains 16-kHz PCM16 mono,
  matching both GTCRN and Parakeet: uncompressed audio, no Bluetooth codec.
  The phone/HAL may already beamform or combine multiple physical microphones;
  this app cannot assume access to all separate raw phone and earbud streams.
  Android 10+ active recording configurations are queried defensively once;
  if Android confirms another input/silencing, the recording fails closed.
  Route remains explicitly *unverified* when Android reports no route. Never
  replace this with `AudioRecord.routedDevice` (prior Oppo native crash suspect).
- `PrivateVoiceChunker` runs on AudioRecord's capture thread but only does
  bounded PCM copying and a small 100ms absolute-energy scan once per ~6s.
  Chunk = 5–6s, boundary chosen at the quietest 100ms slice, overlap **320ms**.
  Every sample is preserved except deliberate duplicate overlap. A pending
  chunk is immediately dispatched to the pipeline; waiting for the next
  recording to end is no longer required. Capture maximum remains **170s**,
  which covers an ordinary two-minute monologue.
- `PrivateVoiceStreamingPipeline` owns each voice session. One bounded chunk
  at a time is sent to crash-isolated `BrrCleanVoiceBridge` → native GTCRN;
  native crash/mismatch/timeout sends its *raw* chunk to `OfflineVoiceEngine`.
  The next chunk starts only when prior ASR completes, preserving order and
  avoiding parallel 110M models or duplicate denoiser children. Pending PCM
  remains in volatile memory, bounded by the capture duration, not dumped to
  files. Queued chunks are wiped on cancel. Cancellation/timeout also sends a
  dedicated `CANCEL` command to immediately kill stale native child work.
  One `VoiceDecodeRequest` controls
  native ASR cancellation; a running decode wipes its PCM only after its
  current window finishes. The ASR worker is scheduled at Android background
  priority; neither GTCRN nor Parakeet runs on the touch/UI thread.
- `OfflineVoiceEngine.transcribe(..., formatTranscript = false)` returns raw
  chunk text. `VoiceTranscriptJoiner` removes matching overlap words in order;
  `VoiceTranscriptFormatter` then applies casing once to the final phrase.
  **No partial text is committed.** Finish delivers one candidate into the
  original editor only after existing editor-generation, selection, password,
  incognito and detached-recovery guards pass. If the user cancels, switches
  fields or a call takes microphone ownership, no stale text is inserted.
- BR Brain reports in-memory counts of decoded chunks, queued chunks, last
  end-to-end chunk latency and phone-mic route, alongside native success,
  fallback and noise-effect state. Latency includes queue wait and decode,
  so it can honestly show if a 2-minute session outruns the device. No raw
  audio, transcript or third-party app identity is logged or uploaded.

## Why not dual-microphone fusion or a different denoiser now?

Android's audio policy may silently route or silence simultaneous input
streams; concurrent different-device capture depends on OEM audio HAL
support. Bluetooth HFP/LE does not necessarily expose independent earbuds
mic capsules. Without two **verified non-silent, time-synchronized** streams,
trying to combine the phone and earbuds would create distorted phase/echo
artifacts and could hurt recognition. The Android documentation:
https://source.android.com/docs/core/audio/concurrent and
https://developer.android.com/media/platform/sharing-audio-input .

The existing 206-KB GTCRN is already positively reported by the owner. Poise
benchmarks its native 16-kHz GTCRN at ~0.12 RTF (realme C67); its DeepFilterNet3
uses 48-kHz and ~3MB weights and ~0.25 RTF on that *different* phone. This is
not evidence of better North Indian English ASR WER on an Oppo Reno10 5G.
Do not silently replace a working default without controlled same-audio WER
tests. Native PCM remains 16kHz, which is the exact model input and avoids
extra resampler stages. References: https://github.com/chabandou/poise-android
and https://github.com/Xiaobin-Rong/gtcrn .

## Verification and acceptance

- Local static contracts `tools/test_offline_voice_contract.py` and
  `tools/test_bundled_voice_contract.py` remain mandatory, plus new
  `PrivateVoiceChunkerTest`: 125 seconds of deterministic input, short-speech
  case, exact reconstruction after discarding only deliberate overlaps,
  immediate cancellation, bounded 6s PCM windows.
- Exact-SHA GitHub debug APK must compile and run entire Android unit suite,
  package verifier must confirm pinned GTCRN bytes and native `.so`, no runtime
  Internet permission. Original-signed Obtainium must independently succeed
  and publish an APK for the **same SHA**. Record run IDs and final tag here.
- Owner acceptance on Oppo: start blue mic with and without Nirvana Ion,
  ensure BR Brain says phone mic preferred/confirmed; check noisier Indian
  English speech in real household/traffic conditions, 5s/30s/120s utterances,
  word-boundary duplicates, native success/fallback and processing queue;
  during calls recording must remain blocked. Compare same-take A/B in Voice
  Lab. The 1–2 second finalization target is a **goal**, not an unsupported
  performance promise; measure actual end-to-end chunk latencies and backlog.

## Future agent requirements

Preserve original package/certificate and main-only fast-forward update.
After every material change, keep this ledger, AGENTS.md, PROJECT_CONTEXT.md,
docs/ENGINEERING_HANDOFF.md, docs/PROJECT_CONVERSATION.md and the post-signed
release-notes delta accurate. Never claim multiple live physical microphones,
speech improvement on untested hardware, non-blocking behavior or published
APK without actual evidence from the corresponding gate.
