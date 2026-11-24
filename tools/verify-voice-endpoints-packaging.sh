#!/usr/bin/env bash
set -euo pipefail
APK="$1"
verify() {
  local actual
  actual="$(unzip -p "$APK" "$1" | sha256sum | cut -d' ' -f1)"
  [[ "$actual" == "$2" ]] || { echo "Model hash mismatch: $1" >&2; exit 1; }
}
verify assets/voice/silero_vad_v6_2_3.onnx 1a153a22f4509e292a94e67d6f9b85e8deb25b4988682b7e174c65279d8788e3
verify assets/voice/smart-turn-v3.2-cpu.onnx 2bb026316b14a660486a75b1733cd3fbab8c2fd0314dc9af7be49f8cca967e4f
LIST="$(unzip -Z1 "$APK")"
for entry in assets/licenses/marmalade-stt-mit.txt assets/licenses/silero-vad-mit.txt assets/licenses/smart-turn-bsd-2-clause.txt lib/arm64-v8a/libbrrvoiceendpoint.so; do
  grep -Fxq "$entry" <<< "$LIST"
done
echo "Verified pinned adaptive voice models, attribution and native adapter."
