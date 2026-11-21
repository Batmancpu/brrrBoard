# iOS-style QuickType glyph-motion implementation (20 November 2026)

## 21 November 2026 — owner-video correction, local source only

The owner's 35.8-second / 30-fps real keyboard recording shows blank suggestion
rows for ~0.37s and ~0.33s, disjoint slot updates, and barely visible/missing
word morphs. Earlier 164ms and LCS-based design passed Android CI but never
passed physical animation acceptance. The following new candidate addresses
the actual pipeline and view lifecycle, not just the animation duration:

- `SuggestionPrefixPreview` immediately carries forward only existing native
  dictionary candidates matching the full current prefix, never Emoji, typed
  candidates or app completions. The typed prefix remains an untappable last
  resort, and previews never claim decoder auto-correction or stored learning.
- `InputLogic` filters the current prefix before async decode; ordinary typing
  requests dispatch without the former 15ms delay, and editor/request/caret
  freshness validation still discards stale results. Exact no-suggestion words
  prohibit eager word completions. Native ranking remains authoritative.
- `LatinIME` briefly keeps the old next-word row only for an eligible general
  editor and dictionary-empty prediction response. It revokes click tags at
  once, clears after 164ms if no real result arrives, and cancels the timer on
  newer results or editor switch. Clipboard/autofill/privacy retain priority.
- `SuggestionStripLayoutHelper` never collapses a one-word/wide-word result
  into a separate full-width slot; the existing three views and dividers remain
  attached. Five-chip mode also rebinds existing views without clearing the
  whole chip strip on each keystroke.
- `SuggestionGlyphMotion` now distinguishes a real shared **prefix of even one
  letter** from coincidental LCS matches between unrelated words. Incoming
  letters grow/fade in, outgoing letters fade/translate, and an unrelated or
  complex-script replacement uses a visible full-word native transition.
  `SuggestionWordMorph` samples the actual confidence-pill opacity/width before
  an interrupted transition. New editor startup retains NO entrance motion.
- Added `SuggestionPrefixPreviewTest`, expanded glyph-transition tests, and
  retained existing accessibility/RTL/secure/Emoji contracts. Tests have **not
  executed**: `./gradlew --offline --no-daemon :app:testRunTests` fails before
  compiling because the Gradle 8.14.4 download host is unreachable; `git diff
  --check` passes. No GitHub upload or APK was performed. Test on Oppo and
  obtain matching high-FPS Apple reference before asserting timing parity.


## 21 November 2026 — second replacement after signed v1.0.207

Owner reports signed v1.0.207 still lacks perceptible iOS-like replacement animation. The old source explicitly *snapped* at fewer than two shared prefix/suffix characters and at unrelated words, ran for only 112ms, and skipped five-word chips and typing-to-prediction style changes. Therefore v207 CI success proved compilation, **not** that the owner-requested behavior worked on Oppo.

New implementation candidate (not device-certified): `SuggestionGlyphMotion` uses bounded LCS over 18-character Latin words; **zero, one, or more** shared characters all produce motion on each replacement. Kept glyphs translate, inserted glyphs move and scale from 88% with immediately readable ink, and removed ink fades. One interruptible 164ms clock updates visible suggestion views and confidence capsule; new/changed chip widths no longer cancel the animation. Complex shaping/RTL/Emoji use native TextView with small settling movement; first keyboard presentation suppresses suggestion motion. First-frame English predictions now use a synchronous, non-autocorrecting generic initial row only in empty, safe prose fields, yielding to real native context predictions. The typed-only prefix remains a disabled center slot in a persistent three-slot row so it is not inserted twice. No app/decoder/BR Brain/voice model/signing changes.

Primary references: [Apple iPhone predictive text](https://support.apple.com/en-ca/guide/iphone/iphd4ea90231/27/ios/27), [WWDC23 Keep up with the keyboard](https://developer.apple.com/videos/play/wwdc2023/10281/), [Apple patent US11120220B2](https://patents.google.com/patent/US11120220B2/en), and a [public keyboard demo video](https://www.youtube.com/watch?v=kWvLX3wXi34). The YouTube playback did not buffer in the available browser, so frame-by-frame measurements were **not possible**. Apple provides no public per-glyph iOS 27 duration/easing specification; **164ms is an estimated continuity budget, NOT proven exact iOS timing**. A high-FPS iPhone capture side-by-side with the owner's Oppo is required to tune that claim. `git diff --check` passed; local Gradle 8.14.4 distribution download unavailable. Exact-SHA CI, signed Obtainium and real Oppo confirmation should be added below only once verified.

## 21 November 2026 — superseding LOCAL-ONLY renderer repair, not published

Owner device feedback from installed **v1.0.202** supersedes earlier source optimism: predictions felt slower, new animation effectively absent, and the previous presentation was preferable. Source audit of the old implementation found new letters beginning at zero opacity and a different fallback at 10% alpha; on-screen lateness was plausible even though decoder latency was not measured. The old renderer also issued up to `old + new` canvas glyph draws per slot/frame, including outgoing ghosts. No full Oppo trace exists, so do not present those mechanisms as a proven measured root cause.

On fresh local `main` `c343761a`, three motion components were rewritten from the ground up without modifying BR Brain, the native decoder, ranker, suggestion tap tags or confidence-pill eligibility:

- `SuggestionGlyphMotion`: simple linear prefix/suffix retention, up to 18 printable ASCII characters and 8dp movement, 112ms settle; no LCS, removed-letter tracks, hidden letters or delay. Unrelated/identical/long/complex words immediately use normal Android TextView.
- `MorphingSuggestionTextView`: fully opaque new word from **frame zero**. Only its currently valid word is drawn, once per glyph, with subtle retained-position movement and near-full-scale insertion. Its TextView still owns the real text, accessibility and hitbox. Positions and paint are prepared once per suggestion change, never in the animation callback.
- `SuggestionWordMorph`: one finite clock for current ink and the existing center confidence capsule, safe cancellation/retarget from the currently visible frame, no span opacity fallback. Motion is disabled by existing Android accessibility/reduced-motion and non-iOS policy.
- `SuggestionGlyphMotionTest`: first-frame visibility, expansion/contraction, interior change, retarget, unrelated-native fallback, RTL/non-ASCII rejection and finite duration.

This is intentionally a **safety-first approximation**, not exact iOS frame-data reproduction. Real typography (kerning, partial spans), fast all-slot replacement, clip boundaries, confidence pill, RTL/Emoji and p95 frame budget still require actual Android/Oppo QA. The underlying `SuggestionStripView.setSuggestions` still rebuilds its layout each update; that larger legacy layout cost has not been benchmarked or rewritten during this narrowly scoped visual change. `git diff --check` passed. Local Gradle could not download 8.14.4 (network unreachable), so **no Kotlin compile, Android test, signed APK or device verification is claimed**. OWNER SAYS NO GITHUB UPLOAD, so do not dispatch CI or publish this local state without a later explicit go.

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
