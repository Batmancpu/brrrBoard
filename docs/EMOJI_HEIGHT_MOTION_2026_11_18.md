# Deliberate Emoji panel height motion — 2026-11-18

## Scope and owner intent

Owner reports compact Browse, Expanded Browse and Emoji Search jump abruptly between distinct IME heights, and requests a short smooth roll-up/grow/shrink transition. This follows the signed v1.0.116 Search-context/compact-spacing batch. Preserve its empty-Search suggestions and all approved Emoji category, footer, host backdrop and internal live-glass layers. Do not confuse animated growth with a fix for the still unverified compact top gap.

## Exact implementation

- `EmojiPalettesView` now captures its current visible body height **before** only four deliberate events: Compact/Expanded Browse → Search, Search → originating Browse, Search → explicit Expand, and Expanded Browse → Compact via Back. It never changes the existing `SharedPanelGeometry` target-height calculations.
- On the first measure of the new mode, if both old and target heights are positive, distinct, fit the current available window and Android animations are enabled, `ValueAnimator.ofInt` interpolates height over 180ms with `DecelerateInterpolator`. Each value triggers only a view remeasure, not any dictionary/blur work or external network service.
- Animated Search's fixed keyboard children can exceed a still-small intermediate panel. Temporarily set `clipChildren=true` during the interpolation; restore the panel's prior `clipChildren=false` behavior on completion/cancel.
- A monotonically increasing generation discards old posted start callbacks and old animator completion callbacks. Rapid Back/Expand toggles start from the current in-flight height; an IME panel switch, hide/detach or fresh Emoji opening cancels motion and immediately returns to correct final layout.
- Reduced-motion setting (`Settings.Global.ANIMATOR_DURATION_SCALE == 0`) and platform animator disable skip motion; small/rotated/one-handed or insufficient-height windows do not grow beyond their available viewport. Ordinary typing, query edits, recycling Emoji rows and hardware keyboard handling do not start motion.
- Pure-JVM bounds `EmojiPanelHeightMotion`, with `EmojiPanelHeightMotionTest` covering grow/shrink/zero/same height, disabled motion, missing visible panel and window caps. This is not a physical ColorOS animation test.

## Verification and safety gates

No GPU blur shader, top/bottom panel glass renderer, smart-context suggestions, underlying host backdrop, app identity, signer, permissions, Emoji dataset or clipboard information is changed. Build and `testRunTests` in debug, full original signer and release workflow, and exact APK/package/signing verification are required before announcing any new version. The user must still test real Oppo Reno 10 5G.

Oppo checks: open normal Emoji (no startup animation), tap Search (grow), Back (shrink to same compact size), repeat rapidly; use Expand → expanded category-only layout → Back collapse; repeat with animations disabled in Android Developer Options (instant transition); rotate to landscape/small window and make sure no visual overflow, footer seams, swallowed taps, keyboard ghosts or typing lag. Source cannot guarantee whether ColorOS independently snaps the IME's outer window frame; if so the panel animation may be visible only internally and requires separate platform-specific investigation, not an artificial bitmap translation.

## Next proposed separate batch

Audit and implement the **original, cohesive brrBoard icon family in two independent stages**, beginning with emoji/category, ABC, mic, Search, backspace, shift and top-level controls, preserving custom overrides and theme contrast. The repository `docs/REMAINING_BATCHES_2026_11_17.md` labels it P0.B / Batch 6; avoid Apple proprietary assets. Clipboard recent-screenshot suggestion needs strict permission + MIME/commitContent checks and is separately queued. Device acceptance for the top gap remains highest priority if the owner reports a v1.0.116 reproduction.
