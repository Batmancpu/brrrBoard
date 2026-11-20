# brrBoard — GTCRN APK-integrated voice enhancement (20 Nov 2026)

> **20 Nov 2026 one-shot privacy update:** Owner approved an explicit BR Brain `Run one private check` in a dedicated non-exported `:brr_voice_probe` process, with synthetic audio only, fixed noBackup stage codes, Android own-process exit category and no raw tombstone/data collection. Live GTCRN remains paused and 110M unchanged. The ~600–700 MB Ultra migration is postponed until real North Indian English WER, memory and call-jank gates pass; no heavy weights in the diagnostic APK. [See safety contract](ONE_SHOT_VOICE_CRASH_DIAGNOSTICS_2026_11_20.md).


> **LATEST EMERGENCY STATUS: INACTIVE/PAUSED IN NEXT SIGNED HOTFIX.** Owner reported a keyboard/process crash 1–2 seconds after tapping voice in v1.0.195. Until ARM64 native startup is root-caused and device-proven, both live capture source and OfflineVoiceEngine are restored from d856244. The 535 KB GTCRN model remains bundled but its native engine must **not** be instantiated or called. BR Brain explicitly shows “GTCRN filtering paused”. The active integration description below is a historical design, **not current runtime behavior**.

Status: **OWNER GO — implementation source staged for exact-SHA Android CI**. Do not claim the owner installed the new APK or the Oppo microphone was tested until real observations exist. This feature is independent of the deferred Ultra ASR replacement and BR ContextLM.

## Decided stack, provenance, license

- Engine: **GTCRN**, official upstream `Xiaobin-Rong/gtcrn`, MIT license (Rong Xiaobin, 2024). Pinned immutable commit `502ebfab64da7c4a9af78dcb9c6ceef1ebb01c73`.
- Exact model: `stream/onnx_models/gtcrn_simple.onnx`, **535,190 bytes**, upstream Git blob `015454eef64bd4328e6ff4946c231d4a4a632e93`.
- Runtime: the already-present **sherpa-onnx 1.13.3**, whose `OfflineSpeechDenoiser` Kotlin interface supports GTCRN through `AssetManager`. No second inference library. The runtime AAR remains SHA-256 pinned in `tools/fetch-board-sherpa-onnx.sh`.
- `tools/fetch-board-gtcrn.sh` obtains exact upstream model only **during development/CI build**, verifies blob and byte count before copying to `app/src/main/assets/voice/gtcrn_simple.onnx`. The model is distributed **inside every debug and original-signed APK**; Android never downloads/extracts it or needs an internet permission.
- `tools/verify-gtcrn-packaging.sh` reads the final APK and verifies exact weight bytes and bundled MIT license; wired into **both** build workflows. The ~136 MB Parakeet 110M voice bootstrap stays separate/optional, preserving existing user-installed Parakeet and signer/package/data. The `verify-voice-model-packaging.sh` lightweight-APK policy exempts **only** `assets/voice/gtcrn_simple.onnx`, with independent hash verification, and rejects every other unapproved ONNX/Parakeet asset. A normal update must **not** purge Parakeet.

## Audio data path

`KeyboardPcmAudioSource` uses Android `AudioRecord` VOICE_RECOGNITION, **16 kHz mono PCM16**, and `PrivatePcmBuffer` in RAM. After Stop, `OfflineVoiceEngine` on its dedicated single decode worker converts each segment to floats. `VoiceNoiseProfile` checks 20 ms RMS windows; GTCRN engages only when 20th-percentile noise floor is at least ~−44 dBFS and 85th percentile is at least 2.5× louder, with >= 800 ms of audio. Clean audio bypasses; this gate is heuristic, **not calibrated WER evidence**.

`PrivateGtcrnDenoiser` warms the bundled engine while the user records, processes selected segments before Parakeet, accepts only valid 16-kHz, same-length, finite-range, non-erased output, and wipes enhanced float arrays after decoding. Missing model/native error or invalid output falls back to original speech; no microphone failure or network fallback. Native recognizer and denoiser share the existing 90s idle release. Captured PCM and transcript continue on the preexisting private paths and are wiped as before. No raw voice or transcript is stored in analytics.

`VoiceDenoiseMetrics` holds only in-process counters (enhanced windows, clean bypasses, fallback counts, last GTCRN processing ms). BR Brain refreshes these in Settings. They reset with the process. No accuracy percentage is inferred from activity.

## Earbuds and multiple microphone constraints

Android ordinarily exposes a *single logical audio input stream* for Bluetooth headset use, often with vendor beamforming/noise suppression already applied inside the earbuds. `getActiveMicrophones()` is not a promise of raw independently addressable bud microphone channels; Android concurrent microphone access can be restricted by platform audio policy. This implementation observes `AudioRecord.getRoutedDevice()` after capture starts and shows that last OS-selected logical route in BR Brain (or “unavailable” when Android does not expose it). It deliberately lets Android route the single active mic through `AudioRecord` and evaluates the **actual PCM**, independent of whether the OS selected earbuds or phone. It does **not** fake both-earbud + phone-mic fusion, change OS-wide routing, or forcibly re-enable Bluetooth SCO. A later multi-channel processing pass needs observable per-channel capture plus device verification.

## Failure modes and test gates

1. `VoiceNoiseProfileTest` covers clean/noisy/constant/silence/short/NaN gates and bad-output fallback.
2. `test_bundled_voice_contract.py` checks private source and that both workflows fetch and verify exact model/attribution.
3. **Exact-SHA debug GitHub Actions:** model fetch; Gradle debug build; APK-integrated blob+MIT verification; existing Parakeet packaging, signing identity, local-only permissions, full `testRunTests`, Python tests.
4. **Original-signed Obtainium workflow** only after exact-commit debug success; independently verifies model blob, optional Parakeet policy, original cert, `com.mangoloads.board`, no INTERNET permission.
5. **Still required:** install signed APK on actual Oppo Reno10 5G; test phone mic, wired, Bluetooth HFP/LE devices in quiet and noisy scenes; 30–100 consented North Indian English/Hinglish utterances; compare paired Parakeet WER raw vs GTCRN auto plus P50/P95 elapsed Stop-to-text, battery, crashes, call route interruption and private/secure fields. Do not declare GTCRN *more accurate* from synthetic noise alone. If WER worsens, adjust gate/revert denoiser without changing Parakeet.

Reference URLs:
- https://github.com/Xiaobin-Rong/gtcrn
- https://github.com/k2-fsa/sherpa-onnx/blob/v1.13.3/sherpa-onnx/kotlin-api/OfflineSpeechDenoiser.kt
- https://github.com/chabandou/poise-android
- https://anecho.ai/blog/does-noise-suppression-help-stt

## Repository instruction for all future agents

When touching voice capture, model, keyboard/BR Brain telemetry or releases: inspect this document plus `AGENTS.md`, `PROJECT_CONTEXT.md`, `docs/ENGINEERING_HANDOFF.md` and the BR Brain topic contracts; update them after material changes with a **tested-vs-pending** account. Never remove the original Parakeet, change signing identity, add runtime downloads/network permission, or promise earbud multi-mic access without hardware proof. Preserve privacy buffers and transparent raw fallback. GitHub `main` only with head lease, no force push. Never claim device installation without evidence.
