# BR Brain: private voice and noise reduction comparison lab

Status: **owner GO, implementation source candidate; Android CI / original-signed APK / Oppo and Nirvana Ion acceptance pending.** This ledger describes source, not a released feature until those gates pass.

## Evidence and decisions

- The owner confirmed one real successful blue-mic transcription on their Oppo Reno10 5G. The supplied photo showed `NOISE_ACTIVE +224ms`, `ASR_READY +3395ms`, `MIC_CAPTURE_STOPPED +9606ms`, `ASR_DECODING +9611ms`, `ASR_DECODED +10258ms`, `COMPLETE +10271ms`. Audio, recognized text, image, and surrounding personal information are not stored in this public repository.
- The existing `brr_voice_platform_noise_suppression` preference already defaults **true**, with OFF available. Preserve prior user choices.
- Owner approved a separate BR Brain screen with truthful live suppression/capture indicators, no-earbud vs earbud microphone selection/comparison, listening before/after Android processing and further offline English/Hinglish optimization while preserving typing/call smoothness.
- `NOISE_ACTIVE` proves OS effect acceptance, **not** measured noise attenuation or WER. One successful session does not establish robust absence of crashes. The original boAt Nirvana Ion has two microphones per earbud/four total with earbud-side ENx; Android normally receives a **processed logical Bluetooth input**, not the four raw channels. Precise microphone hole placement is unverified and not claimed; Ion ANC is a separate variant.

## Code map

| Source | Change |
| --- | --- |
| `settings/screens/BrainDashboardScreen.kt` and `settings/SettingsNavHost.kt` | BR Brain subpage entry and nested navigation; RAM-only live blue-capture state. |
| `settings/screens/VoiceNoiseLabScreen.kt` | Voice lab UI: device AUTO/PHONE/EARBUDS; effect OFF/ON; explicit Record, Stop, Play, Score and Erase; one English and one Hinglish reference sentence. |
| `latin/voice/VoiceNoiseLab.kt` | 16 kHz mono 16-bit AudioRecord, twelve-second bounded takes, one active recorder, PCM held/wiped in RAM, private AudioTrack playback, optional existing Parakeet recognition on a wiped copy; no background mic. |
| `latin/voice/VoiceLabScore.kt` and `app/src/test/java/helium314/keyboard/latin/voice/VoiceLabScoreTest.kt` | Pure word-error rate, signal RMS and clipping tests. |
| `latin/voice/VoiceCaptureNoiseReduction.kt` | Explicit per-lab effect attachment without mutating keyboard default or normal mic metrics. |
| `latin/voice/VoiceDenoiseMetrics.kt`, `latin/KeyboardPcmAudioSource.kt` | True live blue-capture active/completed flags; old production microphone route unchanged. |

## Strict semantics and constraints

1. OFF means brrBoard **does not add** Android NoiseSuppressor; firmware or source preprocessing may still occur. ON only reports active when the OS-created effect was enabled. These are **different recordings**, never the same captured raw audio with a filter switch applied afterward.
2. `AudioRecord.setPreferredDevice` requests an available input. Android may decline/ignore it; `AudioManager.getActiveRecordingConfigurations()` is queried only for the lab's own audio session, and only Android-confirmed route types are called *confirmed*. No `AudioRecord.routedDevice` probing in the live keyboard, no simultaneous phone + Bluetooth mic, no global Bluetooth forced disconnect or SCO mode.
3. Only an explicit tap starts a 12s-cap recorder. Playback occurs after capture, never during. Clips are RAM-only, replaced/wiped on retake, erased on command or screen exit; no app-private audio files, persistent transcripts, log contents, clipboard, backups, network, app/field identifiers, or microphone background service. Existing offline `VoiceDecodeRequest` takes/wipes its separate ASR input copy; exiting cancels it.
4. Refuse new recordings if Android reports a call; OS AudioRecord runs on a background thread, with stop also off the UI thread. GTCRN native remains PAUSED, and Parakeet TDT 110M plus encrypted dictionary and Emoji learners are unchanged. No ~600MB model, network permission, new routing permission or neural ContextLM.

## Release and physical verification

- Local `./gradlew :app:compileDebugKotlin --offline --no-daemon` did not start: the Gradle 8.14.4 distribution is missing and network download failed. Do not call local Android tests passed. Require exact SHA debug/full unit and original-signed Obtainium success before APK is declared installable.
- Confirm `git diff --check`; JSON notes valid; static privacy/packaging tests; original package, permissions, model and signing identity unchanged. Preserve concurrent QuickType glyph-motion and README work when integrating main.
- Simple Reno10 checklist: visit BR Brain → Voice & Noise Reduction, check IDLE without recording; read exact displayed sentence into phone OFF, phone ON, tap Stop/Play/Score for both; connect Nirvana Ion and repeat earbuds OFF/ON; with headset still paired request Phone (route is labeled confirmed only if Android reports it); compare in quiet and fan/noisy room. Leave screen while recording to confirm it stops, try a call (test must refuse), type normally to check jank. No user-generated benchmark spreadsheet, adb, root or USB debugging required.

## Agent continuation

Update `AGENTS.md`, `PROJECT_CONTEXT.md`, `docs/PROJECT_CONVERSATION.md`, `docs/POST_V1_0_19_RENO10_PLAN.md`, this ledger and `docs/ENGINEERING_HANDOFF.md` at each confirmed source/CI/device checkpoint. Never invent WER gain without matched ground truth. Preserve original signer, package and zero-Internet manifest.
