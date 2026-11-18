# Emoji Expanded Browse — 2026-11-18

## Evidence and intent

Owner's 27-second Oppo recording showed compact Emoji Browse with a clipped
Smileys row, then Search with the normal typing keyboard, then unintended
tall multi-row Browse after tapping Search Back. Owner wants the useful tall
state made intentional with a button immediately right of Emoji Search.

v1.0.109 (`f01e20a3`) was the **separate** first-row alignment attempt. Its
actual Oppo acceptance is pending. Never conflate these two releases.

## Isolated implementation

- `EmojiPalettesView` has separate `mSearchMode` and `mExpandedMode` flags.
  Compact browse is the default on every new Emoji session.
- Search shows an Expand control. Tap to close Search and deliberately open
  taller Browse; button becomes Collapse. Expanded Browse keeps the same
  48dp Search bar so users can collapse or reopen Search. Back in Search
  returns to the previously chosen browse size; Back in Expanded collapses.
- Expanded Browse requests 160dp more panel body including its Search bar,
  clamped to the **current** available IME window. Search retains its separate
  typing+results measurement and compact Browse borrows neither height.
- Fixed category tabs, paged/vertical Emoji grid, Search results, fixed lower
  Search/Space/Delete row and approved moving lower glass are not reshaped.
  No changes to host wallpaper/background blur.

## Required gates and real-device checks

1. Debug Android build/unit tests and original-signed release CI.
2. On Oppo: compact first open; Search→Back stays compact; Search→Expand shows
   extra complete emoji rows; scroll both axes; Search again→Back preserves
   Expanded; Collapse restores compact; keyboard Back collapses.
3. Switch categories, ABC, reopen Emoji, landscape and short windows; check
   category strip/footer alignment, accessible controls and cursor behavior.

Separate next request: Emoji-only dark/light tonal adjustment with an
Appearance switch. It must alter internal blurred Emoji source pixels only,
not the already-blurred host backdrop. Do not include in this release.
