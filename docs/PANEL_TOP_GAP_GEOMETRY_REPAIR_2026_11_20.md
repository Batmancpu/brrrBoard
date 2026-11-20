# Panel top-gap geometry follow-up — 20 November 2026

## Scope and evidence

The owner's Oppo Reno10 recordings showed an empty-looking band **above the Emoji category icons** in compact Browse; older recordings separately showed a clipped first Emoji row **below** the icons. These are distinct coordinate problems. Past versions moved categories into `emoji_grid_shell`, moved the 70dp Smart row below them, removed a duplicated 36dp strip and a decorative 8dp list inset, and aligned compact height using the previous native typing frame. None of the historical source fixes proves current on-device elimination of the band: there is still no complete window-to-tab layout trace from the installed release.

This narrow repair starts from actual GitHub `main` `ff440e54f33aa1377c537babe18812e9256f1c35`, which includes the signed v176 slide safeguards and a newer, independent MangoLoads identity source commit. An older local `main` is divergent and contains user-owned unstaged documentation; it was left intact, and this code is prepared in a separate detached clean worktree based on the live `main` commit, with **no additional branch**.

## Source fixes

1. `RoundedKeyboardFrameView.onMeasure`: exclusive Emoji, Clipboard, Klipy, AI Tools and Toolbar panels all own their own header. Suppress the old suggestion-strip/persistent-Emoji-row/divider *before* `LinearLayout` measures preceding siblings for any visible exclusive panel; preserve all three slots for ordinary alphabet/symbol typing. This closes the same one-pass duplicate-header exposure that Emoji already guarded against, for the other panel transitions too.
2. `ClipboardHistoryView.onMeasure`: adopt a saved typing viewport only when the width matches the **current** measure spec. A historical same-width shortcut was absent here, so rotating or changing one-handed width could borrow the wrong native typing height while Search expands. This changes only the bounded Search sizing fallback, not clipboard contents or glass.
3. `EmojiPalettesView.attachFixedCategoryStrip` and `attachSmartRowBelowCategories`: when called again inside the *same* shell, update the real category height and Smart overlay top margin if resource dimensions changed, without adding duplicate children or a second strip. The fixed header still belongs at `Gravity.TOP`, and the Smart overlay still begins after it.

Source regression tests extend the actual parent `RoundedKeyboardFrameEmojiHeaderTest` for Clipboard/Toolbar/Klipy/AI slots and `EmojiBrowseHeaderMountTest` for changed resource heights without reparent duplication. Existing `KeyboardViewportHeightMemoryTest`, `SharedPanelGeometryTest`, Clipboard Search and Emoji composition tests remain relevant.

## Deliberate exclusions and remaining uncertainty

- **Do not** guess a negative margin, add an arbitrary panel-height offset, move fixed categories away from their pager shell, remove optional Smart results, or recolor the host/Emoji glass to mask a measured gap.
- No changes to input gestures, pinned/provenance-safe clipboard deletion, secure-app bypass/slide rail, gold incognito, ruby recording mic, native dictionary/swipe, Emoji CLDR corpus, offline Parakeet, encrypted learned data, app ID, original keystore, or MangoLoads identity.
- Other audit candidates are NOT confirmed bugs: ColorOS IME window/`InputView` mismatched top, too-short Emoji body after `availableHeight` clamping, RecyclerView saved row offset, intentional Clipboard 54dp vs 46dp header breathing, Toolbar 50dp vs 46dp inset, Klipy 6dp top inset, and AI Tools' uncapped requested height. Do not destabilize those surfaces without a measured reproduction.
- A full diagnostic trace should contain **only** window/input/frame/wrapper/Emoji/shell/tab/first-item pixel rectangles, visibility flags, layout mode/width and navigation inset. No editor text, images, clipboard, model input, package names or recording. Compare first Emoji entry and repeated return from Search/Expanded, compact/landscape/one-handed, and persistent row on/off. The precise source of the *remaining* Oppo blank band is still unknown until that trace.

## Gates

1. `git diff --check`, existing Python synthetic checks, and targeted Android/Robolectric unit tests; full Android compile/test in debug GitHub Actions.
2. Fresh exact app-source SHA on `main` only, and independent **original-signed** Obtainium release workflow terminal success. Verify release tag, asset, package, original certificate and SHA256 before advising install.
3. Separately, actual Oppo screenshot/geometry acceptance: categories flush with *the keyboard's top*, first emoji row fully visible, natural Glass top fade, Search/Expanded/ABC near-instant, Clipboard/Toolbar/header heights stable. CI can never establish this visual gate.

Status: source candidate until a current source SHA, both workflows and phone results are recorded; do not claim the physical band is fixed based on these tests alone.
