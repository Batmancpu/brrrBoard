# Final update implementation status — 2026-11-22

## Owner device evidence

The owner confirmed the visual checks and supplied a same-audio voice comparison. Ultra produced fewer recognition errors than 110M on that take, while using more memory and taking longer to load and decode. The owner chose Ultra as the preferred model. This one-device, one-take result is not a broad accuracy guarantee.

## Shipped source changes in this update candidate

- Verified installed Ultra is the default active keyboard model; the 110M pack is selectable as a lower-memory/faster fallback. Keyboard dictation and ordinary Voice Lab transcription use the selected pack; the paired A/B test still explicitly runs 110M and Ultra sequentially on the same recording.
- Everyday tools → Offline voice contains the model radio options and one brrClean on/off switch. brrClean defaults on for new installs; failed cleaning preserves original audio.
- Sound & haptics contains the important-only DND option. Keypress and gesture haptics remain suppressed in DND; explicit privacy changes and confirmed clipboard/history clears can use a brief pulse. The keyboard seeds DND state at startup as well as responding to system changes.
- BR Brain no longer duplicates model/voice diagnostic controls. Settings exposes one Device checkup with seven steps and exactly one final plain-text copy action. The report includes fresh content-free geometry and numeric voice metrics, but excludes test text, reference, transcript, audio, and clipboard content.
- Ultra and 110M weights are not included in the update. Verified private model files remain on the device across an app update. The release workflow retains `bundle_ultra_model` as an explicit future opt-in; this update's path does not change that option.
- Existing Emoji geometry fix, suggestion path, toolbar layout, and progressive blur implementation remain part of the cumulative source.

## Deliberately gated work and limits

ContextLM remains disabled and unbundled: the corrected fixed-holdout candidate still failed to improve the native/n-gram baseline. Do not represent it as shipped until native-decoder quality, false-autocorrection, speed, memory, upgrade, and privacy gates pass. Detailed evidence remains in `docs/QUALITY_GATE_REEVALUATION_2026_11_22.md`.

The workspace cannot access the pinned Gradle distribution through its network proxy. Use exact-commit GitHub Actions debug/unit CI and the original-signer release workflow, as documented in `docs/LOCAL_BUILD_NETWORK_FALLBACK.md`. CI does not replace the owner's final physical-device audit.
