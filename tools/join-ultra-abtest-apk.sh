#!/usr/bin/env bash
# Reassemble the three uncompressed GitHub Actions artifact parts and verify
# the original signed APK before installation.
set -euo pipefail

if [[ "$#" -ne 3 ]]; then
  echo 'Usage: join-ultra-abtest-apk.sh <part-directory> <output.apk> <expected-sha256>' >&2
  exit 2
fi

parts_dir="$1"
output_apk="$2"
expected_sha="$(tr '[:upper:]' '[:lower:]' <<< "$3" | tr -d '[:space:]')"
[[ "$expected_sha" =~ ^[a-f0-9]{64}$ ]] || { echo 'Invalid expected SHA-256.' >&2; exit 2; }

for suffix in 00 01 02; do
  test -s "$parts_dir/brrrBoard-ultra-ab-test.apk.part-$suffix" || {
    echo "Missing APK part $suffix in $parts_dir" >&2
    exit 1
  }
done
if compgen -G "$parts_dir/brrrBoard-ultra-ab-test.apk.part-0[3-9]" >/dev/null; then
  echo 'Unexpected extra APK part; refusing to assemble.' >&2
  exit 1
fi

mkdir -p "$(dirname "$output_apk")"
tmp_output="$(mktemp "${output_apk}.tmp.XXXXXX")"
trap 'rm -f "$tmp_output"' EXIT
cat "$parts_dir/brrrBoard-ultra-ab-test.apk.part-00" \
    "$parts_dir/brrrBoard-ultra-ab-test.apk.part-01" \
    "$parts_dir/brrrBoard-ultra-ab-test.apk.part-02" > "$tmp_output"
actual_sha="$(sha256sum "$tmp_output" | cut -d' ' -f1)"
[[ "$actual_sha" == "$expected_sha" ]] || {
  echo "Reassembled APK hash mismatch: expected $expected_sha, got $actual_sha" >&2
  exit 1
}
mv "$tmp_output" "$output_apk"
trap - EXIT
echo "Verified signed test APK: $output_apk"
echo "Size: $(stat -c '%s' "$output_apk") bytes"
echo "SHA-256: $actual_sha"
