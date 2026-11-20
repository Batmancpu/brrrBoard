#!/usr/bin/env bash
# brrBoard GTCRN: reproducible ~535 KB speech enhancement weights.
# DOWNLOAD HAPPENS ONLY DURING APK BUILD; Android never fetches a model.
# Upstream MIT: https://github.com/Xiaobin-Rong/gtcrn
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEST="$ROOT/app/src/main/assets/voice/gtcrn_simple.onnx"
COMMIT="502ebfab64da7c4a9af78dcb9c6ceef1ebb01c73"
UPSTREAM_BLOB="015454eef64bd4328e6ff4946c231d4a4a632e93"
EXPECTED_BYTES="535190"
URL="https://raw.githubusercontent.com/Xiaobin-Rong/gtcrn/$COMMIT/stream/onnx_models/gtcrn_simple.onnx"

verify() {
  test -s "$1"
  test "$(stat -c '%s' "$1")" = "$EXPECTED_BYTES"
  test "$(git hash-object "$1")" = "$UPSTREAM_BLOB"
}

if [[ -f "$DEST" ]] && verify "$DEST"; then
  echo "GTCRN verified and staged for APK ($EXPECTED_BYTES bytes, blob $UPSTREAM_BLOB)"
  exit 0
fi

WORK="$(mktemp -d)"
trap 'rm -rf "$WORK"' EXIT
curl --fail --location --silent --show-error --retry 3 --connect-timeout 30 \
  --output "$WORK/gtcrn_simple.onnx" "$URL"
if ! verify "$WORK/gtcrn_simple.onnx"; then
  echo "::error::GTCRN is not the pinned upstream Git blob/size; refusing APK build." >&2
  exit 1
fi
mkdir -p "$(dirname "$DEST")"
install -m 0644 "$WORK/gtcrn_simple.onnx" "$DEST"
verify "$DEST"
echo "Pinned GTCRN bundled at build time. SHA-256: $(sha256sum "$DEST" | cut -d' ' -f1)"
