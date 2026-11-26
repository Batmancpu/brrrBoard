# QuickType strip clean-room motion — 2026-11-26

Owner explicitly authorized implementation of **only** suggestion-strip motion, from the supplied 34.73-second, 512×512, 30-fps iPad screen recording. No recording, private text, screenshots or raw typed content are committed. This supersedes the prior word-overlay approach; no dictionary/BR Brain, clipboard, voice, theme, settings, key gesture, signing or unrelated panel changes.

## Observed reference, not private Apple implementation
- 7.50–7.63 s: in-slot glyph replacements and partial-opacity glyphs; 7.87–8.17 s: longer words settle through multiple typing events. No cross-slot word flight.
- 8.23–8.37 s: unrelated labels dissolve with brief simultaneous old/new ink.
- 14.83–14.87 s: confidence highlight appears as internal dividers disappear. 15.33–16.13 s: capsule remains almost the same width across markedly different text lengths. 16.13–16.20 s: it disappears and dividers return. Do not size it to every word in the iOS profile.
- Recording is only 30 fps, so exact easing, onset latency and higher-refresh timing cannot be extracted. 167/133/67 ms and smoothstep are bounded engineering estimates, not Apple's private timing constants. Sources: https://developer.apple.com/videos/play/wwdc2018/803/ ; https://developer.apple.com/videos/play/wwdc2023/10158/ ; https://developer.android.com/reference/android/view/Choreographer ; https://developer.android.com/reference/android/graphics/Canvas .

## Implementation and verification plan
1. Remove SuggestionWordMorph and prefix-only overlay implementation. Keep native TextViews, slot assignment, selection tags, strip re-use, accessibility, confidence decision and editor/session clearing.
2. One clipped Canvas Drawable, one ValueAnimator, bounded ordered LCS character matching for simple Latin; same-slot partial opacity/position interpolation, unrelated label dissolve; sample visible ink on interruption, cap to 80 glyphs per slot. No cloned TextViews or bitmaps, no per-frame layout, no cross-slot movement. Complex shaping/emoji/chips/RTL or structural changes use native rendering, not broken glyph splitting.
3. Fixed slot-sized confidence capsule in iOS motion preset; same clock for divider and capsule opacity. Preserve previous-default content geometry and current color/contrast rules. Restore native colors and divider states on cancellation; clear on editor/session/strip changes; respect reduced motion and battery saver.
4. Unit-test matching, bounded state, unsupported shaping, timing and geometry; exact-source debug suite then original signed release workflow and actual published asset. Do not claim device parity until physical testing.
5. Device tests: slow/rapid letters, backspace, space, swipe update/final, taps during transition, capsule toggle and width, divider stability, dark/light/tint, emoji/RTL fallback, panel switches, incognito/session recall/editor boundary, reduced motion, frame pacing. Compare reference and brrBoard at 30/60/120 fps when possible.

CI and signed release status: pending at commit preparation. Physical acceptance pending. No other features authorized.
