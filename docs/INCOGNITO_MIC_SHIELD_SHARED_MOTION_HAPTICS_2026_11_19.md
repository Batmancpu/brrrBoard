# Incognito mic shield: same-frame privacy choreography (2026-11-19)

## Owner contract

- Wait until both earlier `0af253497e74795d48a3af814150c2480d8ca60b` workflows have **completed** before edits. Verified: debug `44587140096` success; original-signed Obtainium `44587140382` success.
- The protected mic grows a real **shield**, not a circular badge or Liquid Glass surface. The shield alone borrows the exact existing Emoji privacy reveal tone, **#2BCCC9**. Mic glyph colors remain independently controlled by idle, red recording and turquoise processing.
- The mic shield and Emoji icon must **finish on the same animation frame**. The Emoji drawable's original 760 ms entering / 700 ms exiting `ValueAnimator` drives both; the shield receives its actual normalized pose each update. It owns no animation object, delay, ticker, post or separate duration.
- Each genuine user-triggered entry/exit sends **three short and discreet vibration ticks**: 10 ms on, 32 ms off, 10 ms on, 32 ms off, 10 ms on. No extra ordinary emoji-key haptic, audio click, or haptic for passive secure-app changes. Respect user vibration preferences and DND. On API 26+ the three pulses use an intensity-controlled single waveform; on API 24–25 a waveform without amplitudes is the compatibility fallback.
- A mic **long hold while protected** must never record, stop an active recording, switch keyboard, show a popup, or create saved learning history. Instead a single 112 ms denial vibration, a tiny Emoji enlargement (peak +7.5%), a down-glance through the lenses and low-opacity red glint last exactly that 112 ms. The shield stays turquoise.
- Ordinary **single protected mic taps** still toggle the existing private RAM-only on-device transcription path; no external speech service, model rebundling, mic routing or sign-key changes. Protected double taps do not access the IME switcher. TalkBack long-click and explicit mic picker action are protected as well.

## Implementation map

- `FooterIncognitoMotion.kt`: one shared RGBA palette and the `shield` normalized progress channel, continuous under rapid direction reversal.
- `FooterIncognitoEmojiDrawable.kt`: one pose callback to the underlight, no second timer; brief red denial alert for Emoji only; safe animator cancellation order.
- `FooterMicSurfaceEffectsView.kt`: cached vector left/right shield silhouette segments drawn *under* the original mic; theme-independent exact turquoise; no bitmap/radial shield and no touch hitbox expansion. Existing mic-shaped underlight remains independent.
- `BoardFooterView.kt`: frame coupling, explicit-triple-haptic only on actual user toggles; protected raw-touch path ahead of the recording/processing fast-tap branch. Long holds are denied even with recording active, while a discard animation is being interrupted, or through TalkBack.
- `AudioAndHapticFeedbackManager.java`: centralized settings/DND-aware haptics, one Android waveform per interaction, no sounds.
- `FooterIncognitoMotionTest.kt`: boundedness, shared exact endpoints, interruption continuity and original timings.

## Blind spots and device QA

1. Incognito on/off rapidly (including reversing at intermediate animation frames): no shield jump, leftover hat, or out-of-step endpoint.
2. Tap protected mic to start offline dictation, hold *during* recording and processing: denial only; no accidental cancellation, second capture, keyboard picker or popup.
3. Hold within the mic area, slide out, lift, use two fingers or cancel gesture: no late tap or persisted long-press callback.
4. Hold while a prior red-mic trash morph is visible: morph interrupts, but the hold remains protected; deliberate subsequent tap can restart normally.
5. Password editor: mic and shield both hidden, existing Proton Pass shortcut appears only when eligible.
6. Automatically secured app/password field: no explicit-toggle triple haptic; private learning rules remain unchanged.
7. TalkBack: direct click still uses existing offline path; long click and mic's picker action cannot silently switch keyboards under protection.
8. Vibration disabled or DND suppression: visuals still work; no surprise forced pulses. API 24/25 pattern fallback and API 26+ waveform hardware checked.
9. Themes, portrait/landscape, one-handed and small displays: shield fits original fixed 48dp underlay; mic icon stays legible; no new row or dimensions.
10. Dismiss/relaunch IME, rapid editor switching and incognito deactivation during a hold: cancel pending callbacks and denial animator.
11. Device-specific hardware may not reproduce 10 ms pulses exactly. The app requests **precise timings**, but Android hardware and power policy control actual vibration latency and amplitude; verify by hand on the owner's Oppo device.
12. Preserve app package, release signing, all encrypted learned data, current theme and Previous Default. No model weights in the ordinary release, no Liquid Glass.

Local source/static checks are not a replacement for **same-SHA debug/unit + original-signed release CI**, and no physical Oppo visual/haptic acceptance can be claimed without an on-device test.
