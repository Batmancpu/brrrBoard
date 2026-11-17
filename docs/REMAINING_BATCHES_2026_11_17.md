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
