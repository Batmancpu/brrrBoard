#!/usr/bin/env bash
# SPDX-License-Identifier: GPL-3.0-only
# Deterministic format exports from the approved MangoLoads icon masters.
# Requires Inkscape and ImageMagick 7; never regenerates/recolors letterforms.
set -euo pipefail

project_root="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
brand="$project_root/art/brand/mangoloads"
resources="$project_root/app/src/main/res"
store="$project_root/fastlane/metadata/android/en-US/images"

for program in inkscape magick; do
    command -v "$program" >/dev/null || { echo "Missing $program" >&2; exit 1; }
done
test -s "$brand/primary-alpha-lossless.webp"
test -s "$brand/compact-master.png"
test -s "$brand/icon-silhouette-mask.svg"
test -s "$brand/themed-monochrome.svg"

# The lossless WebP is bit-for-bit equivalent to the approved cutout PNG (AE=0
# against the original master+silhouette-mask export). This portable canonical
# source avoids requiring the much larger white-matted RGB reference at build
# time and keeps all colored letters/glass pixels unchanged.
magick "$brand/primary-alpha-lossless.webp" "$brand/primary-alpha.png"

# Full-color artwork is the default, downsampled only for its exact consumer.
magick "$brand/primary-alpha.png" -filter Lanczos -resize 768x768 \
    "$resources/drawable-nodpi/mangoloads_brand_full.png"
magick "$brand/primary-alpha.png" -filter Lanczos -resize 192x192 \
    "$resources/drawable-nodpi/mangoloads_brand_small.png"

# Adaptive icon foreground: 108dp viewport, ~79dp artwork. Critical key labels
# remain in the center while OEM circle/squircle masks clip only outer corners.
magick "$brand/primary-alpha.png" -filter Lanczos -resize 316x316 \
    -gravity center -background none -extent 432x432 \
    "$resources/drawable-nodpi/mangoloads_launcher_foreground.png"
inkscape "$brand/themed-monochrome.svg" \
    --export-filename="$brand/themed-monochrome-source.png" >/dev/null
magick "$brand/themed-monochrome-source.png" -filter Lanczos -resize 316x316 \
    -gravity center -background none -extent 432x432 \
    "$resources/drawable-nodpi/mangoloads_launcher_monochrome.png"

# Legacy launcher bitmaps must be opaque; ambient dark sapphire is a backdrop,
# NOT a recoloring/tint of the brand icon itself.
for entry in "mdpi 48" "hdpi 72" "xhdpi 96" "xxhdpi 144" "xxxhdpi 192"; do
    read -r bucket edge <<< "$entry"
    artwork=$((edge * 88 / 100))
    magick -size "${edge}x${edge}" xc:'#0C1527' \
        \( "$brand/primary-alpha.png" -filter Lanczos -resize "${artwork}x${artwork}" \) \
        -gravity center -composite -quality 94 \
        "$resources/mipmap-${bucket}/ic_launcher.webp"
    cp "$resources/mipmap-${bucket}/ic_launcher.webp" \
        "$resources/mipmap-${bucket}/ic_launcher_round.webp"
done

magick -size 512x512 xc:'#0C1527' \
    \( "$brand/primary-alpha.png" -filter Lanczos -resize 455x455 \) \
    -gravity center -composite "$project_root/app/src/main/ic_launcher-playstore.png"
cp "$project_root/app/src/main/ic_launcher-playstore.png" "$store/icon.png"

# Static premium marketing treatments live in artwork, not runtime blur.
# The one alpha master is only embedded, never tinted or redrawn.
for artwork in readme-hero-light readme-hero-dark github-social-preview store-feature-graphic; do
    inkscape "$brand/${artwork}.svg" \
        --export-filename="$brand/${artwork}.png" >/dev/null
done
cp "$brand/store-feature-graphic.png" "$store/featureGraphic.png"

echo "MangoLoads brand exported: primary alpha, adaptive/legacy launcher, About/welcome, store."
