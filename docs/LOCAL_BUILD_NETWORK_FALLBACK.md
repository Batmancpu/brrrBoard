# Build Android when the workspace network proxy is down

Use GitHub Actions as the reproducible Gradle host when this workspace cannot
fetch the pinned Gradle distribution or Maven artifacts. Do not commit a Gradle
distribution, dependency cache, signing files, model weights, or generated APK
to work around a temporary network failure.

## Diagnose the failure first

The Gradle wrapper uses `gradle/wrapper/gradle-wrapper.properties`. Keep its
checked-in `distributionUrl` and inherited proxy, certificate, and `NO_PROXY`
settings unchanged. Replacing the official URL with a public mirror does not
help when the workspace proxy bridge itself is unreachable and makes builds
less reproducible.

Check the task environment status for `observations_current: true` and
`network_policy.state: enforced`. Then make one small request to the exact host
from the Gradle error:

```sh
curl -sS --max-time 20 -o /dev/null -w '%{http_code}\n' \
  https://services.gradle.org/distributions/gradle-8.14.4-bin.zip
```

If the error names `proxy:8080`, check the documented proxy status endpoint
when available:

```sh
curl --noproxy proxy -fsS --max-time 5 http://proxy:8099/status
```

A refused proxy connection and refused direct egress indicate a workspace
proxy-bridge outage. Do not keep trying alternate mirrors.

## Build and test through GitHub Actions

Push the candidate commit to its review branch. **brrrBoard — Build Debug APK**
sets up Java 21 and Gradle in GitHub Actions, runs repository checks and Android
unit tests, builds the debug APK, verifies model packaging and uploads the
artifact. The debug-signed APK is for inspection only; it cannot update the
production-signed installation.

For an installable Obtainium update, merge the exact tested source to `main`.
The release workflow waits for a successful debug/unit run on the exact same
commit, builds the Obtainium variant, verifies the existing production signer,
package ID, version, offline permissions and model packaging, then publishes
`brrBoard.apk` in the GitHub Releases section. Normal releases omit Parakeet
model weights. Ultra can be bundled only through the release workflow's
explicit manual opt-in; changing a lock file never opts the build in.

## If Git push from the workspace is blocked

If an authenticated GitHub connector is available, it can create Git blobs and
a commit based on the exact inspected branch head, then update that branch with
`expected_sha` set to the observed old head. Verify the new ref SHA and the
exact-SHA workflow run before proceeding. Never force-move a branch or overwrite
an unexpected concurrent update.

## When local access returns

Retry the standard `./gradlew` command through the configured proxy. Keep the
wrapper distribution and dependencies in the normal build cache rather than in
the repository.
