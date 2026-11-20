# brrBoard — privacy-safe one-shot GTCRN native crash diagnosis (20 Nov 2026)

> **Verified distribution:** Source `29d3903dd6d33c44b05394a10bc1333ad3dc26b9` passed exact-SHA debug `44788261280` and original-signed Obtainium `44788261329` workflows (both completed success), published v1.0.201 at `2026-11-20T18:06:13Z`, `https://github.com/Batmancpu/brrrBoard/releases/download/v1.0.201/brrBoard.apk`. Unit/privacy/build/signing **passed**, owner Oppo noise/mic/call/accuracy **pending physical test**. Existing Parakeet 110M retained; no Ultra bundle, no native GTCRN in live IME.


## Actual Oppo result; next diagnostic program (20 Nov 2026)

Owner installed v1.0.200 and photographed an isolated GTCRN child process exit: `last stage=LOADING_MODEL`, `reason=EXIT_SELF`, `status=255`. Historical *main IME* crash was `reason=NATIVE_CRASH`, `status=0`. Separate processes and events: GTCRN appears to exit abnormally before its model loader returns, but a second native SIGSEGV **is not proven** by `EXIT_SELF`; old native crash frame unknown. Never read a raw tombstone or re-enable JNI GTCRN in LatinIME until isolated Android evidence permits it.

The newest owner instruction ends with **keep current Parakeet 110M** in this release, add noise removal and diagnostics, and reserve the large Ultra 0.6B plan for a later separately validated decision. Current user-visible keyboard intelligence remains the existing native English/Hinglish/Emoji system; the separately proposed compact trained neural ContextLM **does not have licensed validated weights yet**.

### Real-session crash trail — opt-in, no privacy leak

`VoiceSessionCrashTrail` adds an independent **Arm next blue-mic test** UI in BR Brain. It does not record a microphone sample itself. When the user next starts the existing blue microphone, `OfflineBoardDictation`, `KeyboardPcmAudioSource` and `OfflineVoiceEngine` report at most 20 whitelisted processing codes: requested, mic initializing/ready/stopped, built-in noise effect active/unavailable/off, native Parakeet loading/ready, decoding/done, result check, complete/fail/cancel, with bounded elapsed milliseconds. One `AtomicFile` in `noBackupFilesDir` survives a main native crash without backup/export and is only read locally. On Android 11+, cached (10-second minimum between reads) own-process `ActivityManager.getHistoricalProcessExitReasons` can supply the sanitized type/status after an unexpected IME exit. No text, audio, transcripts, package/editor identities, Bluetooth hardware IDs, exception messages, stack frames, `ApplicationExitInfo.description`, `getTraceInputStream()` or network. User explicitly copies the safe stages to clipboard, or erases. No PCM-frame/key-event hot-path logging or recurring background diagnostics. This does not identify a native stack frame; user reproduction narrows the stage.

### Noise reduction without the broken GTCRN loader

`VoiceCaptureNoiseReduction` uses Android platform `android.media.audiofx.NoiseSuppressor` (official docs: https://developer.android.com/reference/android/media/audiofx/NoiseSuppressor ) attached to the *same* existing blue `AudioRecord.audioSessionId`. No second mic, native app-provided model, AI denoising worker, network call or runtime download. Release effect with AudioRecord in `finally`; keep original PCM if unavailable. BR Brain has an ON/OFF switch and reports whether Android actually enabled the effect. OEM capture might already perform noise reduction and additional suppression can damage consonants—**do not promise improved North Indian English WER without same-utterance A/B proof**. GTCRN weights stay packaged solely for isolated diagnosis; native denoising stays disabled in the keyboard. Android's microphone-sharing rules can vary (official: https://developer.android.com/media/platform/sharing-audio-input); blue voice checks `MODE_IN_CALL` and `MODE_IN_COMMUNICATION` before capture and ASR warm-up, rather than assuming OS always blocks it. Regular typing never starts voice/noise work.

**Pending hardware verification:** Oppo Reno10 5G effect support, repeat-voice crash trail, mic ON/OFF under quiet/fan/earbuds, keyboard smoothness during calls, Indian-accent WER and failure mode if OEM effect crashes. Local static tests passed, Gradle compilation blocked locally by distribution download and must be validated in signed CI. One self-contained APK remains normal size because no 0.6B model is bundled. The previous full Ultra migration plan below is historical/conditional and does not override the newest owner decision.


## Owner request and scope

Owner reported keyboard process crash 1–2 seconds after voice-button use in GTCRN v1.0.195, then requested a **one-shot local crash logger that reveals the reason without invading privacy**, followed by a properly tested fix and a considered **600–700 MB Parakeet Ultra 0.6B** upgrade. The voice live path was restored in v1.0.196; this diagnostic update does **not** re-enable native GTCRN on microphone recordings, does not change the existing Parakeet 110M, and does not activate Ultra. Original signing/package/data and no-network Android permissions are binding.

## Why a separate process

An Android `Thread.UncaughtExceptionHandler` handles Java/Kotlin exceptions, **not a native SIGSEGV/SIGABRT that terminates its process**. Native code in the same process as the keyboard could close the IME. Android 11+ `ActivityManager.getHistoricalProcessExitReasons()` provides a sanitized reason/status for own process deaths. Android 12+ also has native tombstone protobuf streams, but this design deliberately **never opens `getTraceInputStream()`**, because tombstones could contain private memory.

`GtcrnProbeService` is declared `android:exported="false"`, `android:process=":brr_voice_probe"`, and `START_NOT_STICKY`. Nothing starts it automatically. Only the explicit BR Brain button starts one short-lived process; it uses the already-bundled 535,190-byte GTCRN model and one second of **deterministic generated synthetic PCM**, never `AudioRecord`, native ASR or the user microphone. Do not start the test while Android reports MODE_IN_CALL or MODE_IN_COMMUNICATION. A 20-second main-looper watchdog kills the child even if native inference hangs. Run the CPU at background thread priority with one inference thread, validate finite output length/rate, then release native model and explicitly kill the **child process only**. If JNI aborts, only that child process dies; the IME stays in its previous crash-safe mode.

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
