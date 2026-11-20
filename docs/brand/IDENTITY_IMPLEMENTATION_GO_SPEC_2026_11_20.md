# brrBoard × MangoLoads — exhaustive GO implementation specification

**STATUS: READY TO EXECUTE ON EXPLICIT GO; NOTHING IMPLEMENTED IN THE REPOSITORY.**

This is the implementation blueprint for a single coordinated, original-signed brrBoard identity release. Read `REPOSITORY_AUDIT_2026-11-20.md` first for grounded code paths and baseline SHAs. The incoming code may change while other agents work, so this blueprint must be reconciled to live `main` before editing.

## 0. Non-negotiable owner contract

1. Preserve the exact brand artwork idea: one royal-blue keyboard (not a generic typography app mark), exactly **ten keycaps** `M A N G O` / `L O A D S`, one spacebar with small italic lowercase `brr` at its **lower-right**, one mango fruit/leaf on a pointed translucent/liquid-glass badge overlapping top-center keys.
2. Blur/refract only content geometrically behind the glass badge. Outside that outline, letters and keys are perfectly crisp. Do not deform the N or smear letter portions outside the glass. Avoid a fully opaque milky-white shield: show controlled clarity, crisp specular edges, optical depth.
3. Default is the full brand mark. Compact is the **blue keyboard-only** 10-keys+spacebar without glass or fruit; use only after actual-size failure of the full mark. High-contrast two-tone remains an optional small-context fallback. Android themed icon is a separate system tint mask; do not tint full-color branding in Compose.
4. App name remains `brrBoard`; Android production ID remains `com.mangoloads.board`; keep original signing certificate, same Obtainium source and stable in-place upgrade, current version rules, permissions, user data and keyboard IME behavior.
5. All remote code changes **main only**, after other feature workflow changes have settled. Do not erase concurrently authored changes. Update both `AGENTS.md` and `PROJECT_CONTEXT.md` **after every small implementation batch**, including request, exact files, commands, tests, observed results, blockers and next steps. No GitHub writes at all until the owner explicitly says `Go`.
6. Keep low-latency keyboard performance. No dynamic blur/shader animation in the IME, no new network/AI system, no changed Emoji/mic/suggestion/incognito animation contracts, no unwanted functional glyph substitutions.
7. The owner subsequently requested **premium shades for all supporting redesign surfaces, not for the approved icon**. Follow `PREMIUM_COLOR_SYSTEM.md` exactly as the starting color contract: Obsidian Navy / Deep Atlantic / Cut Sapphire / Glacier / Luminous Pearl / Graphite / restrained Champagne. Protect user-selected keyboard themes, Android dynamic colors, accessibility/status meanings and the approved icon pixels. Apply to tasteful surroundings only, with no extra shader work in the IME.

## 1. GO start gate: 100% read-only reconfirmation

1. Check live `main` HEAD SHA and the latest Actions runs for that same SHA; wait for in-flight source workflows to reach terminal state. If there is an active separate agent/branch migration, coordinate with the owner or preserve exact HEAD through a guarded fast-forward. **Never overwrite** a newer main commit.
2. Read current `AGENTS.md`, `PROJECT_CONTEXT.md`, linked current batch docs and any newly created AGENTS instructions. Record their latest owner priority and active file locks/overlap.
3. Perform a fresh path/ref scan: `rg -n 'ic_launcher|AnimatedBoardIcon|SplashScreenView|icon = "info"|R.mipmap.ic_launcher_round|setup_welcome_image|ic_frostkeys_icon' app README.md fastlane art`. Confirm the source map below still applies.
4. Record repo/worktree cleanliness, current production app ID, versionCode rules and pinned signing workflow. Check whether the identity asset pack has newer user revisions. Do not treat an uploaded PNG as a valid transparent adaptive foreground.
5. Plan atomic batches A→E on `main`. Design-only preparation is not a release; owner should be shown the integrated visual QA and a signed APK after tests pass, not individual speculative builds.

## 2. Brand asset production: create true canonical sources

**Source target:** `art/brand/mangoloads/` (or `brand/`, using the repo's established convention), with a concise `README.md` and source provenance. The current generated asset pack is the visual reference, not production-grade mask/vector authority.

### 2.1 Required layer architecture

- Full composition `primary-master` at at least 1024×1024, RGB/sRGB, lossless, exact geometry; retain the approved high-detail main PNG unchanged as the aesthetic comparison baseline.
- True-alpha **keyboard base** layer: pure blue rounded body with controlled internal gradients, no white external tile.
- **Keycaps-and-ink** layer: 10 individual keys and one wide spacebar with exact letters and `brr` baseline. For the visible glass-overlap region, compose the *same* character data inside the shield's mask to create a controlled local refraction; keep raw crisp keys outside.
- **Shield** layer: pointed badge shape with soft glass translucency, subtle rim/reflection, precomputed refractive content *only inside its clipping mask*. No expensive runtime sampling of a captured host screen. Edge opacity must be clean on bright/dark backgrounds.
- **Mango/leaf** layer: fruit sits on the badge, shadow confined appropriately. An optional pre-rendered specular **edge highlight** layer may serve the welcome intro; no constant blur or broad horizontal flare.
- Flat **composite PNG** and optional WebP for contexts where layers are unnecessary. Keyboard-only compact alpha master plus black/white SVG remain separate brand siblings.

### 2.2 Mandatory export set

| File/format | Sizes or profile | Intended destination |
|---|---|---|
| Primary square PNG | 1024 master, 512, 256, 128, 64 | README, About/welcome, GitHub, social art (where legible), store 512 |
| Full alpha composited graphic | correct transparency, 1024 and Android-targeted density variants | Adaptive foreground and large on-screen UI; **not** the current white-matted main PNG |
| Compact keyboard PNG/WebP | 512, 256, 128, 64, 32 (transparent) | Only small/low-detail placements where primary fails |
| Two-tone SVG + PNG | true 2-color vector + 512, 128 raster | High contrast, monochrome printing, unusually constrained UI |
| Themed single-alpha mask | 108dp VectorDrawable or equivalent Android resource | Android 13+ wallpaper-tinted launcher; no embedded fixed colors |
| Adaptive launcher resources | 108dp foreground/background extents; keep critical shape within OEM mask safe region | `mipmap-anydpi-v26`, round and OEM masks |
| Legacy launcher density PNG/WebP | mdpi 48, hdpi 72, xhdpi 96, xxhdpi 144, xxxhdpi 192; matching round | `mipmap-*` folders |
| Store listing PNG | exactly 512×512, <=1MB if required by store | `app/src/main/ic_launcher-playstore.png`, Fastlane icon |
| Optional repo social preview | 1280×640, if actively used | GitHub/social card; not an Android resource |

**Asset QA before code:** programmatic dimensions and color/alpha profile; magenta/black/white checkerboard halo test; 24/32/40/48/64/192px legibility; no text deformation/outside blur; safe-area circles, squircles, rounds; compare static composite vs layer-animated end frame pixel-aligned; document raster vs vector honestly.

### 2.3 Android launcher resource wiring

1. Replace artwork in `app/src/main/res/drawable-v24/ic_launcher_foreground.xml` or point to an appropriate alpha layer resource; no white square inside an adaptive foreground. Background may be a carefully matched blue or neutral solid/gradient, respecting Android adaptive split. For XML `aapt:attr` embedded raster, validate API24+ inflation and build-tool limits. Use standard resources, not runtime network bitmaps.
2. Replace `app/src/main/res/drawable/ic_launcher_monochrome.xml` with one-color silhouette appropriately weighted at minimum size. A readable fruit silhouette is allowed here (Android platform-themed rendering), but letter strokes might need simplifying.
3. Set `app/src/main/res/values/ic_launcher_background.xml` to the selected background if changed; don't create an unwanted contrasting border.
4. Keep `app/src/main/res/mipmap-anydpi-v26/ic_launcher.xml` and `ic_launcher_round.xml` pointing to their stable IDs, only adjust if necessary. They already contain all three adaptive layers.
5. Regenerate all 10 existing density WebP files for `ic_launcher` and `ic_launcher_round` from the validated source (do not upscale a 48px export). Test launcher/app drawer, circle/rounded-square/squircle masks, themed icons, app-switcher/long-press system UI, Android Settings app information and IME picker where OS controls the image.
6. Replace `app/src/main/ic_launcher-playstore.png`; leave manifest application label/id/IME service permissions alone. The `AndroidManifest.xml` launcher/activity pointers already use stable icon IDs; avoid unnecessary churn.

## 3. In-app brand integration

### 3.1 Exact Settings > About leading icon — MUST

**File:** `app/src/main/java/helium314/keyboard/settings/screens/MainSettingsScreen.kt`.

- `Md3ePreference(icon = "info", title = settings_screen_about, ...)` at ~281 is the exact user-requested row. Replace its *leading visual only*. Preserve title, description, entire row touch target, navigation and corner radius.
- The renderer currently supports `String` Material-Symbol text, or `Int` resource through Material3 `Icon(... tint=onSecondaryContainer)`. This **would recolor** a full-color MangoLoads mark. Extend `Md3ePreference` with a backward-compatible optional `leadingContent: @Composable (() -> Unit)? = null`, or a tightly scoped brand-specific renderer, and use `Image(painterResource(...), contentDescription = null, contentScale = Fit)`. Existing call sites remain unaffected.
- Keep the current 40dp icon slot. Compare full primary at 36–40dp versus compact at 32–36dp in light/dark themes and 1.3× font scaling. Default full; choose compact only if the full is visibly illegible at real density. Document the visual decision.
- The logo is decorative inside a row that has an accessible text title; do not make it separately clickable or duplicate TalkBack labels.

### 3.2 Settings top/title

**Files:** `settings/screens/MainSettingsScreen.kt`, `settings/SearchScreen.kt` (and optionally `settings/SettingsNavHost.kt`).

- The reusable `SearchScreen` already has an optional `icon` composable but `SearchSettingsScreen` wrapper omits it. Expose an optional, default-null `headerBrand` through the wrapper, or make a local `brrBoard Settings` title icon only on main settings.
- Display a **static** full-color image 28–36dp without Material tint. Keep the search action, nav, large collapsing app bar, existing haze, title accessibility and scroll behavior unchanged. Avoid adding a top-level image if it competes with search or causes width clipping in 320dp/small-font-large configurations.
- This is optional *if* the existing header becomes crowded. The About row icon and welcome hero are mandatory; no need to push small logos into every settings subpage.

### 3.3 About page

**Files:** `settings/screens/AboutScreen.kt`, `settings/preferences/Preference.kt`, `settings/Misc.kt` if needed.

- `SettingsWithoutKey.APP` already uses `R.mipmap.ic_launcher_round`. Verify bitmap/VectorDrawable rendering path in `IconOrImage(..., 32)` and update the mark automatically through launcher resources.
- If image appears too tight in the 32dp APP row, add a **single** tasteful static 72–96dp primary hero *within About only* after visual review; do not duplicate five logos in the same screen or break search/filtering. Prefer retaining the original clean list if it is already balanced.
- Leave `ic_settings_about.xml` generic info vector intact for the version/privacy-related preferences that currently use it. Their functions do not become brand logos.
- Keep licenses, original upstream attribution, and Mangoloads links and privacy policy accurate.

### 3.4 Setup wizard and first-run welcome

**Files:** `settings/AnimatedBoardIcon.kt`, `settings/WelcomeWizard.kt`; perhaps new `settings/MangoLoadsBrandMark.kt` and `settings/MangoLoadsIntroMotion.kt`.

- Replace the *current moving outline morph animation* with the approved MangoLoads layers and the exact bounded timeline in `ANIMATION_STORYBOARD.md`.
- Keep two-screen flow, enabled/current IME polling, `ACTION_INPUT_METHOD_SETTINGS`, `showInputMethodPicker`, back handling, screen transition and button behavior. No extra required pages, permissions, carousel or long introductory animation.
- Make the 320dp logo responsive: its bounds must fit smaller portrait windows, landscape, split-screen and 200% font sizes; never cover setup buttons or require vertical scrolling merely to see branding.
- When moving Welcome→Setup, retain the existing direction-aware slide+fade but match timing to new intro; verify no duplicate opacity pulses or re-composition resets.
- Setup page currently has no large brand illustration. Optionally place **one small static** mark aligned with heading if space allows. Its checklist checkboxes, numbered essentials, and arrow icons are *functional* and remain untouched.

### 3.5 System splash

- Keep Android platform splash fast and static by default. Its icon flows from launcher, and a new heavyweight custom view would slow apparent start. There is no existing bespoke `windowSplashScreenAnimatedIcon` source integration requiring replacement.
- If a genuine OS splash animation is later approved, use a tiny platform-compatible vector transformation with fixed duration and API-specific fallback, not an animated full-glass raster. Measure first. **Do not implement solely because it is technically possible.**

### 3.6 Other inherited visuals

- `res/layout/fake_toast.xml` automatically reflects `@mipmap/ic_launcher_round`; do visual check.
- `SettingsActivity2` and App Info inherit launcher IDs; confirm manifest icons at build.
- IME chooser/system input settings may use app icon; test Android/ColorOS actual rendering. Leave `@drawable/ic_ime_switcher` subtype glyph as a functional icon.
- Search for app shortcuts, notifications, widgets and other branding on the final HEAD. The initial scan found no custom notification `setSmallIcon` usage or shortcut icon. **Do not invent surfaces that do not exist.**

## 4. Repository, marketing, and durable identity

1. Add `art/brand/mangoloads/README.md` with versioned asset files, intended contexts, correct casing, source/provenance and minimum rendered sizes; include concise brand usage rules.
2. In root `README.md`, add centered/full-width or modest-height primary mark from a tracked relative path, with meaningful alt text. Do not replace functional screenshots with a static marketing mockup. Preserve README's install/Obtainium, privacy and upstream license notices.
3. Replace `fastlane/metadata/android/en-US/images/icon.png` (currently an old purple symbol); update `title.txt` from `FrostKeys` to `brrBoard` and rewrite `short_description.txt`/`full_description.txt` for the **actual offline and installed feature set**, removing stale claims of AI/GIF/Telegram support if unsupported. Historical changelogs remain as-is.
4. Review each `fastlane/metadata/android/en-US/images/phoneScreenshots/{1..14}.png`: they reflect an old upstream keyboard. Queue authentic brrBoard captures after the new APK is installed; never manufacture UI captures or modify screenshot contents deceptively. If this listing is dormant, label assets as legacy or update appropriately without falsely claiming Store publication.
5. Preserve older `assets/icon/FrostKeys_Icon_with_bg.png`, `art/launcher_icon/*.svg` and `art/stitch_frostkeys_animated_setup/*` as historical design evidence if referenced by docs/license. Make the **new** source directory unambiguous. Only delete orphaned `ic_frostkeys_icon.xml` or `setup_welcome_image.png` with explicit dependency proof and safe scope; removing old art provides no user-visible improvement by itself.
6. The **entire premium README and GitHub first-impression redesign is now in scope for the user-authorized future `Go`**. Execute the independently source-audited `GITHUB_README_PREMIUM_PLAN.md` and `README_COPY_BLUEPRINT.md`: desktop/mobile light/dark hero, truthful real-APK gallery, accurate download/install links, corrected Obtainium package, contributor templates, GitHub About description/topics/homepage/social preview (1280×640, applied via repo Settings), new current Fastlane feature graphic if stale, and coherent future release descriptions. Check the specific owner decision for whether to enable currently disabled Issues; never silently create a public support channel. Changing the owner's GitHub profile avatar, unrelated sites, or Wiki content is *not* implicit authorization.
7. **Premium color specification:** `PREMIUM_COLOR_SYSTEM.md` is the single source of truth for all surrounding-surface brand tones and light/dark variations, visually previewed in `PREMIUM_PALETTE_PREVIEW.svg` / `.png`. Keep the preapproved mango/glass/blue-keyboard icon untouched. Verify actual material, text and gradient contrast rather than mechanically applying token hex values to every element.

## 5. Automation and regression coverage

### Proposed tests (new test files, not yet implemented)

- `app/src/test/java/helium314/keyboard/latin/settings/BrandResourceContractTest.kt`: Android resource inflation for primary/compact/round/foreground/themed, bitmap dimensions and background safety, content provider/provider label sanity, version-specific drawable validity.
- `app/src/test/java/helium314/keyboard/latin/settings/BrandPlacementContractTest.kt`: static source/resource checks of the About leading logo slot and no Material tint, `AboutScreen` first APP row, manifest icon references, no forbidden icon changes to functional toolbar/Emoji/IME subtype assets. Prefer robust composable tests over brittle string matching when feasible.
- Pure `MangoLoadsIntroMotionSpecTest.kt` if extracted a pure immutable timeline: durations, initial/final state, no infinite repeats, clamp bounds, reduced-motion 0ms, cancellation on exit. Simple screenshot/UI test with Compose test dependency only if the existing test infrastructure supports it without unreviewed infrastructure changes.
- `tools/test_mangoloads_assets.py` or equivalent local/CI script: source art hashes/validated exact text (reviewed visually), pixel dimensions, nonempty alpha for transparent layers, correct aspect ratio, expected density outputs, no excessively large source loaded into 32dp row.
- Re-run **existing** `RoundedIconSurfaceTest`/`IconDefaultPreferenceTest`, full Android unit suite, no-internet/voice-model packaging contract, and present signing/package assertion. The brand must not disable other tests.

### Static + manual visual baseline

- Archive a pre-change launcher/settings/wizard screenshot or render only as evidence, not as a branding deliverable.
- After code integration, capture real screenshots/screen recording: launcher with multiple masks; Settings list About row; About APP row; welcome start/settle; Welcome→Setup; dark/light; rotation; 1.3× & 2× font; themed icons; IME picker; root Settings with search/collapsing bar; fake-toast icon if visible.
- Compare small 24/32dp and platform 48/192px icon against primary; switch to compact only where measured legibility makes it necessary, and note exact placement.
- Device speed check on Oppo Reno10 5G: watch welcome 60Hz/90Hz frame pacing (actual hardware setting), confirm no dropped keypresses, no timer after animation, no ANR, no extra startup frame delay or large UI jank. Record measured values; do not invent FPS or latency.

## 6. GO execution batches and precise stop gates

| Batch | Change scope | Gate before next |
|---|---|---|
| A — Asset lock | Canonical layers, transparent foreground, all exports, safe-area previews, brand README | All text, alpha, blur-mask and tiny-size visual checks pass; no jagged edge extraction |
| B — Launcher | Adaptive foreground/background/themed + all legacy/round density resources + store PNG | Resources inflate/compile, OEM masks verified, manifest stable, old launcher replaced |
| C — In-app | About leading icon, About APP row, Settings header if appropriate, new finite welcome animation and responsive sizing | Compose build/tests, TalkBack, no changed setup behavior or extra loops, animation end state matches static mark |
| D — Premium GitHub identity | Premium `README.md`, light/dark hero, real screenshots, `docs/OBTAINIUM.md`, docs index, accurate `CONTRIBUTING.md`/templates, Fastlane icon/metadata/feature graphic, GitHub About/topics/social preview, support-path decision, future release copy as appropriate | GitHub desktop/mobile preview in both themes, real screenshots and working links, no stale branding or false product claims, upstream legal notices intact; repo settings confirmed separately from file commits |
| E — Original-signed APK | Exact-SHA debug/full-unit green, original-signed Obtainium production workflow green, inspect built APK manifest/artifacts, upload verified release | Same app ID/cert and permissions, successful in-place update path, hardware visual QA and clear release notes |

**Commit policy:** small auditably scoped commits to `main` only, preserve work of other agent(s), check HEAD before every push. Each app asset change will trigger the release workflow on `main`; stage commits deliberately to avoid invalidating a running exact-SHA gate. Keep `AGENTS.md` and `PROJECT_CONTEXT.md` in sync at each step. If another agent's active workflow overlaps, stop and reconcile rather than push.

## 7. Deliverable definition

The finished user-visible result is a **single consistent identity**, including: home-screen/app drawer icons, themed round icons, About leading icon (explicit), About APP row, optional balanced Settings header, first-run welcome animation with correct mango/glass brand, smooth wizard transitions, README and current distribution metadata. There is no unwanted logo inside typing, mic, toolbar, Emoji, Clipboard, incognito, suggestions or the keyboard rendering hot path.

The release is **not accepted** until it is built from the exact current code SHA, signed with the original pinned certificate, passes tests, has no network permission regression, is available via the normal Obtainium route, and its actual screenshots/animation have been reviewed on the user's Oppo or a comparable device with any remaining physical QA explicitly labeled open.
