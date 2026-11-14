# BRR Board development plan

## Product target

A fast, fully offline Android keyboard, with iOS 27-style key placement and behavior, the existing Frosted Glass renderer with rounded edges and calibrated defaults, a bundled North Indian Hinglish vocabulary, and inline offline dictation. The app label is BRR Board. Preserve the existing Obtainium package ID, signer, and increasing version code. Work on main. Publish a signed APK at each completed phase, with the phase and remaining gaps recorded.

## Phase 1 — offline baseline and branding

- Remove INTERNET and ACCESS_NETWORK_STATE from the merged manifest using removal markers. Inspect the actual signed APK in CI before publishing.
- Rename launcher, IME, spellchecker, settings, and spacebar defaults. Review localized strings, launcher art, settings promotions, notifications, backup filenames and README separately. Preserve upstream attribution and backup compatibility.
- Remove the cloud settings route and onboarding. In the cleanup follow-through, remove Gemini, Klipy, remote stickers, GIFs and online attachments from toolbar, panels, provider, databases and dependencies. Filter old toolbar preferences and drop stale API credentials. Check data migration for users who previously enabled these options. Keep local Unicode emoji. Review local clipboard and local file import separately from online attachments.
- Verify package and signer, no network permissions, startup, update over 1.0.4, and settings. The first checkpoint covers permissions, primary labels and cloud settings. Removing runtime entry points and dead code remains part of this phase.

## Phase 2 — layout, suggestions and frosted defaults

- Compare PRs [#1](https://github.com/Batmancpu/brrrBoard/pull/1), [#2](https://github.com/Batmancpu/brrrBoard/pull/2) and [#6](https://github.com/Batmancpu/brrrBoard/pull/6). Extract relevant changes against current main. The Jules branch diverged and used a different package.
- Build a key-position matrix from [Apple's iOS 27 guide](https://support.apple.com/guide/iphone/iph3c50f96e/27/ios/27) and iOS 27 device references. Cover letters, shifts, delete repeat, 123, secondary symbols, space, return/search action, Emoji and globe, dictation, long presses, landscape, one-handed layout and field-specific keyboards. Record any extra keys recovered from project history and label positions that still need owner input.
- Audit suggestion strip ordering, candidate width, typed-text preservation, punctuation, autocorrection undo, and inline completion where supported by the Android editor. Test password and incognito fields, accessibility, 3-button and gesture navigation, large font, small screens, and English/Hinglish switching.
- Tune tint, blur radius, opacity, key corner radius, bottom inset and light/dark contrast using the current Frosted Glass code. Preserve user-set overrides and provide a readable fallback when OEM blur is unavailable, disabled or too slow. Liquid Glass rendering is deferred. Compare captures with stable device, orientation, wallpaper and theme settings. Profile IME open, typing and layout transitions.
- Publish a signed layout checkpoint with reference screenshots, parser test results and visual/device gaps.

### Recovered layout experiments

| Source | Recovered choices | What to check on current main |
| --- | --- | --- |
| PR #1 and #2 | 23% 123 and Return keys, flexible Space, separate footer with Emoji and input-method controls. PR #2 tests a press, slide and release chooser, 32dp shell radius and 12dp key radius. | Preserve email and URL shortcuts, accessibility and working emoji and IME switching. |
| PR #6 | 15% Shift and Delete, 23% 123 and Return, flexible Space, Emoji lower left with Clipboard on long press, and keyboard switching lower right. Experimental phone height 260dp and side padding 2%. | Check parser row merging, hit targets and height. Compare each subtype to Apple references and decide where the dictation button belongs relative to the old right-corner switch. Preserve tablet layout until reviewed. |

These values are prior experiments. Validate the current touch targets and Apple reference before adopting them as defaults.

### Owner backup defaults

The backup provides Material keys, Holo icons with individual overrides, 32dp board corners, day and night Frosted Glass, touchpad on vertical space swipe, toolbar choices, a Romanized Hindi subtype and three imported dictionaries. These are the baseline for later iOS layout work. The backup contains no custom layout file. Its two edge-contrast keys have no matching renderer setting on the current branch, so assess them when tuning the shader and contrast. The language-switch microphone icon override is omitted so the keyboard switch can be distinguished from dictation. Existing explicit settings and imported dictionaries keep priority. Learned history and private device state stay outside the repository.

## Phase 3 — Hinglish

- Identify locale and dictionary loading in DictionaryFacilitator, SubtypeLocaleUtils and existing asset tooling. Bundle a local Romanized Hinglish dictionary with an English fallback where the engine supports it. Preserve Hindi-script and English subtypes.
- Curate common conversational spellings and regional variants, then validate newer slang using public usage from Indian sources. Use public Instagram examples only when accessible, cross-check the date and reuse terms without copying captions or handles. The [SCREEN survey](https://www.britishasiantrust.org/media/lgvn0rhx/screen-survey-2026.pdf) is a starting source for contemporary Indian usage.
- Track normalized spelling, aliases, region, meaning, frequency/rank, date and source. Rank common phrases above transient memes. Keep offensive terms out of proactive suggestions and avoid aggressively autocorrecting slang or names.
- Test mixed sentences, variants, prefix ranking, offline behavior, field exclusions, APK size and typo regression. Publish a signed dictionary checkpoint.

## Phase 4 — inline offline voice

- Reproduce [DictateKeyboard #359](https://github.com/DevEmperor/DictateKeyboard/issues/359). The maintainer identifies the shortcut IME and popup paths as keyboard-replacing, and Android RecognitionService as the path that keeps the keyboard visible.
- Inspect the mic callback and editor lifecycle in LatinIME and the keyboard action handler. Add an in-keyboard listening panel with start, stop, cancel and errors. Scope callbacks to the active InputConnection and destroy or cancel on editor change, keyboard dismissal, password fields and interruption.
- Prefer the Android on-device recognizer when available. For Dictate, verify the actual provider and offline model configuration before treating it as a supported offline route. A separate provider can have its own networking permissions. Avoid silent fallback to network recognition and offer a clear unavailable state.
- Verify microphone permission, model missing, revoked permission, rotation, focus switch, cancellation, and English/Hindi/Hinglish on an actual device in airplane mode. Publish a signed voice checkpoint, labeling unsupported devices and configurations.

## Phase 5 — integrated release

Test fresh install and update from 1.0.4 with saved settings. Check offline launch and typing, no online UI, blur on supported and unsupported devices, suggestion ranking, inline voice, performance and Obtainium Refresh → Update. Compare startup, typing frame time, memory and APK size against baseline. Run unit tests and track the existing emoji failure separately until fixed. Publish the final signed APK with a short test matrix and any remaining device-specific gaps.

## Delivery checkpoints

Record each branch baseline, touched files, decisions, evidence, CI run, APK name/version, on-device verification status, and next action in PROJECT_CONTEXT.md. Use the owner’s four-to-five-hour target to prioritize and report progress. Full visual and voice parity requires device evidence. Link the most recent verified APK even when a later phase is still in progress.
