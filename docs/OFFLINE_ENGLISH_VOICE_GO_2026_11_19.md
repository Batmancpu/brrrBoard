# brrBoard + brrSpeak: owner-authorized offline English voice implementation

Date: 2026-11-19. Initial source baseline brrBoard `845f95f16f0f60dbaae08f957129148e75aecdf4`;
brrSpeak `49cb5d7bd70da5e9facf5cf41a802149c718a951`. After the owner-required
25-minute workflow watch, integrate on brrBoard `746a7e18` (record-first
Binder transport + CI compile repair), with brrSpeak `a78cb9c0` verified CI
SUCCESS. This is a live work handoff, **not** a claim of Android device acceptance.

## Current owner decision and exact constraints

The owner first asked for a complete code-file plan without implementation,
then explicitly instructed: **“Is everything done? If anything is left, please
finish that reasoning and start the implementation right now.”** This is the
GO for source implementation. Owner wants English-only, fast short private
voice transcription, the most accurate small offline recognizer we can
validate on an Oppo Reno10 5G, red brrSpeak/provider mode for longer speech,
blue long-press offline in the keyboard, and a manual offline Paste fallback
for slow/failed server dictation. No automatic retraining, no online model
download on phone, no extra text LLM, no new keyboard internet permission, no
audio/transcript history, no clipboard export, no duplicate inserts, and no
changes to user-approved Emoji, suggestions, glass, keyboard switch, numpad,
signing, app IDs or Obtainium update behavior.

## Chosen model and distribution

- NVIDIA Parakeet TDT 110M English INT8, four pinned ONNX/token assets,
  total 136,490,421 bytes (about 130 MiB) plus the native runtime. This is
  a **candidate**, not independently measured to have the best Indian-English
  WER on this phone. Attribution: NVIDIA CC-BY-4.0 weights, sherpa-onnx
  Apache-2.0 export. Source names, exact byte sizes and SHA-256s are recorded
  in `tools/offline-voice-model.lock`, mirrored in `VoiceModelManifest.kt`.
- Runtime: pinned sherpa-onnx v1.13.3 AAR SHA-256
  `243ad797a3b6e75ebbeaf7a2ab4aec0777e7d71b730685abb762a120940b07b6`.
  `tools/fetch-board-sherpa-onnx.sh` places the jar plus arm64 native libs.
  Other Android ABIs still run the keyboard, but offline voice needs an
  appropriate native library package before being offered there.
- The ordinary signed Obtainium APK carries runtime code but **not** the
  136 MB model. `workflow_dispatch` of the existing original-signed
  `obtainium-release.yml` with `offline_model_bootstrap=true` creates one
  separate prerelease asset, `brrBoard-with-offline-model.apk`, from the same
  package, certificate and per-workflow monotonic versionCode. It is **not**
  marked as the normal latest Obtainium update.
- First launch of the special APK copies/hash-verifies model files off the UI
  thread into `context.noBackupFilesDir/voice-models/...`, then atomically
  activates them. Future normal original-signed APKs keep this directory.
  Uninstall/delete-app-data removes it, so recovery requires a new one-time
  special install. Do not install the next ordinary APK before model extraction
  has finished. No runtime internet fetches.

## Implemented source seams in this working batch

| File | Purpose |
| --- | --- |
| `app/.../latin/voice/PrivatePcmBuffer.kt` | Bounded, wiping, RAM-only PCM16 buffer; no WAV/cache/clipboard |
| `app/.../latin/voice/VoiceSegmenter.kt` | 11-second target / 15-second maximum windows, low-energy boundaries and 250-ms overlap |
| `app/.../latin/voice/VoiceTranscriptJoiner.kt` | Word-overlap deduplication for segmented utterances |
| `app/.../latin/voice/VoiceResultArbiter.kt` | Exactly-one successful server or explicit offline insertion |
| `app/.../latin/voice/VoiceModelManifest.kt` | English-only model identity, expected sizes and hashes |
| `app/.../latin/voice/VoiceModelInstaller.kt` | Private atomic one-time verified APK asset extraction |
| `app/.../latin/voice/OfflineVoiceEngine.kt` | Single cached native model/worker; never decode on UI thread; idle unload |
| `app/.../latin/OfflineBoardDictation.kt` | Blue held mic → stop → local English text → same-editor guarded insert |
| `app/.../latin/HybridVoiceBackup.kt` | Same red-mode PCM also decoded locally; show Paste offline chip on slow/error |
| `app/.../latin/KeyboardPcmAudioSource.kt` | Blue mode only: one AudioRecord, no IPC/network/disk; old pipe utility remains for regression tests |
| `app/.../latin/KeyboardRecordedAudioSource.kt` | Red mode: same 16-kHz PCM frames to the existing private recording and optional in-RAM offline backup |
| `app/.../latin/BrrSpeakRecordedAudioClient.kt` | Red mode: authenticated Binder handoff of finalized PCM (not a SpeechRecognizer pipe) |
| `app/.../latin/InlineDictation.kt` | Record-first brrSpeak Binder client plus local backup lifecycle / late-result guard |
| `app/.../latin/BoardFooterView.kt`, `FooterStatusRingDrawable.kt` | Hold mic blue + haptic, red tap, idle double-tap keyboard switch, status and accessible action |
| `app/.../latin/LatinIME.java`, `inputlogic/InputLogic.java` | Cancel across editor/IME lifecycle; new private voice-origin insertion with suggestions refreshed and no typed/swipe history |
| `app/.../latin/App.kt`, `app/build.gradle.kts` | Background bootstrap, pinned native runtime linkage |
| `app/src/main/res/values/strings.xml`, `settings/screens/MainSettingsScreen.kt` | Status, model-missing and Paste offline labels; one-time model-ready indication |
| `.github/workflows/brrrboard-build-debug.yml` | Fetch pinned native speech runtime before CI compilation |
| `.github/workflows/obtainium-release.yml` | Same runtime, model-free normal APK gate, opt-in original-signed bootstrap |
| `tools/fetch-board-sherpa-onnx.sh`, `tools/prepare-offline-voice-pack.sh`, `tools/offline-voice-model.lock` | Pinned reproducible build inputs; source weights excluded from Git |
| `PRIVACY_POLICY.md`, `PrivacyPolicy.md` | Correct microphone/blue/red data-flow disclosure |

All new `voice` directory paths are under
`app/src/main/java/helium314/keyboard/latin/voice/`. Test files include
`app/src/test/java/helium314/keyboard/latin/voice/PrivateVoicePipelineTest.kt`
and the existing `FooterIndicatorPolicyTest.kt`.

## Interaction contract

1. **Idle single tap:** existing delayed tap confirmation (so double-tap
   switching still works), brrBoard records PCM in its private cache and
   sends a finished read-only descriptor to the explicit brrSpeak bound service.
   Mic glows **red** while recording. This avoids Android 15 cross-UID
   RecognitionService pipe failures and uses no keyboard INTERNET permission.
2. **Idle press-and-hold:** GestureDetector recognizes the hold; no brrSpeak
   activity/service is bound. After microphone startup, mic glows **blue**;
   release stops capture, blue processing status remains until the native
   worker returns one English transcript. No implicit server fallback if the
   model is missing or fails.
3. **Red mode with model installed:** the same PCM frames are copied into the
   private bounded in-memory buffer as they are written to the completed
   brrBoard cache file. Local decode starts on capture finish while brrSpeak sends
   to the selected provider. Server success commits normally; after 3 seconds
   of processing or an explicit server failure, a ready local transcript is
   offered as an ephemeral **Paste offline** footer chip. It is **never**
   automatically pasted over a waiting server response.
4. **Exactly-once editor insertion:** a source-selection arbiter and current
   InputConnection/generation guards prevent late server results, cancelled
   requests, editor switching and double tap from inserting old text.
5. **Privacy:** blue mode uses no network/API key or disk audio, and voice
   insert bypasses native typed/swipe history while refreshing ordinary
   suggestions/Emoji presentation. It rejects Devanagari output, but this is
   not an automatic English-vs-Romanized-Hindi language detector.

## Known limits and gates before declaring complete

1. **Real cross-UID Android 15 transport is not yet proven.** The brrSpeak
   authenticated Binder receiver and board sender both compiled/passed
   independently; the actual end-to-end foreground capture and service binding
   must be exercised on the user's ColorOS phone.
2. Red mode now records *before* the Binder handoff. A failed/missing brrSpeak
   service prevents red capture but can never trigger the blue mode to
   silently send audio externally. An offline backup exists only after red
   capture starts; the optional model must already be installed.
3. Red mode's finished PCM file is cache-private and deleted on Binder
   handoff/cancellation. The blue mode never creates a file. Test failures,
   force stop, an unavailable brrSpeak app, stale editor, and a stalled
   provider; do not promise absolute zero red-mode disk residency.
4. The local sherpa decoder depends on an arm64 JNI library and a pinned
   four-file model. Model loading time, thermal behavior, native memory, 16KB
   ELF compatibility and offline WER for spoken Indian English remain **real
   phone acceptance items**. The normal APK will grow by native runtime bytes
   even though the model is excluded.
5. Speech from one app may be temporarily cached in **brrSpeak** as WAV for
   network upload. Separate brrSpeak privacy commits `ae4d7ce` and `b09d6cd`
   force external PCM sensitive/history-off/no-failed-retry; brrSpeak workflow
   `44497381860` finished SUCCESS after the first task-name failure
   `44496227314`. This is separate from blue/100%-local audio handling.
6. The keyboard declares `android:defaultToDeviceProtectedStorage=true`;
   model weights are non-sensitive but an optional future approved-word vault
   must be credential-protected, encrypted and opt-in. **No automatic
   retraining or automatic retention is part of this batch.**
7. Native recognition and exact gesture UI have **not** been evaluated on
   the actual Oppo yet. CI compile success, once obtained, is not proof of
   microphone permission/AppOps, accent accuracy or no audio underruns.

## Verification before publishing

- Local `git diff --check`: pass at source checkpoint.
- Local Android XML/YAML parse and both shell scripts `bash -n`: pass.
- Local Android Gradle: unavailable (wrapper distribution and SDK not cached;
  outbound Gradle download blocked). GitHub Actions must run both debug
  `testRunTests` and original-signed workflow, including certificate pin,
  package ID, no-INTERNET and no-model-in-regular-APK assertions.
- Next: run exact-SHA CI; repair any compile/test/sign failure before calling
  the normal release usable. Then manually dispatch a one-time bootstrap
  APK only after the normal code passes and has a readable in-app model-ready
  indication. Do not silently switch existing Obtainium users to the 137MB
  asset.

## Owner's simple phone checklist (once a candidate APK is signed)

1. Update **brrSpeak** in place only if its signer matches your installed
   copy. Configure the desired **non-Dictate-Cloud** provider; never uninstall
   the old app just to force a new debug signature.
2. Install the separate one-time original-signed **brrBoard-with-offline-model**
   APK; open the keyboard once, wait for verified model-ready indication.
3. Airplane mode, hold mic, speak “Please schedule an appointment for tomorrow
   at five thirty,” release: blue halo, local English text, no external app.
4. Turn network back on, tap mic, speak a five-second Indian-English phrase,
   tap to stop: red halo, brrSpeak text once. Turn network off midway or delay
   the provider: local Paste offline chip, never double insertion.
5. Double-tap **idle** mic: keyboard switch only. Tap active red mic: stop.
   Hold and cancel on blue; switch editors mid-decode: no leaked insertion.
6. Try a 30-second explanation, a noisy room and words such as “UPSC,”
   “Karnataka,” and “Obtainium.” Record *only* whether text and response time
   felt acceptable, not private audio or scientific metrics.
7. After a later ordinary same-signer Obtainium update, blue offline must
   still work with no second model download.

Never describe this as device-tested before the user actually reports those
results. Keep the existing original signer and versionCode monotonicity.
