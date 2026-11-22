#!/usr/bin/env bash
# CI-only, explicit-dispatch fetch of the optional large BR Brain A/B model.
# Never invoke on end-user devices. Every byte is pinned and checked.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TARGET="$ROOT/app/src/main/assets/voice-model/parakeet-ultra-0.6b-int8"
BASE="https://huggingface.co/csukuangfj/sherpa-onnx-nemo-parakeet-tdt-0.6b-ultra-int8/resolve/402e1190ce85b9793118ee223db21fb0d12f52db"
LOCK="$ROOT/tools/offline-ultra-voice-model.lock"

mkdir -p "$TARGET"
while IFS='|' read -r origin destination bytes checksum; do
  [[ -z "$origin" || "$origin" == \#* ]] && continue
  [[ "$destination" =~ ^(encoder\.onnx|decoder\.onnx|joiner\.onnx|tokens\.txt)$ ]] || exit 1
  [[ "$bytes" =~ ^[0-9]+$ && "$checksum" =~ ^[a-f0-9]{64}$ ]] || exit 1
  file="$TARGET/$destination"
  if [[ -f "$file" && "$(stat -c '%s' "$file")" == "$bytes" ]] &&
      echo "$checksum  $file" | sha256sum -c --status; then
    continue
  fi
  curl -fSL --retry 3 --connect-timeout 30 --output "$file.part" "$BASE/$origin"
  test "$(stat -c '%s' "$file.part")" = "$bytes"
  echo "$checksum  $file.part" | sha256sum -c -
  mv "$file.part" "$file"
done < "$LOCK"

echo 'Official pinned Ultra test weights staged; Android devices never fetch model files.'
