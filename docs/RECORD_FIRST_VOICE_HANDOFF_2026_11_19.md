# brrBoard + brrSpeak record-first dictation checkpoint (2026-11-19)

## Scope
Owner explicitly authorized main-only implementation in both repositories. A 25-minute GitHub Actions hold was completed before edits. During that hold, unrelated brrBoard suggestion change `845f95f` passed debug/release, and brrSpeak privacy and CI changes `ae4d7ce` / `b09d6cd` passed its latest gate. Preserve them.

## Design (Wire v1)
- User taps brrBoard mic; `KeyboardRecordedAudioSource` uses foreground `AudioRecord` (VOICE_RECOGNITION) at 16,000 Hz mono PCM16, asynchronously writes to an IME cache-private file, and shows Recording.
- Second tap queues/executes stop. Capture worker finishes and validates 0.1–120 seconds of even-length PCM, nonzero samples and a 3,840,000-byte limit. No text, keyboard/editor state, clipboard, contacts, suggestions or history enter the recording or logs.
- `BrrSpeakRecordedAudioClient` binds **explicitly** to `com.mangoloads.speak/dev.patrickgold.florisboard.dictate.recognition.BoardRecordedAudioService` with versionCode >=142008. Android Binder transfers a `ParcelFileDescriptor.open(file, MODE_READ_ONLY)` via `Messenger`, plus positive session ID, exact format constants and byte count. File is deleted on handoff/cancellation.
- No FileProvider, file://, broad storage grants, network upload from brrBoard, RecognitionService cross-UID framework, switching keyboard, UI activity or clipboard intermediary.
- brrSpeak verifies the system-populated `Message.sendingUid` resolves to installed `com.mangoloads.board`, validates even bounded length and descriptor stat size, then calls **existing** `RecognitionSession`/`DictateController` on external PCM. Existing provider credentials, language, model, prompts, post-processing and privacy gate remain authoritative. It opens no microphone.
- Callback returns final text only or defined integer error to the original session. brrBoard guards generation, original input connection, original EditorInfo and visible IME; stale results are discarded. Session cancellation sends Binder cancellation and unbind; receiver handles unbind and caller process death.
- Timeouts: mic startup 8s; file finalization 20s; brrSpeak result 150s. No IME main-thread disk writes or heavy processing.

## Files
brrBoard:
- `app/src/main/java/helium314/keyboard/latin/InlineDictation.kt` — replace normal live SpeechRecognizer path with record-first binding.
- `app/src/main/java/helium314/keyboard/latin/KeyboardRecordedAudioSource.kt` — private PCM capture.
- `app/src/main/java/helium314/keyboard/latin/BrrSpeakRecordedAudioClient.kt` — explicit Binder/Messenger client and version contract.
- `app/src/main/AndroidManifest.xml` — brrSpeak package visibility; keep INTERNET removed.
- `app/src/main/res/values/strings.xml` — Recording label.
- `app/src/test/java/helium314/keyboard/latin/RecordedAudioHandoffContractTest.kt` — version/PCM/wire tests.
- Existing `KeyboardPcmAudioSource.kt`/audio intent utilities remain only for regression/legacy tests; not called in normal voice flow.

brrSpeak:
- `app/src/main/kotlin/dev/patrickgold/florisboard/dictate/recognition/BoardRecordedAudioService.kt` — receiver and protocol.
- Manifest, receiver tests, README, CI release asset identity check.
- `docs/BRRBOARD_RECORD_FIRST_HANDOFF_2026_11_19.md`.

## Required validation
- brrSpeak: `:app:assembleExperiment`, `:app:testDebugUnitTest`, APK manifest/package identity, release asset. Its current workflow uses **ephemeral debug signing** (CI logs confirm no persistent key); owner explicitly agreed to brrSpeak uninstall/reinstall if necessary. Never confuse it with original `net.devemperor.dictate`.
- brrBoard: debug compile/unit tests, then signed Obtainium release with same exact original pinned certificate, versionCode monotonicity, no INTERNET permission. No signature or package ID changes.
- Real-device success on Android 15/ColorOS is **unverified** until owner installs brrSpeak first, then brrBoard and tests mic tap, speak 5–10s, tap stop, one final insertion, repeated sessions, rapid/empty audio, revoked permission, offline provider, IME dismissal, editor switch and normal typing/emoji/swipe suggestions.

## Follow-up
- Fix CI compilation/tests if they fail; do not bypass gates.
- Report exact SHAs, run IDs, direct APK links only after publication, signing limitation and remaining phone tests.
- No side effects to existing appearance/motion work. Maintain this file, AGENTS.md and PROJECT_CONTEXT.md across future checkpoints.
