# brrrBoard Experiment & Fix History

## 2026-11-17 B14V second visual implementation (CI pending)

Owner report: signed v1.0.67 Clipboard looked too similar to Android; black seam from `android:requiresFadingEdge` and cropped/dark cards, verbose empty state, header off-center with redundant Back. Source remedy: replace root with a single layered `FrameLayout` viewport; list scrolls behind fixed title/Clear control and original bottom keys; custom theme-aware alpha gradient rather than Android black fading edge; bottom material stops translucently over list with no async backdrop bitmap; runtime row-height measurement reserves scroll padding and positions Undo independently; empty `No Clips` hides fade. Redesign cards to 16dp light-outline, subtle translucent fill with 5dp min spacing, fitCenter uncropped images, low-contrast pin. Disable ephemeral rich chip memory/disk Coil cache; no change to committed URI/paste rights. Add two pure alpha/color tests in `ClipboardSurfacePolicyTest`. Static diff/XML pass; full unit/packaging/CI, light/dark visual and Reno hardware checks remain pending. No duplicate footer-status lane. Next: guard main head, publish coherent source docs and review both gates; B10 follows.

## 2026-11-17 B14 Clipboard presentation and safety candidate (CI pending)

Starting main `fb9854389` / original-signed v1.0.66. Observed header uses action-tinted 38dp round buttons, an oversized shrug placeholder, image cards sized to available column with no vertical cap, pending Coil loads not cancelled when recycled, long-press pinning with no labelled accessibility action, raw legacy image-marker fallback, a 27dp accent-colored chip close target, and possible stale undo callbacks. Implemented a compact neutral functional-color 44dp header, localized empty text, bounded 112dp image card geometry, recycled Coil cancellation, `replaceAccessibilityAction` pin/unpin, valid adapter-position/id guards, safe invalid-marker labels, rich image-chip thumbnail and a 36dp neutral dismiss button, and cleared transient Undo on timeout/stop. Added `ClipboardCardGeometryTest`. Added `testRunTests` to the existing production signing workflow **before** sign/publish so a test failure cannot become an Obtainium release. `git diff --check` and XML parsing PASS; Android CI and owner device tests NOT RUN. No keyboard/footer/signing identity/editor permission modifications. Next exact action: publish guarded main commit, watch full debug and signer gates, fix any failure based on logs, append APK hash and regression evidence; B10 follows B14. Historical experiments remain below.

## 2026-11-17 B08/B09 source and automated gate result, B10 stopped

**Problem:** Emoji footer shortcut target duplicates short tap and Toolbar/Clipboard navigation fails when already in Emoji; dictionary suggestions evict active system Proton inline autofill chip, center suggestion visually displaced by compound dots. **Source change:** [952c8c4](https://github.com/Batmancpu/brrrBoard/commit/952c8c41d2a49bcc7ae4cbbfc84caae33d3f3e3f), B08 `FooterShortcutPolicy`, `BoardFooterView` direct routes, RTL/root-window arc bounds, cancellation and disabled-history guard; `KeyboardSwitcher` stops outgoing listeners. B09 `SuggestionStripLayoutHelper` removes compound dots and bitmap hint; `SuggestionStripView` accessible More; `AccessoryPriorityPolicy`/`LatinIME` protect provider ownership and reject hidden/stale editor responses. New policy unit tests. **Result:** full debug build/identity/unit task [43940095584](https://github.com/Batmancpu/brrrBoard/actions/runs/43940095584) green steps and signed original-key production [43940095537](https://github.com/Batmancpu/brrrBoard/actions/runs/43940095537) success with [v1.0.66](https://github.com/Batmancpu/brrrBoard/releases/download/v1.0.66/brrBoard.apk), SHA-256 `0fe5f8bc568ba3495cd8624ef1c10efb38f45e5d4044d6f95f9f6923eeaf1e0a`; `git diff --check` passed. **Limitations:** no Oppo validation, no actual Proton callback simulation with account, Emoji duplicate ABC retained for B10; no B10 modification. **Next:** owner checks latest APK using short `AGENTS.md` checklist after waking; ask permission for B10–B12. Historical B05 permission callback (actual provider error open) and B18 centred multicolour transcribing-glow request documented separately, no raw media/secrets logged.

## 2026-11-17 B08/B09 implementation staging before group-two gate

B08 code inspection: existing `BoardFooterView` arc offered redundant Emoji (same as short tap), Clipboard and Settings, used hardcoded popup positions relative to window origin, and `reset()` during every arc release also reset dictation icon despite an active recognizer. `KeyboardSwitcher.onToggleKeyboard` explicitly rejects ACCESS_POINT from Emoji and can hide IME on same-mode Clipboard. Replaced arc's first target with Toolbar; direct switcher calls, history-enabled guard, RTL mirror and root-origin clamped offset, dedicated arc-only dismissal. Added outgoing panel listener cleanup when entering Toolbar. `FooterShortcutPolicyTest` checks mirrored target locations and refusal of ambiguous clipboard action. B10 owns internal Emoji grid and duplicate ABC removal after variant acceptance.

B09 code inspection: center suggested word carried an ellipsis compound drawable with a *negative* padding hack, a direct source cause of vertical optical shift. Removed only hint/bitmap; preserved existing strip hold/up-flick more-panel and added accessibility action. `LatinIME.setSuggestedWords` previously reset the strip while Android inline autofill view was active; priority guard now protects active provider and keeps input logic word state updated behind the provider, with epoch-tagged platform response and cleanup. Synthetic priority/late callback tests added; real Proton callbacks unavailable without owner credentials and no private content logged. Group-two code not yet published/CI-tested at this checkpoint.

## 2026-11-17 B07 action-tint isolation and key radius

Reviewed `KeyboardTheme` OG frosted branch, `KeyboardView.isOgFrostTintedControlKey`, B04 KeyBackgroundRoleCache and `Colors` role maps. Identified intentional accent allocation to Shift/Delete/symbol/numpad and all special/functional backgrounds. Implemented `FrostAccentRoles` pure palette/role policy; wallpaper accent now only on action key roles, neutral modifiers/surface, with bounded default 8dp radius for narrow landscape/resized keys. Added static unit tests. No visual device measurement available, so old B04 tint leak remains owner-confirmed fixed while B07 restricted accent role remains to be checked. First B06+B07 source build/unit/sign/release pending; no font/icon licensing changes. Avoid broad refactor of glyphs until Reno10 photo comparison.

## 2026-11-17 B06 common measured panel body experiment

Inspected `InputView.onMeasure`, `RoundedKeyboardFrameView.onApplyWindowInsets`, `KeyboardWrapperView`, `EmojiPalettesView.onMeasure`, `ClipboardHistoryView.onMeasure`. Both replacement panels independently invented body height; clipboard subtracted hard-coded 6dp and did not check whether shared strip was visible; emoji added padding and 1px. Replaced with shared pure `SharedPanelGeometry.panelBodyHeight`, using parent constraints and accessory visibility consistently, no extra footer or fake navigation inset. Emoji width also caps to parent spec for narrow/one-handed layouts. Synthetic tests check dimensions; CI and actual Reno10 ±1dp/blur/landscape remain unverified. Existing toolbar downward hide cancels child touch and delegates to Android hide, so no speculative animation replacement. Landscape Emoji bottom-cell clipping retained under B10 grid-specific work. B06 source only; owner authorized B07 automatically.

## 2026-11-17 B05 owner video — actual false-permission attribution

Source/repro: attached 7.433s recording `Record_2026-11-17-00-28-32_ae1f93045b87aacac90efbb155289789(1).mp4` (private; not published) shows mic icon turns red at ~3s, then centered permission error at ~4s. Owner explicitly says brrBoard microphone permission already granted; YouTube voice-search Dictate works. `InlineDictation.start` checks `RECORD_AUDIO` before `footer.showDictationActive(true)` makes red. The later callback `SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS` was mapped to the exact same `inline_voice_permission` resource, explaining misleading UI. This identifies **message provenance** but not why Android/Dictate's inline recognizer emitted the callback. Patch maps local gate to user permission guidance and asynchronous error to `Dictate could not start audio capture`, logs numeric code and boolean, with tests. No Internet permission: Dictate owns provider connectivity. If owner logs later show AppOps/attribution or provider-specific failure, pursue B18; do not speculate. Previous b027748 full test/sign CI passed; new patch CI pending. B06–B09 authorized subsequently without overnight phone tests.

## 2026-11-17 — B05 short-tap follow-up (unverified)

Initial main `57191da` passed full Android debug/unit [43929202118](https://github.com/Batmancpu/brrrBoard/actions/runs/43929202118) and signer/release [43929201775](https://github.com/Batmancpu/brrrBoard/actions/runs/43929201775), v1.0.62. Owner states mic short tap does nothing, long press switches IME, YouTube Dictate voice search works. Source inspection: mic touch listener does call `performClick()` on short release, but then sent synthetic VOICE_INPUT into normal key path; inline service required system secure selection to be Dictate, and used a generic recognizer availability gate. Dictate maintainer [issue #359](https://github.com/DevEmperor/DictateKeyboard/issues/359) gives exact canonical `net.devemperor.dictate/dev.patrickgold.florisboard.dictate.recognition.DictateRecognitionService`, distinguishing inline from separate activity/IME voice. This is a hypothesis about ColorOS, not confirmed root cause. Patch dispatches short tap directly; queries installed Dictate RecognitionService only and uses verified selected/canonical/unique service despite unrelated OEM default; removes redundant global availability gate; shows footer status on failure, retaining Toast. Test wrong-provider/ambiguous refusal. No new recording animation. Local wrapper test attempted with `--offline` but Gradle download network unreachable; no tests skipped. Need full CI and owner trial.

## 2026-11-16 — B05 voice failure and Clipboard privacy/typed paste (CI pending)

- **Baseline/authorization:** `430c10d` signed v1.0.61, B04 complete. Owner authorized B05 only.
- **Root causes:** failed Dictate selection launched Android settings automatically on a mic tap. Clipboard history collector bypassed its strip's sensitive-flag/editor policy. Old dismissed-clip preferences saved raw text/URI. Rejecting a stored image returned the same boolean as plain text and pasted its serialization marker into an editor; rich paste errors logged raw URI and did not revoke a manually granted rejected content URI.
- **Proposed remedy:** reason-coded pure Dictate service selection and no tap-launch; explicit About-screen voice configuration; shared private editor policy for collection, history/panel, screenshots and suggestions; purge obsolete raw pref values; bounded in-memory dismissal fingerprint; typed history paste outcome including malformed/file/non-image reject; safe content MIME/URI route and redacted diagnostics, revoke rejected manual grant on supported Android. Preserve existing successful Dictate route and text/image paste.
- **Regression plan:** pure/Robolectric tests for matching/missing/wrong Dictate service, private/no-learning/sensitive clips, raw pref migration and rejected/committed image outcome; run Python tools, full Android CI, original signer/release. No raw user data or credentials used. Not yet verified or released. Next check and commit this B05-only code with context, then CI; do not implement B06.

## 2026-11-16 — B04 closed after full green retry and owner acceptance

- **Outcome:** PASS. The owner confirms the all-key tint bug resolved on Reno10 after B04 signed v1.0.60. The two newly failing tint pixel tests in first debug run 43921741786 were corrected to validate drawable state; follow-up `1e197e7` full Android unit test/build/signature workflow [43924216687](https://github.com/Batmancpu/brrrBoard/actions/runs/43924216687) **SUCCESS**. Original-signed production [v1.0.61](https://github.com/Batmancpu/brrrBoard/releases/download/v1.0.61/brrBoard.apk) workflow [43924216785](https://github.com/Batmancpu/brrrBoard/actions/runs/43924216785) **SUCCESS**, SHA-256 `25c50c2818e07ffea787eac45b2598f43a73f08b7eaa92045003386db320d704`.
- **Scope boundary:** v1.0.61 test/docs only relative to v1.0.60; no Emoji grid or swipe ranking changes. Landscape Emoji clipping remains B06/B10; swipe suggestion errors B17. No subsequent batch implemented. B05 next after one-batch session boundary.

## 2026-11-16 — B04 regression-test rasterization correction

- **Product result:** source `be62fc5`, original-signed production v1.0.60 (run 43921741598 SUCCESS); owner says the persistent all-key tint has been fixed on Oppo Reno10. Treat this as owner-reported acceptance, not developer-controlled device testing.
- **CI regression:** full test run 43921741786 returned 186 tests, 2 failed: `KeyBackgroundRoleCacheTest.coloredControlDoesNotRetintOrdinaryKeysOrTheirSource` at line 47, `themeChangeDiscardsRoleDrawables` at line 60. Both were `assertEquals(expectedColor, pixel(tintedDrawable))` using Robolectric bitmap graphics; other 184 passed. The failure is in unreliable test tint rendering, not a compile or signing failure. Do not disable or skip these tests.
- **Correction:** test colorizer now directly changes isolated `ColorDrawable.color` property; tests compare the drawable's state (source remains unchanged, neutral/accent distinct and reused, theme reset recolors new clone) without assuming Robolectric's pixel pipeline applies tint exactly like hardware. Production cache code unmodified.
- **Next:** run full CI, record green/red result and direct signed release, update project handoff, then close B04 if green given owner's device confirmation. One batch per chat; require explicit next-batch consent.

## 2026-11-16 — B04 frosted key drawable role isolation (pending CI/owner)

- **Build/source:** started `main` `b6f432e` / v1.0.59; one B04-only implementation, no other batch authorized.
- **Problem/evidence:** owner's Reno10 video shows all normal keys becoming persistent warm accent after Chrome text/decimal/numeric/search/URL inputmode switches, persisting through ChatGPT until force-stop. Other user checks: 5 normal↔Emoji↔Clipboard cycles, hide/reopen, normal landscape keys passed; landscape Emoji bottom row clipped (still open B06/B10).
- **Hypothesis:** material-frosted `KeyboardView.onDrawKeyBackground()` called `mColors.setColor()` repeatedly on the shared `Drawable` returned by `Key.selectBackgroundDrawable()`. Mutating that instance for special/accent roles can leak appearance to normal keys as the editor layout changes. No instrumented device trace confirms this as exclusive root cause.
- **Change:** new `KeyBackgroundRoleCache.java` isolates `Drawable.ConstantState.newDrawable().mutate()` by `(source identity, ColorType)` and mirrors pressed state; `KeyboardView.java` draws isolated role instances and calls `clear()` on theme updates. Resource-backed shapes/intrinsic dimensions and non-frosted/iOS drawing branches remain unchanged. For custom drawables without `ConstantState`, the previous behavior remains as a fallback. Added `KeyBackgroundRoleCacheTest.kt` for base-vs-accent isolation, cache reuse and theme invalidation.
- **Verification:** pending `git diff --check`, tool Python tests, full Android CI and production signer check. Local Android Gradle wrapper previously could not obtain distribution (network), so do not record a local Android pass. Pending on-device reproduction after new signed release; do not call resolved from a compile alone.
- **Next:** run all checks, keep main changes atomic, inspect red CI without suppressing tests, obtain signed v1.0.60 if release automation succeeds, then ask owner to repeat Chrome inputmode→ChatGPT scenario. If tint no longer persists, close that B04 defect; track Emoji separately for B06/B10. Stop and explicitly seek permission before B05.

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

## 2026-10-07 — CI unit-test environment failure (JDK 17 vs Android SDK 36)

- **Problem:** the canonical build reached `:app:testDebugUnitTest` but 10/17 tests failed before test bodies ran.
- **Root cause confirmed:** Robolectric's SDK 36 support requires Java 21; the workflow was installing Java 17. The failures all surfaced as `UnsupportedOperationException` from `DefaultSdkProvider.java:170`.
- **Change attempted:** first workflow revision intentionally used JDK 17 because the project compiles source/target at Java 17; CI exposed the separate Robolectric runtime requirement.
- **Result:** FAIL — this was a CI/toolchain configuration bug, not evidence that those ten tests are functionally broken.
- **Evidence:** GitHub Actions run `37573697017`; 17 tests completed, 10 failed; failed classes included ParserTest, SubtypeTest, XLinkTest, SparkleDustViewTest, ClipboardImageHistoryClipTest, InputLogicTest, StringUtilsTest, SuggestTest, KlipyHistoryDaoTest, and ToolbarUtilsTest. All reported `UnsupportedOperationException at DefaultSdkProvider.java:170`.
- **Next step:** rerun the complete build with JDK 21; keep Java/Kotlin source compatibility at 17 unless the compiler itself requires a change.


## 2026-10-07 — APK-only delivery validation

- **Problem:** the normal workflow was blocked by the unit-test gate and GitHub Actions became congested by an in-progress run.
- **Change:** added `.github/workflows/brrrboard-apk-only.yml` so APK packaging/signing/upload is independently verifiable and not blocked by tests.
- **Result:** APK **BUILD + SIGNING + ARTIFACT UPLOAD PASS**; GitHub Release publication failed only because the Actions integration received HTTP 403 for the Releases endpoint.
- **Evidence:** workflow run `37575456832`; APK `FrostKeys_2.5.8-debug.apk`; SHA-256 `1b594eff45b0f9b41c290e4c234cd3a77f46229565de70a07d53d6c66ae9873a`.
- **Next step:** test this APK on the target phone; keep release publication separate until repository Actions permissions allow Releases.

## 2026-10-07 — Subtype cache/test-state investigation

- **Problem:** `SubtypeTest` failed with `IllegalArgumentException` from `.single()` when resource/enabled subtype collections contained more than one entry.
- **Change:** clear `resourceSubtypesByLocale` before rebuilding resource subtype caches; isolate enabled/selected subtype preferences in the test.
- **Result:** APK compiles with the changes; the full regression workflow has not yet completed after these changes, so this is **PROVISIONAL**, not marked fully fixed.

## 2026-10-07 — Emoji detector diagnostic

- **Problem:** `StringUtilsTest.isEmojiDetectsAllAvailableEmojis` still failed under `testRunTests`.
- **Change:** replaced the silent JVM assertion with an assertion that prints the exact failing emoji and code points.
- **Result:** diagnostic patch committed; final detector correction awaits the next successful test run.

## 2026-10-07 — CI workflow permission finding

- **Problem:** `gh release create` returned HTTP 403 `Resource not accessible by integration` even though the workflow has `contents: write`.
- **Result:** GitHub Actions artifact upload works; Release API publication is currently blocked by the connected integration's permissions.

## 2026-11-16 — B15 reproducibility and data-reset regression

- **Starting evidence:** `main` = `cf5c71c624325222a4dbdb53cbfe5b3d86cf45fc`, signed production v1.0.58, prior B15 asset fingerprint `26de07d1825ce75c8121104a97829b6e414aa179fd8cfe322e7e72123c77ff0a`, 5,981 PHINC-derived entries plus 75 reviewed rules. Prior CI 43874232917 passed 183/183 source tests; that result predates the two new B15 tests.
- **Proposed change, without user-data mutation:** `tools/tests/test_prepare_brrpersonal.py` mocks an authored 1,001-row CSV to test column detection and stable bounded TSV generation entirely offline; `app/src/test/java/helium314/keyboard/latin/personalization/PersonalizationHistoryResetTest.kt` uses a synthetic temporary directory to prove the reset deletes only `UserHistoryDictionary` files, not `brrPersonal_v1`, explicitly added personal words, or unrelated data.
- **Command:** `python3 -m unittest discover -s tools/tests -p 'test_*.py' -v` → **PASS, 2/2**; no external corpus or user text read. `git diff --check` → **PASS**.
- **Command:** `./gradlew :app:testRunTestsUnitTest --tests '*PersonalizationHistoryResetTest' --no-daemon` → **BLOCKED BEFORE COMPILATION**, Gradle wrapper cannot download 8.14.4 (`java.net.SocketException: Network is unreachable`). This is neither a passing nor a failing Android assertion. Full CI and test compile are pending if the repository can be dispatched.
- **Scope decision from owner:** no repeatable dictionary/heap/latency/reset measurements on the phone. Normal typing is owner-reported satisfactory; poor swipe suggestions are tracked for B17. No APK is needed for these test/docs changes; no ranking, dictionary data, signing or package changes.
- **Next:** commit source checks and continuity records in one main checkpoint, verify remote content, and obtain full CI if available. If CI is blocked, leave B15 only partially verified and hand off the exact obstacle. Do not begin another batch.
