#!/usr/bin/env bash
# Build-time only, pinned native GTCRN weights. No runtime downloader exists.
# Source: https://github.com/chabandou/poise-android (MIT) at the exact commit.
set -euo pipefail

ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"
DEST="$ROOT/app/src/main/assets/voice/brrclean_gtcrn.weights"
COMMIT="2014cd0d06b2effa24b1ab1c95013f785f38dc80"
EXPECTED_SHA256="4db48ff819e5ac18f5ccad1dfdce0968dde1a209c0cbbf9f24ba9dc4d9f48ded"
EXPECTED_BYTES="211220"
URL="https://raw.githubusercontent.com/chabandou/poise-android/$COMMIT/app/src/main/assets/gtcrn.weights"

verify() {
  test -s "$1"
  test "$(stat -c '%s' "$1")" = "$EXPECTED_BYTES"
  test "$(sha256sum "$1" | cut -d' ' -f1)" = "$EXPECTED_SHA256"
}

if [[ -f "$DEST" ]] && verify "$DEST"; then
  echo "Verified brrClean GTCRN already staged for APK"
  exit 0
fi
TEMP_DIR="$(mktemp -d)"
trap 'rm -rf "$TEMP_DIR"' EXIT
curl --fail --location --silent --show-error --retry 3 --connect-timeout 30 \
  --output "$TEMP_DIR/gtcrn.weights" "$URL"
if ! verify "$TEMP_DIR/gtcrn.weights"; then
  echo "::error::Native GTCRN weights failed source hash/size verification" >&2
  exit 1
fi
mkdir -p "$(dirname "$DEST")"
install -m 0644 "$TEMP_DIR/gtcrn.weights" "$DEST"
verify "$DEST"
echo "Verified and bundled offline brrClean GTCRN: $EXPECTED_BYTES bytes"
