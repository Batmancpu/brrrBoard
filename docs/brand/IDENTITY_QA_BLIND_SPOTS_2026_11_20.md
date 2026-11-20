# brrBoard MangoLoads — blind spots, test matrix and release gates

No app code, APK, signing, GitHub branch or workflow was modified during this audit. Findings are source-verified at the SHAs in `REPOSITORY_AUDIT_2026-11-20.md`, plus image inspections of the current Fastlane icon and an old listing screenshot. **No installed APK binary was decompiled** and **no real Oppo performance result** has been claimed.

## A. Blind spots that would make a normal icon swap incomplete

| Priority | Blind spot | Concrete mitigation and proof |
|---|---|---|
| P0 | Current primary PNG has opaque white surroundings; adaptive icons need isolated alpha layers | Rebuild faithful foreground/background from the canonical art, verify checkerboard alpha at 32/192/512px. Never ship the jagged experimental extraction. |
| P0 | Gboard-inspired pointed badge hides top letters; AI-generated text can smear outside | Mask *only* shield content; compare exact glyph geometry outside shield before/after, require correct letters and no duplicate keys. |
| P0 | Round/squircle masks may cut off leaf, badge tip or keyboard corners | Preview safe area with circle/squircle/rounded-square/Oppo masks; adjust scale/margins before export. |
| P0 | About row uses `Material Symbols` inside a 40dp tinted circle, not an image resource | Explicit brand-specific untinted Compose `Image`; static, sized and accessible. Do not recolor full mark via Material3 `Icon`. |
| P0 | Welcome is independently animated from current logo; simply changing launcher doesn't change it | Replace finite motion source and verify end frame matches static mark. No random Material shape morphs or perpetual breathing. |
| P0 | Existing welcome 320dp+340dp fixed visual could crowd small windows | Responsive `BoxWithConstraints`/`sizeIn` limits, safe insets, high-font and landscape tests; no blocked setup actions. |
| P0 | Other agent actively commits to `main`; pushing assets during its exact-SHA builds can invalidate CI or overwrite work | Re-read HEAD, Actions and handover files at Go; wait/coordinate, fast-forward only, perform small ledgered batches; never revert parallel feature commits. |
| P0 | Production signing or app ID change causes failed in-place update | Keep `com.mangoloads.board` and original pinned signing key/secrets, full suite and exact-SHA release; verify installed-update path. |
| P0 | A full-size brand bitmap decoded inside 40dp preference row wastes heap and may stutter scrolling | Export small pre-sized bitmap and load through stable `painterResource`, no live resizing or asset decoding per recomposition. |
| P1 | Android themed/monochrome icon is **not** simply black/white full-color art | Single-alpha system icon; inspect wallpaper tint/contrast and mask, keep separate from two-tone branding. |
| P1 | Android system splash may use launcher icon automatically; adding custom entry can show duplicate splash | Keep platform behavior unless measured; no extra artificial delay/loading screen. |
| P1 | Latest Fastlane icon and Store metadata still identify upstream FrostKeys, not brrBoard | Replace actual `fastlane/.../images/icon.png`, `title.txt`, short/full descriptions with truthful current details; consider whether listing is active. |
| P1 | Fastlane images 1–14 look like upstream UI, at least image 1 is demonstrably old | Queue *real* brrBoard captures from new signed APK; never present synthetic UI art as shipped screenshots. |
| P1 | README is text-only; source `.svg` and artwork folders still include older FrostKeys designs | Add canonical `art/brand/...`; README hero; treat older art as historical provenance, not automatically delete/overwrite. |
| P1 | Android system IME picker may use inherited app icon while subtype icons remain technical switcher glyphs | Test both, and only change application identity resources; **do not** replace `ic_ime_switcher` for every language. |
| P1 | `res/layout/fake_toast.xml` inherits round icon and might scale differently | Confirm new round icon has enough margin in fake toast. |
| P1 | ColorOS caches old launcher icon after in-place update | Check after APK install, launcher refresh/restart if needed; distinguish OEM cache from wrong APK resource. |
| P1 | Light/dark background washes out pale shield, outer white matte, or keycaps | True-alpha test on black/white/neutral dynamic-color; avoid heavy backdrop and renderer-level tint of full logo. |
| P1 | Reduced-motion, lifecycle pause, Back gesture and activity recreation may leave half-animated logo | One-shot Compose effect; cancellation, final-frame snap, animator scale 0, rotation/save-state and rapid navigation tests. |
| P1 | Legal source/attribution: upstream license credits and Gboard/Apple design influence | Preserve legal LICENSE/NOTICE, avoid copying Apple/Gboard proprietary paths or calling it an official logo. New artwork should be independently authored and rights-reviewable. |
| P2 | 32px favicon, notification/status bar and repo avatars are tempting but may not exist | Do a final source/ref scan. Do not add unnecessary branding, animated files, or notification infrastructure to create a surface. |
| P2 | Intro animation could trigger every time a settings preference polls | Key effect by wizard **entry**, not composition; no idle timer after first 800ms, static on settings pages. |
| P2 | Spelling/casing mistakes in text or `brr` position at density conversion | Human-read 10 glyphs top/bottom, programmatic dimension/alignment checks at every export size. |

## B. Exact QA acceptance matrix

| Context | Primary check | Fallback/constraint | Evidence |
|---|---|---|---|
| Launcher icon, default Oppo mask | Blue keyboard, badge and leaf fully visible, no white box, visually centered | Keep safe margin, do not distort | Device screenshot + APK resource dump |
| Launcher circle/squircle/round | No leaf/badge tip clipped | Scale adaptive layer safely | Preview sheet + OEM screenshot |
| Themed icons | Readable one-color silhouette matching identity | Platform colors only, no hard-coded mango gradient | Home-screen dark/light screenshots |
| Android Settings app list | New icon in OS-owned surface | May be cached briefly | Screenshot after update |
| Android keyboard picker/IME chooser | App name brrBoard, appropriate app icon | Subtype `ic_ime_switcher` preserved | Oppo chooser screenshot |
| Settings home About leading row | MangoLoads icon replaces `info`, correct 40dp leading alignment | Use compact *only if* full fails at that size | Light/dark screenshot, a11y focus |
| Settings home collapsing header | Static logo only if it fits, no title/search collision | Omit if cluttered | Scroll/search/reachability capture |
| About screen APP row | New launcher icon, sharp at 32dp | Possibly 64dp hero if visually justified | About screenshot |
| About legal/service/voice rows | Correct functional icons remain | Never brand all generic info icons | Row comparison |
| Welcome first frame/settle | Approved primary mark and correct text, crisp outside glass | Reduced motion instantly shows final state | Five timeline frames + short video |
| Welcome→Setup→Back | Quick responsive transition, no half-drawn icon or bounce | Original wizard state/navigation stays | Screen recording with rapid taps |
| Enable/Switch keyboard setup | Same Android intents and success behavior | No extra permissions or steps | Functional test on Oppo |
| Landscape/split/small font+large | No logo over controls or cropped badge | Scale hero down, keep text readable | Orientation/200% font screenshots |
| Settings and IME performance | No new idle ticker, start/scroll/keyboard latency not worse | Fall back to static/reduced animation | Real performance counters and observations |
| Fake toast/listing/README/Fastlane | Matching brand, accurate text/alt text | Historical credits retained | Screenshots/source diff |
| In-place Obtainium update | Same ID/signature and data, no broken keyboard functionality | No uninstall/re-sign | `aapt`/`apksigner` result + user install feedback |

## C. Automated checks at Go

1. Asset lint: validate exact pixel sizes, alpha channel in foreground, no fully opaque external white rectangle, PNG/WebP decoder success, color profile, file sizes, 108dp adaptive safe area, 10+1 key configuration, `brr` at right.
2. Android resource compilation and inflation for legacy/adaptive/themed plus any new on-screen drawables. Check no unsupported `<adaptive-icon>` attributes at minSDK 24 fallback.
3. Kotlin/Compose unit tests for new brand components, demo animation state machine, reduced-motion behavior, exact final transform and rapid navigation interruption.
4. Run existing unit/regression suite, especially `RoundedIconSurfaceTest`, `IconDefaultPreferenceTest`, app permissions/privacy/voice packaging, wizard and launcher tests if available.
5. Build `assembleObtainium`, run `testRunTests` (existing workflows), verify exact same commit SHA is green in debug and original signer. Validate `aapt dump badging`, `aapt dump permissions`, `apksigner verify --print-certs`, APK resource contents, asset hashes and `brrBoard.apk` release file size.
6. Preserve `AGENTS.md` and `PROJECT_CONTEXT.md` changelog at each batch. No guard bypass, no signing-key migration, no CI cancellation of another active agent's work.

## D. Quantified motion/resource goals (targets, NOT measured results)

- Welcome logo entrance **≤850ms total**, immediately interactive; if slow, drop to ~200ms simple reveal rather than lengthening flow.
- Brand image animation **0 running coroutines/frame callbacks after settle**. Settings/About rows static.
- No new background service, no startup network, no custom runtime blur shader, no animated bitmap/Lottie/GIF dependency.
- Warm Settings launch/scroll and IME first-key latency must not show a repeatable regression beyond normal run-to-run variance; collect baseline and after samples on the same Oppo device before quoting numbers.
- Exactly one app-identity source set, with generated derivatives; no proliferation of `final-v3-last.png` or multiple different brand marks in active modules.

## E. GO blockers, escalation and final handoff

- **Concurrent `main` drift:** if another agent is currently writing `MainSettingsScreen.kt`, `WelcomeWizard.kt`, resources or docs, stop before overlap. Reconcile human-visible changes and use a guarded fast-forward.
- **Art mismatch:** if true-alpha layer extraction damages lettering/glass, do not silently ship the artifact. Restore the approved composition and request review of a faithful layered proof.
- **No real device:** publish only after automation green with Oppo physical QA explicitly pending. Do not claim buttery animation or verified ColorOS masks from screenshots alone.
- **Store listing unrelated to releases:** update tracked metadata safely if within requested identity scope, but do not claim it was published to a storefront unless a listing deployment is actually performed.
- **Historical paths:** preserve upstream license/NOTICE and old tagged releases; an identity update does not rewrite history.
- **Exit criterion:** exact-SHA signed APK available via current release/Obtainium path, all owned active identity locations audited, working About logo, smooth wizard and tested animation, no unrelated user-feature regressions, authoritative `AGENTS.md`/`PROJECT_CONTEXT.md` handoff, and clear remaining on-device QA.
