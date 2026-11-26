# QuickType strip clean-room motion — 2026-11-26

Owner explicitly authorized implementation of **only** suggestion-strip motion, from the supplied 34.73-second, 512×512, 30-fps iPad screen recording. No recording, private text, screenshots or raw typed content are committed. This supersedes the prior word-overlay approach; no dictionary/BR Brain, clipboard, voice, theme, settings, key gesture, signing or unrelated panel changes.

## Observed reference, not private Apple implementation
- 7.50–7.63 s: in-slot glyph replacements and partial-opacity glyphs; 7.87–8.17 s: longer words settle through multiple typing events. No cross-slot word flight.
- 8.23–8.37 s: unrelated labels dissolve with brief simultaneous old/new ink.
- 14.83–14.87 s: confidence highlight appears as internal dividers disappear. 15.33–16.13 s: capsule remains almost the same width across markedly different text lengths. 16.13–16.20 s: it disappears and dividers return. Do not size it to every word in the iOS profile.
- Recording is only 30 fps, so exact easing, onset latency and higher-refresh timing cannot be extracted. 167/133/33 ms and smoothstep are bounded engineering estimates, not Apple's private timing constants. Sources: https://developer.apple.com/videos/play/wwdc2018/803/ ; https://developer.apple.com/videos/play/wwdc2023/10158/ ; https://developer.android.com/reference/android/view/Choreographer ; https://developer.android.com/reference/android/graphics/Canvas .

## Implementation and verification plan
1. Remove SuggestionWordMorph and prefix-only overlay implementation. Keep native TextViews, slot assignment, selection tags, strip re-use, accessibility, confidence decision and editor/session clearing.
2. One clipped Canvas Drawable, one ValueAnimator, bounded ordered LCS character matching for simple Latin; same-slot partial opacity/position interpolation, unrelated label dissolve; sample visible ink on interruption, retain at most 40 sampled glyphs, with at most 80 concurrent tracks per slot. No cloned TextViews or bitmaps, no per-frame layout, no cross-slot movement. Complex shaping/emoji/chips/RTL or structural changes use native rendering, not broken glyph splitting.
3. Fixed slot-sized confidence capsule in iOS motion preset; same clock for divider and capsule opacity. Preserve previous-default content geometry and current color/contrast rules. Restore native colors and divider states on cancellation; clear on editor/session/strip changes; respect reduced motion and battery saver.
4. Unit-test matching, bounded state, unsupported shaping, timing and geometry; exact-source debug suite then original signed release workflow and actual published asset. Do not claim device parity until physical testing.
5. Device tests: slow/rapid letters, backspace, space, swipe update/final, taps during transition, capsule toggle and width, divider stability, dark/light/tint, emoji/RTL fallback, panel switches, incognito/session recall/editor boundary, reduced motion, frame pacing. Compare reference and brrBoard at 30/60/120 fps when possible.

CI and signed release status: pending at commit preparation. Physical acceptance pending. No other features authorized.

## Frame-level follow-through
Further close inspection of the same reference found full-width chrome at 29.533 s and 30.100 s before new swipe words resolved; chrome returns to dividers by 29.600 s and 30.367 s. This is a presentation-only active-swipe state, distinct from decoder confidence. Use the existing empty batch start/preview/tail/cancel UI callback to coordinate the new renderer, without changing the decoder or native selection. The paired chrome changes occur in adjacent 30-fps frames, so use a 33-ms estimated transition. At a transient next-word gap, clear the correction highlight and revoke tags immediately; no outgoing glyph overlay is retained. First editor frame is native, preventing old-editor ink reuse. When motion is disabled, skip snapshot work. Tests cover gesture chrome eligibility and keep decoder confidence unchanged.

Initial clean-room renderer checkpoint: [v1.0.360](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.360), commit 2d58b720e3b9f398597c1e020a67bc16f8c059e2; debug 45811366600 and original-signed release 45811366623 both SUCCESS. Final swipe/boundary refinement must pass fresh exact-source workflows; physical parity remains pending.
