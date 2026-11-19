#!/usr/bin/env bash
# Verify that a built brrBoard APK contains exactly the four pinned English model assets.
# Run after signing for Obtainium and on the debug artifact before uploading it.
set -euo pipefail

if [[ "$#" -ne 1 || ! -s "$1" ]]; then
  echo "Usage: bash tools/verify-bundled-voice-model.sh <apk>" >&2
  exit 2
fi

apk="$1"
lock="$(cd "$(dirname "$0")" && pwd)/offline-voice-model.lock"
count=0
while IFS='|' read -r origin name size digest; do
  [[ -z "$origin" || "$origin" == \#* ]] && continue
  case "$name" in
    encoder.onnx|decoder.onnx|joiner.onnx|tokens.txt) ;;
    *) echo "Unexpected model lock entry: $name" >&2; exit 1 ;;
  esac
  asset="assets/voice-model/parakeet-tdt-110m-en/$name"
  if ! unzip -Z1 "$apk" | grep -Fxq "$asset"; then
    echo "Bundled English voice model is missing: $asset" >&2
    exit 1
  fi
  embedded="$(unzip -p "$apk" "$asset" | sha256sum | cut -d' ' -f1)"
  if [[ "$embedded" != "$digest" ]]; then
    echo "Bundled English voice model checksum mismatch: $asset" >&2
    exit 1
  fi
  count=$((count + 1))
done < "$lock"

embedded_count="$(unzip -Z1 "$apk" | grep -Ec '^assets/voice-model/parakeet-tdt-110m-en/[^/]+$' || true)"
if [[ "$count" -ne 4 || "$embedded_count" -ne 4 ]]; then
  echo "Expected exactly four verified model files; saw lock=$count APK=$embedded_count" >&2
  exit 1
fi
if ! unzip -Z1 "$apk" | grep -Fxq "assets/voice-model-attribution.txt"; then
  echo "Bundled English voice model attribution is missing" >&2
  exit 1
fi
echo "Verified four pinned English ASR files and attribution inside $(basename "$apk")."
