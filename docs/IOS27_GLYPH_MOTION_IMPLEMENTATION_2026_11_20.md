# iOS-style QuickType glyph-motion implementation (20 November 2026)

## Owner authorization and release boundary

Owner said **GO** for the consolidated behavior package and requested **one next testable original-signed APK**, not an APK for every internal source iteration. Baseline is published v1.0.201 at `29d3903`; current GitHub `main` at start was `26982d2`. The previous B0–B9 implementation, v1.0.201 voice crash trail, version history, failed-transcript recovery and BR Brain dashboard are **already present** and must not be replaced or silently re-released as new features. The separate 0.6B voice migration/GTCRN JNI remains on hold. Owner's Oppo Reno 10 5G cannot be simulated by CI.

## Evidence, implementation, and honest precision

- Apple patent US11120220B2 describes a retained prefix/suffix moving toward the new center, outgoing characters shrinking/fading, entering glyphs growing/fading, and potentially fixed overall transition duration. The patent provides **no iOS 27 milliseconds or exact spring curve**.
- The author of GitHub `lexrus/LTMorphingLabel` documented a QuickType-iOS8-inspired imitation using **0.600 seconds / 0.026 seconds per character**, not measurements of modern Apple keyboard timings. Do not paste these values into brrBoard as a claimed Apple specification.
- brrBoard prior iOS profile used 164ms settle and 24+92ms new-ink reveal; `SuggestionGlyphDiff` only animated new UTF-16 changed ranges. Expansion abruptly re-centered matching prefix letters and pure deletions snapped.
- `SuggestionGlyphMotion`: bounded 32-character ASCII LCS creates unique moving retained letters, outgoing ink (opacity/scale down) and incoming ink (opacity/scale up), with bounded stagger and unchanged 164ms budget. No per-frame LCS or whole-IME measurement. It does **not** claim to be an Apple timing measurement.
- `MorphingSuggestionTextView`: per-slot canvas drawing inside the single existing TextView, stable accessibility/click/tag hitboxes, no cross-slot translation or ghost overlay. Only plain ASCII suggestions get glyph motion. Complex shaped/RTL/Emoji/long words use existing `TextView`/style-span fallback to avoid incorrectly breaking glyph clusters.
- `SuggestionWordMorph.capture()` snapshots *actually visible* glyph positions/alpha before the preceding animator is ended; newer predictions begin from that visual state, not a stale completed target. The center confidence capsule retains its existing drawable and clock.
- Touch targets and prediction availability remain immediate; only decoration animates. System-reduced-motion uses the existing `KeyboardMotionSpec.enabled()` gate; Previous Default retains its 150ms prior implementation.
- `MainSettingsScreen`: BR Brain standalone card directly below optional setup card, before appearance/typing groups. Existing About → Version history stays intact.

## Tests and acceptance

- Added pure `SuggestionGlyphMotionTest` for `they` ↔ `they're`, inner replacement, unique retained letters, interrupted visual starting state, bounded animation and safe script fallback. No raw user text is recorded.
- Required CI: Android compile, complete `testRunTests` and version-history/brand/privacy/model gates on the **same source SHA**, then original-certificate Obtainium signing. Local Gradle wrapper distribution is unavailable in scratch; **local Android compile not asserted**.
- Physical Oppo checks: fast `they/they're`, `suggested/suggestion/suggestions`, long word, abbreviation, apostrophe, punctuation, URL, Emoji, and three side slots; compare switch during another animation. Check zero duplicate glyphs, tap opens visible word, center pill remains centered, no broken ligatures/RTL fallback, no extra CPU/frames and reduced-motion/TalkBack. Test failed transcript recovery after caret change and same-session Emoji return as previous signed features. The older Emoji transparent top band remains an **unresolved phone-only geometry acceptance item**.
- Exact iOS 27 timing/pixel parity requires calibrated high-frame-rate reference capture and Oppo screen/device comparison. Do not claim what was not measured.

## Other implementation status and remaining limitations

Editor restart privacy/actions, Emoji session memory, trackpad second-finger host-gated selection, correction reversal suppression, protected gesture dismissal accessibility, encrypted last-five recoverable transcription rail, version dropdown/timestamps, and BR Brain dashboard existed before this batch. Voice transcription plus simultaneous typing currently recovers to Clipboard when the caret moves; it does not silently merge text across changed destinations. Wrapped-line/Firefox/RTL vertical host selection, hardware keyboard/stylus, physical ColorOS geometry, real p95 metrics and Oppo TalkBack/reduced-motion remain pending. Advanced voice editing and a new bilingual neural ContextLM are deferred by earlier owner decisions. These are not blockers to publishing a **clearly labeled candidate for Oppo acceptance** when CI succeeds, but cannot be called fully device-certified.
