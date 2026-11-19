#!/usr/bin/env bash
# CI build-time fetch of pinned model weights bundled into each Android APK.
# End-user devices never fetch or contact the model source independently.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
TARGET="$ROOT/app/src/main/assets/voice-model/parakeet-tdt-110m-en"
BASE="https://github.com/DevEmperor/DictateKeyboard/releases/download/whisper-models-v1"
LOCK="$ROOT/tools/offline-voice-model.lock"

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

echo 'Pinned model pack checked for the self-contained normal brrBoard APK.'
