# brrBoard — privacy-safe one-shot GTCRN native crash diagnosis (20 Nov 2026)

## Owner request and scope

Owner reported keyboard process crash 1–2 seconds after voice-button use in GTCRN v1.0.195, then requested a **one-shot local crash logger that reveals the reason without invading privacy**, followed by a properly tested fix and a considered **600–700 MB Parakeet Ultra 0.6B** upgrade. The voice live path was restored in v1.0.196; this diagnostic update does **not** re-enable native GTCRN on microphone recordings, does not change the existing Parakeet 110M, and does not activate Ultra. Original signing/package/data and no-network Android permissions are binding.

## Why a separate process

An Android `Thread.UncaughtExceptionHandler` handles Java/Kotlin exceptions, **not a native SIGSEGV/SIGABRT that terminates its process**. Native code in the same process as the keyboard could close the IME. Android 11+ `ActivityManager.getHistoricalProcessExitReasons()` provides a sanitized reason/status for own process deaths. Android 12+ also has native tombstone protobuf streams, but this design deliberately **never opens `getTraceInputStream()`**, because tombstones could contain private memory.

`GtcrnProbeService` is declared `android:exported="false"`, `android:process=":brr_voice_probe"`, and `START_NOT_STICKY`. Nothing starts it automatically. Only the explicit BR Brain button starts one short-lived process; it uses the already-bundled 535,190-byte GTCRN model and one second of **deterministic generated synthetic PCM**, never `AudioRecord`, native ASR or the user microphone. Run the CPU at background thread priority with one inference thread, validate finite output length/rate, then release native model and explicitly kill the **child process only**. If JNI aborts, only that child process dies; the IME stays in its previous crash-safe mode.

`VoiceNativeCrashProbe` stores a **single fixed-code state record** in `noBackupFilesDir/voice-gtcrn-one-shot-v1.state` using `AtomicFile`. It contains only one stage code, timestamp, and detail code. State writes are limited to one requested probe and its phase changes; there is no continuous monitoring. After `PASSED/FAILED`, Android's own process exit category and numeric status can supplement the report for child crashes. A recent *existing* keyboard process crash category (when available in Android system history) is read for the first BR Brain page render; this may show v1.0.195's failure even without reproducing it.

## Privacy contract

- Never request extra permissions, network, upload, microphone PCM, other-app package names or user input.
- Never read Android raw tombstones, `ApplicationExitInfo.description`, stack traces, exception messages, logcat, clipboard history, contacts or typed text.
- Report includes only `PASS` or bounded `FAILED`/native/Java/memory/ANR/exit category, numeric signal/status and `REQUESTED/LOADING_MODEL/RUNNING_SYNTHETIC` phase. Numeric status is not a stack trace.
- Report is only copied to system clipboard if owner **explicitly** taps `Copy safe result`. Note that Android clipboard content may be exposed to user-installed software; the content contains no speech or field data.
- Erase button deletes the private one-shot status and permits explicit manual retrial; never auto-run a second test or run a persistent background service. No run occurs during ordinary typing or video calls unless owner explicitly initiates from BR Brain.
- Android may have no historical process exit info, or a native failure may not reproduce with synthetic audio; accurately say `unavailable` / `no result` without inventing cause.

## Integration and acceptance

Files:
- `app/src/main/java/helium314/keyboard/latin/voice/VoiceNativeCrashProbe.kt`: explicit one-shot storage, fixed codes, safe OS exit categories and copy/erase.
- `app/src/main/java/helium314/keyboard/latin/voice/GtcrnProbeService.kt`: isolated-process synthetic test, one native thread, no mic.
- `app/src/main/AndroidManifest.xml`: non-exported child process service.
- `app/src/main/java/helium314/keyboard/settings/screens/BrainDashboardScreen.kt`: one-shot card and historical category.
- `tools/test_bundled_voice_contract.py`: static constraints for privacy and isolation.
- `docs/releases/notes.json` and all agent/handoff/Ultra contracts updated.

CI gates: exact SHA debug and full JUnit/unit tests; APK packaging hash for GTCRN; no `INTERNET` permission; original-signed Obtainium certificate, package and monotonic version. Real Oppo must confirm (1) normal typing and calls unchanged, (2) historical exit category visible when available, (3) one tap either says synthetic PASS or safe child crash reason, (4) keyboard does not disappear if child crashes, (5) owner can copy/erase safe result. **A green APK build cannot prove any native child-process crash behavior.**

## 600–700 MB Ultra migration remains conditional

Owner's new request calls for a proper model update **after** the crash reason is understood. The exact planned candidate is Moondream Parakeet Ultra 0.6B INT8 through sherpa-onnx official export, see [Ultra migration plan](ULTRA_OFFLINE_VOICE_INTEGRATION_AND_MIGRATION_PLAN_2026_11_20.md). It is not directly comparable to current 110M by marketing claims; official sherpa export's FLEURS-English WER was Ultra 8.51% versus Parakeet v2 6.48% (different candidates, not a 110M comparison). Confirm paired North Indian English/Hinglish WER, Stop-to-text P50/P95, RSS/thermal and WhatsApp/Instagram/video-call smoothness before replacing 110M. If accepted, first same-signer APK bundles all verified new assets, extracts transactionally, deletes old model only after new ready, and future thin updates omit large weights. Do **not** put uncertain 0.6B native code or 600 MB assets into this one-shot diagnostic APK.

## All future agents

Read AGENTS/context/handoff plus this plan before changing audio. Never activate GTCRN native in LatinIME; never run the diagnostic automatically. No fabricated exact native stack, app installation or WER. Keep cumulative release notes as only the next delta; main-only expected SHA, no force. Record exact CI run IDs and human Oppo results as tested/pending.
