#!/usr/bin/env bash
# Sign a feature-branch-only A/B test APK with the production identity, but do
# not publish it. This lets the owner install it in place and retain private
# 110M model/settings data for the side-by-side Ultra experiment.
set -euo pipefail

if [[ "$#" -ne 3 || ! -s "$1" ]]; then
  echo 'Usage: sign-ultra-abtest-apk.sh <unsigned-apk> <output-apk> <version-code>' >&2
  exit 2
fi

unsigned_apk="$1"
output_apk="$2"
version_code="$3"
[[ "$version_code" =~ ^[0-9]+$ ]] || { echo 'Invalid version code.' >&2; exit 2; }
: "${KEYSTORE_BASE64:?Missing signing keystore secret}"
: "${KEYSTORE_PASSWORD:?Missing signing keystore password}"
: "${EXPECTED_CERT:?Missing expected signing certificate fingerprint}"
: "${ANDROID_HOME:?Missing Android SDK}"

expected_cert="$(tr '[:upper:]' '[:lower:]' <<< "$EXPECTED_CERT" | tr -d ':[:space:]')"
[[ "$expected_cert" =~ ^[a-f0-9]{64}$ ]] || { echo 'Invalid expected certificate fingerprint.' >&2; exit 2; }

umask 077
work="$(mktemp -d)"
trap 'rm -rf "$work"' EXIT
printf '%s' "$KEYSTORE_BASE64" | base64 --decode > "$work/signing.p12"
actual_keystore_cert="$(keytool -exportcert -keystore "$work/signing.p12" -storetype PKCS12 \
  -storepass:env KEYSTORE_PASSWORD -alias brrrboard | sha256sum | cut -d' ' -f1)"
[[ "$actual_keystore_cert" == "$expected_cert" ]] || {
  echo 'The provided keystore does not match the pinned app signer.' >&2
  exit 1
}

apksigner="$(find "$ANDROID_HOME/build-tools" -maxdepth 2 -type f -name apksigner | sort -V | tail -n 1)"
test -n "$apksigner"
sdk_tools="$(dirname "$apksigner")"
"$sdk_tools/zipalign" -P 16 -f 4 "$unsigned_apk" "$work/aligned.apk"
"$apksigner" sign --ks "$work/signing.p12" --ks-key-alias brrrboard \
  --ks-pass env:KEYSTORE_PASSWORD --out "$output_apk" "$work/aligned.apk"

signed_cert="$("$apksigner" verify --print-certs "$output_apk" \
  | sed -nE 's/^.*certificate SHA-256 digest:[[:space:]]*([0-9a-fA-F]{64})[[:space:]]*$/\1/p' \
  | tr '[:upper:]' '[:lower:]' | sort -u)"
[[ "$signed_cert" == "$expected_cert" ]] || {
  echo 'The signed test APK has the wrong signing identity.' >&2
  exit 1
}

permissions="$("$sdk_tools/aapt" dump permissions "$output_apk")"
if grep -Eq 'android\.permission\.(INTERNET|ACCESS_NETWORK_STATE)' <<< "$permissions"; then
  echo 'The offline test APK unexpectedly declares network permissions.' >&2
  exit 1
fi
if unzip -Z1 "$output_apk" | grep -Fxq 'assets/default_backup.zip'; then
  echo 'The test APK unexpectedly contains a settings backup.' >&2
  exit 1
fi
bash tools/verify-voice-model-packaging.sh "$output_apk" both
bash tools/verify-gtcrn-packaging.sh "$output_apk"
bash tools/verify-brrclean-packaging.sh "$output_apk"
for dict in main_hi-Latn emoji_hi-Latn emoji_en-IN; do
  unzip -p "$output_apk" "assets/dicts/$dict.dict" | cmp - "app/src/main/assets/dicts/$dict.dict"
done

badging="$("$sdk_tools/aapt" dump badging "$output_apk")"
grep -Fx "application-label:'brrBoard'" <<< "$badging"
package_line="${badging%%$'\n'*}"
[[ "$package_line" == "package: name='com.mangoloads.board' versionCode='$version_code' versionName='1.0.$version_code'"* ]] || {
  echo 'The test APK package/version does not match the current production installation.' >&2
  exit 1
}
echo "Verified update-compatible, test-only signed APK: $(basename "$output_apk")"
echo "APK size: $(stat -c '%s' "$output_apk") bytes"
echo "APK SHA-256: $(sha256sum "$output_apk" | cut -d' ' -f1)"
