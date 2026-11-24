# Next AI handoff — brrBoard (2026-11-23)

## 2026-11-24 — separate private-gesture candidate compile recovery

The unshipped `wip/private-footer-gesture-20261123` branch has its known Kotlin `FloatArray.fill(0)`/missing-import errors fixed in `b1a4b9fe`, and required branch release notes at `7cc17dba`. Exact-source debug/full Android suite [45328920397](https://github.com/Batmancpu/brrrBoard/actions/runs/45328920397) **completed success** after a prior release-notes-gate failure [45328858762](https://github.com/Batmancpu/brrrBoard/actions/runs/45328858762). Added `PrivateGestureTraceTest` invalid/flat/nonmatching/matching gesture coverage. This is **debug-only** and **not included** in signed v1.0.260. Secret insertion, false-positive/security/device accessibility and encrypted storage acceptance remain unresolved; do not merge or publish this gesture merely because CI is green.


## Read this first

This is the current owner-directed handoff. Read this file, then `AGENTS.md`,
`PROJECT_CONTEXT.md`, `README.md`, and the linked topic plans before changing
the app. If older notes conflict with this file, this file records the latest
owner direction; retain older notes as historical release records.

## 2026-11-24 — continuous voice typing hotfix

Owner reports tap dictation stops after the first 5–7 s batch. See [continuous batch fix](VOICE_CONTINUOUS_BATCH_FIX_2026_11_24.md) for diagnosis, changed files, regression tests and device acceptance gates. This is a main-branch source repair; release and phone verification remain separate. Preserve the gesture WIP.

## 2026-11-24 — newest release and isolated candidate

Current official production APK: [v1.0.260](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.260), source `30e86d2`, exact-SHA debug/full tests [45328763013](https://github.com/Batmancpu/brrrBoard/actions/runs/45328763013) and signer [45328762985](https://github.com/Batmancpu/brrrBoard/actions/runs/45328762985) successful. Asset SHA-256 `cb4a694da22e3ecd7e0bcf21ecc4be1f55d2851adccc1d1853b052f762576c04`; 63,544,172 bytes. v1.0.259's continuous dictation stays present; v1.0.260 adds explicit Cancel haptics and tested Send-only guards. The older release boundary below is historical. The private-gesture WIP compile repair `b1a4b9fe` and release-note follow-up `7cc17dba` remain on their own branch; neither is in production, and biometric device failure remains unresolved.

## Current verified release boundary

- Latest confirmed public, original-signed release is
  [v1.0.257](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.257).
  The release pointer is recorded in `README.md`; source history includes
  `5a7d1e4` (protected access, legacy toolbar actions, and model discovery) and
  `981fa30` (release documentation).
- APK size: 63,544,172 bytes. SHA-256:
  `aae9aad384b8f70a3a30ae05c5f55ee66d50bbe7580c675fc25d5ea275b1e8a9`.
- Release notes state that v1.0.257 fixes protected biometric access, restores
  installed-model discovery across private storage areas, and removes obsolete
  Toolbar actions. The user had referred to “157”; clarify it as v1.0.257 when
  discussing this build. Verify the installed About screen if the device may be
  on another version.
- Release automation must omit all large Parakeet weights. Ultra is already on
  the owner's phone; future APKs should preserve its app-private installed
  copy and must not silently bundle it. No speech-model comparison workflow is
  currently requested.

## Latest owner direction: biometric access is on hold

Although v1.0.257 release notes say protected biometric access was fixed, the
owner reports that biometric access still is not working on the phone and has
explicitly directed that it be put on hold. Device acceptance is not proven by
CI. Do not make the next protected experience depend on a biometric prompt or
claim the device issue is fixed. Pause the automatic biometric entry path in
the next candidate. Preserve existing encrypted note/key material unless a
safe migration is needed; do not erase the owner's data.

The replacement requested by the owner is an opt-in custom keyboard gesture,
drawn in the reserved footer strip below the space bar and between the Emoji
and microphone keys. It should support owner-enrolled gesture(s) and provide
two intentional actions:

1. Open/unlock the app's protected/private information.
2. Insert the owner's chosen secret into the currently focused text field.

The secret gesture is a convenience unlock, not biometric-grade authentication.
Describe that accurately in Settings and the security notes. Keep raw gesture
traces and typed secrets out of logs, analytics, diagnostics, backups and
clipboard. Store the secret/gesture template only in app-private encrypted
storage; zero temporary buffers; never update learned words from secret
insertion. Require a deliberate gesture that starts and ends inside the
reserved footer area, and keep it inactive until enrolled/enabled. Do not
intercept ordinary key, swipe typing, Emoji, mic, cancellation, trackpad,
accessibility or system-navigation gestures. Prevent accidental insertion by
requiring a clear explicit enrollment/enable step and a match threshold with
false-positive tests. Secret entry must never use the clipboard.

The old design in `docs/BIOMETRIC_PRIVACY_AND_PRIVATE_GESTURE_PLAN_2026_11_23.md`
and v1.0.252 implemented a biometric-only vault plus a one-use circle gesture.
That is historical behavior, not the requested final behavior. The owner has
now asked for a configurable/enrollable gesture instead of relying on the
currently failing biometric route. Do not leave the old one-use circle as the
only gesture workflow.

## Checkout and in-progress candidate: not released

- The verified baseline is `origin/main` at `981fa30d` (v1.0.257 release
  documentation).
- The custom-gesture source candidate is now preserved on GitHub branch
  [`wip/private-footer-gesture-20261123`](https://github.com/Batmancpu/brrrBoard/tree/wip/private-footer-gesture-20261123),
  commit `75a4db30`, based on v1.0.257 main. It changes `BoardFooterView.kt`,
  `PrivateFooterGestureView.kt`, `PrivateGestureSession.kt`,
  `PrivateGestureTrace.kt`, `PrivateGestureVault.kt`,
  `PrivateGestureSetupPanel.kt`, `ProtectedPrivacyScreen.kt`, and
  `FocusedSettingsScreens.kt`.
- This is **WIP source only**: no compile/full-suite CI or device acceptance has
  passed for commit `75a4db30`; it is not merged to main and no post-v1.0.257
  APK exists. Preserve the branch and continue its implementation/testing; do
  not treat it as a release. The v1.0.257 signed APK remains the confirmed
  installable version until matching CI, original-signer evidence and a new
  release establish otherwise.

## Other owner requirements to preserve

- Keep the already-installed Parakeet Ultra available as the default active
  offline dictation model when its installed files validate; do not bundle the
  600+ MB weights in routine updates. Keep the small model only as an Advanced
  fallback. Keep voice cleaning user-controllable and default-on as previously
  requested. Never delete working model files during preference migration.
- Hold-to-talk is intended to show guarded interim text outside Incognito,
  show only final text in Incognito, and auto-send on release by default only
  for Send-capable fields, with an opt-out. Swipe-to-Cancel must give visual
  confirmation and haptic feedback, discard both audio and partial text, and
  persist no transcript. The owner has reported missing cancel haptic and
  broken auto-send in prior builds; do not claim those physical flows accepted
  until confirmed on the owner's device.
- ContextLM remains an experiment behind an explicit opt-in because earlier
  held-out quality gates failed. Do not force it into live suggestions or call
  it a quality improvement without fresh evidence, especially for Hinglish.
- The owner wants a unified on-device BR Brain for next-word, correction,
  personalization, swipe learning and Emoji context, with no cloud transfer.
  Preserve native suggestions as the immediate low-latency path and gate
  personalization for Incognito, passwords, secure apps, no-learning fields
  and user-disabled learning.
- Settings should present easy everyday controls first and retain advanced
  controls behind an intentional Advanced entry. Update history stays bounded
  to ten prior entries. Reset belongs under Backup & restore and must have
  deliberate confirmation/undo behavior.
- Emoji Browse's previously diagnosed stale Search-row height was fixed in a
  prior candidate/release. Do not reintroduce speculative top padding. The
  owner also wants the familiar filled smile Emoji key restored, and unwanted
  duplicate Toolbar controls removed; verify these against the installed
  release and current source before saying they are done.
- Exact iOS 27 animation parity cannot be asserted. Use source-derived bounded
  animation behavior and describe it as inspired/approximate unless measured
  against a supplied reference.
- No personal screenshots, emails, transcripts, passwords, package identities
  from the owner's device, or private typing data belong in GitHub. Remove
  accidentally committed personal data only after checking repository history
  and release implications; never invent that a scan passed.

## Required continuation steps for the next AI

1. Read this file and all current `AGENTS.md` instructions; inspect branch,
   HEAD, dirty tree and existing staged changes. Preserve the candidate.
2. Reconcile implementation with the latest gesture direction: stop relying on
   biometric entry, implement configurable footer-gesture enrollment/unlock
   and explicit secret insertion, with clear privacy wording and strong
   accidental-trigger safeguards.
3. Audit the open owner-reported defects against code and release notes:
   protected screen crash, biometric failure path, missing Ultra discovery,
   Toolbar duplicates, restored Emoji artwork, hold cancel haptic/auto-send,
   and suggestion/Emoji stability. Do not make an unverified fix claim.
4. Add focused tests for gesture normalization/matching, false positives,
   sensitive editor policy, encrypted storage, cancellation, and voice delivery.
   Keep existing privacy/build gates; do not weaken tests to force green.
5. Run repository static contracts and exact-source Android debug/full tests.
   The managed workspace may lack Maven access; if so, use the documented
   exact-SHA GitHub Actions build fallback. Require the matching original-signer
   release workflow before publishing. Regular APK must not include large
   speech-model weights.
6. Update this handoff, `AGENTS.md`, `PROJECT_CONTEXT.md`,
   `docs/ENGINEERING_HANDOFF.md`, topic plans, conversation ledger, README and
   `docs/releases/notes.json` with evidence. Separate source-complete,
   CI-verified, published, and physical-device-verified claims.
7. Do not stop at a progress plan when the owner authorized implementation;
   continue through code, tests, CI, and release where the existing signer and
   workflow allow. Report physical-device checks as the owner's final audit,
   not as completed by CI.

## What “context for every AI” means

The repository cannot broadcast this file to unrelated AI accounts. The
durable handoff is committed in the repository, and `AGENTS.md` instructs coding
agents opening this checkout to read it first. Any future AI still needs access
to the repository and must honor its agent instructions; this file does not
configure external AI systems or transfer the uncommitted `/tmp/brrboard-pr9`
source tree by itself.
