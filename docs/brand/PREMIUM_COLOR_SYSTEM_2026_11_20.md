# brrBoard × MangoLoads — Premium Color System

**Status: OWNER APPROVED GO — implemented for brand-facing visual assets and welcome accent on 2026-11-20.** Repository sources and export details are tracked in `IDENTITY_IMPLEMENTATION_LEDGER_2026_11_20.md`. Runtime Material You and keyboard theme choices remain user-controlled.

This specification supplements `GITHUB_README_PREMIUM_PLAN.md` and `IMPLEMENTATION_GO_SPEC.md`. It was added after the owner explicitly requested **premium, carefully nuanced colors throughout the redesign except the icon itself**. Preserve the approved blue-keyboard/mango/glass icon's existing blue, yellow/orange, green and transparent optics pixel-for-pixel. Never recolor, tint or grade that artwork to match the palette.

## 1. The intent: sophisticated tonal color, not primary RGB

The quality comes from a coherent relationship among hues, surface layers, darkness, contrast and sparse highlights. Color values are still represented as ordinary sRGB hex values for reproducible GitHub imagery and Android resources. Calling a shade “premium” does **not** imply it uses a special RGB or HDR color model. Avoid flashy electric primaries, generic purple/neon gradients, blue-on-blue text, muddy gray glass and excessive warm orange.

Design mood: **midnight sapphire × luminous pearl × restrained champagne warmth**. It should feel meticulously graded, tactile and calm, with enough saturation in the brand-blue highlights to feel alive. The warm accent is inspired by the existing mango but should not become a second competing logo color. We are not claiming Apple colors or using Apple's intellectual property.

## 2. Proposed palette, exact tokens

| Semantic token | Hex sRGB | Use | Contrast / caution |
|---|---|---|---|
| `midnight.950` — Obsidian Navy | `#0C1527` | Dark hero, social preview dark foundation, intro backdrop | Near-black blue, not flat `#000000` |
| `midnight.800` — Deep Atlantic | `#162A45` | Dark raised cards, layered setup/welcome surfaces | Subtle tonal lift from backdrop, not another brand blue |
| `sapphire.600` — Cut Sapphire | `#2D60C8` | Light-mode CTA/link accent and precise brand glints | 5.50:1 against `pearl.50` for normal-sized text |
| `glacier.300` — Glacial Blue | `#89ACED` | Dark-mode accent text, soft rimlight, restrained gradient end | 7.97:1 against `midnight.950` |
| `glacier.200` — Ice Mist | `#AFC5E4` | Secondary copy and finer lines on dark hero | 10.35:1 against `midnight.950` |
| `pearl.50` — Moonlit Pearl | `#F7F9FC` | Light hero, cards, banners, whitespace | Warm-neutral white with a tiny blue cast |
| `pearl.200` — Porcelain Mist | `#E8EDF5` | Light raised surfaces and section dividers | Avoid stacking many visible borders |
| `ink.900` — Graphite Ink | `#17263D` | Primary text on light custom art | 14.42:1 against `pearl.50` |
| `ink.600` — Slate Ink | `#52647F` | Secondary text on light custom art | 5.71:1 against `pearl.50` |
| `amber.400` — Champagne Mango | `#D6A366` | Tiny decorative warm glints, controlled halo/light diffusion | 8.06:1 on `midnight.950`; **only 2.14:1 on light pearl—never tiny text there** |
| `snow.100` — Snow Ink | `#F5F7FC` | Primary text on dark custom art | 17.00:1 against `midnight.950` |

These are **provisional locked design tokens**, not a claim that every rendered gradient or translucent surface passes contrast. Exact pairs were calculated for opaque sRGB surfaces; blended overlays must be remeasured in final assets. During implementation the visual reviewer may nudge values slightly but must update this one token table and propagate changes consistently.

## 3. Light, dark and elevated treatments

### Luminous Pearl (light)

- Background: a whisper gradient from `#F7F9FC` through `#EEF3FA` into a nearly invisible cool-blue corner `#E6EEFA`. Do not make white appear dirty or over-saturated.
- Text: `#17263D`; secondary `#52647F`; CTA sapphire `#2D60C8` if the component/background pair passes contrast.
- Raised surface: `#FFFFFF` / `#E8EDF5` with exceptionally soft cool shadows and minimal borders. No hard bevels, purple haze or glass-over-glass clutter.
- Brand icon: the *unaltered original PNG/alpha mark*, shown against a backdrop chosen to respect its native blue and mango color.

### Midnight Sapphire (dark)

- Background: gradient `#0C1527` → `#162A45`, with a small, low-opacity deep-sapphire ambient bloom behind—not inside or recoloring—the icon.
- Text: `#F5F7FC`; secondary `#AFC5E4`; glints `#89ACED`; reserved warm reflection `#D6A366` in tiny non-text decorative use.
- Raised surface: `#162A45`, exceptionally subtle borders around `#AFC5E4` at very low alpha **after** legibility verification.
- Keep material hierarchy visible even in OLED black environments, without resorting to fluorescent stroke effects.

### Premium glass surrounding UI

- Use optical richness sparingly: **material separation, translucency and highlights**, not permanent Gaussian blur.
- A static hero may use pre-rendered diffusion and refraction. The typing IME hot path must not incur a shader, huge blur or per-frame bitmap allocation for branding.
- For the welcome screen, any animated color/opacity should follow the existing bounded motion storyboard; avoid always-running shimmer loops, pulsing rainbow backgrounds or GPU-demanding bloom.
- Favor 1–2 cool tonal gradients total, not a separate gradient on every card/button/badge.

## 4. Where the palette applies under the approved `Go`

| Product surface | Proposed use | Explicit non-goal |
|---|---|---|
| GitHub README hero light/dark | Full Luminous Pearl/Midnight Sapphire grading; original icon untouched, typography + minimal accent | Cannot recolor GitHub's surrounding native page or inject custom CSS |
| Repository social preview (1280×640) | Dark Midnight Sapphire as default, with optional small pearl inset and restrained mango-glint accent | No fake screenshot, fake Apple logo or giant icon-only stretch |
| Fastlane feature graphic (1024×500) | Matching composition adjusted for storefront aspect/readability | Not a copy of GitHub's distinct social image |
| README diagrams/feature cards | Same color tokens, real app captures, minimal borders and no noisy background | Do not recolor actual screenshots; preserve source UI truth |
| First-run welcome/setup wizard | Subtle surrounding premium background and calm accent on brand-specific surfaces | Preserve buttons, accessibility, Material/system colors, setup timing and functionality |
| Settings header and About brand treatment | Tasteful restrained tint/surface if existing layout and dynamic theme allow; full icon appears with **no tint** | Do not paint every settings row, controls or About icon blue |
| Release graphics, docs banners, marketing metadata | Use same palette, typography, margins and icon treatment | No fabricated changelog or launch promises |
| Keyboard keys, active typing themes, functional glyphs | Retain the user's three actual themes and the Android/user dynamic-color logic; verify only for visual brand harmony | **No blanket brand-palette replacement of keycap colors**, no change to mic/clipboard/emoji meanings |

If a custom palette conflicts with Material You, contrast, OLED readability or a user-selected theme, the **functional/system semantic colors win**. Branding is a supporting layer, not a forced theme override. Avoid tinting accessibility/status colors such as error red or secure/incognito indicators merely for palette purity.

## 5. Readability and asset/export constraints

1. Meet WCAG 2.2 AA text contrast where text is part of our custom artwork: **4.5:1 normal text, 3:1 large text**. Test overlay combinations, not just token pairs. Don't encode essential copy only inside images.
2. Build GitHub artwork in sRGB, export losslessly for typography/edges, optimize PNG/WebP after visual comparison; do not rely on Display P3 appearing identical across browsers, GitHub image proxies or older Android displays.
3. Test at GitHub 375px and wider desktop widths in both light/dark, and Android Oppo Reno10 5G in default/large text and enhanced contrast where available.
4. Sample a true source/approved icon alongside swatches at 24, 48, 96 and 256px. The icon must remain recognizable; a surrounding accent may not compete with its bright royal-blue keyboard or orange mango.
5. No gradients behind body copy unless overlaid with a contrast-safe stable surface. No vignette strong enough to obscure image content.
6. Tone down gradient noise, chromatic aberration, metallic gold, artificial gloss and fake “premium” bloom. One excellent accent has more value than ten competing ones.
7. Observe reduced-motion and power constraints. Brand static art should be instant and optional animation should finish, not run forever.

## 6. Acceptance checks for the identity release

- [ ] Primary and compact icon renders compare byte-for-byte or reference-pixel identically to the approved masters; no recoloring or deformed letters.
- [ ] README, social card and Fastlane graphic share the exact token palette, lighting direction, spacing system and MangoLoads logo treatment.
- [ ] Light/dark heroes are elegant and legible on GitHub in mobile/native desktop views; not glossy for gloss's sake.
- [ ] Setup/About brand surfaces feel coherent and do not change actual keyboard theme choices or accessibility semantics.
- [ ] Any gradient and text combination is measured against contrast targets; warm amber never used as tiny text on pearl.
- [ ] No new runtime blur/shaders introduced to keyboard typing or hidden release/Obtainium regressions.
- [ ] Owner has inspected a real visual preview before final launch if the color direction is materially different from the concept here.
- [ ] Repository `AGENTS.md` and `PROJECT_CONTEXT.md` document palette source, changed resources, exact screenshots/test results and any deviations after each batch.

## 7. Source guidance checked

- Apple's [Human Interface Guidelines: Color](https://developer.apple.com/design/human-interface-guidelines/color) encourages consistent semantic color and variants for light/dark/increased-contrast contexts.
- Apple's [Human Interface Guidelines: Materials](https://developer.apple.com/design/human-interface-guidelines/materials) emphasizes material hierarchy and legibility rather than blur for its own sake.
- [W3C WCAG 2.2: Understanding Contrast Minimum](https://www.w3.org/WAI/WCAG22/Understanding/contrast-minimum) sets the normal/large text contrast targets used here.

**The palette is implemented on brand-facing surfaces after the owner's `Go`. It is not an approval to modify the icon's original pixel colors or override users' runtime keyboard themes.**
