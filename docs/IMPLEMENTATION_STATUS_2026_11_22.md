# Final update implementation status — 2026-11-22

## 2026-11-23 — v1.0.252 released; Brain consolidation remains open

Original-signed, non-prerelease [v1.0.252](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.252) is published from exact source `fbd13cdacc1f10c51f3898a931f3ffd49636922b`. Merged-SHA debug/full Android checks passed in `45119781916`; the original-signer workflow `45119781929` passed and published the release. APK size 63,544,172 bytes, SHA-256 `8f20284bb8d0179f1341ba50a801b8f63b40afe24f61a4a3e311c5bec917d1f1`. The [README download section](../README.md#download-brrboard-for-android) now describes the official source, checksum verification, in-place updates and model retention. Both Parakeet weight bundles were excluded.

The release contains the biometric-only protected route and note vault, protected Brain detail counters, default-off one-use private gesture, latest hold-to-talk behavior and settings changes described below. Do not mark the larger Brain architecture complete: a unified coordinator and an app-layer encrypted native UserHistoryDictionary migration remain unfinished; the ContextLM quality gate failed, so its setting remains opt-in and manual-only. Oppo biometric/gesture/haptic/typing acceptance is pending the owner's device. See `BIOMETRIC_PRIVACY_AND_PRIVATE_GESTURE_PLAN_2026_11_23.md` and `BR_BRAIN_UNIFIED_ARCHITECTURE_PLAN_2026_11_22.md`.

## 2026-11-23 — shared BR Brain coordinator source candidate (release pending)

`BrSuggestionBrain` now centralizes editor eligibility and dispatch for Smart Emoji scoring/learning, the existing encrypted swipe-correction adapter, and the opt-in manual ContextLM reranker. The native decoder remains the fast first producer; candidate ordering and automatic correction stay unchanged. The coordinator does not read surrounding editor text itself, load a model, or perform disk/Keystore work on key-down. A pure policy test covers normal chat, no-learning, incognito, password, and non-general-editor exclusions. README now points to signed v1.0.252 and provides platform-specific SHA-256 commands. This follow-up needs full exact-SHA Android CI and the original-signer gate; it is not yet a newer APK. The native history dictionary’s app-layer encryption and Oppo physical acceptance remain open.

## Owner GO — private hold-to-talk shipped in v1.0.251

Original-signed [v1.0.251](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.251) ships long-press/release dictation, guarded live provisional text outside Incognito, final-only Incognito delivery, default-on auto-send only for hold-to-talk in `IME_ACTION_SEND` fields, swipe-to-Cancel mic-to-bin feedback, important-action haptics, and removal/purging of recovered transcript persistence. No spoken accessibility announcement is used. Pause-aware batching uses a bounded amplitude heuristic and the existing six-second hard cap; no neural VAD weights are bundled. Source `2f2e7424883fab5f597e4f2c3889ec6bfb00ee29` passed feature debug/full suite `45057840833`, main debug/full suite `45058407501`, and original-signer release `45058407505`. APK size 63,511,404 bytes, SHA-256 `9f0f4f609beadfede67a5e11b885d7cdc12cbb4e36e0b84f863fa55892f5bf17`; no Parakeet model weights were bundled. Physical interaction acceptance remains owner-side. Full details: [hold-to-talk implementation](VOICE_HOLD_TO_TALK_FOLLOWUP_2026_11_22.md).

## Owner follow-up — BR Brain and Settings refresh

This newer local candidate supersedes the earlier Device Checkup and settings placement bullets below: the guided Device Checkup, one-report copy flow, and now-unused Emoji geometry report/logger have been removed. The 110M model selector is in Advanced controls; Reset is under Backup & restore; About is top-level. The protected-app picker now gives app names and package IDs separate lines. Correction & suggestions is reachable from BR Brain with one restrained champagne-gold entry shimmer. The architecture and privacy migration plan is [BR Brain unified architecture](BR_BRAIN_UNIFIED_ARCHITECTURE_PLAN_2026_11_22.md).

The exact published release and automated build evidence are tracked on the GitHub Releases page and in the repository release history. This workspace cannot perform physical-device acceptance. Ultra stays the preferred default; Advanced controls can explicitly switch to the compact model. Suggestion ranking behavior is unchanged by the Settings/layout cleanup.

## Owner follow-up — explicit ContextLM device test

- Added a roughly 1.25 MB Hindi-English experimental candidate model trained from the repository's pinned CC BY 4.0 public corpus revisions. Its manifest, vocabulary and weights are hash-checked; the APK also includes the corpus attribution.
- Settings → BR Brain exposes a default-off **ContextLM test** switch. When enabled, the model is loaded off the input path and scores a bounded set of existing native candidates on the decoder worker. It can reorder manual word suggestions only in eligible normal-prose fields, checks editor generation before and after scoring, and cannot generate candidates or alter autocorrection.
- BR Brain shows content-free scored-batch, changed-order, load-failure and rolling p50/p95 model-time counts. Typed text, candidate strings and context are not saved or logged. Turning the experiment off drops the model from the live process.
- The candidate remains experimental: the reproducible 3,000-case public-corpus proxy did not improve Recall@3 and is not a native-decoder or device accuracy result. Keep the production ContextLM path off until native candidate recall, correction safety, latency, memory and phone acceptance gates pass. See [quality-gate reevaluation](QUALITY_GATE_REEVALUATION_2026_11_22.md).
- No Parakeet model weights or voice comparison/A-B workflow are included.

## Owner follow-up — suggestion motion and Settings entry point

- Replaced per-glyph matching with a bounded presentation-only transition. For simple Latin labels sharing a prefix, the outgoing suffix clips back toward the unchanged prefix while the new label and tap target are already current; unrelated words use a short whole-label settle. The outgoing `TextView` overlay is non-interactive and capped during rapid retargeting. Prediction and text shaping stay outside the animation path.
- Settings search is labeled “Search” and constrained to a slimmer field. Search remains on-device and deterministic, with phrase intents and setting aliases for natural-language descriptions; no remote context service is used.
- Added a sapphire-and-gold “Type” preview beneath Settings search. It opens an in-place keyboard field, has a live restrained glint, and marks only that field as no-learning/no-microphone.
- Incognito footer controls use a normal smile icon with a small gold shield-lock badge; the mic badge shares the same transition frame and stays legible over voice activity lighting.
- The 126 ms transition duration and easing are local design choices. Apple does not publish QuickType’s exact internal animation timings, and the supplied reference was a still image rather than a motion capture, so this is a restrained visual approximation and must not be described as a measured one-to-one recreation. It follows the general motion principles in [Apple’s Motion HIG](https://developer.apple.com/design/human-interface-guidelines/motion) and the system keyboard guidance in [Virtual Keyboards](https://developer.apple.com/design/human-interface-guidelines/virtual-keyboards).

The Android release workflow is the build and signing authority for each published commit. The current candidate is not yet signed or published; that happens after exact-SHA GitHub Actions succeeds. Physical-device acceptance remains the owner-side audit; this workspace cannot run it.

## Owner device evidence

The owner confirmed the visual checks and supplied a same-audio voice comparison. Ultra produced fewer recognition errors than 110M on that take, while using more memory and taking longer to load and decode. The owner chose Ultra as the preferred model. This one-device, one-take result is not a broad accuracy guarantee.

## Source changes in this update candidate

- Verified installed Ultra is the default active keyboard model when no explicit model preference exists; the 110M pack is selectable as a lower-memory/faster fallback. Keyboard dictation uses the selected installed pack. The owner selected Ultra after the recorded single-take comparison. The Voice Lab UI, comparison engine, isolated test service, and APK artifact job have been removed.
- Everyday tools → Offline voice contains the model radio options and one brrClean on/off switch. brrClean defaults on for new installs; failed cleaning preserves original audio.
- Sound & haptics contains the important-only DND option. Keypress and gesture haptics remain suppressed in DND; explicit privacy changes and confirmed clipboard/history clears can use a brief pulse. The keyboard seeds DND state at startup as well as responding to system changes.
- BR Brain no longer duplicates voice selection/cleaning controls. The previous guided Device Checkup/report flow is removed in the newer candidate above.
- Ultra and 110M weights are not included in the update. Verified private model files remain on the device across an app update. The release workflow retains `bundle_ultra_model` as an explicit future opt-in; changed lock files do not auto-bundle it, and this update does not add voice model weights.
- Existing Emoji geometry fix, suggestion path, toolbar layout, and progressive blur implementation remain part of the cumulative source.

The unified BR Brain coordinator and encrypted UserHistoryDictionary migration are not implemented yet. The experimental ContextLM is a bounded opt-in test adapter, not that coordinator or an approved production reranker. Current typing/Emoji/swipe code still calls its existing component engines directly. Do not claim the whole Brain plan is complete.

## Deliberately gated work and limits

The production ContextLM ranker remains disabled. A separate experimental candidate is bundled solely for the explicit BR Brain manual-suggestion test; its corpus proxy remains below the n-gram baseline and does not establish device accuracy. Do not represent it as production-ready until native-decoder quality, false-autocorrection, speed, memory, upgrade, and privacy gates pass. Detailed evidence remains in `docs/QUALITY_GATE_REEVALUATION_2026_11_22.md`.

The workspace cannot access the pinned Gradle distribution through its network proxy. Use exact-commit GitHub Actions debug/unit CI and the original-signer release workflow, as documented in `docs/LOCAL_BUILD_NETWORK_FALLBACK.md`. CI does not replace the owner's final physical-device audit.
