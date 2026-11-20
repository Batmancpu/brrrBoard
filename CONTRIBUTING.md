# Contributing to brrBoard

Thank you for wanting to improve **brrBoard**, an Android keyboard maintained by MangoLoads. brrBoard builds on FrostKeys, HeliBoard, OpenBoard and AOSP LatinIME; the upstream projects and their licenses are credited in the [main README](README.md). Please send brrBoard-specific changes to **this** repository, not the upstream HeliBoard tracker.

## Before starting

1. Read the [README](README.md), [privacy policy](PRIVACY_POLICY.md), [known issues](docs/KNOWN_ISSUES.md) and [current engineering handoff](PROJECT_CONTEXT.md).
2. Look for similar changes in the current repository history. Public Issues may be disabled; if no discussion channel is available, keep your proposal narrowly scoped and clear.
3. Aim for one fix or feature per change. Avoid broad unrelated refactors of the input engine.
4. Be especially cautious with keyboard latency, editor lifecycle, Clipboards, passwords/incognito, Android IME permissions and offline dictation.

## Getting the source

Clone **this project** and open it in Android Studio or use a compatible Gradle/JDK environment:

```bash
git clone https://github.com/Batmancpu/brrrBoard.git
cd brrrBoard
./gradlew :app:assembleDebug :app:testDebugUnitTest
```

The Gradle wrapper is the versioned build entry point. An Android SDK and the JDK required by the current Android Gradle Plugin are needed. The repository's [debug CI](https://github.com/Batmancpu/brrrBoard/actions/workflows/brrrboard-build-debug.yml) is the reference for the exact toolchain and test command; use the current workflow rather than hard-coding a stale Java version.

The production Obtainium APK is **original-signed** in a protected GitHub Actions workflow. Contributors must never paste signing secrets into source, commit keystores/passwords or change the production package `com.mangoloads.board` to make a local build pass. Local debug APKs are not release APKs.

## Useful code landmarks

- `app/src/main/java/helium314/keyboard/latin/LatinIME.java` — Android IME/editor lifecycle.
- `InputLogic`, `Suggest`, `RichInputConnection`, dictionary services — text input and suggestions.
- `app/src/main/java/helium314/keyboard/settings/` — Compose Settings, About and first-run wizard.
- `app/src/main/res/` and `art/brand/mangoloads/` — resources and generated brand sources.
- `docs/` — feature contracts, experiments, bug ledgers and verified release checkpoints.
- `tools/` — build/release and reproducible resource scripts.

## Code and testing standards

- Keep normal typing responsive. No network, heavy allocations or expensive per-frame branding effects in the IME hot path.
- Protect typed text, passwords, contacts, Clipboard and microphone data. Never add content-bearing analytics, log raw input or attach real private screenshots to a public report.
- Preserve incognito/no-learning and editor-session cancellation invariants. Do not disable tests to make CI green.
- Keep app ID, original signing certificate, release asset name `brrBoard.apk`, versioning and Obtainium compatibility stable.
- For UI work, include meaningful before/after **real app** captures with demo text only, and test light/dark, large font, TalkBack, rotation and reduced motion where appropriate.
- For library imports or code copied from other projects, name the source and verify GPL-compatible licensing. Imported gesture libraries have their own licensing/import requirements; do not silently ship proprietary binaries.
- Update related docs and tests when behavior changes. Each maintainer/agent batch also updates `AGENTS.md` and `PROJECT_CONTEXT.md` with exact changes, commands, results, blockers and next step.

## Proposing a change

The maintainer currently develops on `main`. That **doesn't mean contributors should force-push main** or change live releases directly. A small reviewable proposed patch/PR is welcome only when the repository has an active contribution route. Do not start parallel automated release workflows or change repo settings, signing and permissions without explicit coordination.

If you submit a PR, explain:

1. **Why:** user-visible problem and intended behavior.
2. **What:** exact code/resources/docs changed, and intentional non-changes.
3. **Validation:** local tests, device checks, privacy/performance results; distinguish tests not run from passes.
4. **Risk:** installation/update, theme/layout, IME focus/editor, model/gesture and accessibility implications.

Review may take time. Thank you for helping brrBoard stay careful, fast and genuinely useful.
