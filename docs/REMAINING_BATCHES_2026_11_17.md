# brrBoard — Remaining Engineering Batches (November 17, 2026)

Owner: continuing implementation engineer. Branch: `main` only. Target: Oppo Reno 10 5G. This is the live execution and acceptance sequence, not a claim that untested work is done.

## Non-negotiable surfaces and release policy
- Whole IME: one visually continuous native/fallback frosted surface sampling host-app background when Android allows it.
- Internal panels: separate content-aware local blur from the *panel's own scrolling Emoji, Clipboard or Toolbar content*, beneath fixed sharp controls, including spacebar and footer. Never double-blur the host app to simulate the inner blur.
- Keep Original Frosted Glass, iOS Clear and iOS Tinted; don't globally tint the backdrop.
- Avoid Apple-proprietary symbol assets/fonts. Closely match visual proportions with original or redistributable vectors; keep optical size, weight, tint and hit targets consistent.
- Keep original production signing / in-place updates. Ship from GitHub CI only after required unit tests and signed verification pass.
- Each batch gets durable context, tests or explicit physical-device checks, and a clear release checkpoint.

## Ordered batches

### P0.A — Recover glass and Emoji Search foundation
State: Source commit `93a437ac`, released original-signed as v1.0.83; physical acceptance PENDING.
- Remove opaque internal glass underpaint and footer black side gutters; local CLDR English Emoji search fallback; ABC exits search.
- On-device gate: yellow/red Emoji and Clipboard images move beneath the softened spacebar/footer; no artificial black/grey slabs, no top blank block, Search returns English results and ABC exits immediately.

### P0.B — Cohesive Apple-inspired icon family (STARTED)
- Original 24x24-optical icon family for Emoji/ABC/microphone/Toolbar, search/delete/return/globe/Clipboard, with unified keyboard/Toolbar mapping and consistent Emoji-category vector weight.
- Keep user custom icon overrides; all themes honor foreground tint; avoid shipping Apple-proprietary files.
- Fix tiny category icons caused by 6dp insets, improve unselected contrast, reduce selected-category oversized circle.
- Gate: no hollow/thin/black mismatched icons on bright or dark host app, icons identifiable at phone size, buttons remain accessible.

### P0.C — Shared real panel geometry and compositor validation
- Audit `PanelMaterial.extendIntoFooter` and coordinate mapping of RenderNode content; eliminate artificial overlay bars, stray overflow, footer/lower-strip slabs, clipping seams and duplicate background painting.
- Ensure one moving vertical content source, fixed glass header/controls, localized top fade versus hard-start bottom material, correct touch exclusion.
- Gate: visual frame-by-frame swipe behind both bars on Emoji, Toolbar and Clipboard on Oppo, with no ghosting, black gutters or host-app double sampling.

### P1.A — Emoji navigation and Search behavior
- Complete three QWERTY letter rows and normal bottom row during Search, one horizontally scrolling results row, reliable English CLDR index and no host text leaks.
- Finger-following horizontal category pages, spring-back and fling thresholds, preserve per-category vertical rows, proper gesture disambiguation.
- Gate: slow partial drag settles correctly; horizontal and vertical gestures don't interfere; Search smile/heart/coffee and no-match state work.

### P1.B — Clipboard/Toolbar interaction design
- Centered fixed headings, image cards with natural aspect ratio, pinned/text/rich-content behavior, control row alignment and cross-panel consistent glass.
- Remove duplicate ABC/back buttons and card overlap, ensure undo/clear/pin safety and repeated opening never crashes.
- Gate: image + text clipboard populated and empty, scrolling behind glass, all legitimate tools operate.

### P2.A — Normal typing geometry and theme reconciliation
- iOS-like key shape/size/spacing, bottom-row and suggestion centering, Search/Enter variants, keyboard height and no unexplained navigation gap.
- Preserve 3 themes, glass accent restraint, brrBoard caption, system dismissal animation, globe hold menu, big number pad hold-123.
- Gate: day/night, portrait/landscape, Chrome/WhatsApp/Instagram keyboard input with consistent geometry.

### P2.B — Dictation and audio integration
- Diagnose Oppo permission/AppOps/RecognitionService with sanitized stage/error logs; verify English language output and external Dictate compatibility before changing audio architecture.
- No raw speech, typed text, passwords or Clipboard in diagnostics; no unsolicited upstream issue.
- Gate: start/cancel/transcribe with correct English and no silent audio-capture failures.

### P2.C — Swipe typing, performance and accessibility
- Confirm shipped gesture data/library, long typing responsiveness, low GC over scrolling, stable panel height, TalkBack/touch targets, regression coverage and signing stability.
- Gate: stable performance and zero critical crashes in physical-device run.

## Current acceptance truth
Automated compilation, local tests and release signing cannot prove glass quality, Apple-like optical balance, microphone access on ColorOS, or actual swiping behavior. Those are physical-device gates. Do not close these batches based solely on a green CI badge.

## November 17 changes after physical-device comparison
- P0.B icon set was released v1.0.84 after both CI gates completed; await Oppo icon acceptance.
- P0.C source patch: no extra tint when internal content is absent; local 18dp live blur of in-panel source, top alpha-masked fade; Toolbar's title moves to Clipboard's top-header position and the original suggestion strip disappears; pinned tools move into Toolbar grid. Await CI and phone proof.
- P0.D local-only full brrBoard backup: MediaStore Downloads/brrBoard export and import of custom swipe native module with stored checksum, learned dictionaries, settings, layout and database, plus safe non-recursive restore. This must be physically verified BEFORE app ID changes.
- P1.D new iPad image requirements: default visible Search Emoji field at top, Emoji category row near bottom, higher keycap contrast only, contextual 'Paste' or 'Paste image' without unverified source attribution. Plan an explicit structural repositioning, not an overlay.
- Package-ID identity rename: BLOCKED until user has a verified local backup/restore. Changing `com.orion.frostkeys.brrrboard` will break in-place update and Android will isolate the old data; give a migration procedure and require explicit phone acceptance. Clean Frost/Orion UI branding independently where safe; code package renames only when compilation and manifests are checked.


## 2026-11-17 P1.E — iPad-like Emoji navigation and contrast (source checkpoint)
- Oppo/iPad reference demanded an always-visible "Search Emoji" pill at the TOP of Emoji browse, vertically scrolling content underneath, bottom Emoji category icons plus Delete directly above the shared ABC/mic footer, and no second top category/suggestions strip. Reparent the *existing* category views into one 50dp fixed bottom navigator (no duplicate buttons) and hide the obsolete Emoji bottom spacebar/search/delete keyboard in browse. Preserve it in code for compatibility while routing new explicit Delete through the host input listener.
- Put a fixed tappable browse search pill over the full-width pager. Tapping it enters the established one-horizontal-results-row Emoji Search with a complete alphabet layout; exiting Search restores browse search and category strip. The grid receives 60dp top padding so the first interactive row is not hidden beneath the pill, but scroll content can still move behind fixed frosted controls. Pager extends into footer per current PanelMaterial mechanism.
- Default alphabet/special keycap opacity increases slightly from 98/93 to 128/123 (light/night) for physical key identification. A one-time migration updates only values equal to previous defaults; explicit customized transparencies and the *overall keyboard background glass* are unchanged. No new global blur or tint.
- Caveat: no Android device screenshot is yet available for this source checkpoint. CI compile/tests, original signing and actual Oppo visual acceptance are separate gates. Inspect top pill placement, bottom tab touch widths, scroll under internal glass, Search/ABC transitions, all keyboard rows and low-memory performance. Do not equate test green with visual acceptance. Source committed on main only; monitor workflows continuously during this active session.
- Identity migration remains BLOCKED on local backup/restore validation; no package ID or signing key changed. Future contextual clipboard chip should say just "Paste"/"Paste image" unless provenance is verifiable.


## 2026-11-17 — Identity cutover and visual defects after owner backup confirmation

Owner confirmed a full backup is saved in phone Downloads and explicitly authorized a breaking applicationId change; manual restore remains untested. Production identity: **brrBoard**, attribution **By Mangoloads**, exact Android package **com.mangoloads.board**. Production Obtainium variant drops old .brrrboard suffix; DEBUG remains separate. This is a NEW Android installation, side by side with former com.orion.frostkeys.brrrboard. Original release signing keystore/alias and CI secrets are intentionally unchanged; never invent a new production signer. Obtainium users must add/update the app's package source; no in-place migration is possible. Do not delete the old app until backup ZIP import and data/settings gesture library verification on the new app.

Implementation checkpoint: About links now point to repository, Star (no donations), Issues, Mangoloads site; repository README/privacy/issue routes updated; funding config no longer advertises upstream donor; upper-right Clipboard Clear UI hidden per owner (preserve deletion via item actions); suggestion strip no longer reserves hidden persistent toolbar slot and uses brighter iOS-inspired dark separator strokes; rounded outer keyframe highlight uses actual corner arcs instead of doubled quadratic junctions. Source and release manifest/package assertion must pass original-signed CI; device acceptance is PENDING.

Remaining HIGH priority: physically verify v1.0.86 top Search Emoji pill, footer category nav, contrast; live content-aware internal glass under footer and Toolbar top, zero static blur on empty regions, proper clipping of sharp source; original consistent icons for all controls and tools including one-handed/split; Clipboard local text search newest-first left of spacebar and full isolated search keyboard; no dead ABC/Emoji Search; IME suggestion center/three slots at Oppo width; Dictate technical diagnosis, not guessed permission changes; all mode transitions, swipe input and undo/redo stable. Detailed screenshots on user device prove defects, not source fixes. Do not claim a CI-green APK is visually accepted. Preserve legally required open source attribution and package namespace until safely migration-reviewed; user-facing identity must not expose stale Frost/Orion brand.


## 2026-11-17 — Owner screenshot review: top glass highlight correction and four live options

**Evidence:** latest Oppo photo shows brrBoard over setup screen with top highlight touching both vertical edges, resembling a complete white keyboard outline; owner provided an earlier screenshot with a floating top specular line and clearly requested restoration of its glass-slab illusion. Do NOT replace it with a full border. The source in RoundedKeyboardFrameView.drawTopGlassHighlight from 9ed25a2 used 180→270 and 270→360 quarter-circle arcs, which extended to the far outer edges and created the perimeter effect. The earlier rounded concept is the intended visual direction, but specular strokes at far left and right must fade away.

**Implementation:** added Settings.PREF_KEYBOARD_TOP_HIGHLIGHT_STYLE with persistent default 0 (Classic Lift) and three non-default selectable variants: 1 Soft Horizon, 2 Studio Glints, 3 Quiet Edge. Classic Lift uses partial symmetrical arcs (214°→270°, 270°→326°), preserving a lifted glass top without reaching the side edges. A horizontal alpha fade eliminates bright endpoints, and all variants remain open TOP-only marks, never a closed border. Shadow/backdrop/internal panel blur responsibilities are unchanged. RoundedKeyboardFrameView listens to style preference changes and redraws only itself.

**In-app interactive preview:** Appearance → Adjust Frosted Glass now has four styled options directly underneath its existing input field. The test field requests focus so the actual brrBoard IME appears while the user switches styles; after each selection focus returns to the text field. Changes are previewed live by the IME via preference observer without dismissal. Cancel restores the previous style; Save retains it; Reset returns to Classic Lift. The test field and the four buttons remain in a scrollable dialog. This visual behavior is source-intent, PENDING actual Oppo acceptance.

**QA gates:** compile, Android unit tests, production signing and release workflow, then real Oppo dark/light visual acceptance and interaction. Test no bright vertical corner stubs, no full outline, no clipped first/last key, preview keyboard stays visible when switching all four modes, saved choice survives restart, Cancel restores previous choice. Do NOT claim visual acceptance from CI. Existing Dictate audio-capture error visible in the older reference remains a separate unresolved diagnostic issue, not caused or fixed by this highlight change.

## 2026-11-17 — Restore first-ever glass highlight, expose chooser in Settings search

USER: Latest highlight does not look like original first lifting glass edge; search for "highlight" in Settings finds nothing. Restore the original highlight as default while removing hard side-tip border. Retain three alternatives.

HISTORY CHECK: RoundedKeyboardFrameView.kt first gained drawTopGlassHighlight in a271e5a63c55 (2026-11-16); 10ca174c24d8 earlier did not have the method. Original path: moveTo(edge,radius), quadTo(edge,edge,radius,edge), lineTo(width-radius,edge), quadTo(width-edge,edge,width-edge,radius). 9ed25a2 replaced quadratic corners with 90-degree arcs; 093b06f trimmed arcs to 56 degrees. Both altered the visual silhouette against user's original reference.

SOURCE CHANGE: Style 0 now uses the exact a271e5a quadratic corner geometry ("Original Lift"), applying only the later horizontal alpha fade at far left/right to avoid hard perimeter. The remaining Soft Horizon, Studio Glints, Quiet Edge options are untouched. No glass backdrop, wallpaper blur, or key layout alterations.

FINDABILITY ROOT CAUSE: Four cards were only inside the "Adjust Frosted Glass" dialog and its listed title did not contain "highlight", so Settings search could not discover them. Appearance now lists a separate, searchable "Keyboard highlight styles" preference opening the existing live-IME dialog scrolled to the four cards. The Adjust Frosted Glass entry remains unchanged in purpose, all share the same persistent preference, Save/Cancel/Reset behavior.

VALIDATION STILL REQUIRED: Debug compile, unit tests, original-signed CI run, physical Oppo Reno 10 5G review. Confirm original lifted top silhouette, soft side fade, no bright vertical edge, style chooser from search, live keyboard preview and Save/Cancel/Reset, retained existing settings. Source edits alone do not confirm the visual result.

NEXT SEPARATE BATCHES: neutral-palette action Search key tint (CIELAB matching forces muted gray to orange); Emoji top fixed category strip, central emoji grid, fixed Search/Space/Backspace above ABC/mic with content-only live blur; clipboard search/clear and screenshot audit; toolbar icons/scroll clipping; suggestion divider height; optional low-energy motion-reflection. Keep commits focused and append-only agent/context history.

### 2026-11-17 follow-up — restore first-highlight luminance under a fade shader

Review of RoundedKeyboardFrameView.reflection showed both Paint.color alpha and LinearGradient color-stop alpha set to the same sub-opaque value. Canvas combines these opacities; the intended original 0x52/0x30 bright top line became excessively faint under the new fade. When a shader owns the fade, make Paint.color opaque white; preserve the per-style gradient alpha exactly, and leave the non-shader fallback using its previous alpha. This restores the luminosity of the a271e5a original geometry with the desired small side fade, without touching real blur or adding double blur. Verify visually on Oppo after signed CI.

## 2026-11-17 — Focused batch: neutral Material You Search/Enter accent

USER: Oppo Reno 10 system Material You palette is gray, yet action Search key is peach/orange. Want only action key tinted when intentionally colorful, with contrast and subdued Apple-esque vocabulary, never a tinted whole glass slab.

ROOT CAUSE: KeyboardTheme.nearestAppleAccent() unconditionally finds a nearest member of a saturated twelve-color iOS palette in CIELAB, including apple orange and brown, even for near-neutral Android system_accent1_500. That large chroma jump breaks users' neutral grayscale wallpaper palette. Both iOS Tinted Search and optional OG Frost action tint use this function; their text colors already use a contrast check.

IMPLEMENTATION: After the existing CIELAB conversion, compute chroma hypot(a,b). If chroma < 26, preserve L* and return neutral LABToColor(L*,0,0); otherwise use unchanged nearest-Apple palette selection. Scope limited to action-key accent mapping, no changes to background blur, main surface tint or user theme preference. New Robolectric regression test AppleAccentSelectionTest checks gray/taupe neutral behavior, colorful Apple red identity mapping and approximate lightness preservation.

ACCEPTANCE: CI compile/test and original signed APK required; physical Oppo needed to verify neutral Search key under actual wallpaper, legible search icon, optional vibrant theme still colorful and no full-panel tint. No visual acceptance claimed yet.

NEXT: Emoji browse search/category/grid/footer restructuring in separate batch, clipboard text search, icon redesign, dividers and motion highlight after this.

## 2026-11-17 — Focused Emoji browse repair: fixed top categories, Search/Space/Delete row, clean live bottom glass

USER-REPORTED BUG: Emoji Browse sometimes shows Search Emoji text drawn directly atop emoji glyphs; the search field and categories can disappear during swipes, spacebar is missing; long dead vertical area and ugly overlap. Explicitly revert the experimental iPad-style top Search Emoji pill and bottom category navigator. Desired fixed top categories, central continuously scrolling/swipable emoji pages, fixed Search / Space / Delete row immediately over ABC/microphone. Keep complete search keyboard as a separate mode when pressing Search. The lower blurred row must blur only emoji pixels and never reblur/tint the host screen.

ROOT CAUSE: commit 1d5d212 moved the existing category view from external strip into a transparent overlay at the bottom of emoji_grid_shell and added another transparent Search Emoji overlay at its top, both over a full-height sharp ViewPager2. The labels and emoji glyphs literally painted into the same pixel region. It also hid bottom_row_keyboard and instructed KeyboardSwitcher to hide the upper strip, so search/spacebar vanished. Native panel glass blurred a copy of the pager but its sharp underlying pixels still drew in the same region (double exposure).

FOCUSED CHANGE: Restore category tabs to their original fixed upper strip without reparenting; show strip in KeyboardSwitcher.setEmojiKeyboard. Remove the erroneous browsing Search Emoji top pill and bottom category strip from the layout; reinstate native bottom_row_keyboard and its existing built-in Search/Space/Delete action row. Keep the full Emoji Search editing flow untouched (search icon opens search mode; back/ABC return as designed), preserve virtualized ViewPager2 scrolling and scroll-driven glass invalidation. Hide the now-unnecessary top emoji glass overlay so first emoji row is crisp directly beneath categories (no blank fade band).

LIVE GLASS CORRECTION IN THIS PANEL: The new EmojiGlassFrameLayout clips only the sharp pager draw at the start of emoji_bottom_glass, while PanelGlassOverlayView still independently records the complete live pager into GPU RenderNode for blur under the fixed action row. The shared footer can sample those continuing real emoji pixels, without duplicating sharp emoji under the blur and without capturing wallpaper or double blurring the host-app. No CPU bitmap storage; bottom row click targets and scroll source remain unchanged. This specialized mask does not alter Toolbar or Clipboard sources.

VERIFICATION: GitHub debug build and Android unit tests, original-signed Obtainium release, then Oppo Reno 10 5G on-device tests. Inspect browse start under light/dark background, 10 category clicks, rapid horizontal/vertical swipes, first/last emoji row, no overlap/lost tabs, Search/Space/Delete event accuracy, full Emoji Search typing/results and ABC return, and lower blur with zero emojis and colorful emojis. Automated build cannot certify physical layout.

RELEASE NOTE: intermediate original-signed run #91 on 7cb0010 failed unrelated online XLinkTest.otherLinks (assert HTTP 200 at XLinkTest.kt:79; 245 tests/1 fail), despite correct compile. Newer signed run #92 and debug run #182 on cf1ad80 passed; v1.0.92 published. Avoid altering unrelated website links during this focused Emoji fix unless a recurring release gate failure proves it necessary.

FUTURE MOTION REFLECTION SCOPE CLARIFICATION: User explicitly restricts gyroscope-controlled glow/prismatic effects to the NARROW TOP HIGHLIGHT WINDOW and its rounded tips. NEVER overlay moving light on keyboard keys, footer, emoji or whole keyboard. Make optional and battery-efficient after Emoji/Clipboard/Toolbar stability. Preserve all earlier history.

### 2026-11-17 Emoji browse spacing follow-up

During post-commit layout audit, EmojiCategoryPagerAdapter.onBindViewHolder still reserved 60dp of empty top padding for the now-removed experimental Search Emoji pill (introduced in 1d5d212). Restore the 8dp top grid padding from pre-change 7830f85, retaining the bottom space needed to scroll the final row above native Search/Space/Delete and ABC/mic. This specifically eliminates the large blank band between the restored top categories and first emoji row. Treat the subsequent signed release as the visual-test candidate; never test an earlier intermediate layout release as final. No changes to search indexing or user data.

## 2026-11-17 — Focused Toolbar live-glass overlap repair (candidate pending CI/Oppo)

OWNER REQUEST: After the signed v1.0.94 Emoji Browse repair, proceed to the next most consequential defect one issue at a time. The Toolbar's supposed live blur can look like a frozen/stamped background, shows doubled/ghosted icons and exposed tiles behind its fixed title and ABC/microphone area. Do not touch overall theme, iOS icon artwork, user dictionary/backup, package identity, signing keys, top highlight shape or future motion-glow. The gyroscope effect, when later attempted, remains strictly INSIDE the narrow top highlight window, never across keys/panels/footer.

CAUSAL CODE AUDIT: AccessPointMenuView already invalidates its top and bottom PanelGlassOverlayView plus BoardFooterView whenever ScrollView scrollY changes, and PanelGlassOverlayView re-records the CURRENT ScrollView into a GPU RenderNode on every invalidated draw. Yet the same ScrollView still renders sharply as the first child of the Toolbar FrameLayout directly UNDER both glass overlays. The two copies occupy identical pixels, creating double exposure/weak blur. Separately the common RenderEffect blur used Shader.TileMode.CLAMP; at an empty capture edge CLAMP can extend the last nontransparent edge texels into the blurred region, resembling static leftovers. Android's official RenderEffect/RenderNode references explain blur edge treatment and display-list recording: https://developer.android.com/reference/android/graphics/RenderEffect and https://developer.android.com/reference/android/graphics/RenderNode.

FOCUSED IMPLEMENTATION: Add ToolbarGlassFrameLayout and make it the root of access_point_menu.xml. On Android 12+ hardware Canvas, drawChild clips ONLY the sharp access_point_scroll to the open central band between the real measured header and bottom overlay edges. The fixed title and bottom glass instead receive ONLY the already implemented live RenderNode blur of the unclipped ScrollView; an overlay's call to content.draw(recording) bypasses the parent's sharp-view clip. This is the same proven architectural separation introduced for EmojiGlassFrameLayout, applied solely to Toolbar sharp-source composition. Leave older/software rendering as a conservative unclipped fallback, rather than blanking controls. Use RenderEffect TileMode.DECAL in common PanelGlassOverlayView instead of CLAMP, so out-of-source edge pixels become transparent rather than repeated; no new tint, CPU bitmap or host-app screenshot/blur. A new Robolectric ToolbarPanelCompositionTest validates root, unique ScrollView source and overlay sibling order; it does NOT certify HWUI pixel effects on a physical Oppo.

REGRESSION/ACCEPTANCE: Verify full GitHub debug/unit and original-signed Obtianium workflows, then physical Oppo Reno 10 5G at light/dark backdrop with 0, 1 and many toolbar rows, slow scroll/fling, long press drag, title legibility, first/last tile occlusion, footer ABC/mic button hit targets and 10 open/close cycles. Confirm blurred icons actually MOVE while passing under title and bottom, no duplicate sharp glyphs, ghost pixels vanish once nothing is behind, no gray slab/double host blur, acceptable frame rate. Existing Emoji/Clipboard use the shared DECAL edge policy and must be regression-tested. If perception remains static, gather Oppo video and GPU rendering evidence before claiming fixed. No physical validation yet.

RELEASE CONTINUITY: Previous production #94 (run 44124810511) failed attempt 1 on unrelated XLinkTest.layoutsLinksInternal outbound HTTP HEAD assertion (247/248 passed); without code changes, attempt 2 passed unit tests, deterministic safety gates and original-signing, then published original-signed v1.0.94 APK for exact 48c113c888e81d1579ff3551d8e6858f1fd112f7. Debug #184 (44124810119) also passed. Online link checks are an independently known flaky release-gate problem; preserve and improve local link validation in a separate focused batch. This Toolbar candidate requires fresh workflow and signed-APK evidence; do not mark it shipped or Oppo-accepted prematurely.

NEXT (after physical Toolbar check): Clipboard local substring search (newest matches first), centered Clear Clipboard action; Cupertino-quality original vector icon pass; suggestion divider height and contrast; optional low-duty-cycle motion-highlight INSIDE original highlight stroke; voice recording permission diagnosis. Append-only context history preserved.

### 2026-11-17 Toolbar release-gate repair after first CI attempts (pending replacement gate)

AUTOMATED EVIDENCE: Toolbar candidate main 62b93a611dbc527763ed1822686a0cc2e3d8298f launched debug #185 / 44129045926 and originally signed release #95 / 44129045824. Both compiled all new Java/Kotlin/XML without compiler errors. Both attempt 1 unit gates failed with 72 tests discovered, 22 unrelated Robolectric class initialization failures at MavenArtifactFetcher.java:129 caused by HttpURLConnection IOException while fetching the test runtime; this did not execute application assertions. Both were rerun once unchanged. On production #95 attempt 2, Robolectric loaded and 249 tests ran; only XLinkTest.layoutsLinksInternal and XLinkTest.otherLinks failed due outbound GitHub HTTP HEAD calls despite the tested internal repository paths being present. No APK was signed/published because the gate correctly stopped. Debug attempt 2 status must be checked separately; do not hide failure or claim success.

FOCUSED RELIABILITY FOLLOW-UP: Preserve all functional unit, syntax and original-signing gates. Modify ONLY XLinkTest in the runTests CI build type: 17 links in layouts.md starting app/src/ are checked against real local filesystem existence (strip URL fragments and query) rather than mutable GitHub web HEAD status; external document/wiki links are parsed as URLs and required to have HTTP(S) scheme plus nonempty host, without demanding a live 200 during a build. Existing live reachability and wiki content checks stay intact for non-runTests build types. The knownDictionaries test already bypasses remote Codeberg access under runTests. This eliminates release success depending on transient external GitHub rate limits and makes internal references strictly deterministic. It is NOT skipping application runtime tests or allowing unsigned APKs. Re-run debug/Android and original signer/Android gates at the resulting main commit; both must pass before a new release is delivered. Robolectric Maven download instability remains an external infrastructure risk and needs future caching/provisioning audit if recurrent.

PHYSICAL STATUS: No new user Oppo hardware evidence; do not present toolbar perception or the shared DECAL edge behavior as physically accepted. The last verified signed version stays v1.0.94 until a new successful workflow publishes the new tag. Older history preserved.

### 2026-11-17 — Toolbar live-glass batch source/CI/signing gate closed; Oppo acceptance open

FINAL VERIFIED EVIDENCE: Corrected implementation + deterministic link test on main b23e03c1032a553494a0a6d806cd6e1b7569f995. Debug [#186](https://github.com/Batmancpu/brrrBoard/actions/runs/44131275982) (run ID 44131275982) finished SUCCESS, including Java/Kotlin/XML compilation, Robolectric Android unit tests, and debug artifact. Original signed Obtainium production [#96](https://github.com/Batmancpu/brrrBoard/actions/runs/44131275659) (run ID 44131275659) finished SUCCESS: full Android test gate, deterministic seed/comparator safety gates, APK signing and verification, GitHub release publication. Published [v1.0.96](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.96) with direct original-signed brrBoard.apk https://github.com/Batmancpu/brrrBoard/releases/download/v1.0.96/brrBoard.apk (38,068,572 bytes), release target exactly b23e03c1032a553494a0a6d806cd6e1b7569f995. Main changes since v1.0.94 comprise ToolbarGlassFrameLayout drawing separation, DECAL blur edge behavior, new ToolbarPanelCompositionTest and CI deterministic XLinkTest plus append-only logs. No signing package/cert or private data changes.

PHYSICAL Oppo Reno 10 5G remains PENDING for live blur appearance, no double exposure, empty-space ghost elimination, header/fixed footer composition, latency/FPS and all three themes. Do not confuse successful tests/signature with measured glass quality. User should install v1.0.96 and report toolbar slow scroll/fling with colorful tiles and empty ends. Future visual issues must be diagnosed from new video. This documentation-only checkpoint deliberately uses a CI skip marker so version/source stays v1.0.96; it does not require a second APK for unchanged code.

### 2026-11-17 — P0 Emoji browse seam + blank-band correction (source candidate; CI/signing pending)

OWNER REPORT & VISUAL REFERENCE: On Oppo image, the live colored Emoji blur under Search/Space/Delete and ABC/microphone is finally the approved treatment. Preserve its saturation, transparency and underlying host-glass appearance. Two defects remain: a horizontal join between two separately blurred bottom windows and a wide blank band between fixed top Emoji categories and first Emoji row. iOS 27 reference image informs later original-vector Emoji/mic optical sizing; do not copy proprietary artwork. This is a **one-issue-at-a-time** correction before premium icon overhaul.

SOURCE: `PanelGlassOverlayView.kt` gives BOTTOM GPU RenderNode recordings extra vertical real-source sampling above/below the overlay (1.5x 18dp blur sigma), then crops only the final output. Both adjacent Emoji bottom strips sample pixels across their common edge; no extra gray/tint rectangle, backdrop double-blur, CPU bitmap, or host screenshot. TOP progressive glass stays unchanged. `EmojiPalettesView.onMeasure` reconciles bottom-aligned Emoji body height with existing keyboard wrapper height only when the discrepancy is bounded by 2 suggestion strips plus footer; the panel's bottom action bar/footer remains anchored and search/landscape respect available space. New pure `SharedPanelGeometry.retainExistingPanelViewport` with unit cases for under/over growth/available caps. Existing 8dp RecyclerView top padding is unchanged (was already correct in v1.0.94). The gap cause is a **source-layout inference**, not a measured Oppo layout trace.

STATUS: This document records an unverified source candidate. Do not claim fixed or signed until GitHub debug and original-signed Obtainium release workflows both succeed and a published APK is verified. Physical Oppo validation then checks the seam under moving yellow/orange emojis, no unwanted tint/double exposure, first emoji row proximity to top categories, consistent bottom search/actions, all three themes, landscape, dynamic category/recents changes, frame rate and battery. Next distinct batch: icon inventory, original cohesive premium vector family for toolbar/keyboard/footer/app; keep user overlays intact.

### 2026-11-17 — Owner-approved Emoji Browse top-gap and 3-key width correction (source candidate)

BASELINE: v1.0.97 original-signed release target `9c795f48e4a08cddac7633bafbafec0c90845199`; debug run #187 (`44136360744`) and original-signed production run #97 (`44136361013`) SUCCESS. Direct APK: https://github.com/Batmancpu/brrrBoard/releases/download/v1.0.97/brrBoard.apk. Owner's new photo expressly **APPROVES the moving, colored bottom blur as perfect**, including the seam removal; PROTECT that code and geometry. Owner identifies a persistent empty black region between category strip and first Emoji row, top clipping, a too-wide center spacebar and too-small Search/Delete. Owner explicitly vetoed adding a fourth Action button; retain exactly Search / Space / Backspace in standard Emoji Browse. Do not edit local/whole-window blur, `PanelGlassOverlayView`, `PanelMaterial`, `BoardFooterView`, `EmojiGlassFrameLayout`, original signer or the extra-action layout file.

SCOPE: `emoji_bottom_row.json` changes Search/Delete fractions from 0.15 each to 0.22 each, so the residual Space takes 0.56. `EmojiPalettesView.onMeasure` uses max of established wrapper height and previously measured alphabetic keyboard height, bounded by the existing `SharedPanelGeometry.retainExistingPanelViewport`, because the wrap-content wrapper may shrink when the alphabet view becomes GONE. `updateContextSmartVisibility` now always removes a stale 70dp Smart context row on category exit, even mid-scroll; only *introducing* a new context row is deferred until scrolling stops. Added small pure Java `EmojiSmartRowVisibilityPolicy` and tests; updated Robolectric panel contract to assert exactly three Browse keys and their widths. The prior 8dp list top inset and live pager gestures remain unchanged. These are evidence-based **candidate causes** for the black bar, not a claim of device-proven correction.

RELEASE/TEST GATE: This append-only log is written with the source candidate; full debug/unit/production original signing CI and release link still to be verified. Oppo Reno 10 5G gate: black area gone on first open and after Recents->Smileys while scrolling; no half-clipped top row; Search/Space/Delete optical width comparable to alphabet bottom row; no fourth control; footer/action blur 100% visually unchanged (yellow/orange emoji scrolling beneath), horizontal and vertical scrolling stay smooth, no FPS/battery regression; check all 3 themes, portrait/landscape. Separate premium icon redesign intentionally PAUSED pending approval of this safer corrective batch.
