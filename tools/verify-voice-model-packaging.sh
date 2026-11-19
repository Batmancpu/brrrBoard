#!/usr/bin/env bash
# Verify the explicit release packaging mode, never infer it from APK size.
set -euo pipefail

if [[ "$#" -ne 2 || ! -s "$1" ]]; then
  echo 'Usage: verify-voice-model-packaging.sh <apk> <unbundled|bundled>' >&2
  exit 2
fi

apk="$1"
mode="$2"
case "$mode" in
  bundled)
    exec bash "$(dirname "$0")/verify-bundled-voice-model.sh" "$apk"
    ;;
  unbundled)
    entries="$(unzip -Z1 "$apk")"
    if grep -Eq '(^assets/voice-model/|^assets/.*\.onnx$)' <<< "$entries"; then
      echo 'Refusing lightweight APK: speech model weights are embedded.' >&2
      exit 1
    fi
    echo "Verified no English ASR model weights in $(basename "$apk")."
    ;;
  *)
    echo "Unsupported packaging mode: $mode" >&2
    exit 2
    ;;
esac
