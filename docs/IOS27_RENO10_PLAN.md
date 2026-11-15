# brrBoard iOS-inspired behavior plan for Oppo Reno10 5G

Status: v1.0.16 is the last signed, verified release. The active follow-up is on `codex/ios27-behavior-cleanup`, based on source commit `4d61deed437288c9bd4d8574a1fb7647bff116ca`. Draft changes are not a signed APK or phone-verified repair yet. See [PROJECT_CONTEXT.md](../PROJECT_CONTEXT.md#resume-here) for the current checkpoint and [IOS27_BEHAVIOR_AUDIT.md](IOS27_BEHAVIOR_AUDIT.md) for source-level behavior and conflict ordering.

## Owner's latest priority

Audit how every existing control behaves before adding more. Where code paths conflict, choose an explicit priority and remove the competing implementation. Keep the app fast and offline, maintain package/signing/update compatibility, retain the owner-confirmed globe hold menu, and publish a substantive signed APK.

The owner specifically superseded the prior finger-follow gesture: the keyboard must **not** translate with the finger, suppress/restore blur, or spring back. A deliberate swipe from the suggestion strip asks Android/ColorOS to hide the IME; the operating system owns the close animation. Early release should leave the keyboard open.

## Current draft changes

- **Swipe dismissal:** removed translation, custom settle/spring, and drag-time window-blur suppression from `InputView`. Whole-strip downward capture ends with `LatinIME.requestHideSelf(0)` on threshold/flick. Child clicks are canceled only once a downward drag is recognized. The expected native animation and blur-flicker fix need Reno10 confirmation.
- **Alphabetic 123:** tap stays the persistent Symbols toggle. Hold from alphabetic 123 opens the large numpad. `PointerTracker` saves the starting layout before `SYMBOL_ALPHA` changes the live keyboard; the special hold is handled before popup metadata. Sliding away cancels the hold and cannot accidentally select a symbol. Check pointer-up, long-press timing and return layout on device.
- **Globe/Emoji:** Emoji tap opens Emoji. Hold-drag shows a single three-target arc for Emoji, Clipboard and Settings and selects on release. The competing `PopupMenu` long-press code was removed. The owner already confirmed these three release targets on-device; this interaction is retained.
- **Footer microphone:** tap requests Android's on-device recognizer within brrBoard. Tap while listening stops; tap during pending support or result processing cancels. Hold switches input methods. Listening state is shown on the microphone itself, not in the suggestion strip. A model/provider may still be absent on the phone.
- **Panels and footer:** existing changes use a 36dp suggestion strip, a 38dp footer, 28dp footer icons, and the footer inset after the key panel. Emoji scale default changes from the former 1.3046509 to 0.95; only the old shipped default migrates, not a user's chosen value. Dynamic grid columns mean the intended 7–8 columns and equal keyboard/panel bounds must be checked on-device.
- **Themes:** add OG Frost Tinted alongside OG Frosted Glass, iOS Clear and iOS Tinted. Wallpaper-derived accent maps to the nearest Apple palette color on action keys only. iOS Tinted remains a white/neutral frosted surface. Existing low-opacity lower-right brrBoard spacebar caption and package identity stay intact.

## Planned priority by phase

1. **Build and verify source state.** Confirm the branch is based on v1.0.16, inspect the final diff, compile/package the Obtainium variant, and use the signed release workflow to verify pinned signer, unchanged package, increasing version, app label, bundled dictionaries and no `INTERNET`/`ACCESS_NETWORK_STATE`. Do not run the debug test suite unless the owner asks; its prior status is 14 failures in 180 tests.
2. **Resolve input conflicts.** Confirm one path for alphabetic 123 tap/hold/slide-off, one path for the Emoji hold arc, and a clear mic tap/hold/state transition. Keep EditorInfo action keys and protected-field behavior ahead of screenshot imitation.
3. **Remove competing dismissal animation.** Keep only gesture recognition plus Android hide request. Do not animate board translation or window blur. Preserve ordinary suggestion taps, toolbar button taps and upward More Suggestions behavior.
4. **Tune geometry and colored actions.** Compare the actual key row, rounded keycaps, bottom-row ratios, footer gap, navigation inset, three keyboard panels, emoji columns, and accent contrast at default and larger display scale. Do not claim measured Apple point/radius values from concept images.
5. **Offline features and relevance.** Exercise local phrase predictions, clipboard chips by receiving MIME type, swipe-library import, and offline English/Hindi/Romanized Hinglish dictation. Track model availability separately from app code correctness. Do not add cloud recognition or a slow AI feature.
6. **Owner phone acceptance and next context.** After install, gather exact Reno10/ColorOS results for swipe, 123, panel sizes, emojis, dictation, and in-place update. Update `AGENTS.md`, this file, [PROJECT_CONTEXT.md](../PROJECT_CONTEXT.md), and [PROJECT_CONVERSATION.md](PROJECT_CONVERSATION.md) with the result; never convert source intent into a verified claim.

## Recording and reference review

The owner supplied a 116.8-second video and a 25.7-second clip plus six iOS-style reference screenshots. They remain private. The longer clip was reviewed at one-second intervals with state-change sequences inspected again: normal typing and suggestions, Symbols and the enlarged numpad, globe arc, Emoji and Clipboard panels, and the previous microphone Listen/Stop/Cancel/processing UI are visible. The shorter clip shows Android's offline-model prompt and input-method picker. Some displayed suggestions appear unrelated to the phrase. The clips do not prove offline audio recognition, text insertion, exact build/theme, all swipe origins, or actual screen dimensions. No claim is made that every 30 fps frame was individually inspected. Spoken narration is not reliably transcribed; preserve only the owner's written corrections.

The reference images repeat a compact suggestion row, rounded keycaps, 123/space/action bottom row, detached globe/emoji and mic footer, curved outer bottom corners and a small brrBoard caption. They vary by app/field and by light/dark style. Treat them as visual targets, not universal dimensions. See Apple's current [typing guide](https://support.apple.com/guide/iphone/type-with-the-onscreen-keyboard-iph3c50f96e/27/ios/27), [predictive text](https://support.apple.com/en-asia/guide/iphone/use-predictive-text-iphd4ea90231/27/ios/27), [dictation](https://support.apple.com/guide/iphone/dictate-text-iph2c0651d2/27/ios/27) and [keyboard switching guide](https://support.apple.com/guide/iphone/add-or-change-keyboards-iph73b71eb/27/ios/27). Apple documents word/emoji/information suggestions, keyboard-switch hold actions, and on-device dictation in many languages; Android app MIME support and speech models are not equivalent guarantees.

## Device acceptance checklist

- Install as an update over the existing brrBoard app and confirm settings/dictionaries and signing continuity.
- Alphabetic 123 tap, hold just below/after threshold, hold until large numpad appears, release, and slide off. Confirm no accidental character and the expected return layout.
- Swipe down from left, center and right of the strip: small pull, long pull, quick flick; also tap a prediction and toolbar control. Confirm only a single OS-owned dismissal and report whether the blur flash persists.
- Compare normal typing, Emoji, Clipboard and enlarged numpad outer top/bottom bounds in gesture-navigation and three-button-navigation modes. Check row/footer gap, emoji columns and hit target size.
- Test search, email, URL, numeric, phone and password editors for the expected key layout/action and no sensitive clipboard/personalization behavior.
- Test all four presets, dynamic action accent only, white iOS Tinted glass, low-opacity caption, dark/light, curved edges, portrait/landscape, key sound/haptic and rapid typing.
- In airplane mode test dictation with the installed provider/model for English, Hindi and Romanized Hinglish. Try start, stop, cancel, focus change and result insertion. Report the visible message and speech provider/model availability if it fails.
- Test clipboard text and, where the receiving app advertises support, image/file paste; test swipe-library import locally. Do not publish typed or clipboard content in reports.
