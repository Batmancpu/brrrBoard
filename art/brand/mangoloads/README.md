# MangoLoads × brrBoard brand sources

**Canonical identity:** the approved MangoLoads blue keyboard, ten keys `M A N G O` / `L O A D S`, lowercase italic `brr` on the spacebar, orange mango/green leaf and a pointed translucent glass badge. The badge alone optically obscures its overlapped top-center keys; letters outside it remain sharp. The app's public name is **brrBoard**, with MangoLoads as the creator.

## Source and provenance

- `primary-alpha-lossless.webp`: **canonical repo source**, a bit-for-bit-lossless 1254×1254 alpha cutout of the user's approved RGB composition. This is the production raster authority. Its decoded pixels were checked against the approved deterministic cutout with ImageMagick `compare -metric AE` yielding **0 differences**. The original white-matted 1254×1254 `primary-master.png` remains in the previously delivered identity preparation pack, not needed to rebuild from this repository.
- `compact-master.png`: preapproved 1254×1254 RGBA keyboard-only design. No fruit or shield; use only in contexts where the complete logo is demonstrably illegible.
- `compact-two-tone.svg`: separate reproducible two-tone keyboard mark for unusually constrained displays/print.
- `icon-silhouette-mask.svg`: code-native SVG clipping guide documenting how the approved original full-color raster's white outer canvas was replaced with true alpha. It is not a redraw of fruit or keys; the equivalent **lossless result** is already tracked as WebP above, so a checkout does not need the RGB reference.
- `themed-monochrome.svg`: platform single-alpha shape for Android 13+ wallpaper tint. Its appearance is controlled by the system and is **not** the standard full-color brand mark.

The original raster masters are design-authorized artwork, not screenshots from an installed APK. The vector silhouette is a deterministic postprocessing/export guide; it cannot introduce runtime refraction. In particular, **do not use the white-matted RGB master as the transparent launcher foreground**.

## Rebuild exports

From repo root run:

```bash
bash tools/export-mangoloads-brand.sh
```

Requirements: ImageMagick 7 (`magick`) and Inkscape. The script creates/refreshes:

- `primary-alpha.png` reconstructed losslessly from the tracked alpha WebP;
- `app/src/main/res/drawable-nodpi/mangoloads_brand_full.png` (768) and `mangoloads_brand_small.png` (192);
- `drawable-nodpi/mangoloads_launcher_foreground.png` (432, safe inset) and `mangoloads_launcher_monochrome.png` (432);
- five densities of legacy `ic_launcher.webp` and `ic_launcher_round.webp` (48/72/96/144/192);
- `app/src/main/ic_launcher-playstore.png` and Fastlane `images/icon.png` (512 square opaque);
- `readme-hero-light.png`, `readme-hero-dark.png` (1440×520), `github-social-preview.png` (1280×640), and Fastlane `images/featureGraphic.png` (1024×500).

Keep the SVG sources and full-resolution source PNGs; do not edit exported rasters by hand. The GitHub social preview file must also be **uploaded separately in repository Settings**—committing a PNG does not automatically set it.

## Brand usage

| Context | Asset | Rule |
|---|---|---|
| Launcher/app drawer/system app details | Adaptive full-color foreground + dark-sapphire background; legacy density exports | Keep established Android icon IDs and mask safety |
| Welcome wizard | `mangoloads_brand_full` | Only one finite entrance motion; then static |
| Settings main About leading icon | `mangoloads_brand_small` | No Material tint, keep existing row navigation |
| About app row | `@mipmap/ic_launcher_round` | Existing image path updates automatically |
| GitHub README | Theme-aware hero from the SVG sources | Real Markdown title/links below the artwork |
| GitHub social preview, Fastlane feature graphic | Respective horizontal premium banners | Do not stretch the square launcher icon into a wide rectangle |
| Android themed icon | One-color `mangoloads_launcher_monochrome` | Platform applies tint; unrelated to full-color art |
| Exceptionally small/low-detail place | Compact master / two-tone icon | Only after comparing final rendered legibility; do not force replacements |

## Guardrails and QA

- The **icon's colors are intentionally untouched** by the premium `#0C1527` / `#F7F9FC` / `#2D60C8` surrounding palette. Only background/marketing surfaces change.
- Verify no detached pixels or halo on dark and light backgrounds, all MANGO/LOADS spelling, exact `brr`, glass-only obscuration, corner rounding and at-size Android masks.
- The normal keyboard, Clipboard, Emoji, microphone, privacy shield and user-selected themes are functional surfaces—not brand-logo placements.
- Compare 24/32/40dp, 48/192px and OEM circle/squircle/rounded-square masks. Real Oppo Reno10 5G visual acceptance is separate from static exports or successful CI.
- Never replace this approved identity with an AI-regenerated approximation that deforms the letters or introduces a new fruit or shield geometry.
