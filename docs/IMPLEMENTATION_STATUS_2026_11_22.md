# Implementation status — 2026-11-22

## Exact-source verification and test APK

Feature branch `feature/parakeet-ultra-brain-abtest` is at source commit `5997430406b91edf1e2a1682d6a8bacae67581b4`. GitHub Actions run [44999224034](https://github.com/Batmancpu/brrrBoard/actions/runs/44999224034) completed successfully: Android debug build, signing identity checks, full Android unit-test gate, Ultra A/B build, original-signer verification, and artifact uploads all passed.

The run produced the update-compatible versionCode 236 A/B test APK as an Actions artifact only. The full artifact is ID `14744893999` (606,508,631-byte ZIP). The downloader's 512 MiB limit is handled by three parts: IDs `14744784264` (314,572,984-byte ZIP), `14744938992` (314,572,984-byte ZIP), and `14744534978` (240,381,381-byte ZIP). Checksum artifact ID `14745637031` contains the APK SHA-256. All artifacts expire 2026-12-06. Download all three part artifacts and the checksum artifact from the same run, extract into one folder, and run:

```sh
bash tools/join-ultra-abtest-apk.sh <part-folder> <output.apk> <APK-SHA256>
```

The joiner refuses to leave a result unless all ordered parts match the expected SHA-256. This APK is for the owner's test; no GitHub/Obtainium release was created.

## Work completed in source

- Fixed the supplied Emoji top-gap cause: a fixed 56dp Search-results sibling remained visible when Browse mode was active. Search result/empty rows are now visible only in Search mode and hidden during Browse measurement; source regression coverage and privacy-safe geometry report visibility fields are included.
- Smart Emoji uses a bounded, editor-bound precomputed ranking snapshot rather than duplicate panel scoring and synchronous editor queries.
- Toolbar layout, pinned Settings access, Emoji/Settings footer behavior, Emoji hold-shortcut removal, progressive glass invalidation, and cached panel rendering are implemented on the branch as recorded in the feature notes.
- BR Brain compares the existing Parakeet 110M and optional Ultra on the same volatile PCM recording, sequentially and cold-loaded, with WER reference, timings, RTF and sampled PSS. Ultra runs in a private process and does not replace default dictation.
- `tools/join-ultra-abtest-apk.sh` and packaging regression checks cover exact signer/staging filename agreement, all parts, and hash-before-output behavior.
- Workspace network fallback is documented in `docs/LOCAL_BUILD_NETWORK_FALLBACK.md`: use exact-SHA Actions and the authenticated GitHub Git Database API if the workspace's dead proxy blocks push/download. A prior artifact failure was specifically a two-`r`/three-`r` filename mismatch; it is now covered by a regression assertion.

## Gates that remain intentionally open

- **Reno10 acceptance:** the code fix is CI-verified, but install/visual verification of the Emoji gap and toolbar/glass motion on the physical phone is not available from this workspace. Ultra WER, latency, memory, and thermal comparison on that device also remain unmeasured.
- **ContextLM:** do not enable or bundle the current neural candidate. Corrected training/evaluation and the 250,000-pair fixed holdout still lost Recall@3 against the proxy. Keep it disabled until actual native-decoder candidate quality, false-autocorrection, latency, memory, and privacy gates pass.
- **Ultra model quality:** the host FLEURS benchmark improves English WER but is about 3.5x slower in host CPU RTF; it does not establish Indian-accent or Reno10 quality. Keep Ultra available only in the owner-requested test APK until matched device results are reviewed. Do not publish that APK as the next product release.

Local Python suites passed (41 tools tests and 16 CI/branding tests); Bash syntax and `git diff --check` passed. CI supplies the Android build and full Android unit-suite evidence above.
