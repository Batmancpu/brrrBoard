# brrrBoard Experiment & Fix History

This file is the project's durable engineering ledger.

**Rule:** every attempted bug fix, UI experiment, CI repair, rollback, or workaround that is not obvious from the final code should be recorded here, including failed attempts. Never delete a failed experiment; append the outcome instead.

## Entry format

- **Date / build:** YYYY-MM-DD / version or commit
- **Problem:** exact symptom
- **Hypothesis:** suspected root cause
- **Change attempted:** files/functions changed
- **Result:** PASS / FAIL / PARTIAL / REVERTED
- **Evidence:** CI run, APK SHA-256, device/model, logcat, screenshot, etc.
- **Next step:** what should be tried next

## 2026-10-07 — CI / APK recovery audit

- **Problem:** main had no active APK workflow; multiple temporary workflows had been created, patched, and then deleted during repeated CI repairs.
- **Hypothesis:** build/release infrastructure had become more fragile than the application code and could not reliably produce a current test APK.
- **Change attempted:** restore one canonical debug-build workflow with full checkout, JDK 17, Gradle setup, persistent debug signing, unit tests, APK verification, artifact upload, and GitHub release publication.
- **Result:** IN PROGRESS — this branch is the validation vehicle.
- **Evidence:** main's latest commits removed `brrrboard-ci-test.yml`, `brrrboard-release.yml`, `brrrboard-signing-bootstrap.yml`, `build-debug-apk.yml`, and `release-apk.yml` after repeated workflow repairs.
- **Next step:** keep the new workflow only if the build/test/signing verification passes.

## 2026-10-07 — Debug versionCode collision hardening

- **Problem:** debug `versionCode` used minute-resolution wall-clock time as its fallback.
- **Hypothesis:** two builds produced within the same minute can receive the same `versionCode`, causing Android update/install failures even when the APK is signed correctly.
- **Change attempted:** increase the fallback clock resolution from minutes to seconds while retaining the git-revision-count floor.
- **Result:** IN PROGRESS — requires CI build verification and device update verification.
- **Evidence:** `GitCommitCountValueSource` previously used `System.currentTimeMillis() / 60_000L`.
- **Next step:** install two successive debug APKs over the same package and verify that the second installs as an update without uninstalling.

## 2026-10-05 — Debug settings crash

- **Problem:** opening Debug settings crashed with a Compose nested-scroll measurement exception.
- **Root cause confirmed:** a `LazyColumn` was nested inside another vertically scrolling `LazyColumn`.
- **Fix:** render Debug settings directly in the parent `ColumnScope`; make filtering injectable.
- **Result:** FIXED.
- **Evidence:** commit `9c022e9977894c0d4e35b0451f07769843f3977d`.

## 2026-10-05 — Klipy GIF search returned zero results

- **Problem:** GIF searches such as cat/dog/hello returned no results.
- **Root cause confirmed:** GIF requests were filtered with `png`, although the GIF endpoint supports gif/webp/jpg/mp4/webm.
- **Fix:** use `gif,webp` for GIF searches while preserving `webp,gif,png` for stickers.
- **Result:** FIXED.
- **Evidence:** commit `65af63a1bd30c566ae2da432dd5916d1f0715be5`.

## 2026-10-05 — In-keyboard search lifecycle / panel regressions

- **Problem:** emoji/Klipy search panels could be destroyed or flicker during keyboard element switches.
- **Fix:** guard panel teardown, preserve search-mode state, add transition-state tracking, rebind search listeners/pointer trackers, and fix Klipy Compose lifecycle initialization.
- **Result:** FIXED in source; runtime verification still matters on physical devices.
- **Evidence:** commit `b08e9170dcc618b7a05d920b1b2cb2dfc49a5695`.

## 2026-08-16 — Frosted Glass compatibility crashes

- **Problem:** blur effects crashed on older Android versions and some non-bitmap drawable paths could fail.
- **Fix:** gate native blur to Android 12+, harden gradient drawable cropping, validate decoded background bitmaps, and add theme logging/fallbacks.
- **Result:** FIXED in source.
- **Evidence:** commit `4f2daef1d378e4b7ad9f8cd851dbabf3c5ad3b3`.

## 2026-06-14 to 2026-06-25 — InputConnection / resize / blur stabilization

- The project went through several related input-lag, cursor-query, resize-overlay, and native-blur fixes.
- **Important:** treat these as regression-sensitive areas; any change here should be accompanied by typing, cursor-movement, resize, and blur tests on a real device.
- **Evidence:** commits `a7dde8aea5d83e5c8a739b44b9e5f251dc624e9f`, `c967f4f891c873fdffa368fa99fd5974379b5724`, `f7b11443feb7a5457c955c6c0c539c18c5185d6d`, `7998fad4ebd8b0499d638c89d0e5ea5888dd2a05`.

## How to record a failed experiment

Use the template above even when the change is abandoned. A failed experiment is useful because it prevents repeating the same hypothesis without new evidence.