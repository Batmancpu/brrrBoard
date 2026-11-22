#!/usr/bin/env bash
# Download the exact reviewed public research corpora used by the offline ContextLM
# experiment. Files are for local evaluation only and are never copied into the APK.
set -euo pipefail

DEST="${1:-${TMPDIR:-/tmp}/brr-contextlm-corpus}"
mkdir -p "$DEST"

fetch() {
  local url="$1" output="$2" expected_size="$3" expected_sha="$4"
  local tmp="$output.part"
  if [[ -f "$output" && "$(stat -c '%s' "$output")" == "$expected_size" ]] &&
      echo "$expected_sha  $output" | sha256sum -c --status; then
    return
  fi
  rm -f "$tmp"
  curl -fSL --retry 3 --connect-timeout 30 -o "$tmp" "$url"
  test "$(stat -c '%s' "$tmp")" = "$expected_size"
  echo "$expected_sha  $tmp" | sha256sum -c -
  mv "$tmp" "$output"
}

COMI="https://huggingface.co/datasets/LingoIITGN/COMI-LINGUA/resolve/0214cb358e59e36da60e01d3d59ddd24df897749"
HINGE="https://huggingface.co/datasets/LingoIITGN/HinGE/resolve/e25a10382d9adb77f3a4f03b721fbd0a43c0de4c"
fetch "$COMI/COMI_TN_train.csv?download=true" "$DEST/COMI_TN_train.csv" \
  12111945 307cf5ad66f64411fa90a938a9958147bfdb09cabec528d8383fe62e69277258
fetch "$COMI/COMI_MT_train.csv?download=true" "$DEST/COMI_MT_train.csv" \
  60989602 9a9bf88ced2cd94925549cce0b3f361eb4ae1f70b1d4abf9fbeec22deae1a2cc
fetch "$HINGE/HinGE.csv?download=true" "$DEST/HinGE.csv" \
  1591630 dcfaf37ca9d7528e27da7be6b0cfffafb8bbe7825fe4da45077b96236cd9c9c3

echo "Pinned local-only ContextLM corpora verified in $DEST"
