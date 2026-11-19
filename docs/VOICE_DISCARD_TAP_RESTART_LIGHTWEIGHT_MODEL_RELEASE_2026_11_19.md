# brrBoard: interruptible discard + lightweight signed updates

## Owner instruction (2026-11-19)

The owner observed that the previous mic-glow, cancel-morph and Emoji-band
changes were in a local commit and never reached GitHub/Obtainium. Publish the
full set to **Batmancpu/brrrBoard `main`** and let the existing original-signed
debug/release gates build a normal in-place update. Additionally, tapping the
mic while the 520 ms white-bin discard morph is playing must interrupt that
visual **immediately** and start listening in the same gesture. The next APK
must NOT contain another copy of the English Parakeet model: the owner says
the verified model is already installed locally. Retain a deliberate manually
triggered, model-inclusive **Obtainium Release** workflow option for recovery.

## Exact source implementation

- `BoardFooterView` arms a discard restart only on ACTION_DOWN while the morph
  view is visible. It immediately cancels the animator, restores the idle
  microphone and keeps the existing 48dp touch target. On a valid in-bounds
  ACTION_UP it calls the original mic click action to start a new offline
  session, **without the 300 ms idle double-tap delay**. ACTION_CANCEL,
  pointer-down and dragged-away releases do not start recording. Direct
  accessibility clicks interrupt the animation and route through the same
  offline-only toggle. Reset disarms in-progress gestures.
- Event-only red/turquoise discarding, 520ms bin/lid/swallow/morph and the
  mic-shaped cached underlight are unchanged. No more cancellation events
  are inferred from other lifecycle transitions.
- `VoiceModelInstaller.isInstalled()` first checks the private, hash-verified
  copy in `noBackupFilesDir`. If present it returns READY before trying the
  optional APK model assets. Thus an **in-place, same-package, same-signer**
  update retains the installed model. Uninstalling the app, clearing its data,
  or never having completed the previous extraction can make the lightweight
  update report "Offline model missing". The user can then explicitly dispatch
  a model-inclusive release; the application never downloads weights itself.
- `.github/workflows/obtainium-release.yml` builds a small model-free APK on
  every ordinary `main` push. A manual `workflow_dispatch` exposes a
  `bundle_voice_model` BOOLEAN input, default false. Only true fetches pinned
  weights and checks all four SHA-256s in the final original-signed APK.
- `.github/workflows/brrrboard-build-debug.yml` never bundles weights.
  `tools/verify-voice-model-packaging.sh` rejects accidental model files in
  normal debug/signed APKs and delegates to the exact-lock verification when
  explicitly requested. The sherpa-onnx arm64 native runtime remains: it is
  code needed to use the already installed model, **not model weights**.
- The model-inclusive manual release is a new version (`github.run_number`) of
  the same package and original certificate, so Obtainium still sees it; do
  NOT run the large bootstrap unless the owner actually needs recovery.

## Required verification

1. Python contract/packaging tests, Bash syntax, XML/YAML source parse, staged
   whitespace checks; local Android compile is unavailable without SDK.
2. Before publishing, read-only verify remote `main` is the expected ancestor;
   do not force push or create an extra branch. If remote advanced, stop and
   reconcile without overwriting other commits.
3. Push one fast-forward source commit to `main` only with explicit owner
   authorization (given above); check exact-SHA debug and original-signed
   Obtainium CI. A CI failure is NOT an installable, accepted update.
4. Device: while the red mic is recording, tap stop; during turquoise decoding
   tap cancel; while trash morph is dropping tap again and ensure listening
   begins immediately, with NO stale recognition result inserted. Test touch
   abort, TalkBack, keyboard hide, and a subsequent successful transcript.
5. Inspect the signed APK model asset paths and signer; install over the
   existing package, check Settings shows voice model READY after startup,
   mic works offline. Check Emoji Browse top gap on Oppo Reno10 5G.

## Scope separation

No new unified learning engine, app-context tracking, speech hotwords, swipe
model learning, Emoji typing suggestion ranking, Liquid Glass or new Android
permissions in this batch. That is the owner's NEXT discussion after the
visual changes are shipped and tested.
