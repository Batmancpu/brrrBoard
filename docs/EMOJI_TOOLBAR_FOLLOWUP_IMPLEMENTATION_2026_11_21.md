# Emoji and Toolbar follow-up implementation checkpoint

Date: 21 November 2026  
Base: v1.0.227 source `ff2d082e`  
Status: **released as original-signed v1.0.230** from `ff33da1e1d5c3bc2cf1e1fdc49f47681986e89d4`.

## Implemented in source

- Removed Settings from the Emoji footer long-press shortcut arc. Clipboard and Toolbar remain; Settings is already an action inside Toolbar.
- When the Toolbar panel is visible, the footer microphone slot now displays an Emoji icon, announces “Switch to Emoji keyboard,” and dispatches the Emoji action. Accessibility actions switch with the visible mode so the same footer button does not expose stale voice behavior.
- Changed the full Toolbar panel to a fixed two-row grid inside the horizontal scroll container. The grid fills row-major and drag preview reflows into the same two rows. Tile footprint and icon/label sizes are compacted, with the large gray icon background removed.
- Removed repeated Smart Emoji ranking from the UI-thread suggestion-strip path. `Suggest` carries at most 48 ranked Emoji strings in an immutable `SuggestedWords` snapshot, does not retain the source text, and binds the snapshot to its editor identity. The strip rejects an editor mismatch. Normal text typing skips the extra contextual rank pass unless prediction or Emoji candidates require it.
- Added focused tests for two-row cell placement, two-shortcut placement, and bounded/copy-safe/immutable Emoji snapshots.

## Validation and open gates

- `git diff --check`: passed at initial review; rerun after all documentation edits.
- Android debug/full suite `44937170179`: passed, including 522 tests after updating `ToolbarPanelCompositionTest` to expect two rows and 72x60dp tiles.
- Original signer `44937170230`: passed same-commit debug gate, signing and publication.
- Signed APK: [v1.0.230](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.230), 64,969,203 bytes, SHA-256 `5ab4454364e4840a76e8cf31a566265daf17593b19feda0b4e558d2727a5176b`; independently downloaded and hash-verified.
- CI recovery: first SHA `60d77e35` compiled and had 521/522 tests pass; the sole failure was the stale Toolbar layout assertion. First repair SHA `bdf953f6` was rejected before compilation because the required release notes file was omitted. Final SHA `ff33da1e` included the test and notes correction and passed both gates.
- Reno10 visual/touch/accessibility and Emoji top-gap root cause: not verified. The repository includes a content-free `BrrImeBounds` probe, but no Reno10 trace or adb device is available here. Do not add a guessed margin or claim that the gap is fixed.
- ContextLM: not activated or packaged. The existing candidate lost held-out Recall@3 versus the native proxy, and held-out fusion selected native-only (neural weight 0.0); the runtime rejects unapproved manifests. A generic on-device LLM is not a substitute for measured keyboard prediction quality, field privacy, memory and latency.
- Voice: keep the established Parakeet TDT 110M unchanged. The Ultra export is hundreds of MB and has no matched English/Hinglish WER, actual-device latency, memory, thermal or cancellation win. One-time model bundling authorization does not make Ultra an evidence-based upgrade.

## Next engineering sequence

1. Refresh `main` before publishing to avoid overwriting concurrent work; validate release-note/static contracts and obtain exact-SHA full Android CI.
2. If CI is green, run the original-signer workflow and verify artifact SHA, package/certificate continuity, model/permission provenance, and release metadata.
3. Install on the Reno10, capture the single geometry-only log line, compare parent/child coordinates, and fix the first demonstrated divergent boundary. Recheck Browse/Search/Expanded, keyboard height, orientation, footer and toolbar interactions.
4. Profile real-device typing, Smart Emoji, panel open/scroll, Toolbar drag and voice p50/p95 with BR Brain. Keep measurements content-free.
5. Resume ContextLM only with reproducible holdout improvement over the native baseline and target-device gates. Resume Ultra only with same-audio quality improvement and device/resource gates. If either fails, preserve native/current voice as the safe product behavior.
