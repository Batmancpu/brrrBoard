# brrBoard B07V — Apple-inspired icon surface audit (2026-11-17)

## 2026-11-17 CI and visual evidence checkpoint

Original source `ab21a07f` failed debug `44034065255` and prod `44034065241` due Kotlin `prefs(context)` instead of the imported `Context.prefs()` extension. Fixed app+Robolectric invocation in `fbbc6b1`; [debug 44034657924](https://github.com/Batmancpu/brrrBoard/actions/runs/44034657924) and [production 44034657851](https://github.com/Batmancpu/brrrBoard/actions/runs/44034657851) succeeded with original signer; [signed v1.0.75](https://github.com/Batmancpu/brrrBoard/releases/download/v1.0.75/brrBoard.apk) (SHA-256 `916c16a91f870d54b7b8aba0ada3e445eaf4399baf50289853bbb0d5d7d11616`). [Before vs after seven glyph vectors](B07V_BEFORE_AFTER.svg) shows XML-path-derived static art from v1.0.73 vs v1.0.75; **not an Android screen render or real Oppo visual validation**. UI geometry, contrast, Tap/hold/TalkBack behavior and exact Apple-inspired similarity remain owner checks.


Status: **source and full Android CI/original-signer release verified as v1.0.75; real Oppo visual acceptance pending**. This document is not a claim of exact Apple icon or Liquid Glass parity.

## Rights and sources

- Reviewed the current official upstream licenses: [Tabler Icons](https://github.com/tabler/tabler-icons/blob/main/LICENSE) (MIT, Paweł Kuna, 2020–2026), [Phosphor](https://github.com/phosphor-icons/core/blob/main/LICENSE) (MIT, Phosphor Icons, 2023), [Lucide](https://github.com/lucide-icons/lucide/blob/main/LICENSE) (ISC, Lucide contributors, 2026 and Feather-derived acknowledgments).
- **No third-party SVGs or Apple SF Symbols were imported in B07V.** All new/replacement artwork below was authored specifically as original Android VectorDrawables with a GPL-3.0-only notice, using lawful general design principles (24-unit optical grid, rounded stroke endpoints, coherent line weight). The earlier Apache/Material vectors stay in Git history, and any unused compatibility assets remain installed for saved user custom choices.
- Keep Apple branding, actual SF Symbols files, and third-party extracted Apple assets out of APKs.

## Common visible system

- 24-unit coordinate viewBox for newly authored vectors; 1.65–1.7-unit strokes, round caps and joins, transparent interior, reduced heavy filled silhouette. Original intrinsic drawable dimensions are preserved for clickable Settings/toolbar widgets.
- Settings graphics use themed `@color/foreground`; keyboard/clipboard vector glyphs retain the existing white base tintable by `Colors`/`ImageView` color filters. Key tint role behavior from B04/B07 is untouched.
- Existing `ic_board_emoji`, `ic_board_mic`, `ic_board_toolbar` are already round-line originals with accepted outer sizes; deliberately preserved. Do not enlarge or move the owner's accepted Emoji/mic tap zones.
- Branded app logos remain trademark-specific exceptions; do not redesign GitHub/Discord/Telegram brand marks as if they were generic Apple symbols.

## Surface inventory and B07V modifications

| Surface | Active rendering and source treatment | State |
| --- | --- | --- |
| Ordinary keys / symbol/number / popups / Search, Go, Enter, Shift, Delete | `KeyboardIconsSet` uses `STYLE_ROUNDED` defaults, not legacy Holo. Preserves custom-icon name/ID preference overrides and previous icon action bindings. | Default mapping unified; real editor/tint/caps state acceptance pending |
| Emoji category navigation | All 11 tab glyph mappings across `themes-default-base.xml`, `themes-rounded-base.xml`, `themes-holo_base.xml`, `themes-lxx-base.xml` now point to the one rounded family. New original rounded animal/paw icon fills prior missing category slot; Recents runtime uses existing sparkle Smart glyph. | XML/source mapped; category touch accessibility retained |
| Settings / onboarding / about navigation | Thirteen `ic_settings_*` assets redrawn as one round-line family while keeping each intrinsic size. `ic_settings_about_github` retained as brand/logo exception. | XML/source changed, visual QA pending |
| Clipboard pin, Clear, delete/bin and keyboard Settings icon | Four old heavy Material-filled resources replaced with original line vectors (`ic_clipboard_pin_rounded`, `sym_keyboard_clear_clipboard_rounded`, `ic_bin_rounded`, `sym_keyboard_settings_rounded`). | Source changed, touch/event semantics unchanged |
| Emoji and microphone footer; Toolbar state | Previously tuned `ic_board_*` forms untouched, 1.65 rounded strokes. | Preserved; real device accepted sizes per older user report, new full icon QA pending |
| GIF/sticker provider marks, GitHub logo, imported custom icon selections | Preserve legal service-brand marks or explicit owner custom choices; do not pretend one visual system owns external identities. | Intentional exceptions |
| Legacy unused Holo and Material vector resources | Kept for compatibility with older stored custom-icon names and external theme resources. Runtime defaults no longer select them for standard typing. | Not removed; full asset-by-asset replacement explicitly **not** claimed |

The repository contains approximately 202 `ic_*` and `sym_keyboard_*` drawable files including old alternatives. A file count is **not** the number of icons simultaneously visible. The active runtime mappings and theme attributes listed above determine current visual coverage; retained assets may still be selected by users via saved customization. Some special popups/accessibility contrast variants require real-device scrutiny.

## Preference/migration protection

`Defaults.PREF_ICON_STYLE` now uses `STYLE_ROUNDED`; the fresh-install custom-icon JSON is `{}` so legacy Holo/Material overrides cannot silently defeat the rounded default. `customIconIds(context)` now actually decodes the user's stored custom-icon preferences (rather than the baked-in JSON constant); existing selections are not destroyed. `IconDefaultPreferenceTest` verifies the default and saved override. Existing signing, package, version, themes and user data are unaffected.

## Verification and remaining risk

- Static checks: all modified resource XML files should parse, all drawable references must exist, `git diff --check`, `IconDefaultPreferenceTest` and full Android CI.
- Manual after final APK: light/dark keyboard keys, Search/Enter/tinted-action semantics, Shift caps and Delete, Emoji tabs, clipboard pin and clear and Undo, Settings nav icons, toolbar/menu popups, TalkBack labels and tap targets. Image evidence is still needed to judge optical size and Apple-inspired likeness; no screenshot or actual Oppo rendering is claimed here.
- Explicitly unverified: precise pixel/icon stroke weight vs iOS references, 200+ legacy alternative assets, all custom icon permutations, actual responsive density and 1.0/1.3 font scale. Treat B07V physical acceptance as pending even if automated gate is green.
