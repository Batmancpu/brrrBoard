# 2026-11-19 owner GO — private English voice correctness follow-up

## Authorization and baseline

The owner explicitly said **GO** after the separate no-change research period.
Work only on brrBoard's existing blue offline hold and red brrSpeak fallback;
preserve the Apple-motion work, GitHub main-only development, original pinned
production signer, Obtainium update identity, swipe dictionary, Emoji and
suggestion engine. Never add `INTERNET` to brrBoard, automatic voice retraining,
voice transcript history or a heavyweight LLM. The special model-inclusive APK
is a separate **manual one-time** artifact, never a normal Obtainium download.

Live main at the start of this batch was `a662e29143cde04ee117e72d014b1a45d81273dc`,
which includes cold-start brrSpeak service leasing and lazy offline-engine
initialization. The local scratch checkout was older, so source has to be
reconciled with current GitHub files and published only by an exact-head lease.

## Correctness and privacy changes

1. `InputLogic.onPrivateVoiceTextInput` clears both prior swipe correction
   states and never stores the dictated phrase in long-lived `mEnteredText`.
   Existing suggestions and Emoji refresh still execute *after* insertion;
   no voice word is sent to swipe or typing history.
2. `VoiceDecodeRequest` owns an ephemeral audio buffer. Cancellation *before*
   the worker starts immediately zeroes that PCM. Cancellation during native
   decoding only signals the next chunk boundary and waits until the reader
   stops before wiping; it never corrupts a buffer under JNI. The user never
   receives stale results, and both blue mode and red fallback dispose of the
   handle when cancelled or superseded.
3. The shared `OfflineVoiceEngine` schedules only one idle-release timer,
   checks cancellation between bounded transcription sections and wipes
   per-section FloatArrays and the original PCM even on errors.
4. `VoiceTranscriptFormatter` only fixes exact casing of reviewed technical
   terms, with URL/email and word-boundary guards. It never guesses a new word,
   uses swipe-gesture data, loads another model or learns from microphone text.
   Red brrSpeak results are not modified by the offline formatter.
5. Preserve the latest Apple-motion implementation's stable outer mic disk and
   glyph-only pulse/glow. The recording status is a visible tappable **Cancel** action,
   and the accessibility cancellation path uses the correct blue/red owner.
6. Model `NOTICE` now attributes the NVIDIA hybrid TDT-CTC 110M source model's
   transducer head (`nvidia/parakeet-tdt_ctc-110m`) instead of an unrelated
   0.11b model URL. Reconfirm redistribution and the exact pinned ONNX hashes
   before shipping the optional model bootstrap.

## Tests and gates

- Pure regression tests: queued cancellation promptly wipes audio, active
  cancellation defers wiping until decode ends, and casing pass preserves
  unrelated words, URLs and punctuation. Existing segment coverage, overlap,
  once-only commit, context action and UI motion tests must stay green.
- Debug workflow must compile Kotlin/Java and pass **all** tests, not just the
  new tests; the original signed Obtainium workflow must match the exact SHA,
  monotonically update versionCode and retain the original certificate.
- Physical Oppo Reno10 acceptance remains pending: offline 3/10/30s English,
  cold/warm latency, Indian-accent correctness, long-section joins, airplane
  mode, cancellation while switching editors, swipe→voice→suggestion, Emoji,
  red slow/failure manual paste, quiet audio, accessibility and blue/red visuals.
- The model-inclusive one-time artifact must not be advertised as tested until
  an actual matching signed APK is produced and installed with extraction
  completed. Ordinary signed APKs contain engine code but **not** weights.

## Deferred if benchmarking does not justify it

No auto-retraining, no shared swipe-vault voice substitutions, no semantic
mini-LLM, no experimental Parakeet beam/hotword decoding. A user-editable
local glossary may follow only with deterministic safety rules and a verified
Oppo latency/word-error-rate comparison; never promise an accuracy improvement
without on-device evidence. Silence/energy and overlap thresholds must also
be measured before imposing filters that could discard softly spoken words.

## 2026-11-19 — Voice formatter punctuation CI repair (fresh CI REQUIRED)

Initial voice follow-up `14a6d32` compiled successfully but debug run [44510814687](https://github.com/Batmancpu/brrrBoard/actions/runs/44510814687) executed 371 tests with one failure: `PrivateVoicePipelineTest.formattingOnlyChangesUnambiguousCasingWithoutRewritingSpeech`, first assertion at line 95. The URL/email guard excluded ALL trailing dots, accidentally preventing `android.` from becoming `Android.` at the end of an ordinary sentence. Narrow fix excludes only `.` followed by an alphanumeric domain component, and tests terminal punctuation plus `support@github.com`; neither core engine nor red brrSpeak integration changes. **Do not ship the first failed SHA; wait for BOTH exact new-SHA debug and original-signed release gates.**

