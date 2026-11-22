# Final update implementation status — 2026-11-22

## Owner follow-up — BR Brain and Settings refresh

This newer local candidate supersedes the earlier Device Checkup and settings placement bullets below: the guided Device Checkup, one-report copy flow, and now-unused Emoji geometry report/logger have been removed. The 110M model selector is in Advanced controls; Reset is under Backup & restore; About is top-level. The protected-app picker now gives app names and package IDs separate lines. Correction & suggestions is reachable from BR Brain with one restrained champagne-gold entry shimmer. The architecture and privacy migration plan is [BR Brain unified architecture](BR_BRAIN_UNIFIED_ARCHITECTURE_PLAN_2026_11_22.md).

The exact published release and automated build evidence are tracked on the GitHub Releases page and in the repository release history. This workspace cannot perform physical-device acceptance. Ultra stays the preferred default; Advanced controls can explicitly switch to the compact model. Suggestion ranking behavior is unchanged by the Settings/layout cleanup.

## Owner follow-up — suggestion motion and Settings entry point

- Replaced the per-glyph matching renderer with a presentation-only whole-slot crossfade. The outgoing label uses a non-interactive native `TextView` overlay while the current suggestion and tap target update immediately; the new label fades and settles in with a slight vertical movement and small side-slot stagger. In-flight outgoing labels are bounded when fast typing retargets the row. Prediction and text shaping stay outside the animation path.
- Settings search is labeled “Search” and constrained to a slimmer field. Search remains on-device and deterministic, with phrase intents and setting aliases for natural-language descriptions; no remote context service is used.
- Added a sapphire-and-gold “Type” preview beneath Settings search. It opens an in-place keyboard field, has a live restrained glint, and marks only that field as no-learning/no-microphone.
- Incognito footer controls use a normal smile icon with a small gold shield-lock badge; the mic badge shares the same transition frame and stays legible over voice activity lighting.
- The 126 ms transition duration and easing are local design choices. Apple does not publish QuickType’s exact internal animation timings, and the supplied reference was a still image rather than a motion capture, so this is a restrained visual approximation and must not be described as a measured one-to-one recreation. It follows the general motion principles in [Apple’s Motion HIG](https://developer.apple.com/design/human-interface-guidelines/motion) and the system keyboard guidance in [Virtual Keyboards](https://developer.apple.com/design/human-interface-guidelines/virtual-keyboards).

The Android release workflow is the build and signing authority for each published commit. Physical-device acceptance remains the owner-side audit; this workspace cannot run it.

## Owner device evidence

The owner confirmed the visual checks and supplied a same-audio voice comparison. Ultra produced fewer recognition errors than 110M on that take, while using more memory and taking longer to load and decode. The owner chose Ultra as the preferred model. This one-device, one-take result is not a broad accuracy guarantee.

## Source changes in this update candidate

- Verified installed Ultra is the default active keyboard model; the 110M pack is selectable as a lower-memory/faster fallback. Keyboard dictation and ordinary Voice Lab transcription use the selected pack; the paired A/B test still explicitly runs 110M and Ultra sequentially on the same recording.
- Everyday tools → Offline voice contains the model radio options and one brrClean on/off switch. brrClean defaults on for new installs; failed cleaning preserves original audio.
- Sound & haptics contains the important-only DND option. Keypress and gesture haptics remain suppressed in DND; explicit privacy changes and confirmed clipboard/history clears can use a brief pulse. The keyboard seeds DND state at startup as well as responding to system changes.
- BR Brain no longer duplicates voice selection/cleaning controls. The previous guided Device Checkup/report flow is removed in the newer candidate above.
- Ultra and 110M weights are not included in the update. Verified private model files remain on the device across an app update. The release workflow retains `bundle_ultra_model` as an explicit future opt-in; this update's path does not change that option.
- Existing Emoji geometry fix, suggestion path, toolbar layout, and progressive blur implementation remain part of the cumulative source.

The unified BR Brain coordinator, encrypted UserHistoryDictionary migration, and new reranker are not implemented yet. Current typing/Emoji/swipe code still calls its existing component engines directly; this candidate changes their Settings access and presentation only. Do not claim the whole Brain plan is complete.

## Deliberately gated work and limits

ContextLM remains disabled and unbundled: the corrected fixed-holdout candidate still failed to improve the native/n-gram baseline. Do not represent it as shipped until native-decoder quality, false-autocorrection, speed, memory, upgrade, and privacy gates pass. Detailed evidence remains in `docs/QUALITY_GATE_REEVALUATION_2026_11_22.md`.

The workspace cannot access the pinned Gradle distribution through its network proxy. Use exact-commit GitHub Actions debug/unit CI and the original-signer release workflow, as documented in `docs/LOCAL_BUILD_NETWORK_FALLBACK.md`. CI does not replace the owner's final physical-device audit.
