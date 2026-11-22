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
21 and `gradle/actions/setup-gradle@v4`, runs repository checks and the Android
unit suite, builds the debug APK, and uploads it as an Actions artifact. This
workflow does not create a GitHub release or run the original-signer Obtainium
workflow.

Pushes to branches run debug verification. The dedicated
`feature/parakeet-ultra-brain-abtest` branch automatically stages the large
experimental Ultra weights into its debug artifact because the owner asked to
test that model. Other branches remain lightweight unless a maintainer
explicitly sets `include_ultra_test_model` to `true` in **Run workflow**. This
option is debug-artifact-only; it cannot publish an Obtainium update.
Alternatively, from a host where `gh` is authenticated and network access
works:

```sh
gh workflow run brrrboard-build-debug.yml \
  --repo Batmancpu/brrrBoard --ref <feature-branch> \
  -f include_ultra_test_model=false
```

Wait for that exact branch/commit's run to complete successfully, then download
the debug APK artifact from the run. Confirm the artifact commit SHA before
using it. Keep the original-signer release workflow and release publication
separate; a debug artifact is a test build, not an Obtainium update.

## When local access returns

Retry the standard wrapper command, which will download the pinned Gradle
distribution and dependencies through the configured proxy. Never commit the
Gradle distribution, Maven cache, signing files, model weights, or temporary
build artifacts to work around a transient network failure.
