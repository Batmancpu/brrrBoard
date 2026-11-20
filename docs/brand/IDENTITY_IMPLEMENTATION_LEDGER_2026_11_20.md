# MangoLoads × brrBoard identity implementation ledger

**Owner action:** Explicit `Go` received 2026-11-20 IST. This is a coordinated visual/product GitHub redesign and Android icon/setup/About integration. It does **not** authorize unrelated keyboard engine changes, a new AI system, a new signer/package or a new public support channel.

**Initial remote baseline:** `main` at `e0bef77507629bafba761d431417e8fc04307c3f`, fetched in a fresh clean filtered/sparse clone. At start the exact-SHA debug workflow run `44685259701` and original-signed Obtainium run `44685259738` were both **completed SUCCESS**. Those runs certify the *before* code only, not this identity change. Recheck live HEAD before publication to avoid overwriting other chat work.

## Batch I — canonical icon and exports (local source staged, release pending)

**Requests preserved:** exactly ten keys `MANGO` / `LOADS`, one `brr` spacebar, orange mango, green leaf and pointed glass badge; out-of-shield text crisp; full mark default; smaller keyboard-only and monochrome where necessary. Premium palette applies to *surrounding* surfaces only.

1. Copied the approved 1254×1254 raster and keyboard-only master from the user-approved preparation pack, plus two-tone and platform-themed SVG references into `art/brand/mangoloads/`.
2. Generated a deterministic vector-silhouette mask `icon-silhouette-mask.svg` to make the outer white matte transparent while preserving the interior original RGB and glass-only blur. Viewed the result over deep sapphire and light backgrounds; no detached speckles seen in that deterministic export. Two earlier AI background-extraction candidates produced stray fringe pixels and were **rejected**, not used in app resources.
3. Added reproducible `tools/export-mangoloads-brand.sh` using Inkscape and ImageMagick 7. Exports a transparent 432px adaptive foreground, independent Android-themed bitmap mask, five each legacy/round density bitmaps, 512px store PNG, 768/192px Compose images and horizontal branded static art.
4. Launcher resource IDs preserved: `ic_launcher_foreground.xml`, `ic_launcher_monochrome.xml`, both adaptive XML files and `ic_launcher_background`; new background color `#0C1527`. Production package/signing/permissions untouched.
5. Static visual check: full key spelling and `brr` visible on 512px store art, zero outer alpha at canvas corner, centered foreground-safe artwork, dark-background preview inspected. Android OEM mask/actual device verification **PENDING**.

**Commands:** `bash tools/export-mangoloads-brand.sh`, `magick ...` alpha composition/previews, `inkscape ...` SVG raster export, `git diff --check` (passed). **Pending:** Android resource inflation + real ColorOS launcher/round/squircle/themed test. Do not claim user-approved final alpha edges from an AI-generated fringe candidate.

## Batch II — Settings About and welcome motion (local source staged, release pending)

- `MainSettingsScreen.kt`: the existing `Md3ePreference` About leading slot now accepts `brandIcon` opt-in, displays full-color `mangoloads_brand_small` as `Image` without Material tint, retains the row's label/navigation/click target. Other preferences keep their existing symbols/colors.
- `AboutScreen.kt`: its APP row already displays `R.mipmap.ic_launcher_round` through `IconOrImage`; the updated launcher density resource propagates without rewriting the About list. Other About icons intentionally remain function icons.
- `AnimatedBoardIcon.kt`: removed infinite randomized MaterialShape morph/rotation and replaced it with one stable full-color resource plus a **780ms one-shot** alpha/scale/6dp translation. After settling no repeat or CPU work; Compose disposes the effect with the welcome destination. The single-raster animation is intentionally simpler than independently animated glass layers because exact lettering and badge registration are more important than staging extra animation channels.
- `WelcomeWizard.kt` Splash: removed 2300ms breathing loop, made the radial blue halo static and low opacity, used surrounding premium sapphire/glacier accents, and sized the logo with `BoxWithConstraints` against available width and height to avoid fixed 340dp clipping. Wizard Enable/Switch/back/state-check flow unchanged.
- Android system splash remains static by design; no extra launch delay, fake motion or keyboard/Emoji/mic brand effect added.

**Pending:** Kotlin compile, Compose resource/test gate, 320dp/landscape/large text/TalkBack/reduced-motion visual QA on real hardware. Source review does not prove 60/90Hz performance.

## Batch III — premium GitHub/docs/store presentation (local source staged, release pending)

- Rebuilt root `README.md` around theme-aware 1440×520 `<picture>` hero, a real Markdown heading, one clear signed APK download link and Obtainium link, concise verified feature description, privacy/model/bootstrap caveats, three-step install, migration, FAQ, contribution/credits. Hero and social preview are **brand illustrations, not app screenshots**.
- Produced `readme-hero-{light,dark}.svg/.png`, `github-social-preview.svg/.png` at 1280×640 and `store-feature-graphic.svg/.png` at 1024×500, preserving the original icon pixels against premium `#0C1527`/`#F7F9FC` surroundings. Verified rendered previews visually in both themes.
- Corrected `docs/OBTAINIUM.md`: current package `com.mangoloads.board`, old package separate; preserve existing signed asset/version workflow. Added `docs/README.md` and this durable brand record.
- Rewrote `CONTRIBUTING.md` to refer to the brrBoard repository, protected Android signer, local tests, offline/privacy/IME constraints and honest device verification. Updated `.github/PULL_REQUEST_TEMPLATE.md` and issue templates to remove upstream HeliBoard issue-reporting links and warn against posting typed/Clipboard/audio material.
- Updated Fastlane title/short/full descriptions, current icon and feature graphic to brrBoard; removed outdated FrostKeys/Gemini/GIF/Telegram marketing claims. Historic changelogs and **14 old screenshot assets** retained pending **authentic screenshots of the new signed APK**; never fabricate a UI capture.
- GitHub API read showed public Issues disabled. README and both privacy-policy variants now say so rather than linking people to a dead tracker. Existing About `REPORT_ISSUE` action still needs an owner-approved public support policy or future tracker enabling; do **not** silently turn on public Issues.
- Public GitHub About description, homepage/topics and social-preview Settings still need authenticated settings access; tracked assets alone do **not** modify them.
- Added a lightweight standard-library identity contract test step to the existing debug CI workflow. It verifies adaptive resource IDs, imagery size, no infinite welcome animation, About tint behavior, palette contrast and stable package. The original-signed Obtainium workflow and release signing gates remain untouched.

## Verification matrix (no invented passes)

| Check | Current result | Next gate |
|---|---|---|
| Image generator candidates | Rejected due to fringe artifacts | Not shipped |
| Deterministic alpha + PNG/WebP exports | Export commands success; visuals manually inspected | OEM/system mask QA |
| README dark/light hero, share card | Rendered from SVG and inspected | Actual GitHub mobile/desktop preview after commit |
| Package ID and signer files | Source-level unchanged: `com.mangoloads.board`, original workflow untouched | Inspect built APK and original cert via CI |
| `git diff --check` | Passed at early source checkpoint | Repeat after final docs edits |
| Seven brand resource/animation contract tests | **7/7 PASSED locally** (`python3 -m unittest discover -s tools/tests -p test_mangoloads_brand.py -v`); now also added to debug workflow | Verify the same step passes on the exact new SHA |
| Android resource XML parsing | `xmllint` not installed | Use standard XML parser / CI AAPT |
| Compose/Kotlin build & Android unit suite | **NOT YET RUN** (no local Android SDK) | GitHub debug/full-unit workflow on exact new SHA |
| Original-signed Obtainium release | **NOT YET RUN** for identity source | Exact SHA release workflow/cert/SHA check |
| Oppo Reno10 5G visual, IME performance and model/data continuity | **PENDING physical owner test** | Install once original-signed release passes |
| GitHub repository Settings metadata | **NOT YET CHANGED** | Authenticated repo-settings capability or owner instructions |

## Release and handoff rules

1. Refresh `origin/main` and Actions immediately before any remote write; if the other chat advanced head, reconcile and do not force push.
2. Rebuild exports after all source art changes, inspect `git diff --check`, XML/resources, Markdown links and tests. Do not claim CI success from the *before* SHA.
3. Publish **main only**, with no changes to `.github/workflows/obtainium-release.yml` signing, version rules, asset name or secrets; use exact-SHA debug + original-signed Obtainium terminal gates and check release APK identity/cert.
4. Preserve `AGENTS.md` and `PROJECT_CONTEXT.md` at each small batch with commands, failures, observed outputs and next steps. Any pending physical QA must remain labeled pending.
5. Brand and README are static when idle. Do not add icon animation in the keyboard hot path; don't use fake storefront screenshots or misleading feature claims.

**Current source state described here:** working-copy implementation in progress. This record is not a claim the APK or GitHub live appearance has shipped. Consult the newest entry in `AGENTS.md` and `PROJECT_CONTEXT.md` after future batches.
