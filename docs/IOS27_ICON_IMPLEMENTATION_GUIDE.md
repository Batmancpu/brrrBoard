# brrBoard · iOS 27-inspired symbol handoff

Source: `Batmancpu/brrrBoard` (`main`, **d744690cb2742c1915adbedd729433af15668c0a**). Audit date 2026-11-22.
This ZIP contains design assets and a migration map ONLY. **Nothing has been pushed or implemented.**

## What is covered

* 221 named XML icon resources examined individually; 2 identity-related icons excluded.
* 171 replacement vector drawables: independently MIT-licensed Ionicons artwork where semantics match, with custom keyboard-specific artwork for Shift, Caps Lock, 123, space, incognito, special glyphs and other exceptions.
* 15 original vectors intentionally retained when the proposed art lost meaning (for example `123`, emoji musical notes, and the incognito silhouette).
* 33 existing layer-list/ripple wrappers preserved verbatim to avoid breaking drawable dimensions/ripple states; their referenced base vectors may be refreshed or intentionally retained.
* 25 named settings-font ligatures have their own companion SVG + Android vector in `manual_vectors/`.
* 6 programmatically drawn icon surfaces mapped for manual integration, and 9 genuinely additional optional icon states.
* Two complete base appearance sets, `drawable/` and `drawable-night/`, each 219 XML resources. Ink, contrast and selected accents differ; no opaque tile or background is baked into any vector.
* 874 explicit light/dark `pressed`, `disabled` and (when semantically applicable) `selected` vector candidates, each mapped by filename in `INTERACTION_APPEARANCE_MATRIX.csv` and `INTERACTIVE_STATE_MAPPING_ALL.json`.

## Integration instructions to another AI

1. Preserve everything in `PROTECTED_APP_ICON_DO_NOT_TOUCH.txt` **unchanged**. Do not edit launcher icons in any density/format, adaptive foreground/monochrome, `ic_frostkeys_icon`, all MangoLoads/branding art, AndroidManifest launcher icon mapping, or `AnimatedBoardIcon`.
2. When future integration is authorized, use BOTH `android_drop_in/app/src/main/res/drawable/*.xml` and `drawable-night/*.xml` at their exact same Android resource paths. Dark XML is the Android night appearance; brrBoard's independent keyboard glass themes **may not follow system night mode**, and need explicit logic to choose ink and assets. This ZIP does not make any such code change.
3. Check *every* line of `ICON_INVENTORY_ALL.csv` and compare visually. `retained_existing_semantics` rows keep original geometry with monochrome light/dark recoloring only. Do NOT replace wrappers. Preserve original dimensions, dynamic tint overrides and RTL mirroring. `INTERACTION_APPEARANCE_MATRIX.csv` provides disabled/pressed/selected assets as candidates; they are NOT auto-wired. An existing `ColorFilter` or Compose tint can override XML ink and must be inspected at each call site.
4. For `settings_font_ligatures.csv`, **do not replace the bundled font file**; the current Compose settings use text ligatures. Use `manual_vectors/brr_symbol_*.xml` with appropriate `painterResource` and MaterialTheme tint only after code migration. No broken font substitutions.
5. `manual_code_drawn_icons.json` records animated Canvas/Compose drawings. Static SVGs are visual guides, not drop-in replacements for animation. Keep animations and runtime states when implementing.
6. `optional_new_icons` contains 9 optional symbols, each exported in light/dark. They are *not* applied automatically. Wire only if a real UI need exists. See `ON_OFF_PRESSED_DISABLED_STATE_PAIRS.json`: Shift, Caps Lock, microphone permission versus live recording, one-handed, incognito, autocomplete and emoji expansion **are different states/actions**, not generic fill variants.
7. **Pressed is not solely an icon-color change.** The app may also require a ripple/pressed keycap background; do not bake a rounded rectangle into a transparent icon. Disabled uses an alpha 0.38 candidate; selected uses filled silhouettes where available **only** for a real selected state; light/dark merely changes ink and contrast. Ensure accessibility labels and focus outlines are programmatic. Respect 24dp source viewport, original XML requested width/height, clipping, touch targets, Android 9 vector support, and mirrored logical arrows (never mirror physical forward/back navigation without analyzing the source).
8. Test Oppo Reno10 5G: light/dark/OG Frosted Glass/iOS Clear/iOS Tinted, true keyboard dark mode independent of Android uiMode, shift-off/on/caps-lock, Search/Go/Send/Done/Return, microphone/emoji/globe, transparency on all backgrounds, selected/unselected, pressed/disabled, glyph tint/size, Settings, accessibility/RTL, keyboard speed, and every toolbar/emoji page. **Device test remains pending.**

## iOS fidelity and licensing

As of November 2026, Apple documents iOS 27 and SF Symbols 8. The CSV provides **semantic SF Symbols search suggestions**, not Apple's copyrighted files. Artwork in this pack combines the independently designed MIT-licensed Ionicons family and original brrBoard-compatible key glyphs, creating a cohesive *iOS-influenced* Android design proposal. Apple original SF Symbols/design resources are not included; their licenses restrict redistribution and use outside Apple-platform contexts. Do not mistake matching semantics for pixel-perfect reproduction or measured iOS screenshots.

**100% pixel-identical iOS 27 fidelity is NOT verified or claimed.** Ionicons is an independently authored icon family, not Apple SF Symbols. The user-requested exact Apple look is limited by licensing, inherent visual differences, device-specific rendering, and untested implementation. This ZIP is a much more consistent, attribution-complete production-design candidate, not a factual Apple-original collection. No controlled Apple screenshot overlay or real Oppo Android render test has been completed.

Apple primary references:
- https://developer.apple.com/design/human-interface-guidelines/virtual-keyboards
- https://developer.apple.com/design/human-interface-guidelines/sf-symbols
- https://developer.apple.com/sf-symbols/
- https://developer.apple.com/design/resources/
- https://developer.apple.com/support/downloads/terms/apple-design-resources/Apple-Design-Resources-License-20230621-English.pdf

## Files

* `ICON_INVENTORY_ALL.csv`: row per source named vector icon, category, SF equivalent, path, three audit columns.
* `IMPLEMENTER_MAPPING_EXACT.json`: every old XML path, exact replacement path, drawable-wrapper dependencies, all call sites, and the precise keyboard bindings.
* `ALL_DRAWABLE_REFERENCE_SITES.json`: complete code and layout usage locations (no twelve-path display truncation).
* `RESOURCE_COVERAGE_ALL.csv`: every drawable, mipmap, and font resource—including backgrounds, PNG trademarks, and launchers—accounted for with a disposition.
* `android_drop_in/`: exact filenames for an implementer (no app identity files).
* `android_drop_in/.../drawable-night/`: same resource names in optimized dark contrast.
* `editable_svg/`: Ionicons or custom editable vectors matching the Android art.
* `editable_svg_selected/`: editable filled/selected glyph proposals for new vectors where selection is a real state. Retained original glyphs use tinted Android XML instead.
* `INTERACTION_APPEARANCE_MATRIX.csv` and `INTERACTIVE_STATE_MAPPING_ALL.json`: every existing non-brand resource in every appearance, with explicit pressed/disabled/selected availability and exact asset paths.
* `ON_OFF_PRESSED_DISABLED_STATE_PAIRS.json`: named control/event pairs, including on/off and three-way Caps Lock; not all controls have an ON state.
* `settings_font_ligatures.csv` and `manual_vectors/`: Compose ligatures; manual integration.
* `manual_code_drawn_icons.json`: animation/canvas cases, mapped to code.
* `optional_new_icons/`: additional missing-purpose symbols, not present in source.
* `OPTIONAL_NEW_ICONS_PURPOSES.md`: activation state, intended location, and ambiguity warnings for each new icon.
* `PROTECTED_APP_ICON_DO_NOT_TOUCH.txt`: strict exclusion list.
* `AUDIT_THREE_PASSES.json`: frozen source and coverage counts.
* `CONTACT_SHEET.html`: visual inspection at 36px and 60px.
* `previews/`: rendered before/after and full proposed icon catalogs.
* `QUALITY_REVIEW_NOT_PIXEL_IDENTICAL.md`: issues discovered and corrected by a second visual review.
* `LICENSE_IONIC_IONICONS.txt`, `IONIC_SOURCE_ATTRIBUTION.json`: the MIT license, upstream commit and precise SVG source names for professional icon art.

## Notes

No word, number, mathematical character, *actual Unicode emoji*, accessibility label, keycap background, sheet background, or third-party branded logo should be replaced by a monochrome icon. Unicode emoji themselves are rendered by system fonts and are distinct from the emoji **category buttons** included here. Logo bitmap PNGs and the settings Google icon font are left unchanged.

Retained and derivative brrBoard art follows the original project's relevant notices/licenses (including GPL, Apache and cited upstream sources). Independent Ionicons artwork remains MIT-licensed; its full license and source manifest are bundled. The ZIP does not relicense Ionic's images as GPL or claim those are Apple originals. Review the parent repository's LICENSE/NOTICE and the included MIT text before redistribution.
