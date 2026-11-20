#!/usr/bin/env bash
# Fail closed unless the exact independently pinned MIT GTCRN model is inside
# the APK. Parakeet's larger optional model is verified by a separate script.
set -euo pipefail
APK="${1:?Pass built APK}"
WEIGHTS="assets/voice/gtcrn_simple.onnx"
LICENSE="assets/licenses/gtcrn-mit.txt"
EXPECTED_BLOB="015454eef64bd4328e6ff4946c231d4a4a632e93"
EXPECTED_BYTES="535190"
test -s "$APK"
unzip -Z1 "$APK" | grep -Fx "$WEIGHTS" >/dev/null
unzip -Z1 "$APK" | grep -Fx "$LICENSE" >/dev/null
ACTUAL_BLOB="$(unzip -p "$APK" "$WEIGHTS" | git hash-object --stdin)"
ACTUAL_BYTES="$(unzip -p "$APK" "$WEIGHTS" | wc -c | tr -d '[:space:]')"
if [[ "$ACTUAL_BLOB" != "$EXPECTED_BLOB" || "$ACTUAL_BYTES" != "$EXPECTED_BYTES" ]]; then
  echo "::error::APK GTCRN bytes differ from approved upstream model." >&2
  exit 1
fi
unzip -p "$APK" "$LICENSE" | grep -F 'Copyright (c) 2024 Rong Xiaobin' >/dev/null
echo "Verified APK-integrated GTCRN: $ACTUAL_BYTES bytes, Git blob $ACTUAL_BLOB"
