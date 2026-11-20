# brrBoard — premium GitHub README and repository identity overhaul

**Status: RESEARCH AND PREPARATION ONLY. Apply only after the user explicitly says `Go`.**

Prepared 2026-11-20 IST. The live repo is [Batmancpu/brrrBoard](https://github.com/Batmancpu/brrrBoard), default branch `main`, and other chats are currently writing to it. Earlier visual-art preparation is documented in `IMPLEMENTATION_GO_SPEC.md`. This plan expands the project to the entire GitHub *first impression* and the truthfulness/quality of its user-facing documentation. **No repository metadata, README, settings, workflows, screenshots or releases were changed during preparation.**

## 1. Read-only findings: the actual public repository today

| Surface | Observed state on 2026-11-20 | Proposed change after Go |
|---|---|---|
| `README.md` | Short, text-first ~50-line page; accurate brrBoard introduction, minimal visual design, no hero, product screenshots, feature showcase or visual hierarchy. | Fully original, premium product landing page rendered within GitHub's normal Markdown/HTML constraints, with honest quick install. |
| GitHub About description | Still says `Heliboard is the goat (ig)` and quotes `FrostKeys` and `Gemini AI integration` plus casual aside. | Replace with concise, accurate one-sentence brrBoard promise. Preserve upstream credit in README/LICENSE instead. |
| GitHub About website | Empty. | Link to an existing verified Mangoloads/brrBoard website only if appropriate, e.g. `https://batmancpu.github.io/mangoloads/`; no invented landing-page URL. |
| Repository topics | Empty. | Add focused discoverable topics (e.g. `android`, `keyboard`, `ime`, `kotlin`, `privacy`, `offline-first`, `frosted-glass`, `open-source`) subject to final verification. |
| GitHub Issues | `has_issues=false`, even though README, privacy policy and issue templates link to Issues. | **Decision gate:** confirm whether owner wants public Issues enabled. If yes, enable with privacy-safe brrBoard issue forms; if no, remove broken calls-to-action and supply an honest support path. Do not silently open unsolicited public support channels. |
| GitHub Discussions | Disabled, but `.github/DISCUSSION_TEMPLATE` exists. | Do not enable merely for visual polish. Remove or archive misleading active references if any, keep templates historic unless owner asks to open Discussions. |
| GitHub Wiki | Enabled; current content/use not audited. | Check for outdated upstream pages; align navigation or keep wiki unobtrusive. Do not delete wiki content without separate scope. |
| `CONTRIBUTING.md` | Starts as HeliBoard document and points cloning/issue workflow to HeliBoard repository; brrBoard contributors could go to wrong repo. | Rewrite for brrBoard while preserving upstream acknowledgment and GPL compatibility rules. |
| `.github/PULL_REQUEST_TEMPLATE.md` | Still links to HeliBoard's CONTRIBUTING and warns of maintainer availability. | Make a clean brrBoard PR checklist (scope, tests, permission/privacy, original signer untouched, screenshots if UI). |
| `.github/ISSUE_TEMPLATE/bug_report.md`, `feature_request.md`, `other.md` | Already exist but link to upstream HeliBoard reporting guidance and lack prominent keyboard privacy warning. | Update for brrBoard; request OS/device/build/version/repro steps, redact typed text, Clipboard, contacts and audio. Work only if Issues enabled. |
| `.github/ISSUE_TEMPLATE/config.yml` | brrBoard privacy/Mangoloads links correct; `blank_issues_enabled: true`. | Keep or tune according to support policy; don't force issue forms if owner wants quick bug reporting. |
| `docs/OBTAINIUM.md` | **Incorrectly says current package ID is `com.orion.frostkeys.brrrboard`**. Actual production package is `com.mangoloads.board`. | Fix the ID and migration instructions. Preserve existing package/signing/Obtainium update mechanics. |
| `fastlane/metadata/android/en-US/title.txt` | `FrostKeys`. | `brrBoard`. |
| Fastlane description files | Old FrostKeys promotional copy mentions Gemini/AI tools, GIFs, Telegram and other potentially outdated features. | Rewrite from verified *shipped* capabilities, not old marketing text. |
| Fastlane `images/icon.png` | Old purple upstream mark, directly inspected. | New canonical MangoLoads full-color 512px mark. |
| Fastlane `images/featureGraphic.png` | An existing 1024×500 RGB feature graphic is tracked; its visual branding still needs explicit QA against the new identity. | Inspect and redesign the feature graphic to match the approved logo if it is stale, using the channel's actual specification. |
| Fastlane screenshots 1–14 | At least screenshot 1 visibly depicts old keyboard marketing (`Stylish look!`). | Replace stale storefront images with **authentic new APK captures** when that distribution channel is in scope; never imagegen a fake app UI and present it as a screenshot. |
| `.github/FUNDING.yml` | Existing comment explicitly says no donations and points users to star the project. | Leave donation settings unchanged; do not add fundraising/sponsor badges. |
| Latest release page checked | v1.0.173, title `brrBoard Obtainium 1.0.173`, body mainly commit ID, model packaging note and APK SHA-256. | Future release descriptions get polished user-facing changes plus preserved technical integrity fields. No retroactive release rewrites necessary. |
| `README` engineering links | Directly exposes `PROJECT_CONTEXT.md`, `KNOWN_ISSUES.md` and `ENGINEERING_HANDOFF.md` without product/developer information architecture. | Keep available through a compact Developer section or `docs/README.md` index; primary first screen remains about the product/install. |
| Licenses, privacy and credits | Root GPL, Apache notices, CC asset license, README upstream attribution and `PRIVACY_POLICY.md` exist. | Preserve and surface unobtrusively; do not remove required author/upstream credit or misrepresent Apple/Gboard affiliation. |

Live source and settings may change while other agents work. At Go, recheck `main`, latest signed release, docs, workflows, repository API metadata and linked destinations before applying this plan.

## 2. Design goal: GitHub should feel like a mature app landing page

The README is for **three audiences** simultaneously:

1. **A normal Android user** needs to understand what brrBoard is, why it's desirable, what it looks like, whether it is private, and how to install/update within ~15–30 seconds.
2. **A technically curious user** needs honest capability boundaries: offline network permission, local voice model provisioning, separately imported swipe library, supported Android/device constraints, backup migration.
3. **A contributor/reviewer** needs code pointers, licensing, build/test instructions and trustworthy release verification without forcing all the details onto the first fold.

Design principles:

- Apple-like restraint, spacing, rhythm, crisp typography and one strong brand moment. Do **not** mimic Apple's proprietary assets, claim affiliation, or use excessive animations.
- Full-color MangoLoads mark anchors the top; `brrBoard` stays the app name. The ten `MANGO/LOADS` keys and `brr` spacebar are design details, not extra product marketing names.
- Premium does not mean maximal: no ten shields badges, neon gradients, typing GIF loops, giant auto-playing videos, emoji carpets, or walls of collapsible details.
- GitHub README is GitHub-flavored Markdown with limited sanitized HTML, not a full website. Treat its actual light/dark and mobile rendering as the source of truth. No custom CSS/JS, autoplay or page scripts.
- Align hero/title/features/links with the APK that is actually published, not future voice/AI/swipe experiments. Mark unverified or optional items explicitly.
- Keep performance-conscious assets: compressed static media, retina-crisp where needed, legible on 375px phone widths, no dependence on fragile external image hosts for primary branding.

## 3. Visual design system for the README

### 3.1 Color and art direction

**New owner-directed color contract:** Implement the exact surrounding-surface palette and accessibility rules in `PREMIUM_COLOR_SYSTEM.md`. Its visual swatch reference is `PREMIUM_PALETTE_PREVIEW.svg` / `.png`. The proposed premium visual tones are Obsidian Navy `#0C1527`, Deep Atlantic `#162A45`, Cut Sapphire `#2D60C8`, Glacial Blue `#89ACED`, Moonlit Pearl `#F7F9FC`, Graphite Ink `#17263D`, with highly restrained Champagne Mango `#D6A366`. The icon's **existing** colors/glass are unmodified; the README hero, social preview, marketing graphic, welcome surround and suitable Settings branding are recolored harmoniously around it. GitHub's native Markdown chrome remains GitHub-controlled.

| Role | Proposed design language | Constraint |
|---|---|---|
| Primary visual | Rich brrBoard blue keyboard with warm mango yellow-orange and subtle glass rim | Use the exact approved icon; no extra leaves, drops or random letters. |
| Light surface | Off-white/white neutral with a restrained blue halo | Work with GitHub's white/light page background; no fake whole-page UI. |
| Dark surface | Deep navy/charcoal with cool blue edge illumination | Make glass visible; avoid a white rectangular matte around full icon. |
| Typography | Strong `brrBoard` wordmark and calm, short subheadline | GitHub body uses native fonts. Text inside image is decorative; repeat essential title as real Markdown for accessibility. |
| Accent | Mango gradient only as a small warm counterpoint | Orange is not a global CTA color or a new UI theme. |
| Motion | Static README and screenshots | No animated logo/GIF/loop. The animation remains inside the app's welcome screen only. |

### 3.2 Required new *tracked* graphics after Go

1. `art/brand/mangoloads/readme-hero-light.png` and `readme-hero-dark.png` (or optimized WebP when GitHub verified): suggested composition ~1400×480, icon on one side and tasteful `brrBoard` product wordmark, sufficient safe margins for Github mobile and dark themes. The text can instead stay outside raster to avoid alt duplication.
2. `art/brand/mangoloads/readme-icon-512.png` for README/Docs; use approved icon with correct matte/alpha. Relative paths to a checked-in source, not raw URLs to another branch.
3. `art/brand/mangoloads/github-social-preview-1280x640.png` matching brand but framed for horizontal social cards, with title and one-line subtitle; **do not** stretch square app icon to fill banner. GitHub recommends 1280×640 for best display. It must be uploaded in repository Settings; merely committing the file does not set preview.
4. `docs/assets/screenshots/` with *real* screenshots from the signed identity APK, named by view/feature and theme, e.g. `typing-frosted-light`, `typing-ios-clear`, `emoji-search`, `clipboard`, `settings-about`, `setup-welcome`; no fake composited UI features. Use only a curated subset in the README; preserve originals in an audit location if privacy allows.
5. Optional 2–3 static close-up crops highlighting keyboard keys, glass material and Settings. Cropping/reframing is fine; never erase defects or fabricate UI. Label demo media accurately.
6. A small `docs/assets/screenshots/README.md` recording APK release/source SHA, device, theme and screenshot date. Strip user content, contact names, personal dictionary entries and application notifications before publishing.

**Dark/light implementation:** Use GitHub-supported `<picture>` with `source media="(prefers-color-scheme: dark)"` and a default `<img>`, or GitHub's built-in image variants. Keep alt text and stable relative sources. Test native mobile GitHub, desktop light/dark and high-contrast.

### 3.3 Badge policy

Show at most **three or four verified status badges** near the top. Candidates:

- Actual main debug workflow status (GitHub-hosted badge for `.github/workflows/brrrboard-build-debug.yml`).
- Current release version/link (dynamic or maintained; verify not showing a stale tag).
- GPL-3.0 license link, not a license invented for branding.
- Optional Android 7.0+ compatibility if confirmed from minSdk 24 and actually supported by runtime dependencies; not an unsupported-device blanket promise.

Avoid downloads/stars/view counters, random build shields, tracking-heavy image services and green '100% secure/AI-powered' claims. If dynamic badge URLs are unreliable, remove them rather than leaving broken graphics.

## 4. Exact README information architecture and reading order

Target approximately 140–240 lines of well-spaced Markdown, visually scannable on mobile. Preserve critical safety and license information. Put deep implementation details in linked docs.

1. **Brand hero** — approved icon/wordmark in light/dark image; real Markdown `# brrBoard` for searchability and screen readers.
2. **One-line proposition** — suggested copy: *A fast, private Android keyboard with frosted-glass design and thoughtful iOS-inspired motion.* Do not claim literal Apple Liquid Glass or Apple endorsement.
3. **Minimal credible badges** — CI/release/license, no vanity metrics.
4. **Primary action** — prominent bold link `[Download latest signed APK]` to GitHub Releases latest asset; second link `[Install / update with Obtainium]` to the corrected guide. Make both real clickable links, not button-shaped images without links.
5. **Quick 3-step install** — Download → install → enable/select the IME. State supported Android version and the system keyboard trust prompt in plain language. Separate old-package migration below.
6. **Real visual showcase** — a *single* high-quality full keyboard screenshot or side-by-side light/dark images at sensible width, then 2–3 feature details (Emoji, Clipboard, Settings/setup). Use actual signed build.
7. **Why brrBoard** — three/four concise mini-sections: frosted visual material, fast typing/gestures, local privacy, functional tools. No generic superlatives, no made-up benchmark numbers.
8. **Themes** — show original Frosted Glass, iOS Clear and iOS Tinted only if these are the three real shipped presets at Go. Clarify material/color behavior without promising impossible live refraction.
9. **Privacy** — short trust summary: offline/no Android internet permission, local words/clipboard, user-initiated microphone; link to complete policy. Do not falsely claim every optional third-party action is private or that no data can ever leave other apps.
10. **Microphone and gesture availability** — precise distinction between private offline voice model already installed versus first-time bootstrap, and optional gesture library import/licensing. Avoid scary wall of text in the hero; put a short `Important` note near install/FAQ.
11. **Updates and migration** — stable original-signed Obtainium, versioning, current ID `com.mangoloads.board`, old `com.orion.frostkeys.brrrboard` migration, private unencrypted ZIP caution. Link full steps in corrected `docs/OBTAINIUM.md` or `docs/MIGRATION.md`.
12. **FAQ** — 4–6 targeted questions (is internet required; why input-method system warning; how swipe library works; mic model; upgrade data; where to report private bugs). Use `<details>` only if readability benefits; don't bury install instructions.
13. **Project status** — link current known limitations and a human-readable roadmap; no misleading 'done' status for unverified Oppo tests or planned intelligence work.
14. **Contribute / report issues** — correct brrBoard CONTRIBUTING and active Issues path *only if enabled*. Include a privacy warning before screenshots/logs. Optional GitHub Discussions only if actually enabled.
15. **Credits and license** — fair, visible appreciation of FrostKeys, HeliBoard, OpenBoard and AOSP, GPL/Apache/CC licenses. By Mangoloads with real link, no implied Apple endorsement.

### Mobile-first vertical priority

On a narrow phone viewport, before the user scrolls much they should see: **logo → what app does → Download → Obtainium → one real screenshot**. Avoid a full-screen hero that pushes the APK link below the fold. The desktop hero may be wider, but the title and primary CTA remain natural Markdown text.

## 5. Usability and trust details for a premium README

**Installation**

- Link directly to `https://github.com/Batmancpu/brrrBoard/releases/latest/download/brrBoard.apk` only while that exact asset name exists in current release. Keep a separate Releases page fallback.
- Add a single sentence that users may need to permit APK installation from their chosen browser/file manager; do not instruct disabling Play Protect or security protections.
- Explain that keyboard enablement is an OS action and permission prompts are standard; users should review keyboard trust/privacy.
- Keep old-package migration away from the default brand pitch but easy to find via clearly labeled link.

**Voice and swipe**

- The app currently routes the mic to a private offline model, but normal updates reuse model weights previously installed. Confirm the exact latest behavior at Go before writing *'built-in on first launch'*. The separate bootstrap/recovery release can be large. An inaccurate voice claim harms trust.
- The gesture library has distinct licensing/import requirements; do not market it as bundled if not present. Link `GestureTyping` setup docs if publicly appropriate.

**Security and privacy**

- The APK's SHA-256 and signing certificate are publishable verification data; never include private signing keys, base64 keystores, passwords, internal backups or personal audio logs in README.
- Don't claim ISO certification, audited encryption, Apple-grade security, no-tracking telemetry beyond what source/release checks support. Use actual privacy policy as authority.
- Screenshots must have demo text only. Never expose typed conversations, contact list, clipboard history, private app names, dictation transcript, or credential prompts.

**Accessibility**

- Every meaningful image has concise alt text. If logo illustration repeats visible `brrBoard`, avoid redundant wordy alt text.
- Contrast and readable font: no gray 12px caption baked into full-width banners, no blue text on blue gradient, and all essential information outside images.
- Header links can be keyboard navigated normally; no custom interactions. Don't rely on GIF animation to explain app functionality.

**Performance**

- Hero preferably <500KB after reasonable optimization without text/fringe degradation; each README screenshot optimized, target a reasonable total under a few MB. Exact budgets validated by rendered output.
- Keep all core assets repository-local via relative links so branches and forks render. CDN-hosted images only if permanent, trusted and necessary.

## 6. Whole GitHub repository presentation, beyond README

### 6.1 Repository About sidebar and discovery

- **Proposed description:** `brrBoard — a fast, privacy-first Android keyboard with frosted-glass themes, fluid typing and on-device tools. By Mangoloads.` Refine length to GitHub's allowed field during implementation.
- **Homepage:** use the verified Mangoloads URL unless a dedicated brrBoard site exists. No blank marketing link or dead URL.
- **Topics:** choose 6–9 actual descriptors; omit `ai-keyboard`, `gboard-clone`, `official-apple` and unsupported `liquid-glass` tags. Example: `android`, `keyboard`, `android-ime`, `kotlin`, `offline-first`, `privacy`, `frosted-glass`, `open-source`.
- **Social preview:** upload the curated 1280×640 static image in Settings. Verify link unfurl/thumbnail in at least one preview surface. Do not mistake a committed PNG for an applied repo setting.
- **Website/description/topics preview** should agree with README first sentence, Mangoloads byline and current release.

### 6.2 Releases as the product's update page

- Keep signed `brrBoard.apk`, pinned certificate verification, SHA-256, version tag and currently working Obtainium scheme **unchanged**.
- For **future releases**, make release body scannable: `What's improved`, `What to check`, `Install/update`, `Privacy & model packaging`, `Technical verification` (commit, SHA-256). The version/asset links must be accurate, not manually guessed.
- If workflow generates release body, carefully change only templating after reviewing current `.github/workflows/obtainium-release.yml`. Do not touch signing steps, permissions checks, run-number versioning, debug gate or output name.
- Avoid retrospectively editing dozens of historical release bodies merely to make them uniform. The current and future release experience matters first.
- Optional curated `CHANGELOG.md` or `docs/RELEASE_NOTES.md` for meaningful human updates, but only if maintainable by the existing workflow. No auto-generated fabricated feature summary.

### 6.3 Community and contribution experience

- Rewrite root `CONTRIBUTING.md`: correct repo clone (`Batmancpu/brrrBoard`), supported JDK/Gradle instructions based on real code/workflows, branch policy reflecting owner's main-only preference, test checks, privacy/licensing and screenshots in PRs.
- Replace `.github/PULL_REQUEST_TEMPLATE.md` upstream HeliBoard link with brrBoard checklist: reason, scope, paths, test commands, UI screenshots if relevant, no new network permission without owner discussion, no change to original signer/package.
- If owner enables Issues, update `.github/ISSUE_TEMPLATE/bug_report.md`, `feature_request.md` and `other.md`: brrBoard links and meaningful fields (device/OEM/Android version/app version, steps, expected/actual, logs **without sensitive typed data**). Include privacy redaction note.
- Current `has_issues=false` means README and privacy policy's issue links may lead nowhere useful. **Owner decision**: enable the existing issue tracker or use a verified alternative. Do not pretend the link works.
- Current `has_discussions=false`; no need to enable. If owner chooses to enable Discussions later, inspect upstream `.github/DISCUSSION_TEMPLATE/*` and rewrite for brrBoard before inviting users.
- Add a narrowly scoped `SECURITY.md` only if it provides a genuine vulnerability-reporting path the maintainer can handle (private GitHub reporting if supported), not an invented email address.
- Code of Conduct optional: respectful community guidelines are useful, but not required just to make a checklist green. Avoid busywork.

### 6.4 Documentation, distribution and legacy cleanup

- **Mandatory:** `docs/OBTAINIUM.md` currently names *former* package `com.orion.frostkeys.brrrboard`. Update it to `com.mangoloads.board`, with explicit distinction from older installations, and verify release-title regex, apk filename, signing and model bootstrap notes against current source.
- Create a simple `docs/README.md` index: `Install & update`, `Privacy & data`, `Features / setup`, `Known issues`, `Contributing`, `Engineering archives`. Keep high-churn internal runbooks accessible but not front-and-center in the consumer README.
- Crosscheck root `PRIVACY_POLICY.md`, `STICKERS_README.md`, `CONTRIBUTING.md`, Fastlane metadata and `.github` templates for stale upstream names/links. Distinguish active user-facing claims from legally required upstream attribution and historical files.
- Review Fastlane storefront title, icon, short/full descriptions and all fourteen screenshot files when the signed identity APK is available. If no public storefront uses this metadata, preserve but clearly align the tracked assets for future use.
- Inspect `fastlane/metadata/android/en-US/images/featureGraphic.png` (1024×500 in the audited tree) and prepare the matching banner only if this storefront surface is used; do not conflate this with GitHub's independently applied 1280×640 social card.
- Leave `.github/FUNDING.yml`'s no-donations position untouched. Confirm GitHub's visible Sponsor UI, if any, agrees with that policy; do not manufacture a funding channel as a design embellishment.
- Optionally review how the pinned repository tile appears on the maintainer's GitHub profile, but changing account avatar, profile README, profile bio, organization branding, or an unrelated Mangoloads website requires distinct owner authorization. They are not implied by a repository README redesign.
- Review enabled Wiki links/contents; archive/redirect outdated active pages only with explicit scope. No unrelated deletions.
- Don't modify project source code, signing, permissions, version number or settings only for README polish. Keep app identity release and docs redesign coordinated but separately testable.

## 7. Exact change/file map when Go

| Target path / setting | Action | Build/behavior risk |
|---|---|---|
| `README.md` | Fully rewrite layout/copy with reviewed assets, accurate install/feature/FAQ/privacy/credits | Docs-only; links/asset rendering require QA |
| `art/brand/mangoloads/readme-hero-{light,dark}.png` | Create brand hero variants with consistent typography and no white matte | Static assets; optimize size/alpha |
| `art/brand/mangoloads/github-social-preview-1280x640.png` | Create horizontal share preview | Only effective once uploaded via repo settings |
| `docs/assets/screenshots/` | Add curated **real** screenshots of signed APK and provenance text | No fake/mislabeled shots, privacy-sensitive |
| `docs/README.md` | Create a navigable documentation map | Docs-only |
| `docs/OBTAINIUM.md` | Fix wrong package ID, verify filter/setup notes | Critical install correctness, no app code |
| `CONTRIBUTING.md` | Replace misleading HeliBoard contribution instructions with brrBoard-specific ones | Keep original upstream acknowledgment |
| `.github/PULL_REQUEST_TEMPLATE.md` | brrBoard-specific review checklist | User-facing contributor experience |
| `.github/ISSUE_TEMPLATE/*.md`, `config.yml` | Rewrite links/privacy wording **if** Issues enabled | External issue setting is separately reviewed |
| `fastlane/metadata/android/en-US/{title,short_description,full_description}.txt` | Correct product identity and claims | Store copy only; cannot claim publication |
| `fastlane/metadata/android/en-US/images/icon.png` | Replace purple upstream icon with canonical MangoLoads logo | Store listing icon only |
| `fastlane/metadata/android/en-US/images/featureGraphic.png` | Inspect and refresh as needed to match the approved brand, preserving storefront dimensions | Independent from GitHub social preview |
| `fastlane/metadata/android/en-US/images/phoneScreenshots/*` | Replace stale files only with approved real screenshots | Authenticity and privacy QA |
| `.github/workflows/obtainium-release.yml` | Optional future-release-body template edit only if required | Signing/version/test logic **must not** change |
| GitHub repo Settings: About description, homepage, topics, social preview, Issues | Apply individually after confirming current config and owner choice | External metadata writes; no Git commit necessarily |
| `AGENTS.md`, `PROJECT_CONTEXT.md` | Record exact change/batch/result; linked artifact filenames and URLs | User-required continuous handoff |

## 8. Delivery batches and verifiable stop conditions

1. **Gate R0 — Refresh:** check live `main`/in-flight workflows and repo Settings. Coordinate with other chat. Extract actual feature truth, current package, release asset, screenshots and applicable docs. No edits yet.
2. **Gate R1 — Design:** lock README hierarchy, product copy and icon/hero/preview art; render desktop light/dark and mobile-width prototypes from draft. Confirm content with owner if any material unknown (Issues policy, public support channel, social preview).
3. **Gate R2 — Draft assets:** produce the actual two theme heroes, social preview, source PNG/WebP, authentic screenshots, alt text, dimensions and provenance. Preflight images on white/black backgrounds and GitHub markdown rendering.
4. **Gate R3 — README/doc updates:** rewrite `README.md` plus `CONTRIBUTING`, `docs/OBTAINIUM`, docs index, relevant templates and Fastlane copy in a coherent small batch. Run Markdown/links checks. App source untouched by this docs-only batch.
5. **Gate R4 — Repository settings:** update description, homepage, topics, social preview with owner choice. Resolve Issues-disabled contradiction (enable after consent, or fix links). No unnecessary Discussions/Wiki changes.
6. **Gate R5 — Release alignment:** integrate canonical identity assets and signed APK release from companion plan; keep current signer/Obtainium. If human-friendly release-note templating is approved, change *only* the release body output and validate exact current pipeline. Avoid accidental duplicate releases from other agent's commits.
7. **Gate R6 — Final audit:** GitHub desktop/mobile dark/light, readable logo and screenshot sizing, no broken links/images, no false release badge, verified APK/latest download, correct package/migration docs, truthful feature descriptions, licensing intact, working support path, updated `AGENTS.md`/`PROJECT_CONTEXT.md`.

**Failure/stop rules:** If GitHub Issues remain intentionally disabled, remove broken issue links instead of enabling support without approval. If screenshots cannot be captured from a verified actual signed build, publish tasteful static art and defer gallery; never fake. If copy cannot be verified from live app, omit/qualify claim. If another workflow is active or HEAD moved, pause the overlapping write and re-evaluate. If external repository settings cannot be changed with available permissions, tell owner exactly which settings to click; do not pretend they changed.

## 9. Quality rubric — what “premium” means here

- A newcomer can understand, **download**, and **enable** brrBoard without reading engineering docs.
- Visually consistent MangoLoads full logo, no mismatched upstream purple icon, no Gboard/Apple brand confusion.
- README has at most 3–4 badges, 1 primary CTA, curated real product imagery, clean whitespace and concise sections.
- On phone light/dark themes, no cropped text, horizontal scroll due to tables, unreadably tiny screenshots or huge blank above-the-fold hero.
- GitHub About description, topics, homepage and social preview all agree with the README; release page is trustworthy and easy to understand.
- The public project does not claim AI/Gemini features it has removed, does not mislead about offline model bootstrapping, does not expose signing secrets and does not incorrectly list old app IDs.
- Privacy-safe contributor/reporting templates and accurate links, with upstream licenses and author credits retained.
- Docs-only work does not slow, break or alter the APK, signing or existing keyboard workflow. No repo edits before `Go`.

## 10. Authoritative platform references reviewed

- GitHub [README relative links and images](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/about-readmes).
- GitHub [writing with light/dark images](https://docs.github.com/en/get-started/writing-on-github/getting-started-with-writing-and-formatting-on-github/quickstart-for-writing-on-github).
- GitHub [social preview image](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/customizing-your-repositorys-social-media-preview) recommends best display at 1280×640.
- GitHub [community profile checklists](https://docs.github.com/en/communities/setting-up-your-project-for-healthy-contributions/about-community-profiles-for-public-repositories) and [issue template requirements](https://docs.github.com/en/communities/using-templates-to-encourage-useful-issues-and-pull-requests/configuring-issue-templates-for-your-repository).
- GitHub [repository topics](https://docs.github.com/en/repositories/managing-your-repositorys-settings-and-features/customizing-your-repository/classifying-your-repository-with-topics) and [release notes](https://docs.github.com/en/repositories/releasing-projects-on-github/about-releases).
