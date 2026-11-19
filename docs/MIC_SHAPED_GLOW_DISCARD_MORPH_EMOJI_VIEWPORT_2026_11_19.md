# brrBoard underlit mic, descending-bin cancellation, compact Emoji viewport

## Owner's visual amendment (2026-11-19)

The microphone's recording light must trace its **tall microphone silhouette**,
not a circle, disk, white rim, or separate capsule. When the mic scales up,
the blurred light beneath it must scale by the same amount. Red on recording,
turquoise on private decoding. The existing mic/Enter keys, footer height,
Frosted Glass material and gesture hitboxes must not change.

Only on confirmed active user cancellation, a white bin should emerge downward
from the Enter-key region above the mic, open its lid, swallow the red recording
mic, shut, travel to the idle-mic position and seamlessly become the idle mic.
If cancellation happens during private decoding, the swallowed mic starts
turquoise, preserving the actual phase color. Success, error, idle stale cancel
and IME/editor exit must never play this animation. No status text while decoding.
The owner also reports a blank band at the TOP of compact Emoji Browse and
requests a source-level gap fix before discussing the shared smart engine.

## Source changes

1. `FooterMicSurfaceEffectsView.kt`: original microphone silhouette path is
   shared by a 48dp surface underlight and a 96dp non-interactive cancel canvas.
   The underlight makes one cached 48dp blurred bitmap per active color/size;
   the 1,600ms existing `FooterMicPulseDrawable` and glow use the SAME
   `FooterMicPulsePolicy.scale`. Only bitmap alpha/scale redraw each frame;
   no blur shader regeneration or new animation dependency.
2. `BoardFooterView`: recording glyph is iOS-like red with no ring; the light
   is on the keyboard under the glyph, not a border. Private decoding remains
   turquoise with a smaller mic-only pulse and no message. `showDictationDiscarded`
   is called from an explicit `USER_CANCEL` exit of an active session only.
3. `FooterMicMorphPolicy`: one normalized, 520ms clock controls drop, lid,
   swallow, bin slide, bin fade, and returning white mic. The final 8% of time
   blends the white mic toward the real theme-compatible idle icon. Trash
   canvas extends into the Enter-key region without stealing touches.
4. Reduced-motion: show a still bin briefly (220ms) and return to idle; never
   force animation when Android has animations disabled.
5. `EmojiPalettesView` / `SharedPanelGeometry`: the original first-open
   same-width typing snapshot may be 0 on OEM initial measure. A measured
   same-width `KeyboardWrapperView` becomes the *fallback* viewport source;
   existing typing snapshot takes precedence. No arbitrary top-padding offset,
   no cross-width portrait/landscape reuse, no Search/Expanded resize.
6. Contract/Kotlin tests cover explicit-event gating, bounded cancel keyframes,
   color states and measured viewport fallback. Handoff files updated.

## Motion timeline (design targets, not proprietary WhatsApp measurements)

| Time | Visual phase |
| --- | --- |
| 0–150ms | White bin drops roughly 45dp from just above mic, while the existing mic stays visible |
| 110–205ms | Bin lid opens 63 degrees |
| 185–355ms | Red/turquoise mic travels 23dp toward bin, 8dp down, shrinks to 23% |
| 340–410ms | Lid closes while bin travels toward the mic slot |
| 400–520ms | White mic emerges as bin dissolves; last ~42ms blend back to the actual idle theme color |

The design is **WhatsApp-inspired, original artwork**; no copied app assets,
outside API, hidden UI reflection, per-frame bitmap allocations or Liquid Glass.
The same session-generation guards still cancel native decode and wipe PCM.

## Emoji top-band evidence and limitations

Earlier owner video showed a stationary region above the first complete Emoji
row. Prior fixes already moved the category header into `emoji_grid_shell`,
removed the duplicated strip, and aligned the first row's RecyclerView padding.
The remaining possible mismatch is initial timing: before a measured alphabet
snapshot is cached, a shorter bottom-gravity Emoji child can be aligned to a
larger already measured wrapper, leaving a blank strip ABOVE the categories.
The new fallback uses exactly that wrapper **only when widths match**. This is
a bounded source fix, **not yet proof** of the exact Oppo visual result; a fresh
screen capture and view-bounds device test are still required. Never crop an
Emoji row to cosmetically erase the band.

## Validation and release gates

- `python3 -m unittest discover -s tools -p 'test_*.py' -q`, `git diff --check`,
  and XML parsing pass locally. Kotlin/Android/renderer code still needs CI.
- Android debug/test and original-signed Obtainium on the **new exact SHA** must
  pass before an APK can be called ready. No independent unsigned APK, changed
  package/signature, or new Android permission.
- Physical Oppo Reno10 5G: red recording on both themes, exact silhouette
  glow synchronized with mic scale, no disk or clipping, turquoise decoding,
  cancel via center and mic during capture/processing, no cancel morph on
  normal transcript insertion/error/keyboard hide, rapid restart and TalkBack.
- Emoji: first opening in normal compact Smileys, Recents, and Flags; categories
  visible and first row complete, no dead band; scrolling, switching category,
  expanded Search/Back, landscape and one-handed cases all intact.

## Deferred smart engine (explicit owner sequencing)

Do **not** combine voice dictionary biasing, swipe-context learning or emoji
typing-suggestion ranking with this motion/layout batch. The next discussion
will design one efficient privacy-gated local intelligence service shared by
the dictionary, gesture disambiguation, emoji suggestions and voice hotwords.
It must preserve stale-context expiry after pauses, competing swipe hypotheses,
app/editor context only when appropriate, zero transcript capture by default,
and current offline Parakeet. No new LLM.

## Publication status

This source change is local-only until main publication is explicitly authorized.
Prior local base `d19760d` was never pushed to main. No APK/device acceptance
should be claimed from tests or screenshots not actually observed.
