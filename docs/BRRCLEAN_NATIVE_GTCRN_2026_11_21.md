# brrClean native voice denoising — 21 November 2026

## Owner instruction and scope

The owner rejected the old sherpa/ONNX GTCRN voice filter: its selector did not
reliably work, and Nirvana Ion earbuds captures were silent. They authorized
rebuilding the optional voice denoiser from zero and implementing the result.
Keep the current offline Parakeet 110M ASR and learned typing intelligence.
Never run denoising while a user types or while a telephone/video call is active.

## Exact implementation

- Three explicit choices in BR Brain → Voice & Noise Reduction: **OFF** (raw
  microphone), **Android** (optional capture-time `NoiseSuppressor`), and
  **brrClean** (native C++ GTCRN post-capture). Android remains the safe default;
  legacy Android-suppression OFF preference maps to OFF. Saving Android also
  resets the legacy boolean that previously prevented a mode selection from
  taking effect.
- brrClean's inference, STFT and weights are ported from `chabandou/poise-android`
  at **`2014cd0d06b2effa24b1ab1c95013f785f38dc80`**, MIT license bundled
  inside every APK. `gtcrn.cpp`, `gtcrn.h`, `nn.h`, `stft.cpp`, `stft.h` are
  vendored in `app/src/main/jni/brrclean/`; our `brrclean_jni.cpp` converts
  16 kHz mono PCM to native 256-sample GTCRN frames, performs 512-point causal
  STFT and iSTFT with one-hop alignment correction, and returns the same length.
  Source of weights: `https://raw.githubusercontent.com/chabandou/poise-android/2014cd0d06b2effa24b1ab1c95013f785f38dc80/app/src/main/assets/gtcrn.weights`.
  Exactly **211,220 bytes**, SHA-256
  **`4db48ff819e5ac18f5ccad1dfdce0968dde1a209c0cbbf9f24ba9dc4d9f48ded`**.
  `tools/fetch-board-brrclean.sh` downloads **during CI build only**, validates
  hash/size, and packs into `assets/voice/brrclean_gtcrn.weights`; no model
  download runs in the installed app. `tools/verify-brrclean-packaging.sh`
  checks every debug/release APK for exact weights, license and `libbrrclean.so`.
- `BrrCleanVoiceService` is non-exported in process `:brr_clean_denoise`.
  `BrrCleanNative` loads `libbrrclean` there and nowhere else. Messenger request
  carries at most 384,000 PCM bytes (12 seconds); the child has an 8s kill
  watchdog and parent 9s fallback. No audio file or transcript is stored. JNI
  failure/process death/timeout uses the untouched captured PCM for existing
  offline Parakeet. Memory buffers are wiped after use. Entire old active
  `GtcrnVoiceService`/`GtcrnVoiceBridge` path was removed, not layered upon.
  Historical opt-in synthetic GTCRN probe remains for old diagnostics only.
- Lab records **one** physical take. Original and brrClean outputs come from
  that identical PCM; they are replayable/scorable in memory. Android's
  platform effect is capture-time and cannot honestly be replayed with the
  same untreated input after the fact. A/B scores only mean something when
  the owner actually speaks the chosen phrase.
- For Bluetooth on Android 12+, the lab requests a **Bluetooth communication
  device** via `AudioManager.setCommunicationDevice`, establishes communication
  mode, records `VOICE_COMMUNICATION`, checks active input route/silencing and
  discards silence/mismatches/unverified earbuds routes. It restores both the
  prior communication mode and device on exit. `MODIFY_AUDIO_SETTINGS` (normal
  Android permission) is needed for this test. It is not a guarantee that the
  ColorOS/Nirvana Ion firmware exposes its microphone; that requires device QA.
  Standard Android generally cannot synchronously record multiple independent
  physical phone+earbud microphones for one fair combined input.
- BR Brain now reports selected OFF/Android/brrClean, the Android effect status,
  successful native jobs, raw fallbacks and latest native latency, in memory
  only. Neither these counters nor Android effect `ACTIVE` proves measurable
  SNR or word-error-rate improvement.

## Verified here and gates still required

- `tools/test_voice_noise_lab_contract.py` and
  `tools/test_bundled_voice_contract.py` enforce isolation, routing, privacy,
  provenance, APK scripts and raw fallback.
- `tools/test_brrclean_native.cpp` performs an actual native GTCRN/STFT pass
  with the pinned weights and checks finite output and no silent failure. It
  does **not** establish North Indian English WER improvement. It runs in
  exact-SHA debug CI before the Android build.
- Complete Gradle/Robolectric Android tests, both GitHub exact-SHA debug and
  original-signer Obtainium, APK hash/permissions, and physical Oppo/Nirvana
  Ion audio comparison must pass before calling this a device-confirmed fix.
  Do not fake microphone route success, claim multi-mic mixing, or replace
  Parakeet with a larger model as part of this patch.

## Owner hardware acceptance steps

1. Install the signed APK after both workflows are green. Open BR Brain →
   Voice & Noise Reduction, select OFF and record a **phone mic** take; verify
   raw sound. Select brrClean, record one phone-mic take, listen to Original
   and brrClean **from that same recording**, then score with Parakeet if desired.
2. Connect Nirvana Ion. Select Earbuds mic and record one take; verify actual
   route says Bluetooth headset mic **confirmed**, not requested/unverified.
   If Android rejects/silences it, report the exact onscreen status.
3. In ordinary keyboard blue mic, try noisy North Indian English in Android
   then brrClean, count errors and compare **similar but not identical** takes
   fairly. Observe BR Brain success/fallback/latency; try cancellation,
   long dictation beyond 12s, video call, WhatsApp, and Instagram keyboard.
   Accept only if crash-free and no typing lag. No offline app can promise
   better accuracy on every microphone/noise pattern.

## Permanent instructions for subsequent AI agents

Maintain `AGENTS.md`, `PROJECT_CONTEXT.md`, `docs/ENGINEERING_HANDOFF.md`,
`docs/PROJECT_CONVERSATION.md`, this topic ledger, and post-signed release
notes on each substantive continuation. Do not reintroduce UI-process GTCRN,
fake Android effect reprocessing, network model download, raw recording logs,
or an unconfirmed Bluetooth mic route. Never bypass either CI/signer gate or
claim installed hardware behavior without owner testing.
