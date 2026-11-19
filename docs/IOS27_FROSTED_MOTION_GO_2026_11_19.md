# iOS-inspired motion and behavior on existing Frosted Glass — source checkpoint

## First exact-SHA CI feedback and repair

- Published tree `47ab67c` triggered debug run `44521147323`; it **failed** Java compilation due to duplicate `MainKeyboardView.onDrawKeyTopVisuals` overrides. The first candidate is **not installable**.
- Follow-up integrates draw-only key scaling and trackpad label hiding into the existing override, retaining its spacebar/alt-code/popup-hint drawing logic; gesture-trail scheduler and fade settings now follow live profile changes without IME recreation.
- This corrective revision requires a fresh **new SHA** debug + signed Obtainium build/test pair. Do not treat the failed source as released.

Date: 2026-11-19. Owner explicitly authorized implementation after a 20-minute quiet hold.

## Hard scope boundary

- **NO Liquid Glass implementation**. Keep brrBoard's existing Frosted Glass renderer, compositor/blur helpers, theme colors, native dictionary, Glide swipe recognition, brrBrain, Emoji and Clipboard data, Big Numpad, mic visuals, brrSpeak and optional private voice model.
- Keep Android/ColorOS in charge of the actual IME window show/hide animation. Do not restore the custom finger-following slide or blur-flash behavior.
- Preserve original app package `com.mangoloads.board`, original enrolled release signer, Obtainium versionCode progression, no INTERNET permission, installed app data and `main` only. Do not touch the brrSpeak repository.
- The current old frosted appearance and behavior must remain selectable as **Previous Default**, while **iOS 27 Motion — Frosted Glass** becomes the new initial motion profile. Custom color/size/haptic preferences must survive a profile switch.

## Quiet hold and baseline

- Start: 2026-11-19 11:36:02 UTC, HEAD `6ec5918b34f9f304a28eedb82d8baa8205134c42`.
- Repeated 30-second GitHub HEAD and Actions polls through 11:56:02 UTC: no new commit and no queued/in-progress run. Both prior debug and original-signed runs were terminal green. No repository changes during the hold.
- Clone used only as a read-only working copy during hold; code editing began after it expired.

## Source implementation

1. New independent `Settings.PREF_MOTION_PROFILE` with default `ios27` and explicit `previous_default`. Appearance displays both choices, reloads the keyboard on selection; the existing color/blur theme remains unchanged. No old preferences are erased or overwritten; default key previews are enabled only for new-profile users without an explicit saved popup setting.
2. `KeyboardMotionSpec` hosts the two profiles and timing decisions. Legacy durations/alphas preserved. New search result settling has restrained opacity/translation, short profile-specific durations and ease-out. The system animation-enabled signal still prevents decorative motion when disabled.
3. `MainKeyboardView` adds a bounded draw-only per-key pressure/release scale (50/110 ms); key hit geometry, touch routing and text commitment are unaffected. Fixed key preview shape is a detached rounded rectangle with no tail, with 85 ms appearance and 90 ms dismissal. Preview recycling waits until exit animation completes and clears animation state when views detach.
4. Existing `SuggestionWordMorph` retains fixed slots, a single real glyph layer, instant current click targets and the existing 150 ms synchronized confidence-pill clock. New-profile glyphs settle by subtle width scale rather than an opaque outgoing copy or double-image fade; Previous Default still uses its shipped fade.
5. Swipe trail recognition remains unchanged. New-profile trail drawing uses display-frame callbacks rather than a 20 ms delayed drawing loop and smoothly eases the tail opacity. Original colors are still drawn from current theme roles. Previous Default keeps the existing fixed update interval and linear fade. Pending drawing callbacks are cleared on memory teardown.
6. Emoji Search result replacement, eligible Clipboard panel-height motion, internal panel transitions, footer action and Klipy result entrance draw from centralized profile-specific timing. Existing cancellation and no-bright-host-flash guards are kept.
7. Space trackpad mode in the new profile hides key labels while finger-driven cursor selection is active, then restores the original labels. Previous Default retains its prior half-alpha trackpad state.
8. Haptic manager and haptic preferences remain exactly the existing semantic key/long-press/move feedback path; do not generate an extra vibration on each animation frame or overlay unrequested haptic effects. Voice/mic pulse remains unmodified.

## Values (not asserted to be Apple's internal private millisecond timings)

| Motion | New profile | Previous Default |
| --- | ---: | ---: |
| Key down / release scale | 50 / 110 ms | no added key scale |
| Floating preview enter / exit | 85 / 90 ms | instant |
| Suggestion single-layer glyph + pill | 150 ms, width settling | 150 ms, existing alpha settling |
| Internal panel switch | 180 ms | 160 ms |
| Emoji search viewport | 190 ms | 210 ms |
| Eligible Clipboard height | 225 ms | 260 ms |
| General search reveal | 185 ms | 170 ms |
| Search pending | 95 ms | 90 ms |
| Footer action | 175 ms | 150 ms |
| Klipy search entry | 210 ms | 350 ms |
| Swipe tail fade | smooth-step, same 100 + 240 ms | original linear 100 + 240 ms |
| Swipe redraw | per-display-frame | 20 ms timer |

## Automated and physical acceptance gates

- `git diff --check`, XML parse, source review; pure `KeyboardMotionSpecTest` verifies stable profile IDs and previous-default choice.
- Full GitHub Actions **exact SHA** debug compile/unit/generator/safety and original-cert Obtainium workflow must BOTH finish terminal success before claiming an APK. If compilation/tests fail, fix the actual new-SHA fault and rerun; do not label failed releases as installable.
- On Oppo Reno10 Android 15, manually compare every rapid typing, `sugg`→`suggested` candidate, swipe word/path, first-row preview clipping, 123/ABC, Emoji query, Clipboard Search, globe hold, trackpad mode, dark/light, selected/corrected candidates, haptic system settings, editor focus, passive/system keyboard hide, keyboard reopen, reduced-motion and 60/120 Hz.
- Legacy/Previous Default must match the preserved source baseline. A shared Frosted Glass renderer ensures no separate fake material or Liquid Glass code path.
- This CI/source checkpoint **cannot certify actual Apple timing parity or physical ColorOS appearance**. Those remain owner-video calibration and real-device acceptance.
