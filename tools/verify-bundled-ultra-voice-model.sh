#!/usr/bin/env bash
# Verify the exactly pinned optional Ultra pack and its attribution inside an APK.
set -euo pipefail
if [[ "$#" -ne 1 || ! -s "$1" ]]; then
  echo "Usage: bash tools/verify-bundled-ultra-voice-model.sh <apk>" >&2
  exit 2
fi
apk="$1"
lock="$(cd "$(dirname "$0")" && pwd)/offline-ultra-voice-model.lock"
count=0
while IFS='|' read -r origin name size digest; do
  [[ -z "$origin" || "$origin" == \#* ]] && continue
  case "$name" in encoder.onnx|decoder.onnx|joiner.onnx|tokens.txt) ;; *) exit 1 ;; esac
  asset="assets/voice-model/parakeet-ultra-0.6b-int8/$name"
  unzip -Z1 "$apk" | grep -Fxq "$asset" || { echo "Missing Ultra model asset: $asset" >&2; exit 1; }
  [[ "$(unzip -p "$apk" "$asset" | sha256sum | cut -d' ' -f1)" == "$digest" ]] || {
    echo "Ultra model checksum mismatch: $asset" >&2; exit 1;
  }
  count=$((count + 1))
done < "$lock"
embedded_count="$(unzip -Z1 "$apk" | grep -Ec '^assets/voice-model/parakeet-ultra-0\.6b-int8/[^/]+$' || true)"
[[ "$count" -eq 4 && "$embedded_count" -eq 4 ]] || {
  echo "Expected exactly four pinned Ultra assets (lock=$count APK=$embedded_count)." >&2; exit 1;
}
unzip -Z1 "$apk" | grep -Fxq 'assets/voice-model-attribution.txt' || {
  echo 'Ultra attribution is missing.' >&2; exit 1;
}
echo "Verified four pinned Parakeet Ultra test files and attribution inside $(basename "$apk")."
