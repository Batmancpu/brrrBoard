# 2026-11-19 — Owner decision: self-contained offline voice on every mic tap

## Superseding instruction

The owner explicitly replaced the prior hybrid/red-server mic behavior:
**one microphone tap must only record for brrBoard's own offline English model**,
and the model must be **bundled inside the next normal brrBoard update** so the
phone never has to obtain a separate model APK, open another dictation app, or
download weights independently.

This supersedes all earlier documentation that described a red/brrSpeak tap,
blue/offline hold-only mode, or manual one-time model-bootstrap release. The
existing brrSpeak code is retained only as dormant compatibility plumbing;
neither normal footer mic taps nor hardware VOICE_INPUT keys may call it.

## Input UX and privacy contract

1. Idle **single tap** (after the existing idle double-tap recognition window)
   starts `OfflineBoardDictation` in brrBoard's own IME process. Only its local
   `AudioRecord` source is used, no IPC/service/intent to brrSpeak. Pulsing
   mic glyph and steady surrounding disk are **blue**. Existing haptic applies.
2. **Second tap** while recording stops capture; private worker transcribes.
   Tap during decoding cancels the result; the visible Cancel label and
   accessibility Cancel action discard current audio and text. No stale editor
   insertion after switching fields.
3. **Press-and-hold** continues to start the same local-only recording and
   finishes on release (cancel on pointer interruption); no secondary pipeline.
4. **Idle double tap** remains the keyboard switch action. It never opens a
   microphone. Existing Emoji, swipe/predictions, Apple-motion presets,
   keyboard focus/height geometry and panel animations are untouched.
5. PCM16 stays in bounded RAM for local transcription and is wiped after use.
   No local PCM disk files, clipboard bridge, history, model retraining or
   network request; keyboard manifest still has **no INTERNET permission**.
   The old server fallback and offline Paste chip are not entered from the mic.

## Model distribution and install behavior

- NVIDIA Parakeet TDT 110M English INT8 model: exactly four pinned files,
  **136,490,421 bytes (about 130 MiB)** total; verified against public GitHub
  asset SHA-256 metadata and locked in `tools/offline-voice-model.lock`.
- Both regular original-signed Obtainium and debug CI fetch these assets
  **on the GitHub build runner**, hash verify them, bundle them into the app,
  and then check each embedded APK asset hash after packaging/signing.
- The user's phone downloads **only the updated APK**. First app startup
  after update verifies/extracts the bundled assets off the main/UI thread
  into private `noBackupFilesDir`; Settings displays progress/retry.
  It never requests the assets from GitHub on the phone.
- The standard Obtainium release stays latest, on `main`, same
  `com.mangoloads.board` package and original pinned signing certificate
  with monotonic `versionCode`. No special prerelease/manual dispatch.
- **Tradeoff:** Unless distribution is changed again, **each** future full
  APK update includes the 130 MiB of weights. The private extracted model
  persists across same-signer updates, so repeated extraction is not needed.
  A future deliberately model-free update would reduce downloads but break
  self-contained fresh installs; do not silently change this user decision.

## Changed seams

- `BoardFooterView.kt`: footer mic click explicitly toggles local-only voice;
  blue hold and haptics unchanged.
- `LatinIME.java`: both compatibility `toggleBoardDictation` and hardware
  voice key forward to the private local-only controller. No brrSpeak toggle.
- `OfflineBoardDictation.kt`: tap-to-start, tap-to-stop, tap-to-cancel
  processing, same-editor and cancellation guards.
- `App.kt`, `VoiceModelInstaller.kt`, Main Settings and strings: describe
  normal APK bundled model and first-launch verification, not a manual APK.
- `tools/prepare-offline-voice-pack.sh`: pinned CI-only downloads.
- `tools/verify-bundled-voice-model.sh`: checks four exact embedded hashes.
- Both `.github/workflows/*` builds: model bundling and hash gates, debug
  tests and original-signer Obtainium release.
- README and both privacy policy copies revised. Old documentation retained
  as historical decisions; this document is authoritative for the new mic UX.

## Gates / unresolved physical tests

Before calling the release installable, require both same-commit debug
Android test-suite and original-signed Obtainium workflows **green**, verify
package/version/signing/API permissions and all four model hashes within
the actual APK, and publish a direct signed release link.

On the user's Oppo Reno10 5G: update *over* existing app (no uninstall),
open brrBoard to finish offline model setup, airplane mode tap→speak→tap,
hold→speak→release, double-tap keyboard switch, mid-decode cancellation,
editor switch, 3/10/30-second Indian-English phrases, punctuation and
technical words, private keyboard text/suggestions, accessibility/haptics,
quiet/noisy input, 16-kHz mic startup, cold/warm latency, thermal usage,
memory, and persistence after subsequent same-signer update. **Do not claim
physical correctness, highest Indian-English WER, or speed until measured.**
