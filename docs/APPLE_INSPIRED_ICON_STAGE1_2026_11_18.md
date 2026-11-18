# Original Apple-inspired icon family — Stage 1 (2026-11-18)

## Owner decision / baseline

The owner asks for the closest practical **consistent iOS-like visual language on every icon and symbol**, including keys, Emoji categories, Toolbar, Clipboard, Settings and bottom controls. This is a full staged visual audit, **not** a claim to distribute Apple SF Symbols, Apple artwork, an Apple font, or to have measured one-to-one screenshot parity on their Oppo Reno 10 5G. Maintain the existing app package, original signer, default offline behavior, theme tint, user-selected custom icons and approved Frosted Glass/Emoji–mic footer geometry. Stronger iPad-style contrast is **keycap-only**, not an extra host-glass tint.

Last proven signed source before this batch: `d6c310a691681133824d5045f842cd06be7dd74d`, v1.0.117, debug `44263321481` SUCCESS, original signer `44263321433` SUCCESS, APK `https://github.com/Batmancpu/brrrBoard/releases/download/v1.0.117/brrBoard.apk`.

## Precise Stage 1 bug and treatment

`Defaults.PREF_ICON_STYLE` is `STYLE_ROUNDED`, but `KeyboardIconsSet.keyboardIconsRounded` still mapped five active default symbols to legacy, visually unrelated assets (Numpad, Incognito, Split, ZWNJ and ZWJ). Six more rounded action assets still embedded large 960-unit original Material paths, despite their filenames. The Search/Expand pair used stroke weights `2.35`/`2`, creating an adjacent optical mismatch. The app already has original rounded Emoji category icons and the owner-approved `ic_board_emoji`, `ic_board_mic`, `ic_board_abc`, and Toolbar control, so rewriting these would needlessly risk layout regressions.

Source corrections:

| User-visible role | Resource and intent |
| --- | --- |
| Numeric pad | New `sym_keyboard_numpad_key_rounded.xml`, 32dp intrinsic, rounded 123 outlines |
| Private/Incognito | New `sym_keyboard_incognito_rounded.xml`, thin hat/glasses contour, 24dp |
| Split keyboard | New `ic_ime_switcher_rounded.xml`, two outlined halves, 24dp |
| ZWNJ / ZWJ | New `sym_keyboard_zwnj_rounded.xml`, `sym_keyboard_zwj_rounded.xml`, outward/inward arrows at 26dp |
| Go / Send / Done / Tab | Original 24-unit round-cap vectors replacing Material-filled paths; Done keeps 30dp intrinsic |
| Numeric-layout Space / disabled voice | Original round-cap vectors replacing Material paths; keep their 38×34dp / 24dp intrinsic bounds |
| Emoji Expand / Collapse | 24-unit paths retained, optically centered and thickened to 2.25 stroke to suit adjacent Search; no panel state changes |

`KeyboardIconsSet.kt` updates **only the rounded default mappings**, preserving Holo/Material alternatives, `getAllIcons` candidate availability and `defaultIds + customIconIds(context)` precedence. `IconDefaultPreferenceTest.kt` checks the actual loaded rounded drawable resources and an explicit saved override. This does not alter Emoji paging, Search queries, bottom footer, keycap contrast, the system glass or accessibility labels/hit regions.

## Verification and gates

- Source: parse every added/changed vector XML, verify icon names resolve, Kotlin resource mapping tests in Robolectric/full CI, `git diff --check`.
- Release: independent Android debug/units gate plus original-key release-signing gate must both reach terminal SUCCESS. Note release tag, APK digest and exact source SHA after observed publication, not before.
- On-device Oppo: compare light/dark Numpad/Incognito/Split and Go/Send/Done/Tab on representative editors; verify Shift/Delete unchanged, Emoji Expand ↔ Collapse visible and tappable; Search/Expanded/Compact first row and top-gap still OPEN from older batches; verify user custom icon override survives update. Inspect at normal and enlarged font scale and at 100% screenshots against the provided iPad references.
- **No Android device render or Apple 1:1 optical acceptance is claimed from source inspection.**

## Immediate adjacent Stage 2

Replace the remaining active Material-source `_rounded` drawable paths with original matched 24-unit rounded icons: Arrow Horizontal/Left; Autocorrect; Close; D-pad Left; Redo; Select All/Select; To End; Undo; Word Left; Copy/Cut/Paste; Start/Stop One-Handed. Symmetric right/up/down variants already have matching original forms and must be audited, not blindly changed. Recheck all eleven Emoji category symbols and Settings/Clipboard active resource mappings/selected-state contrast; preserve external GIF/sticker branding, the user-approved Emoji/mic footer geometry, and actual user custom choices. Deliver another *separate* original-signed build, then physical screenshot acceptance; do not wait idle while Stage 1 CI is in progress.
