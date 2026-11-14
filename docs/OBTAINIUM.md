# Install and update brrBoard in Obtainium

Once the first APK appears under [GitHub Releases](https://github.com/Batmancpu/brrrBoard/releases):

1. In Obtainium, tap **Add App** and enter `https://github.com/Batmancpu/brrrBoard`.
2. Set **Filter Release Titles by Regular Expression** to `^brr+Board Obtainium`. Leave prereleases disabled.
3. Tap **Add**, then **Install**. For later builds, refresh in Obtainium and tap **Update**.

The new package ID is `com.orion.frostkeys.brrrboard`. It can install alongside the old APKs. If migrating, export and import settings where supported, and enable the newly installed keyboard in Android settings.

## Set up signing once

On a trusted computer with GitHub CLI, a JDK and OpenSSL, open the repository checkout and run:

```bash
gh auth login
bash tools/setup-obtainium-signing.bash /path/to/private-backup-directory
```

Use a private backup directory outside the checkout. The script generates a key and password, saves them to that directory, uploads them as GitHub Actions secrets, pins the public certificate fingerprint, and starts the first release. Keep both backup files in secure storage. Repeat with the same original files to recover a setup. The script rejects a different signer if a fingerprint is already pinned.

For manual enrollment, add `OBTAINIUM_KEYSTORE_BASE64` and `OBTAINIUM_KEYSTORE_PASSWORD` under Actions Secrets. Set `OBTAINIUM_CERT_SHA256` under Actions Variables or Secrets. If both exist, the variable takes precedence. Paste the 64-character lowercase fingerprint without colons.

For a first-install/update test, install the first Obtainium release before starting a new workflow run on `main`. Rerunning an old run keeps the same version. Older debug and Jules APKs use other package IDs.

Check the [release workflow](https://github.com/Batmancpu/brrrBoard/actions/workflows/obtainium-release.yml) and the release asset. After a second build, verify that Obtainium updates the first install in place. Keep the same package and signer. The workflow run number drives the increasing Android version code and the matching version name and GitHub tag.

App/build changes to `main` trigger publication automatically. Manual runs are also available on `main`. The signed release workflow checks the APK's certificate, package and version. The separate debug workflow reports unit tests.
