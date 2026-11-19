# brrBoard — Apple-like motion and active-only mic refinement (2026-11-19)

## Owner corrections after v1.0.146

The prior visual batch `a91367acf6cc992fe86e8e6fb52c1656df6fdc50` published
original-signed v1.0.146. Both independent CI gates finished SUCCESS:
debug `44483255465`; Obtainium original-signer `44483255544`.

The user then clarified that two-layer glyph crossfades look like faint duplicate
letters/shadows on typing and are **not** the continuous movement desired.
In addition, the user rejected always-visible Emoji/ABC and mic rings. Idle
footer controls must retain their previous native unframed appearance. Only an
actively recording or processing microphone gets a slightly larger filled,
softly red-glowing button; **no hard stroke**. Clipboard search text must be
vertically centered and its colored query ink should cast a restrained ambient
glow into the capsule, including while a query has no matches.

### New user recording analysis

`ScreenRecording_11-19-2026 13-23-04_1.mp4` is 736 × 512, ~16.94 seconds,
30fps (container advertises 508 frames). The screen shows repeated Emoji Search
query refinement from `red` to longer `reddish`/`reddish`-like values and back,
then `red heart`. A visible results row, a `No Results` state, and the search
field occupy a stable vertical band. For visual inspection, a 3fps full-frame
contact sheet and 5fps cropped emoji-row sheet were generated, and the entire
video was decoded once for frame-difference analysis. The relevant continuity
is stable geometry and a visual carry between result sets, not a repeated
whole-row dim/brighten with each keystroke.

### Changes in this refinement

- `SuggestionWordMorph.kt`: remove *all* outgoing `TextView` overlay ghosts.
  Reused identical words move between measured positions after layout;
  replaced words settle subtly as a single real glyph layer. Generation-token
  checks cancel stale posts. Real click targets, prediction policy and layout
  are unchanged. No duplicate text persists in overlay/pools.
- `EmojiSearchResultMorph.java`: capture the **visible old viewport only**
  (bounded to 400k pixels), then dissolve and shift it minimally as the current
  result appears. The snapshot is noninteractive, inaccessible and disposed
  after ~210ms or on cancellation/detach. Search pending leaves the previous
  row unchanged visually and touch-gated; identical result sets never animate.
  The `No Results` state transitions as a first-class result instead of flashing
  via temporary idle suggestions.
- `BoardFooterView.kt`, `FooterStatusRingDrawable.kt`: original Ripple/borderless
  backgrounds on Emoji/ABC and idle mic. Active mic alone gets a 21dp filled
  circle and soft radial red halo in a 48dp touch area, synchronized with the
  existing 1.6s red mic pulse. Processing intensity remains gentler; no outline.
- `clipboard_history_view.xml`: center the editable text baseline vertically
  in the unchanged 36dp Search capsule, avoiding extra vertical padding.
- `ChromaticSearchEditText.kt` and `ClipboardHistoryView.kt`: continue animating
  **glyph ink only** (not a rainbow highlight behind text) and increase the
  subtle, phase-colored text-shadow spill to ~3.8dp. A nonblank query retains
  its ink even with zero matches. Clear/exit cancels the animation.

### Safety / verification gates

1. Debug Kotlin/Java/Android unit tests and original signer both green;
   B16A deterministic seed / B16B synthetic next-word gates stay green.
2. Manual Oppo Reno 10 5G: type rapidly and verify no double-glyph shadow;
   observe repeated predictions and pills without visual jumping.
3. Emoji Search: `red`, `reddish`, backspace, `red heart`, clear, no-results,
   multiple quick keystrokes. No stale Emoji can be tapped while worker runs;
   newest result wins; no blank blink, row height change, or crash.
4. Footer: verify all idle icons unchanged in light/dark and ABC modes; only
   active dictation has filled pulsing circle and red icon, with gentle processing
   pulse; mic double tap and accessibility gesture still work.
5. Clipboard: query text baseline centered and multicolor glow subtle rather
   than a filled rectangle; works with/without matches, light/dark, keyboard
   swipe typing, focus/caret and reduced motion.

Device appearance, rendered shadows and perceived native timing remain physical
phone acceptance tests; CI cannot certify Apple's private keyboard behavior.
Keep the separate brrBoard → brrSpeak PCM feature/handoff untouched.

### Authoritative external guidance

- [Apple HIG — Motion](https://developer.apple.com/design/human-interface-guidelines/motion)
- [Apple UIKit — cross-dissolve transition](https://developer.apple.com/documentation/uikit/uiview/animationoptions/transitioncrossdissolve)
- [Android — property animation and crossfade](https://developer.android.com/develop/ui/views/animations/overview)
