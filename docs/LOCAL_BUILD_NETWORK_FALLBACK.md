# Android build when the workspace network proxy is down

Use this when `./gradlew` cannot download the wrapper distribution or Maven
artifacts from the managed workspace.

## Diagnose the failure first

The Gradle wrapper uses `gradle/wrapper/gradle-wrapper.properties`. Keep its
checked-in `distributionUrl` and inherited proxy, certificate, and `NO_PROXY`
settings unchanged. Replacing the official URL with a public mirror does not
help when the workspace's proxy bridge itself is unreachable, and makes builds
less reproducible.

Check the task environment status for `observations_current: true` and
`network_policy.state: enforced`. Then use the ordinary configured proxy to
make one small request to the exact host from the Gradle error:

```sh
curl -sS --max-time 20 -o /dev/null -w '%{http_code}\n' \
  https://services.gradle.org/distributions/gradle-8.14.4-bin.zip
```

If the error says it cannot connect to `proxy:8080`, check the documented proxy
status endpoint when available:

```sh
curl --noproxy proxy -fsS --max-time 5 http://proxy:8099/status
```

If the proxy endpoint refuses connections and direct egress also refuses,
record this as an environment/proxy-bridge outage. A green/unrestricted
network-policy setting alone does not prove that the proxy process is reachable.
Do not keep trying alternate mirrors or change Gradle's pinned distribution.

## Build and test through GitHub Actions

Use the repository's **brrrBoard — Build Debug APK** workflow. It sets up Java
21, restores the Gradle cache, runs repository checks and the Android unit
suite, builds the debug APK, verifies its signing identity and model-packaging
policy, then uploads the debug APK as an Actions artifact. It does not create a
GitHub release or invoke the Obtainium publishing workflow.

On `feature/parakeet-ultra-brain-abtest`, a second job runs only after the full
debug/unit-test gate passes. It stages the exact pinned 110M and Ultra packs,
builds with the current production versionCode, signs with the existing app
certificate, verifies package/version/offline permissions/assets, and uploads
an update-compatible test APK to Actions. This is test-only; the regular
Obtainium workflow never includes Ultra from this branch. Both whole APK and
three 300 MB split artifacts are retained for 14 days. Split parts can be
reconstructed with the repository script after extracting them into one folder:

```sh
bash tools/join-ultra-abtest-apk.sh ./downloaded-parts ./brrrBoard-ultra-ab-test.apk <APK-SHA256>
```

The script requires all three named pieces, verifies the complete APK hash
before leaving the output file, and prints the final size/hash. Obtain the
expected hash from the checksum artifact or the corresponding workflow log.
The source implementation of the split upload is in
`.github/workflows/brrrboard-build-debug.yml`.

Always select the run for the exact branch and source SHA. Do not use the
unsigned/debug APK to update the production install; only the separately
verified original-signed A/B artifact is update-compatible. A test artifact is
not an Obtainium release and does not change the app's published version.

## If both workspace Git and artifact transfer are blocked

A dead workspace proxy can block both `git push` and downloading a large APK.
Do not keep retrying alternate mirrors. If an authenticated GitHub connector is
available to the coding agent, it can use the Git Database API to create blobs
for the exact changed files, create a tree based on the inspected remote head,
create one commit with that head as parent, then update the feature ref with
`expected_sha` set to the inspected old head. Verify the new ref SHA and exact
workflow run afterwards. This was used when the local proxy refused port 8080.
Never force-move the branch or overwrite an unexpected concurrent update.

For artifact access, the connected GitHub downloader has a 512 MiB per-artifact
limit. Keep the browser-downloadable complete artifact for convenience, and
also upload independently named pieces below the cap. Download the checksum
and all parts from the same exact-SHA run, extract them together, and use the
join script above. Do not join parts from different workflow runs.

## When local access returns

Retry the standard wrapper command, which will download the pinned Gradle
distribution and dependencies through the configured proxy. Never commit the
Gradle distribution, Maven cache, signing files, model weights, or temporary
build artifacts to work around a transient network failure.


### 2026-11-22 — artifact transfer regression fixed

The first split-package attempt failed because the signing step wrote `brrBoard-ultra-ab-test.apk` (two `r` characters after `b`) while staging expected `brrrBoard-ultra-ab-test.apk` (three). The failure was a concrete filename mismatch, not workspace cleanup. The signer output and staging source now use one exact name, and `tools/test_ultra_voice_packaging.py` asserts that relationship. Exact-source CI run `44999224034` passed both jobs and uploaded the full artifact, all three parts, and checksum. When adding packaging steps, assert source and destination names in a test before uploading.
