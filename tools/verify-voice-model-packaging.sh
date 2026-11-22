#!/usr/bin/env bash
# Verify the explicit release packaging mode, never infer it from APK size.
set -euo pipefail

if [[ "$#" -ne 2 || ! -s "$1" ]]; then
  echo 'Usage: verify-voice-model-packaging.sh <apk> <unbundled|bundled|ultra|both>' >&2
  exit 2
fi

apk="$1"
mode="$2"
case "$mode" in
  bundled)
    bash "$(dirname "$0")/verify-bundled-voice-model.sh" "$apk"
    if unzip -Z1 "$apk" | grep -Eq '^assets/voice-model/parakeet-ultra-0\.6b-int8/'; then
      echo 'Legacy-only bootstrap package unexpectedly includes Parakeet Ultra weights.' >&2
      exit 1
    fi
    ;;
  ultra)
    bash "$(dirname "$0")/verify-bundled-ultra-voice-model.sh" "$apk"
    if unzip -Z1 "$apk" | grep -Eq '^assets/voice-model/parakeet-tdt-110m-en/'; then
      echo 'Ultra-only test package unexpectedly includes the legacy model weights.' >&2
      exit 1
    fi
    ;;
  both)
    bash "$(dirname "$0")/verify-bundled-voice-model.sh" "$apk"
    bash "$(dirname "$0")/verify-bundled-ultra-voice-model.sh" "$apk"
    ;;
  unbundled)
    entries="$(unzip -Z1 "$apk")"
    # Tiny pinned GTCRN is mandatory in every APK; larger Parakeet is optional.
    # An independent APK verifier checks the exact approved GTCRN bytes.
    if grep -Eq '^assets/voice-model/' <<< "$entries" ||
       grep -E '^assets/.*\.onnx$' <<< "$entries" |
           grep -Fvx 'assets/voice/gtcrn_simple.onnx' >/dev/null; then
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

# Fail closed if any unexpected ONNX file or arbitrary model asset sneaks into
# a build, including under a differently named directory.
while IFS= read -r entry; do
  [[ "$entry" == assets/voice-model/* || "$entry" == assets/*.onnx ]] || continue
  case "$entry" in
    assets/voice/gtcrn_simple.onnx) ;;
    assets/voice-model/parakeet-tdt-110m-en/encoder.onnx|\
    assets/voice-model/parakeet-tdt-110m-en/decoder.onnx|\
    assets/voice-model/parakeet-tdt-110m-en/joiner.onnx|\
    assets/voice-model/parakeet-tdt-110m-en/tokens.txt)
      [[ "$mode" == bundled || "$mode" == both ]] || {
        echo "Unexpected 110M model asset in $mode package: $entry" >&2; exit 1;
      }
      ;;
    assets/voice-model/parakeet-ultra-0.6b-int8/encoder.onnx|\
    assets/voice-model/parakeet-ultra-0.6b-int8/decoder.onnx|\
    assets/voice-model/parakeet-ultra-0.6b-int8/joiner.onnx|\
    assets/voice-model/parakeet-ultra-0.6b-int8/tokens.txt)
      [[ "$mode" == ultra || "$mode" == both ]] || {
        echo "Unexpected Ultra model asset in $mode package: $entry" >&2; exit 1;
      }
      ;;
    *)
      echo "Unapproved model weight/asset in $mode package: $entry" >&2
      exit 1
      ;;
  esac
done < <(unzip -Z1 "$apk")
