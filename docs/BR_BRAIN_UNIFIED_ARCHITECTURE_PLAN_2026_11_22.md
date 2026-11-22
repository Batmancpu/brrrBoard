# BR Brain: unified on-device suggestions plan

Date: 22 November 2026
Status: Settings cleanup is implemented as an unbuilt source candidate; unified Brain coordination, learning-store migration and a new ranking model remain unimplemented. No new ranking model is activated by this plan.

## Direction

Build BR Brain as the small, privacy-gated coordinator for the existing typing, next-word, autocorrect, swipe and Emoji systems. It should improve candidate ranking while leaving candidate generation and immediate insertion on the mature native keyboard path. Do not put a general-purpose LLM in the per-keystroke path. The current ContextLM candidate did not beat the native baseline on the corrected held-out evaluation, so it remains off until a new candidate earns its way in.

The “one place” is one BR Brain API and one encrypted personal-learning profile, not one enormous data/model file. Bundled language dictionaries, runtime voice models and user-owned dictionaries keep their native formats and lifecycle; BR Brain reads their candidates through adapters and does not duplicate them.

## Repository audit: current starting point

- `Suggest.kt` and `DictionaryFacilitatorImpl.kt` still own the established native candidate path for typing, next-word prediction and correction. Static dictionaries and the native decoder remain the fast baseline.
- `BrPersonalContextFeatures` already defines a bounded, non-persisted feature snapshot for up to 12 native candidates, and `BrContextLmInt8` is a reference CPU executor. Neither is wired into live ranking, and no approved weights are present. Reuse this candidate contract rather than duplicating a feature pipeline.
- `BrrSmartEmojiEngine` combines reviewed phrase cues, recent Emoji and a bounded encrypted selection-pattern model. It learns only after an Emoji selection and rejects ineligible fields. Its candidate scoring is currently invoked from the suggestion path; measure this carefully before adding any more synchronous work.
- `SwipeCorrectionLearner` stores bounded gesture-skeleton-to-confirmed-word associations in `noBackupFilesDir` and seals them using `BrainLearningVault` (AES-GCM with a non-exportable Android Keystore key). It is an alternative candidate prior, not a silent replacement.
- `BrainPerformanceMetrics` keeps content-free timing windows in RAM. This remains the place to inspect typing/next-word/swipe/Emoji timings; it is not a guided device checkup.
- `VoiceModelChoice` selects between installed Parakeet packs. Voice recognition should remain a separate producer: no speech model should run during ordinary typing, and BR Brain must never ingest microphone audio or intermediate transcripts. Only an explicitly committed, eligible correction may be considered as a text-learning event.
- `UserHistoryDictionary` is an `ExpandableBinaryDictionary` persisted as a `.dict` file under `filesDir`. That file is app-private and protected by Android storage security, but unlike the swipe/Emoji profile it is not sealed by the app's AES-GCM vault. This is the first data-migration question to solve before claiming app-level encryption for all learned text.
- The manifest already sets `allowBackup=false` and uses private extraction rules. Android's documentation notes that on some devices this can still allow device-to-device transfer; the personal learning profile should therefore live in a no-backup location and never depend on OS restore for its key or encrypted records.
- `BrainLearningVault` currently bounds a sealed record to 16 KiB. A unified store may need versioned chunks/snapshots rather than expanding this limit without measurement.
- `LatinIMEHandler.editorGeneration()` and the batch-input path already reject work from an old editor. A coordinator should carry/reuse that generation through any later asynchronous reranking rather than introduce a second unrelated counter.

## Brain contract

`BrSuggestionBrain` will own coordination and eligibility, while feature adapters keep their specialized domain logic:

1. **Candidate producers:** native decoder/spell checker, user-history dictionary, gesture decoder, reviewed Emoji index, and the selected voice recognizer after the user commits dictation.
2. **Candidate contract:** each candidate carries its surface, source, native confidence/rank, language, editor generation and only the features that are allowed for that editor. Text candidates and Emoji candidates remain separate types and separate presentation lanes.
3. **Ranking:** native candidates render immediately. A small deterministic or compact learned reranker may reorder the already-produced top K only when it returns before a short deadline and the editor generation still matches. Missing, late, malformed or low-confidence results fall back to native order.
4. **Learning events:** learn from explicit corrections, accepting a correction, selecting a swipe alternative and selecting a suggested Emoji. Raw sentences, audio and screenshots are not training records. Preserve intentional typing and never auto-correct high-risk tokens just because the model is confident.
5. **One lifecycle owner:** editor switches, cursor moves, composing resets, incognito changes and screen changes invalidate stale requests and cached candidates. Use monotonically increasing generations to discard results from the previous field.

## Privacy, encryption and deletion contract

- **On-device only:** no server inference, telemetry upload, remote learning, or model download. Do not add network permission or a background sync path for Brain data.
- **Hard eligibility gate:** reject passwords, secure/protected apps, incognito, `IME_FLAG_NO_PERSONALIZED_LEARNING`, non-prose fields, and any editor that asks the IME not to learn. The gate must be called before candidate features are assembled and again before a learning event is queued.
- **App context:** default to editor type and language, not the host application's package name. If per-app personalization is later offered, require an explicit opt-in; derive a keyed HMAC scope from the package ID, store only that scope inside the encrypted profile, cap its count, allow per-app erase, and expire stale scopes. Never show or log the raw package ID as a suggestion feature.
- **Encrypted persistence:** versioned AES-GCM records with Android Keystore keys, atomic writes, authenticated format/version headers, bounded file size and no plaintext fallback. Decryption failure must disable learning and fall back to the native engine; it must not silently create a plaintext replacement.
- **Legacy dictionary migration:** first prove the native decoder can read a RAM-backed or otherwise non-persistent decrypted dictionary image. Then export a bounded snapshot, seal it, authenticate/decode it, compare word/entry counts and sample lookups, and only then retire the old `.dict`. If the decoder requires persistent plaintext mmap, keep the feature gated until a design avoids leaving a plaintext cache behind; do not claim that a cache file is encrypted.
- **Uninstall:** place learned state and its key only in app-private no-backup storage. Android removes app-specific files on uninstall; deleting the Keystore key makes any orphaned sealed bytes unusable. Keep backup disabled and explicitly exclude Brain data from device-transfer backup rules. An owner-created Downloads backup is outside the app sandbox and cannot be purged by uninstall; make any future export explicit and encrypted with a user-controlled passphrase.
- **User control:** BR Brain should expose one simple learning switch, an optional per-app context switch only if implemented, a clear/erase action with honest scope, and a concise “what is stored” explanation. Resetting ordinary Settings must not unexpectedly erase learning; Brain reset/erase is a distinct explicit action.

## Latency and reliability contract

- No file reads/writes, Keystore operations, model loading, host `InputConnection` calls, or lock waits in key-down, candidate-row draw or Compose rendering.
- Load one bounded immutable Brain snapshot on a background executor, publish it atomically, and batch writes through a single low-priority worker. Keep only short-lived editor context in RAM.
- Keep the native decoder first and immediate. Give optional reranking a strict deadline and a candidate cap (initially 8–12); discard late output and preserve the current strip rather than flashing or clearing it.
- Use the existing content-free percentile timers to establish a device baseline. Enable a new ranker only if its added p95 compute stays inside the agreed typing budget on the Reno10-class device, memory remains bounded, and no visible key/strip jank appears during video calls or other foreground workloads. “Fast on a desktop” is not acceptance evidence.
- Bound queues and coalesce updates: if several keystrokes arrive, score only the newest request. Model or Keystore failure must never block input or crash the IME.

## Learning and quality gates

### Training signals

- Word correction: explicit accepted correction, autocorrect undo/cancel, and deliberately selected alternatives; preserve the raw intended word only in the encrypted local user dictionary where existing semantics need it.
- Next-word: count-based short n-grams or a compact ranker from eligible, committed text; no whole-message history. Candidate source, locale, recency and a bounded context feature can be used without saving the preceding sentence.
- Swipe: retain the existing small confirmed mapping; collect no touch traces or coordinates.
- Emoji: retain reviewed phrase/CLDR features plus recency and selected Emoji associations; context stays ephemeral and only the learned association is stored.
- Voice: use the selected Parakeet to transcribe only on explicit mic action. Treat the recognized text as untrusted until committed. Never persist audio, intermediate transcripts or reference text as Brain training data.

### Evidence before activation

1. Freeze candidate splits and provenance for any public training material; record source, license and preprocessing.
2. Compare the native baseline, existing local personalization, and a candidate reranker on held-out real decoder candidates. Report top-1 accuracy, Recall@3, correction reversal rate, false automatic corrections, Emoji relevance, language/code-switch slices and calibration.
3. Keep shadow scoring content-free: aggregate counts/timings only, no candidate words, phrases, app IDs, audio or field text.
4. Turn on only a confidence-calibrated reranker that improves a held-out metric without raising harmful corrections. Keep an immediate off switch and the native ordering fallback.
5. If no neural model passes, keep native n-grams + local learning. Do not ship a model only to satisfy the “AI brain” label.

## Presentation

- BR Brain is the home for correction, prediction, emoji and learning controls; the existing correction preference keys remain the source of truth until the settings screen is intentionally consolidated.
- Use the current pearl/midnight palette and the theme's muted tertiary gold for a small BR Brain accent. A single entry shimmer is decorative and one-shot; no continuous animation, gold blanket across Settings, or low-contrast text.
- Keep model-specific voice controls under Offline voice / Advanced controls. Keep the 110M fallback discoverable under Advanced controls while the preferred Ultra model remains in the everyday voice page.
- Put destructive reset with Backup & restore, outside the Settings home. Keep About at the top-level Settings home.

## Execution order

1. **Settings and privacy cleanup (source candidate complete):** remove the recurring Device Checkup and obsolete geometry-report UI/instrumentation; move Parakeet 110M selection to Advanced controls; move Reset into Backup & restore; keep About outside Advanced controls; separate protected-app labels from package IDs; link Correction & suggestions from BR Brain and apply a restrained gold accent. Compilation and Android acceptance remain open.
2. **No-behavior-change coordinator:** add `BrSuggestionBrain` request/result types that reuse `BrPersonalContextFeatures`, the existing strict editor eligibility policy, and `LatinIMEHandler.editorGeneration()`; adapt the native, Emoji and swipe paths; keep the existing display order and learning unchanged. Prove no per-keystroke I/O.
3. **Encrypted learning store:** inventory every personal word/association/backup file; design native decoder interop; migrate old data with verification and rollback; prevent app/cloud/device transfer and ensure clear/uninstall semantics.
4. **Measured ranker:** capture an on-device baseline, train/evaluate only on licensed data, run shadow scoring on actual native top-K candidates, and tune confidence/rollback behavior. Keep ContextLM disabled until it clears quality and latency gates.
5. **Adapters and refinement:** connect next-word, text correction, swipe, Smart Emoji and committed voice events through one coordinator. Preserve independent component fallbacks and per-surface safety rules.
6. **Acceptance and release:** after implementation, run targeted privacy, storage migration, ranking, performance, accessibility and update-preservation evidence; then use exact-commit debug and original-signer CI before publishing. Do not bundle new weights or increase APK size without an explicit request.

## Research notes

- Android's `EditorInfo` explicitly defines `IME_FLAG_NO_PERSONALIZED_LEARNING`; this is a must-obey gate, not a signal to infer around. [Android EditorInfo](https://developer.android.com/reference/android/view/inputmethod/EditorInfo#IME_FLAG_NO_PERSONALIZED_LEARNING)
- AOSP LatinIME keeps a user-history dictionary as a distinct provider integrated with native candidate generation. That supports preserving the decoder and adapting its candidates instead of replacing the keyboard with one general LLM. [AOSP LatinIME UserHistoryDictionary](https://android.googlesource.com/platform/packages/inputmethods/LatinIME/%2B/fa1e65cb3a5dcce6299a6dd067cee95720107307/java/src/com/android/inputmethod/latin/personalization/UserHistoryDictionary.java)
- Android recommends moving long/non-UI work off the main thread; this aligns with a coalesced, bounded Brain worker and immediate native fallback. [Android threading guidance](https://developer.android.com/topic/performance/threads)
- Android Keystore limits key use and protects key material; the current vault already follows this general design with AES-GCM. [Android Keystore](https://developer.android.com/privacy-and-security/keystore), [KeyGenParameterSpec](https://developer.android.com/reference/android/security/keystore/KeyGenParameterSpec.Builder)
- App-specific files are removed on uninstall, while Auto Backup/device transfer has additional behavior and `allowBackup=false` is not a sufficient universal guarantee for every OEM. Put personal Brain state in no-backup storage and inspect extraction rules explicitly. [App-specific storage](https://developer.android.com/training/data-storage/app-specific), [Auto Backup](https://developer.android.com/identity/data/autobackup)
- Reddit discussion about offline keyboards is useful evidence of user expectations, not a technical or accuracy source. [Community privacy discussion](https://www.reddit.com/r/androidapps/comments/1sbo5f9/keyboard_app_with_better_privacy/)

## Open engineering question

The main technical unknown is how to preserve the fast native binary user-history dictionary while guaranteeing that no plaintext persistent learning file or cache remains. Investigate the decoder's mmap/file requirements before choosing the encrypted-at-rest migration. This is a design gate, not a reason to weaken the native fallback or silently keep plaintext.
