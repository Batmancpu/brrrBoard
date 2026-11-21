#!/usr/bin/env bash
# Refuse any APK with a missing or substituted native engine, weights or license.
set -euo pipefail
APK="${1:?Pass built APK}"
WEIGHTS="assets/voice/brrclean_gtcrn.weights"
LICENSE="assets/licenses/brrclean-poise-mit.txt"
EXPECTED_SHA256="4db48ff819e5ac18f5ccad1dfdce0968dde1a209c0cbbf9f24ba9dc4d9f48ded"
test -s "$APK"
unzip -Z1 "$APK" | grep -Fx "$WEIGHTS" >/dev/null
unzip -Z1 "$APK" | grep -Fx "$LICENSE" >/dev/null
unzip -Z1 "$APK" | grep -Eq '^lib/[^/]+/libbrrclean\.so$'
ACTUAL_SHA256="$(unzip -p "$APK" "$WEIGHTS" | sha256sum | cut -d' ' -f1)"
ACTUAL_BYTES="$(unzip -p "$APK" "$WEIGHTS" | wc -c | tr -d '[:space:]')"
if [[ "$ACTUAL_SHA256" != "$EXPECTED_SHA256" || "$ACTUAL_BYTES" != "211220" ]]; then
  echo "::error::Packaged brrClean native weights differ from approved upstream" >&2
  exit 1
fi
unzip -p "$APK" "$LICENSE" | grep -F 'Copyright (c) 2026 chabandou' >/dev/null
echo "Verified brrClean native library, weights and MIT license in APK"
