# FrostKeys / brrrBoard project context

## Resume here

- **Repository:** [Batmancpu/brrrBoard](https://github.com/Batmancpu/brrrBoard), the user's fork of [AshwinSoni-01/FrostKeys](https://github.com/AshwinSoni-01/FrostKeys), based on HeliBoard.
- **Confirmed goals (2026-11-14):** keep a stable signing identity and make the normal phone update flow Obtainium → Refresh → Update. Keep enough repository context that a new chat or agent can resume the same work, including verbatim project requests, changes of direction, and results. [Conversation record](docs/PROJECT_CONVERSATION.md).
- **Product direction supported by repository history:** a personal, privacy-focused Android keyboard fork with cleaner settings and iOS-inspired controls. Earlier PRs explored offline cleanup, geometry, diagnostics and Liquid Glass. PR intent and the status of features on main are documented below. The owner has now set the product requirements in the [BRR Board roadmap](docs/BRR_BOARD_ROADMAP.md).
- **Last app baseline reviewed:** `main` at [`2f1ffec`](https://github.com/Batmancpu/brrrBoard/commit/2f1ffec4369ff7f2e04a17bfb68dce96bfbe5a12), the 2026-10-14 upstream merge. The only other branch reported by GitHub was `jules-board-10408838174702712066` at `2d178e4`, 135 commits ahead and 56 behind that baseline in [the comparison](https://github.com/Batmancpu/brrrBoard/compare/main...jules-board-10408838174702712066).
- **Current status:** the owner reported that the Chrome in-place update from v1.0.3 to [v1.0.4](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.4) worked. CI verified the same pinned signer and package `com.orion.frostkeys.brrrboard` with version codes 3 and 4. The owner did not separately report settings retention. The phone's Obtainium limit led to testing through Chrome.
- **Next action:** verify the signed backup-defaults checkpoint on a fresh install and on an update. Check Frosted Glass in both themes, Hinglish and emoji suggestions, and that existing explicit preferences survive. Continue the layout, offline cleanup and inline voice roadmap.
- **Open decisions:** which historical offline, geometry, diagnostics, and iOS-layout changes to bring into `main`, and in what order. The release test through Obtainium and explicit settings retention confirmation remain open.

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
| Nov 14 | Obtainium run 43584759527 passed enrollment and built the unsigned APK. The signer check failed. The verifier now reads both known apksigner output formats and compares the uploaded keystore certificate separately. |
| Nov 14 | [Run 43585285361](https://github.com/Batmancpu/brrrBoard/actions/runs/43585285361) built, signed, verified and published `brrrBoard.apk` as `v1.0.3`. The feed is paused at this release pending a first install. |
| Nov 14 | The owner confirmed v1.0.3 is installed and chose Chrome for the update test due to the Obtainium limit. Triggered a new signed release from `main` using a workflow comment and a context update. |
| Nov 14 | [Run 43587440323](https://github.com/Batmancpu/brrrBoard/actions/runs/43587440323) signed, verified and published v1.0.4. The comparison to v1.0.3 contains only documentation and a workflow comment. Waiting for confirmation that Android updates 1.0.3 in place. |

| Nov 14 | The owner confirmed the in-place update worked and requested a handoff for Codex that preserves exact project conversation, changing goals, and engineering history. Added the linked conversation record and strengthened the agent instructions. |

| Nov 14 | [Baseline attempt 43612601748](https://github.com/Batmancpu/brrrBoard/actions/runs/43612601748) failed Kotlin compilation because WelcomeWizard and a preview still referenced removed cloud UI. Removed those references. Blocked legacy AI/GIF/sticker key commands and panel-toggle entry points while their source/dependencies are audited. |

| Nov 14 | [Run 43612969896](https://github.com/Batmancpu/brrrBoard/actions/runs/43612969896) built and published [v1.0.6](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.6). CI checked the signer, higher version code, unchanged package and the absence of INTERNET and ACCESS_NETWORK_STATE from the signed APK. The APK carries the BRR Board primary labels and blocks legacy online commands. Device validation and full source cleanup remain pending. |

| Nov 14 | Reviewed the owner's settings backup. Converted reusable preferences to code defaults, bundled the three imported dictionaries as assets, and retired the automatic full-backup restore. Learned typing history, clipboard database, protected and device-specific state, and the personal spacebar caption were excluded. |

## Technical details worth retaining

- **Main APK identity:** `app/build.gradle.kts` declares base ID `com.orion.frostkeys`, debug suffix `.debug`, version name `2.5.8`, and min SDK 24 at the reviewed baseline. CI verifies the persistent debug signing certificate. The audited main manifest still declares `android.permission.INTERNET`. The README still describes upstream FrostKeys capabilities. [Build config](app/build.gradle.kts) · [Manifest](app/src/main/AndroidManifest.xml).
- **Jules APK identity:** the branch build config uses `com.mangoloads.brrrboard.debug` for debug. It is a different Android package from the main debug APK. Cross-build signer and update continuity for older branch APKs remain unverified. [Jules build config](https://github.com/Batmancpu/brrrBoard/blob/2d178e41e7e0166617e243c2cdbb9fcfa1868ffb/app/build.gradle.kts).
- **Main test failure:** `StringUtilsTest.isEmojiDetectsAllAvailableEmojis` failed at line 209, the assertion calling `isEmoji(it)`. The Actions console excerpt shows the assertion location without the failing code points. Assemble, signer verification, and artifact upload succeeded before the test failure. The separate APK publication step was skipped because the gate failed. The older audit's HTTP 403 occurred during a publication attempt. [Test source](app/src/test/java/helium314/keyboard/latin/StringUtilsTest.kt) · [Job log](https://github.com/Batmancpu/brrrBoard/actions/runs/38551096640/job/115808962919).
- **Regression watchlist:** IME startup, live blur and OEM fallbacks, keyboard resize and input lag, emoji/media search lifecycle, clipboard delete/undo/paste and pins. [Known issues](docs/KNOWN_ISSUES.md) · [Experiment history](docs/EXPERIMENT_LOG.md).

## Add a handoff entry

Append one dated entry per work session. Preserve the exact project-relevant user wording in [the conversation record](docs/PROJECT_CONVERSATION.md), with a link back to the related work when useful. Update **Resume here** whenever any of its statements changes. For small fixes, add a short bullet with the affected path, behavior, and verification under that session. Link deep diagnostics from the experiment log.

- **Date and starting point** — branch, starting commit, goal and acceptance criteria.
- **User decisions and plan changes** — previous decision, new decision, reason and remaining questions.
- **Work and attempts** — changed files or symbols, behavior, rationale, failed attempts and reversions.
- **Verification** — exact commands or linked CI runs, outcomes, APK details when relevant and untested behavior.
- **Handoff** — current commit or local changes, blocker, and the next concrete action.
