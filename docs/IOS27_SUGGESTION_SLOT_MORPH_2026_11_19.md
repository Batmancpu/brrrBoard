# Suggestion-strip slot continuity — owner video correction (2026-11-19)

## Scope and observed problem

The owner supplied two 30-fps screen recordings: a 37.57-second YouTube
comparison of iOS keyboards and a 16.37-second brrBoard capture. The reference
changes suggestion letters *inside stable left/center/right positions*. The
brrBoard capture shows neighbouring words seeming to collide, with duplicate
or shadow-like glyphs during changes such as `suggested` / `suggestion` /
`suggestions`. Do **not** copy or commit private screen-recording contents.

The root cause was in `SuggestionWordMorph.kt`: it matched identical words
across the entire strip, set `TextView.translationX/Y` from a previous slot,
and drove every unmatched full word via `ViewPropertyAnimator`. Meanwhile
`SuggestionStripLayoutHelper` immediately rebuilt a fixed-inset confidence
pill. Even though only one physical word view was intended, the words could
travel over each other, creating the perceived doubling.

## Implemented design

- The three conventional suggestion slots and their actual `TextView` tap
  targets stay fixed. The text/tag/content description switches to the correct
  new decoder suggestion **before** any animation starts. There is no cloned
  outgoing word, cross-slot translation or delayed click target.
- `SuggestionGlyphDiff` computes one changed UTF-16 range of the new word using
  `BreakIterator` character boundaries. Unchanged prefix/suffix remain opaque
  and steady. A single `CharacterStyle` affects only the changing portion of
  the latest text. Its ink progresses subtly from 74% to full opacity; the
  existing 150-ms keyboard motion curve drives it. This is an iOS-**inspired**
  reconstruction, not a claim to use or know Apple's private implementation.
- The center capsule `SuggestionConfidencePillDrawable` is drawn once, centered
  within the original full-width hitbox. Its width is content-based with a
  compact minimum and slot maximum; the same geometry is used by the static
  helper and animated frames. The original decoder's `mWillAutoCorrect` and
  center suggestion tag remain the sole activation gate. Existing accessible
  neutral-gray default/theme-tint opt-in color behavior is preserved.
- One `ValueAnimator` drives both new characters and capsule width/opacity.
  On rapid successive keystrokes it is cancelled, temporary spans are removed,
  and the latest text remains authoritative. When the keyboard detaches or
  changes surface, the cleanup path runs without retaining stale overlays.
- Punctuation, chip strip, different input-style and direction transitions
  keep their existing immediate rendering. No change to voice capture, swipe
  decoder, model training, APK ID, signing, permissions, network or Big Numpad.

## Files / regression coverage

`SuggestionWordMorph.kt`, `SuggestionGlyphDiff.kt`,
`SuggestionConfidencePillDrawable.kt`, `SuggestionStripView.kt`, and
`SuggestionStripLayoutHelper.java`; tests:
`SuggestionGlyphDiffTest.kt` and `SuggestionConfidencePillGeometryTest.kt`.

Review cases: expansion, same-word no animation, same-suffix replacement,
word deletion, Unicode emoji surrogate safety, pill compact-width clipping,
confidence on/off, and interruptions faster than 150 ms.

## Verification and release policy

Base commit `1291a755536812a8759f99572c201f5b6572d23c` includes the *separate*
brrSpeak PCM sender, whose debug `44488658761` and original-signed release
`44488658707` are terminal SUCCESS. This motion batch must itself pass new
exact-commit debug and original-signed Obtainium CI before a release may be
claimed. Inspect the updated `main` with a fast-forward lease to preserve
concurrent work. Owner phone review remains necessary for optical 30-fps
acceptance, as CI cannot establish iOS visual parity or OEM rendering.

Physical Oppo acceptance checklist:

1. Type `sugg`, `suggest`, `suggested`, `suggestion` quickly, and watch all three
   cells; no left/right word travels over another, duplicates, or full-word
   opacity flashes. Verify tap chooses the word visible at that instant.
2. Backspace rapidly and type again; animation interruption must not make the
   strip vanish, retain an old clickable tag or show a second text layer.
3. Trigger decoder-approved autocorrect: neutral-gray capsule (or chosen tint)
   appears in the center, changes width with its new content and disappears
   smoothly; no purple default or hard outline.
4. Check Emoji Search, Clipboard Search, Toolbar, active microphone, Big
   Numpad and RTL layouts for regressions; these controls are not redesigned
   in this narrowly scoped motion batch.

All earlier owner-requested panel/mic visual improvements are separate work;
this document does not mark their physical acceptance complete.
