# brrBoard engineering handoff

## Latest handoff — 2026-11-16 B04 editor tint isolation (CI/phone verification pending)

Baseline `main` `b6f432e`/signed v1.0.59; full B15 source test workflow 43917170656 and production release workflow 43917170723 succeeded. B15 is owner-accepted complete, with normal daily typing reported satisfactory. B01/B02/B03/B15 complete, B00/B04 partial, all B05–B14 and B16–B19 pending. User specifically authorized B04 and no subsequent batch.

Reno10 owner-tested: five normal↔Emoji↔Clipboard cycles, hide/reopen and field switches good; normal landscape keys and height good. **Unresolved:** landscape Emoji row is obscured by the footer (B06/B10, horizontal scroller cannot vertically reveal it); all ordinary keyboard keys become persistently orange after Chrome inputmode test fields and stay tinted in ChatGPT until force-stop. Owner supplied video; sanitized descriptions only, no private media committed. Ordinary swiping works but suggests poor words (B17).

New B04 source proposal isolates shared mutable material-frosted key backgrounds in `KeyBackgroundRoleCache` keyed by base drawable identity and color role; `KeyboardView` applies a role-specific clone, synchronizes pressed/locked state and invalidates clones after theme updates. Unit tests exercise role isolation across editor-like transitions and theme reset. This does not redraw Emoji, change geometry or modify the planned B07 tint palette. **Do not call the device bug fixed until full Android CI and owner reproduction on the released APK.** If green, publish the next original-signer in-place release. Owner checklist: (1) update through Obtainium; (2) repeat Chrome text/numeric/search/URL then open ChatGPT—letters stay dark/neutral; (3) normal/Emoji/Clipboard cycles and hide/reopen; (4) optional screenshot if tint recurs. B04 may be closed after feedback; next dependency-safe implementation B05, only with explicit owner permission. Run `git diff --check` and Python generator tests locally; Android full gate in Actions as local Gradle wrapper download was previously network-blocked. Record exact new commit, workflow runs and APK URL in a follow-up handoff note; never erase earlier logs.

## Start here

## Active checkpoint — B04 and B15 partial; source CI green (2026-11-16)

B04 source checkpoint `2be4e5494d54632517e5aa5fb3ddfdc17793277e` passed CI. `KeyboardSwitcher` now guards delayed animation end actions with a monotonic generation and target identity; `LatinIME` invalidates pending transitions at window hide and input-view start/finish; direct Emoji↔Clipboard switches stop the outgoing panel listener/search state. Two pure guard tests were added. CI [43871586879](https://github.com/Batmancpu/brrrBoard/actions/runs/43871586879) passed build, debug-signature verification and the full test task; audit build [43871586907](https://github.com/Batmancpu/brrrBoard/actions/runs/43871586907) passed and skipped release. Debug test prerelease [brrrboard-test-20261116-130630-2be4e54](https://github.com/Batmancpu/brrrBoard/releases/tag/brrrboard-test-20261116-130630-2be4e54), APK SHA-256 `e127696ca0d03db03584e68966dcf3012919d1f2624d8f96cbe5ae56b252d44c`.

B04 remains **partial**: no OPPO Reno10 access, so rapid alpha→Emoji→Clipboard→alpha, hide/reopen, editor change/restart, rotate, hardware attach, background, landscape and real nav inset/footer measurements are unverified. Main remains `7ab4bc5`; stable v1.0.57 unchanged. The audit-only automatic release is disabled pending repository permission review; test build publishing uses the passing debug workflow.

B15 source/CI checkpoint `c229f9d` passed build, signing, generated dictionary fingerprint, and all 183 tests. It remains partial pending the R3 accuracy/latency/heap benchmark and Reno10 reset acceptance. Next: define disjoint R3 phrases; capture cold/warm metrics on device; verify the history reset preserves brrPersonal and explicit Android words. B16 is blocked until these baseline targets exist. B04 Reno10 fast-panel/lifecycle/inset checks remain open; B05–B14 remain dependency-gated.
## Active checkpoint — B02 complete; B03 in progress (2026-11-16)

B02 is complete on audit branch `brrrboard-audit-b01-currency-20261116`. Starting checkpoint `2df304cdcd98b7f08bb912f8974fcd93631b24d5`; ending source/checkpoint commit `e65b62185a6a32a96defc8c3e1ab17a009eae31f`. It pins URL/autospace test policies explicitly and fixes InputLogic's URL composition resume when a pending phantom space follows a URL connector. The change allows resume with NONE or PHANTOM space state and skips automatic spacing after composition resumes. Production defaults and test expectations remain unchanged.

CI run [43858699048](https://github.com/Batmancpu/brrrBoard/actions/runs/43858699048), job `133269541469`, built, signer-verified and uploaded the debug APK; all 181 tests completed with one remaining failure: `StringUtilsTest.isEmojiDetectsAllAvailableEmojis`. All seven B02 URL/autospace tests now pass. Test XML artifact `14221548884` was uploaded. `git diff --check 2df304c..HEAD` passed. The first B02 source transfer accidentally included a truncation marker; recovery `33afb1f` restored full files and verified their Git blob hashes. Local Gradle remains unavailable (8.14.4 download network error); the scratch execution environment is currently unavailable. `adb` is absent, so B00 Reno10 checks remain pending. Next: B03, inspect the Emoji test failure's exact code point and isolate ICU grapheme boundary vs regex/version mismatch, then patch and run full CI.


**Architecture research is complete.** This mission authorized only documentation commits. A later engineer should verify the owner request, main tip, installed version and this checkpoint. The [implementation master plan](MASTER_IMPLEMENTATION_PLAN.md) gives the next exact action.

| Checkpoint | State | Resume |
| --- | --- | --- |
| R0 — Repository, scope and evidence baseline | Recorded below | R1: read runtime and test paths at pinned source SHA |
| R1 — Source architecture and test triage | Published in this checkpoint | [SOURCE_ARCHITECTURE_AUDIT.md](SOURCE_ARCHITECTURE_AUDIT.md) |
| R2 — Reno10, iOS references and visual system | Published in this checkpoint | [RENO10_VISUAL_SPEC.md](RENO10_VISUAL_SPEC.md) |
| R3 — Dictionaries, Smart Emoji and glide | Published in this checkpoint | [LOCAL_TYPING_ENGINE.md](LOCAL_TYPING_ENGINE.md) |
| R4 — Clipboard, inline autofill, Dictate and lifecycle | Published in this checkpoint | [IME_PROVIDER_INTEGRATIONS.md](IME_PROVIDER_INTEGRATIONS.md) |
| R5 — Ordered implementation and acceptance plan | Published in this checkpoint | [MASTER_IMPLEMENTATION_PLAN.md](MASTER_IMPLEMENTATION_PLAN.md) |
| R6 — Final handoff verification | Complete | [Verification and readiness](#r6--verification-and-readiness) |

## B00 — Implementation baseline (2026-11-16)

- The owner has now authorized implementation of the full 20-batch plan and requests autonomous work during a 4–5 hour absence. This supersedes the earlier planning-only authorization stated in R0–R6.
- Start commit was `cb1a35d6b506b2214bb19e56d62e50d8b80e48b6`; local `main` matched `origin/main`, was clean, and the diff from `3ee9da9e9b2a0b4a8c42b1e27d93f4686ff65b21` is documentation only (11 Markdown files, 457 insertions). Repository branches were `main`, `codex/incognito-voice-guard-20261115`, `codex/reno10-ime-boundary-20261115`, and `jules-board-10408838174702712066`; no open PR was found.
- Latest release remains signed [v1.0.57](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.57), APK 37,949,189 bytes, SHA-256 `a57da3ad314f4f6e21dd64cf18996c5e215134d631d93f4a3a396e75b32bdbfd`.
- Latest debug job [133178974688](https://github.com/Batmancpu/brrrBoard/actions/runs/43831906039/job/133178974688) succeeded through APK build, signer verification and artifact upload, then failed the test gate: 180 tests, 13 failures (five `ParserTest` currency, seven `InputLogicTest` URL/autospace, one `StringUtilsTest` Emoji). Debug publication was skipped. No device acceptance was reported.
- Local `./gradlew :app:testRunTestsUnitTest` was attempted under JDK 21.0.13 and stopped before Gradle configuration because Gradle 8.14.4 was not cached and Java could not reach `services.gradle.org` (`java.net.SocketException: Network is unreachable`). `adb devices -l` could not run because `adb` is not installed. These are environment limitations, not application/test results.
- B00 is **partial**: repository/release/CI/concurrency review complete; Reno10 configuration, host-app interaction matrix and device baseline remain owner-device work. B01 has since completed on the separate audit branch and is recorded below. Do not claim the suite is locally green. Keep unverified code off release-triggering `main`; record each source checkpoint in all four required continuity documents.

## B01 — Currency fixture result (2026-11-16)

Starting from `7ab4bc570b18ad79bd5e4d8761ae910f10de2174`, source commit `e4a4a01ce13edf7020335cb802c4687c165db841` on audit branch `brrrboard-audit-b01-currency-20261116` changes only `app/src/test/java/helium314/keyboard/KeyboardParserTest.kt` plus the required continuity files. It uses a Locale.US subtype, a blank fixture override, restores previous preference state in teardown, clears locale cache, and adds a configured-₹ check. Product defaults/runtime code are unchanged. CI run [43848172243](https://github.com/Batmancpu/brrrBoard/actions/runs/43848172243), job [133233579041](https://github.com/Batmancpu/brrrBoard/actions/runs/43848172243/job/133233579041), built, signer-verified and uploaded the debug APK; 181 tests completed, five parser/currency failures resolved, and eight known InputLogic/Emoji failures remain. Test gate failed, so debug prerelease was skipped. Local tests remain blocked by unavailable Gradle 8.14.4. B01 is complete; next B02 isolates URL/autospace fixture policy and reruns focused/full CI.

## R0 — Repository baseline on 16 November 2026

- GitHub repository [Batmancpu/brrrBoard](https://github.com/Batmancpu/brrrBoard), ID 1358166218, public, default branch main. GitHub connection reports read and push access. Git and API both resolved source tip [3ee9da9](https://github.com/Batmancpu/brrrBoard/commit/3ee9da9e9b2a0b4a8c42b1e27d93f4686ff65b21), committed at 14:01 IST. The previous source change was [8463fc8](https://github.com/Batmancpu/brrrBoard/commit/8463fc80c1f8695c0f0b11075db25d5e0d3434db), with the intervening 9408a43 documentation checkpoint. No running or queued workflow was visible at discovery. Recent commits show an earlier implementation burst. A live editor or unpushed work cannot be observed. Recheck main and workflow activity before every update.
- Latest signed tag [v1.0.57](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.57), published 14:05 IST by [Obtainium run 43831906150](https://github.com/Batmancpu/brrrBoard/actions/runs/43831906150). Release asset 37,949,189 bytes, SHA-256 a57da3ad314f4f6e21dd64cf18996c5e215134d631d93f4a3a396e75b32bdbfd (GitHub asset metadata). No owner-device acceptance is known for this version.
- [Debug run 43831906039, job 133178974688](https://github.com/Batmancpu/brrrBoard/actions/runs/43831906039/job/133178974688) compiled, verified the debug signer, and uploaded an artifact. Debug APK SHA-256 ee16d7015324dfc54ec5eb0e0968bdbd6d7daab0ee815ae66901148ed90bc40d. The test gate failed 13/180 (5 Parser currency, 7 InputLogic URL/autospace, 1 StringUtils Emoji); the subsequent debug publication step was skipped. Read the actual assertions in R1. The existing release and debug pipelines make distinct promises.
- Current branch inventory: main, codex/incognito-voice-guard-20261115, codex/reno10-ime-boundary-20261115, and jules-board-10408838174702712066. All six listed issues were closed PRs. No branch or tag changed for discovery. Checkpoints use a fast-forward Markdown-only commit and GitHub's commit skip marker because debug push CI includes a publish step and has no path filter. Obtainium's main push filter excludes these documentation files.
- Product target: bring the existing FrostKeys-derived Android IME toward a cohesive iPhone-like typing experience on the owner’s gesture-navigation OPPO Reno10 5G. Keep local typing fast and stable and keep the established frosted-glass identity. The separate experimental Liquid Glass keyboard has its own scope. OPPO India lists CPH2531 at 1080 × 2412 physical pixels with up to 120 Hz screen refresh. The installed OS build, logical density, font scaling, actual refresh and live IME insets require owner-device capture. [OPPO specifications](https://www.oppo.com/in/smartphones/series-reno/reno10/specs/)
- Evidence labels used in this handoff: source inspected at the pinned SHA, workflow/release verified, owner-reported behavior, inherited media description, official reference, and proposal. A reported symptom and a plausible code path are separate claims. No new APK or device run was authorized.
- The supplied mission text ends during F6. The coverage ledger will include the complete visible A–F6 sections and known continuation topics from the repository and earlier owner messages. Unseen wording requires later reconciliation if supplied. The inherited C1–C11 media descriptions are the visual brief. Only sanitized observations belong in this public repository.

## Existing records

- [AGENTS.md](../AGENTS.md) and [PROJECT_CONTEXT.md](../PROJECT_CONTEXT.md#resume-here) are agent entry points.
- [Conversation record](PROJECT_CONVERSATION.md) preserves owner decisions and requests in chronological order.
- [Post-v1.0.19 plan](POST_V1_0_19_RENO10_PLAN.md) contains earlier implementation sequencing and many per-batch handoffs. The status table above tracks the current research.
- [Behavior audit](IOS27_BEHAVIOR_AUDIT.md), [iOS/Reno10 plan](IOS27_RENO10_PLAN.md), and [historical roadmap](BRR_BOARD_ROADMAP.md) contain earlier product decisions.
- [Known issues](KNOWN_ISSUES.md) and [experiment log](EXPERIMENT_LOG.md) retain failures, reversals and acceptance gaps.
- [README](../README.md), [layout documentation](../layouts.md), [dictionary provenance](../assets/dictionaries/README.md), and [Obtainium delivery](OBTAINIUM.md) describe distribution and extension points.
- No root context.md or separate research master plan was present in the tracked tree at R0. The linked history remains the record of previous attempts.

## R6 — Verification and readiness

| Checkpoint | Durable commit on main |
| --- | --- |
| R0 repository and CI baseline | [3a1122b6](https://github.com/Batmancpu/brrrBoard/commit/3a1122b655f01d10f61aae012ee83e332931bdeb) |
| R1 runtime paths and test triage | [3c14a5df](https://github.com/Batmancpu/brrrBoard/commit/3c14a5df5681c8636cf2bc0dd4c2b3e2f25a7f15), with conversation record restored by [894c6f5d](https://github.com/Batmancpu/brrrBoard/commit/894c6f5d66feb45e2a3dc411e099f89979bf90fd) |
| R2 media and Reno10 geometry | [3300da01](https://github.com/Batmancpu/brrrBoard/commit/3300da01f1dfef60ef34aa02e3fe84ad3aa3caa7) |
| R3 dictionaries, contextual Emoji and glide | [9d594a41](https://github.com/Batmancpu/brrrBoard/commit/9d594a41d460014fcc39d8ae6ef23d7c3fb3b48b) |
| R4 Clipboard, Android inline autofill and Dictate | [586a1830](https://github.com/Batmancpu/brrrBoard/commit/586a18306551a14bb99d7ed768accb4af6851f61) |
| R5 implementation sequence with 20 batches | [b8f7c8a0](https://github.com/Batmancpu/brrrBoard/commit/b8f7c8a004055615a2c6383adf33a506d7c52a3b) |

- The R1 transfer temporarily wrote an empty PROJECT_CONVERSATION.md blob. The full file was restored in the immediate forward commit using the intact checkout and verified against its predecessor. Each following remote tree was checked against local blob hashes before moving main. All original nonblank lines in AGENTS.md, PROJECT_CONTEXT.md, POST_V1_0_19_RENO10_PLAN.md and PROJECT_CONVERSATION.md remain present in their original order. No forced ref update or rewritten history was used.
- Git diff against 3ee9da9 contains Markdown additions and modifications only. The application, assets, Gradle config, tools, workflows and tag history were outside the changes. Relative links and batch identifiers were validated. GitHub's latest release and workflow lookups remained v1.0.57 and the original source jobs after R5. The architecture phase intentionally did not run tests, build packages or trigger releases.
- **Ready for a later authorized implementation session.** Read the [master plan](MASTER_IMPLEMENTATION_PLAN.md#session-recovery-and-the-next-exact-action), revalidate main, and start with B00 and the B01–B03 test repairs. The reported 13/180 baseline, actual Reno10 geometry, latest Android/ColorOS and Dictate versions, English-only device results, final visual acceptance and default glide qualification all remain open. The source claim and device claim for each item are separately tracked in the [feature ledger](MASTER_IMPLEMENTATION_PLAN.md#reconciled-feature-and-issue-ledger).
- Input coverage includes the whole supplied brief through its F6 cutoff, consulted project history and five directly reviewed media files. Exact unseen text after F6 and several other stills' iOS provenance remain unverified. The inherited brief is preserved as requirements with explicit provenance.


## 2026-11-16 — B15 baseline dictionary and reset audit (in progress)

Starting source: `6048c2280acb6859fbf8ca3e34e114321546834c`; branch `brrrboard-audit-b01-currency-20261116`. B15 is independent of B04’s pending Reno10 panel checks and B05–B14. This checkpoint verifies source/provenance and adds a CI fingerprint for the exact generated brrPersonal asset. No locale catalog, dictionary frequencies, suggestion ranking, or reset behavior changed.

The build-time generator `tools/prepare_brrpersonal.py` pins PHINC revision `44b5471df75e508b9d6fcdbcc93df04bb1e84056`; `NOTICE` identifies CC BY 4.0, the source paper, transformations, and that the corpus is not bundled. Generation downloads the pinned CSV only during Gradle build, emits a sorted bounded TSV, and the app loads it locally through the existing native expandable dictionary. The added workflow step checks the asset is nonempty and reports data-row count and SHA-256 after assembleDebug, making output reproducible from CI evidence. Fingerprint is pending the triggered run.

The catalog remains 19 main language dictionaries plus existing Emoji dictionaries; available locale behavior is unchanged. brrPersonal loads for English or Romanized Hindi; explicit Android personal words are separate. Existing reset code clears only TYPE_USER_HISTORY / learned history and leaves brrPersonal and Android’s explicit personal dictionary intact. No data was cleared or rewritten.

Status: PARTIAL pending workflow fingerprint, language-specific cold/warm latency and accuracy benchmark, and Reno10 device behavior. The local runtime and phone are unavailable in this session; no latency/quality numbers are claimed. Next exact action: read the generated TSV fingerprint from the triggered Actions run, then establish the disjoint R3 phrase set and capture cold/warm latency before any frequency or ranking change. If the run fails, diagnose its log before continuing.
For the next action and current implementation status see the dated checkpoint in this file and the master plan.
