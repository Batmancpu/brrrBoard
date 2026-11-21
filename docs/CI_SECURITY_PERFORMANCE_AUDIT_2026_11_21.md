# CI security and efficiency audit — 21 November 2026

## Owner direction and coordination

The owner requested a five-minute GitHub Actions observation period before any writes, deletion of genuinely obsolete workflows, removal of redundant build/test work, and safe security/privacy/runtime-efficiency investigation without discarding necessary tests, app functionality, or signing continuity. On 2026-11-21 04:44–04:49 UTC, 11 Actions polls over 301 seconds observed **no queued or active runs**. The only remote branch was `main` at `c343761a25b1758fd045674cd828954ef728101f`. The original local checkout held an unrelated unpushed privacy-hardening commit; a detached clean worktree from current `origin/main` preserved it without creating a branch or force-pushing.

## Measured baseline (pre-change, from Actions step timings)

| Workflow | Run | Total job | Build | Android unit suite |
| --- | --- | --- | --- | --- |
| Debug | [#314](https://github.com/Batmancpu/brrrBoard/actions/runs/44863519421) | 6.52 min | 3.33 min | 2.45 min |
| Signed Obtainium | [#204](https://github.com/Batmancpu/brrrBoard/actions/runs/44863519280) | 5.05 min | 2.22 min | 1.90 min |
| Signed Obtainium | [#203](https://github.com/Batmancpu/brrrBoard/actions/runs/44797992164) | 8.85 min | 5.10 min | 2.68 min |

These are completed run observations, **not** a controlled before/after benchmark. The signed workflow also waited for the exact same commit's debug workflow to pass, and both were previously running the same `testRunTests` command. Saved billable runner work may exceed or differ from saved wall-clock release latency because the two workflows run concurrently.

## Narrow, safe implementation

1. **Delete `.github/workflows/brrrboard-apk-only.yml`.** It only listened for `brrrboard-audit-*` branches, none of which exist; it duplicated debug compilation and had broad `contents: write` access. Its historical code remains recoverable in Git history.
2. **Debug only on `main` push or explicit dispatch.** Remove the dead audit-branch trigger from the remaining independent debug workflow.
3. **Run the complete Android unit suite once per source commit.** Preserve `./gradlew --no-daemon --stacktrace testRunTests` in the debug workflow. Omit its duplicate execution in signed Obtainium, whose existing exact-SHA, status-completed, conclusion-success debug gate remains immediately before signing. If debug unit tests fail, signed APK publication still fails closed. All signed workflow-specific Python, offline model, manifest/network/privacy, certificate, versioning and APK verification gates remain.
4. **Add `tools/tests/test_ci_workflow_contract.py`** and run it early in debug CI. Its three tests protect the workflow inventory, single Android-suite invocation, and exact-SHA original-signer gate order. This catches accidental gate weakening before spending time on compilation.

## Confirmed private diagnostic logging (same narrow safety batch)

The source review also found actual plaintext interpolation into brrBoard's `Log` wrapper, which records lines both in Android logcat and a resident in-app history. Sites included an unguarded editor-echo mismatch, debug typed-word/ngram output, dynamic dictionary mutation failures, Contacts/app-name dictionary trace messages, spellcheck input, blacklist words, swipe suggestion objects, clipboard image URIs, custom key labels and sticker names. The public `dumpAllWordsForDebug()` could enumerate and print every word, including private learned terms.

These messages are now static sanitized event descriptions; their functional code paths, stored data, diagnostics timing, learning and prediction results are otherwise unchanged. The dictionary dump entry point remains callable but refuses to reveal individual words or scan and stringify the entire dictionary. A new early `tools/tests/test_private_log_contract.py` guards the known leakage patterns and the disabled plaintext dump. This reduces log-string work and the risk of sensitive data persisting in Android/in-app diagnostics, but it is **not** a claim that a whole-app data-flow audit is complete. Real voice/clipboard/IME runtime acceptance must still be verified.

## Required validation and follow-up

### First remote attempt and recovery

The first atomic source push reached remote commit `b5d5e82c47916bbbe659921d8fb8967910400b58`. Debug Actions [#315](https://github.com/Batmancpu/brrrBoard/actions/runs/44869383480) failed the existing early **Require per-release change notes** gate because this batch initially omitted `docs/releases/notes.json`. All newly added CI/privacy/brand/settings static gates passed. Signed Actions [#205](https://github.com/Batmancpu/brrrBoard/actions/runs/44869383477) built its unsigned APK and passed its signed-specific static checks, but correctly refused original signing because the exact-SHA debug run failed. This is a release-process mistake, not evidence of an Android compilation failure.

Recovery: update the notes to describe **only changes after signed v1.0.204** and include `docs/releases/notes.json` in the signed workflow's push paths, so a notes-only repair triggers both debug and signed CI. Preserve the fail-closed exact-SHA debug gate. Retest and monitor the corrected commit to terminal success before claiming a signed update.

- Run new static CI contract tests, existing brand/privacy tests, YAML parse checks, `git diff --check`, and Android unit tests/build when tools permit. **Do not claim complete Android verification from Python tests alone.**
- After updating `main` only if its head remains unchanged and no other Actions job is active, require both the exact-SHA debug build/full unit suite and original-signed Obtainium workflow to finish successfully. Record final evidence accurately; if either fails, investigate and repair, never weaken the gate.
- Compare post-change signed build/test step timings over several commits; do not invent a speedup from a single run or assume a task is cacheable.
- Defer riskier opportunities until profiled and regression-tested: time-varying debug `versionCode` (changes installable upgrade semantics), `ndkBuild -j1` (OOM/native stability), untracked 16 KB ELF alignment, configuration-cache portability and encryption, and third-party Action SHA pinning. Gradle build/configuration/parallel caching is already enabled in `gradle.properties`; `gradle/actions/setup-gradle@v4` is already used. Preserve native ABI coverage and the 16 KB patch.
- Runtime security/privacy audit continues separately: the latest isolated GTCRN voice path, keyboard input/clipboard/gesture traces, permission-free offline microphone behavior, lifecycle/memory/crash handling, and expensive rendering or prediction hot paths should be evaluated with suitable tests and real-device evidence before modifications. This CI batch makes **no Android production code or signing-key changes**.

## Acceptance boundaries

Keep `com.mangoloads.board`, the permanent original signer, strict certificate pin, no INTERNET / ACCESS_NETWORK_STATE permissions, existing model hashes, all language dictionaries, 16 KB native alignment, all other test gates, same-commit debug verification, and owner-facing debug/test APK ability. Existing hourly automation may make independent changes; never force-push or clobber a newer `main`. Hardware and full Android validation remain separately tracked.
