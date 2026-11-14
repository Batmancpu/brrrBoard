# FrostKeys / brrrBoard project context

## Resume here

- **Repository:** [Batmancpu/brrrBoard](https://github.com/Batmancpu/brrrBoard), the user's fork of [AshwinSoni-01/FrostKeys](https://github.com/AshwinSoni-01/FrostKeys), based on HeliBoard.
- **Current goal (2026-11-14):** install and update the personal fork through Obtainium and keep the same private signing identity across releases. Maintain this repository handoff.
- **Historical product goal:** build a personal, privacy-focused keyboard fork with reliable APK updates, cleaner settings and iOS-inspired controls. Historical milestones also explored offline cleanup, keyboard geometry, diagnostics and Liquid Glass. Current product scope beyond this handoff is open.
- **Last app baseline reviewed:** `main` at [`2f1ffec`](https://github.com/Batmancpu/brrrBoard/commit/2f1ffec4369ff7f2e04a17bfb68dce96bfbe5a12), the 2026-10-14 upstream merge. The only other branch reported by GitHub was `jules-board-10408838174702712066` at `2d178e4`, 135 commits ahead and 56 behind that baseline in [the comparison](https://github.com/Batmancpu/brrrBoard/compare/main...jules-board-10408838174702712066).
- **Current status:** the owner entered the three signing values as Actions secrets. The first retry failed at enrollment because the workflow read the fingerprint only from Actions Variables. The workflow now also accepts it from Secrets. First signed release pending [Actions verification](https://github.com/Batmancpu/brrrBoard/actions/workflows/obtainium-release.yml).
- **Next action:** verify publication of the first signed APK and wait for the owner to confirm installation through Obtainium. Start a fresh workflow run on `main` for the second APK, which must use the same signer/package and a higher version code. The separate debug emoji test is still open.
- **Open decisions:** whether to carry selected offline, geometry, diagnostics or iOS-layout changes into `main`; The historical stable-update goal requires a compatible package ID, signer and increasing version code. Physical-device validation remains open.

## What happened

| Date | Work and outcome |
| --- | --- |
| Oct 4–5 | [PR #1](https://github.com/Batmancpu/brrrBoard/pull/1) proposed the Mangoloads brrrBoard milestone: offline cleanup, removal of network/AI/Klipy surfaces, cleaner settings, refreshed microphone and Return icons, and repeatable debug signing. Closed without merging. Deeper source cleanup and Liquid Glass were deferred in the PR. |
| Oct 5–6 | [PR #2](https://github.com/Batmancpu/brrrBoard/pull/2) tried root keyboard geometry fixes and a manual milestone APK pipeline. [PR #3](https://github.com/Batmancpu/brrrBoard/pull/3) built on that work with padding fixes and diagnostic ZIP export. Both closed without merging. PR #3 reported an assembled APK and baseline test parity; physical-device verification was pending. The diagnostics classes are absent from the audited `main` tree. |
| Oct 7 | [PR #4](https://github.com/Batmancpu/brrrBoard/pull/4) merged the APK delivery audit, persistent debug signing, version-code collision hardening, subtype cache/test isolation and durable logs. An early test run exposed the Robolectric SDK 36 requirement for JDK 21. APK-only builds produced signed artifacts, and the audit recorded a historical Releases API 403. [Experiment log](docs/EXPERIMENT_LOG.md). |
| Oct 7 | [PR #5](https://github.com/Batmancpu/brrrBoard/pull/5) merged versioned debug release tags and described publishing even if tests failed. At the audited main tree, `Fail workflow when tests fail` comes before `Publish GitHub release`, so the actual workflow skips publication after a test failure. [Workflow](.github/workflows/brrrboard-build-debug.yml). |
| Oct 8 | [PR #6](https://github.com/Batmancpu/brrrBoard/pull/6) tried iOS 27-style defaults, including bottom-row Emoji and input-method switching keys and revised dimensions/scaling. It closed without merging. The Jules branch later published [a branch APK](https://github.com/Batmancpu/brrrBoard/releases/tag/jules-apk-2d178e4). |
| Oct 14 | [`2f1ffec`](https://github.com/Batmancpu/brrrBoard/commit/2f1ffec4369ff7f2e04a17bfb68dce96bfbe5a12) merged upstream FrostKeys updates including scoped screenshot-folder access, clipboard image handling, 16 KB native-library work and dictionary loading guards. The following [main run](https://github.com/Batmancpu/brrrBoard/actions/runs/38551096640) reached the test gate. |
| Nov 14 | Reviewed repository identity, live branches, PRs #1–#6, current workflow, existing engineering logs, release metadata, and the last main build's job log. Added this handoff and the root agent instructions. New Android builds, device installs and updates were outside this documentation task. |

| Nov 14 | Added a dedicated Obtainium build/package, main-only signing and release workflow, and owner enrollment script. The legacy debug key is public, so the new app uses a separate package and private signer. The workflow run number drives version code, version name and release tag. Enrollment and device verification are pending. |

| Nov 14 | Retried the Obtainium workflow after the owner added all three signing values under Actions Secrets. Enrollment failed because the fingerprint was previously read only from Variables. Added support for the fingerprint as an Actions secret. Wait for the first install before publishing the update test. |

## Technical details worth retaining

- **Main APK identity:** `app/build.gradle.kts` declares base ID `com.orion.frostkeys`, debug suffix `.debug`, version name `2.5.8`, and min SDK 24 at the reviewed baseline. CI verifies the persistent debug signing certificate. The audited main manifest still declares `android.permission.INTERNET`. The README still describes upstream FrostKeys capabilities. [Build config](app/build.gradle.kts) · [Manifest](app/src/main/AndroidManifest.xml).
- **Jules APK identity:** the branch build config uses `com.mangoloads.brrrboard.debug` for debug. It is a different Android package from the main debug APK. Cross-build signer and update continuity for older branch APKs remain unverified. [Jules build config](https://github.com/Batmancpu/brrrBoard/blob/2d178e41e7e0166617e243c2cdbb9fcfa1868ffb/app/build.gradle.kts).
- **Main test failure:** `StringUtilsTest.isEmojiDetectsAllAvailableEmojis` failed at line 209, the assertion calling `isEmoji(it)`. The Actions console excerpt shows the assertion location without the failing code points. Assemble, signer verification, and artifact upload succeeded before the test failure. The separate APK publication step was skipped because the gate failed. The older audit's HTTP 403 occurred during a publication attempt. [Test source](app/src/test/java/helium314/keyboard/latin/StringUtilsTest.kt) · [Job log](https://github.com/Batmancpu/brrrBoard/actions/runs/38551096640/job/115808962919).
- **Regression watchlist:** IME startup, live blur and OEM fallbacks, keyboard resize and input lag, emoji/media search lifecycle, clipboard delete/undo/paste and pins. [Known issues](docs/KNOWN_ISSUES.md) · [Experiment history](docs/EXPERIMENT_LOG.md).

## Add a handoff entry

Append one dated entry per work session. Update **Resume here** whenever any of its statements changes. For small fixes, add a short bullet with the affected path, behavior, and verification under that session. Link deep diagnostics from the experiment log.

- **Date and starting point** — branch, starting commit, goal and acceptance criteria.
- **User decisions and plan changes** — previous decision, new decision, reason and remaining questions.
- **Work and attempts** — changed files or symbols, behavior, rationale, failed attempts and reversions.
- **Verification** — exact commands or linked CI runs, outcomes, APK details when relevant and untested behavior.
- **Handoff** — current commit or local changes, blocker, and the next concrete action.
