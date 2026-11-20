# brrBoard — Premium Settings overhaul

Owner-approved implementation notes, 20 November 2026. The MangoLoads launcher, About art, original Android package name, signing lineage, and actual IME renderer are deliberately preserved.

## Design principle

Native, predictable, quietly premium. Settings use a dedicated pearl/sapphire palette in light mode and deep midnight/sapphire in dark mode. Material Symbols Rounded glyphs use an outlined axis for restrained, consistent iconography. The MangoLoads brand mark remains full-color where a brand mark is called for. No extra logo variants, incessant animations, glass effects on every row, or animation of the launcher icon.

`SettingsBrandTheme.kt` wraps **SettingsActivity**, including setup; it does not change `latin/utils/Theme.kt`, keyboard drawable colors, user-selected themes, wallpaper, or IME theme computation. Screen transitions use system animation scale and a measured 220 ms duration. The search header uses a 14 dp Haze pane only on Android 12+, with a solid fallback on older Android versions.

## Home information architecture

| Group | Row | Destination | Underlying source |
| --- | --- | --- | --- |
| Make it yours | Appearance & glass | `appearance` | Existing Appearance and Frosted Glass prefs |
| | Languages & layouts | `languages` | Existing language/subtype system |
| | Keys & layout | `keys_layout` | Existing Preferences and advanced input-action prefs |
| | Toolbar | `toolbar` | Existing Toolbar prefs |
| Typing | Correction & suggestions | `text_correction` | Existing correction, prediction and spacing prefs |
| | Swipe typing | `gesture_typing` | Existing JNI-gated gesture prefs |
| | Dictionary & learning | `dictionaries` | Existing dictionaries + brrBrain dashboard link |
| Everyday tools | Sound & haptics | `sound_feedback` | Existing sound/vibration prefs |
| | Clipboard | `clipboard` | Existing clipboard, screenshot and retention prefs |
| | Offline voice | `voice` | Actual local voice-model availability and failure status |
| Privacy & data | Privacy & protected apps | `privacy` | Existing secure-app/incognito/permission prefs |
| | Backup & restore | `backup` | Existing backup UI |
| More | Advanced controls | `advanced` | Remaining compatibility/experimental/debug prefs |
| | About brrBoard | `about` | Original full-color brand, version, licenses, privacy, setup help |

The old `preferences` route remains valid to protect existing deep links; individual `Setting` instances, pref keys and persistence flows are reused. No migration of saved user settings. Advanced no longer duplicates privacy, backups or input gesture toggles that now live in focused pages. Appearance has one Adjust Glass entry; the highlight style is a section inside Glass Studio. The previous highlight shortcut key is retained for compatibility and search.

Setup completion comes **only** from Android `UncachedInputMethodManagerUtils.isThisImeEnabled` and `isThisImeCurrent`. When both true, the home screen shows no setup completion card at all. When either false, a compact **Enable brrBoard** link to the existing setup wizard appears. Optional gesture, suggestions or dictionaries are never a setup prerequisite. About offers a setup wizard entry for users who explicitly want to repeat it.

## Search architecture

- Purely offline; no indexing of typed content, no telemetry, no external request.
- Rank exact title, title prefix, word prefix, description, category, then safe topic aliases.
- Return a readable breadcrumb and setting description, and navigate to its actual owner page; do not put a live toggle in an unrelated screen.
- Include searchable virtual routes for voice, language/layouts, and the brrBrain dashboard (which are otherwise not leaf preference keys).
- Preserve conditional visibility rules on destination screens. Parent/dependent preferences must continue to use the same `Settings.PREF_*` keys.
- An empty query only displays the current page; no-results state provides practical alternative keywords.

## Glass Studio: scope and invariants

`FrostedGlassAdjustDialog.kt` continues to use `FrostedSettingsSnapshot(light, dark, dustEnabled)` staged in Compose state. Slider drags only set `KeyboardTheme.livePreviewValues` and ask the IME for an already-throttled redraw. Existing light/dark profile values, slider bounds, optional dust controls, and four reflection styles are preserved.

- Clarity: blur radius, key and background translucency.
- Color & contrast: blend, saturation, special/alphabet vibrance.
- Surface detail: optional dust controls only on capable devices.
- Light & reflection: four curated highlight treatments, live IME text field.
- Optional wallpaper image preview: decode with bounds/sample size for an image no larger than ~1440 px on its longest edge, with memory-friendly RGB_565. Selection is staged; persistence happens only after Save, not while previewing.
- Save writes profile values, reflection style, and chosen wallpaper preview URI; Cancel restores the one reflection preference used by the live IME. No writeback of unchanged sliders on Cancel. A no-op open/close does not trigger a hard IME reset.
- Footer actions remain outside the slider scroll area and are at least 48 dp tall; sheet respects IME/system insets. Rendering remains opaque below Android 12.

## Acceptance before release

1. Compile debug and signed original-key Obtainium workflows from the **same main commit SHA**. Reject stale green results from unrelated commits.
2. Confirm installed package `com.mangoloads.board` (the Kotlin source namespace is not the app ID), unchanged signing certificate, release asset `brrBoard.apk`, and correct latest-release SHA.
3. Verify Settings startup both light/dark, day/night transition, edge-to-edge status/navigation bars, 200% font scale, gesture navigation, rotation, and Android older than 12.
4. Verify installed+selected keyboard shows no finished setup card; disabled/not-default states present a working setup link; About still links to setup.
5. Visit every home row and inspect all settings previously found in Appearance, Preferences, Advanced, Toolbar, Text Correction, and Dictionary. Check dependent rows change when toggled.
6. Test searches `glass`, `haptic`, `clipboard`, `incognito`, `brrBrain`, `dictation`, `height` and a no-results query. Tap results and validate destination context and back-navigation.
7. In Glass Studio test light/dark, styles, each slider, multiple wallpaper sizes, Cancel, Save, Reset current profile, keyboard remained onscreen, and no preference corruption on reopen.
8. Verify secure-app suppression, incognito, clipboard screenshot storage permission, backup/restore, offline voice missing-model truthfulness, and swipe typing unchanged.
9. Do not describe an Oppo/device visual or performance check as passed until tested on that device. Signed CI is a build validation, not a substitute for human visual acceptance.

## Rollback

Revert the dedicated Settings commit on main and rebuild with the **same** signed-release workflow. Nothing here migrates or deletes existing user prefs or rewrites the actual IME service; settings data is preserved. Coordinate with the owner before publishing rollback, and never undo other conversations' Emoji/panel-header safety changes.
