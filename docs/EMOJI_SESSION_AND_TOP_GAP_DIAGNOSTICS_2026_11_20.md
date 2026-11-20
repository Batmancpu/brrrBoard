# Emoji session navigation and unresolved Oppo top-gap — implementation ledger

**2026-11-20; owner GO.** This is a focused implementation on the verified live
`main` baseline `7de71e30275b0ea7821f0be52356b55cd11550db`, which includes
the independently implemented premium Settings overhaul and voice **plan-only**
handoffs. The owner still sees the blank band above Emoji categories on Oppo
Reno 10 5G. Passing Android unit CI must not be reported as fixing that band.

## Source-confirmed failure

`EmojiCategory.initialize()` previously read the last category and a single
global page from preferences. Every category/page change wrote preferences.
`EmojiPalettesView.startEmojiPalettes()` then explicitly selected that previous
category; `EmojiCategoryPagerAdapter` also retained per-category scroll rows.
`stopEmojiPalettes()` never ended navigation state. A different app/editor thus
reopened at an unrelated editor's prior position, and a detached RecyclerView
could repopulate cleared rows after a session boundary.

## Batch A — session-scoped navigation (source candidate)

- Category and page selection now stay in RAM; old navigation preference keys
  remain untouched/inert. The `SharedPreferences` object itself remains used
  for the actual Recent Emoji keyboard, which is **not cleared or migrated**.
- The initial and new-session category is Recents when it has entries or
  eligible Smart picks; with neither available, Smileys is a usable fallback.
  Incognito deliberately starts at Smileys so an unrelated editor's Recents
  are not revealed in a protected field. Contextual Smart picks remain
  eligibility-gated and ephemeral.
- `LatinIME.onStartInputViewInternal()` and `onWindowHidden()` signal new
  Emoji sessions through `KeyboardSwitcher`. ABC↔Emoji toggles **inside** one
  editor do not signal a new session; they retain the current category and row.
- The pager increments a navigation generation on each reset. Old ViewHolders,
  scroll callbacks and delayed anchor correction must ignore previous-gen rows.
- New tests cover the private fallback, Recents/Smart selection, stale pager
  callbacks and same-session reopening.

## Batch B — observe the actual top band before changing geometry

Previous source patches hid the old outside strip, moved category icons into
the pager shell and tried nominal, typing-viewport and complete native-frame
heights. The owner **still sees the band**; its actual layer is undetermined.
`InputView.reportNextEmojiGeometry()` now samples **once on the next pre-draw**
of Emoji Browse, after the layout transition, with a line under tag
`BrrImeBounds`. Only visible view rectangles (screen x/y, width/height) or the
words `hidden`/`absent` are emitted for `decor`, system `inputArea`, `input`,
`frame`, external `strip`, `wrapper`, `emoji`, `shell`, `categories`, and
`footer`. **No typed text, editor/package identity, clipboard data, audio,
model content, Recents contents or other private data enters this probe.**
It does no synchronous window reconfiguration or frame-by-frame tracing.

The rectangles distinguish:

1. Window/input-area top above the keyboard frame: OS/container sizing or
   bottom-gravity issue (a likely ColorOS path).
2. Frame top above wrapper/Emoji top: stale outer header, frame padding or
   child measurement.
3. Emoji top above shell/categories: internal Search/Smart/geometry offset.
4. All tops aligned but host app still displays a blank band: host resize,
   `onComputeInsets()` or background/blur ownership mismatch.

The **next** corrective geometry batch must use this Oppo evidence to patch
the owning level, not add a guessed 36/48/100dp spacer. Verify compact,
expanded, Search, one-handed, portrait/landscape, keyboard hide/reopen and
Emoji↔ABC. The installed original-signed build can be examined with a filtered
ADB logcat line `adb logcat -d -s BrrImeBounds:I`; no root or data deletion is
needed. If ADB is unavailable, keep physical geometry status OPEN and request
an ordinary non-private screenshot rather than inventing a measured result.

## Verification gates and rollback

- `git diff --check`, Java session policy tests, Android compile and full unit
  suite on the exact published SHA; record actual terminal debug CI result.
- Original-signed Obtainium workflow must target that **same** SHA and pass
  pinned signer, package-ID and asset checks, without touching the existing
  offline model, learned data, Swipe, CLDR, mic or incognito animation.
- On Oppo: (1) pick People/category row 3, tap ABC→Emoji in the same field and
  confirm row/category persists; (2) close keyboard/reopen or switch app/field
  and confirm Recents/Smart top; (3) confirm protected editor opens Smileys;
  (4) check old top band and collect only the geometry line if possible.
- Source and CI success are **not** device acceptance for the top-gap. No
  proactive IME insets/blur/height mutation is approved by this diagnostic
  checkpoint until evidence identifies the actual layer.

**Status at authorship:** source candidate; CI, signed release and physical
Oppo acceptance are pending. Update this ledger with exact SHA, workflow IDs,
release target and measured device findings after the batch.
