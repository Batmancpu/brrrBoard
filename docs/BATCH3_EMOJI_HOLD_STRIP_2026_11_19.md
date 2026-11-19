# Batch 3A: emoji-key shortcut shelf and suggestion-strip ownership

## Owner request and live baseline

- Owner confirms original-signed v1.0.140 for Batch 2 is green after retrying a transient Robolectric HTTP-404 runtime artifact fetch. Debug full Android tests and release full Android tests both passed; the original signing key/certificate is unchanged. Final Reno10 physical acceptance is pending.
- Current direct text: long-press Emoji should offer **Clipboard, Toolbar, Settings** in that order. Emoji is already the short-tap action and must not occupy a long-hold slot. The old glass shelf is visually blocky and targets need a cleaner iOS-like presentation, legible labels, larger reliable drag-to-select regions, and reduced-motion respect.
- Owner reports that the normal suggestion strip sometimes vanishes. They will upload the reproduction video later. Source audit independently found a deterministic lifecycle hazard: `EmojiPalettesView.exitSearchMode()` posts `restoreEmojiCategoryStrip` to the next frame, but that callback unconditionally hid the **shared** `strip_container`; returning to the alphabet before the callback ran let a stale Emoji callback permanently hide live text suggestions. `EmojiPalettesView.onMeasure` had a related cross-panel visibility side effect.

## Focused implementation

- `FooterShortcutPolicy.Action`: remove redundant `EMOJI`, add `TOOLBAR`, preserve Clipboard-disabled safety and Settings. Expand three target rectangles from 80×82dp to 88×94dp; mirror RTL coordinates within a 296dp shelf and retain root-window clamping.
- `BoardFooterView`: explicitly navigate to Clipboard/Toolbar/Settings; no same-mode toolbar toggle rejection when originating in Emoji or Clipboard. Draw a 296×112dp floating rounded material island (quiet theme-aware surface, subtle outline, circular 48dp icon wells, 12sp labels, subtle selected fill and ~100ms scaling/haptic feedback). Keep the main keyboard keycap focus fade, existing single-tap Emoji/ABC behavior, mic hold/tap, and slide selection. Respect disabled system animators.
- `strings.xml`: spoken descriptions accurately list Clipboard, Toolbar, Settings.
- `SuggestionStripRestorePolicy`: add testable guard for only a visible non-Search Emoji Browse owning external strip visibility. `EmojiPalettesView.restoreEmojiCategoryStrip()` now no-ops after the Emoji panel is gone. `onMeasure()` may no longer hide the shared strip while Emoji is hidden. This fixes the observed code-level stale callback race without broad changes to text prediction, Emoji Search ranking, or glass rendering.

## Tests and remaining acceptance

- Existing and updated `FooterShortcutPolicyTest`: target dimensions/RTL symmetry, disabled clipboard explicit action, safe window clamping.
- `SuggestionStripRestorePolicyTest`: posted Emoji callback cannot hide the alphabet strip or active Emoji Search; active Emoji Browse still owns its strip.
- Local source diff and 11 Python safety harness tests pass. Full Android unit suite and original-signed release workflows are gating; this is NOT yet device-verified.
- Oppo test checklist: normal short Emoji tap; hold+slide into each of 3 new cells from typing, Emoji, Clipboard and Toolbar; first/last cells at screen edges/RTL; release without selection; reduced-motion setting; immediate search→ABC and rapid Emoji Browse→alphabet with suggestions present; compare separately with owner's pending frame-by-frame video.
- Future separate authorized contextual footer lane: transient Clipboard swipe Undo, safe keyboard-owned paste/delete Undo/Redo, Dictate failed→Retry when provider state permits. It must never expose clipboard text by default, duplicate the mic, rewrite host text beyond verified owned range, or crowd the footer. The next dedicated batch will connect actual state owners and tests before publication.
