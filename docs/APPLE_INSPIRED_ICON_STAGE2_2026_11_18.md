# Original Apple-inspired icon family — Stage 2 (2026-11-18)

## Scope and audited source cause

The owner wants a coherent iOS-like visual language across **all active** brrBoard icons and symbols. Stage 1 `f74b90370a955f770cf054326280b73aac57b4f9` corrected five actual rounded-default legacy mappings, six action vectors and Emoji Expand/Collapse. **v1.0.118 is verified:** Android debug #213 `44266012733` SUCCESS, original signer #118 `44266012566` SUCCESS, [signed APK](https://github.com/Batmancpu/brrrBoard/releases/download/v1.0.118/brrBoard.apk), SHA256 `4f551b2852c800bbf718adfe82ee2faf4b36f8fed7176de5ecf52c4f61816a4b` (38,114,020 bytes). Stage 2 was prepared while those tests ran and is now authorized to push, because the earlier workflow completed without cancellation.

After Stage 1, exactly sixteen `KeyboardIconsSet.keyboardIconsRounded` mapped files still contained old Material-source glyphs. Further audit found direct Settings/Clipboard surfaces bypassing that mapping entirely: `Icons.kt`, `SettingsSwitch.kt`, `clipboard_history_view.xml`, Settings dictionary/subtype screens and accessory dialogs loaded unrounded `ic_edit`, `ic_plus`, `ic_close`, `ic_arrow_back`, `ic_bin`, `ic_link`, `ic_share`, `sym_keyboard_search_lxx` and related art. These are concrete resource identifiers, not guesses based on appearance.

## Source implementation

- Reauthor all **sixteen active remaining default rounded mappings** with original white/tintable 24-unit vectors, consistently rounded ends and optical balances, keeping 24dp intrinsic and action meaning: `ic_arrow_horizontal_rounded`, `ic_arrow_left_rounded`, `ic_autocorrect_rounded`, `ic_close_rounded`, `ic_dpad_left_rounded`, `ic_redo_rounded`, `ic_select_all_rounded`, `ic_select_rounded`, `ic_to_end_rounded`, `ic_undo_rounded`, `ic_word_left_rounded`, `sym_keyboard_copy_rounded`, `sym_keyboard_cut_rounded`, `sym_keyboard_paste_rounded`, `sym_keyboard_start_onehanded_rounded`, `sym_keyboard_stop_onehanded_rounded`.
- Add seven original vector resources for direct Settings/Clipboard controls: `ic_arrow_back_rounded` (RTL-auto-mirrored), `ic_edit_rounded`, `ic_plus_rounded`, `ic_chevron_down_rounded`, `ic_drag_indicator_rounded`, `ic_link_rounded`, `ic_share_rounded`.
- Redirect active direct references in `app/src/main/java/helium314/keyboard/latin/utils/Icons.kt`, `app/src/main/java/helium314/keyboard/settings/preferences/SettingsSwitch.kt`, relevant `settings/screens` and `settings/dialogs`, and `app/src/main/res/layout/clipboard_history_view.xml`. Shared `ic_board_toolbar` now supplies the Settings Toolbar row. Text/click handlers, hit areas, appearance preferences, tint by Compose/Colors and RTL back behavior remain intact.
- Add `RoundedIconSurfaceTest.kt` Robolectric inflation/intrinsic-size test for all 23 drawable resources. Stage 1 already tests actual `KeyboardIconsSet` mappings and saved custom precedence.
- The original rounded **eleven Emoji category** vectors and **thirteen Settings navigation** glyphs from prior B07V are still the default; do not wipe their identities. Existing user-selected overrides, legacy alternatives, third-party provider brands and the owner's accepted Emoji/mic footer remain intact. No Apple proprietary symbols or fonts imported.

## Verification and limits

Local: XML parse for all changed vectors, source-mapping scan confirms **zero remaining Material-credited icons in the active `keyboardIconsRounded` map**, `git diff --check`. Full Android build, resource inflation tests, original-signed release, hash, and exact source must be evidenced independently. Do not claim Android success or an APK until both workflows reach terminal SUCCESS and the release tag resolves.

Phone acceptance: inspect dark/light and normal/high font scale key/action strokes (Shift/Delete vs Go/Send/Done); Toolbar editing and one-hand symbols; Emoji category/Expand/Search continuity; Clipboard Search back/clear; Settings disclosure, check/close, add/edit/bin, drag grip, link/share. Verify visual tone and tint with an actual Oppo Reno 10 5G screenshot, no tap-target changes, custom icon override honored, and the existing top Emoji gap independently. Pixel-identical Apple/iOS parity **cannot** be established by code or JVM tests alone.

## Remaining visual acceptance / Stage 3 proposal

Stage 2 finishes the **mapped original glyph-family migration**, not the hardware comparison. Next is a screenshot-driven full light/dark *optical* pass: category active/inactive contrast, Settings list accessory sizes and color roles, keycap-only contrast, footer untouched, large text/accessibility, RTL, Emoji/Clipboard Search icons. Only modify specific remaining outliers demonstrated in on-device screenshots; maintain rights and existing app identity. If the previously reported compact Emoji top gap persists on the latest signed APK, prioritize an instrumented layout trace over further cosmetics.
