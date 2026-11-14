#!/usr/bin/env bash
set -euo pipefail
umask 077

repo=Batmancpu/brrrBoard
backup_dir="${1:?Usage: bash tools/setup-obtainium-signing.bash /path/to/private-backup-directory}"
gh auth status --hostname github.com
gh secret list --repo "$repo" >/dev/null
existing_cert="$(gh variable list --repo "$repo" --json name,value \
  --jq '.[] | select(.name == "OBTAINIUM_CERT_SHA256") | .value')"

mkdir -p "$backup_dir"
keystore="$backup_dir/brrrboard-obtainium.p12"
password_file="$backup_dir/brrrboard-obtainium.password"
if [[ ! -e "$keystore" && ! -e "$password_file" && -z "$existing_cert" ]]; then
  openssl rand -hex 32 > "$password_file"
  keytool -genkeypair -keystore "$keystore" -storetype PKCS12 \
    -storepass:file "$password_file" -alias brrrboard -keyalg RSA -keysize 3072 \
    -validity 10000 -dname "CN=brrrBoard Obtainium"
fi
if [[ ! -s "$keystore" || ! -s "$password_file" ]]; then
  echo "Restore both original backup files before continuing." >&2
  exit 1
fi

cert="$(keytool -exportcert -keystore "$keystore" -storetype PKCS12 \
  -storepass:file "$password_file" -alias brrrboard | openssl dgst -sha256 -r | cut -d' ' -f1)"
[[ "$cert" =~ ^[a-f0-9]{64}$ ]]
if [[ -n "$existing_cert" && "$cert" != "$existing_cert" ]]; then
  echo "The backup does not match the pinned signing certificate." >&2
  exit 1
fi

base64 < "$keystore" | tr -d '\r\n' | gh secret set OBTAINIUM_KEYSTORE_BASE64 --repo "$repo"
tr -d '\r\n' < "$password_file" | gh secret set OBTAINIUM_KEYSTORE_PASSWORD --repo "$repo"
gh variable set OBTAINIUM_CERT_SHA256 --repo "$repo" --body "$cert"
echo "Obtainium signing certificate SHA-256: $cert"
gh workflow run obtainium-release.yml --repo "$repo" --ref main
