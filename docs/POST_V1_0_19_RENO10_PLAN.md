# Post-v1.0.19 Reno10 keyboard plan

**Status (2026-11-15 UTC / Nov 16 IST):** Latest signed build is [v1.0.37](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.37), source [a655882](https://github.com/Batmancpu/brrrBoard/commit/a655882050ef39a0f7ad7471ba4f4382ed785a71), Obtainium run [43774564238](https://github.com/Batmancpu/brrrBoard/actions/runs/43774564238), APK SHA-256 `fd29dcf69ccfb5b061004f422a5e2787f0b2314497661ea25387e2d7492c4bfe` (51,024,650 bytes). Incognito microphone hiding and start guards are included in v1.0.36/1.0.37. Source commit [283b179](https://github.com/Batmancpu/brrrBoard/commit/283b17987d025fa2ca869df817755be37723eb4b) requests Android language detection and refuses confidently identified non-English or Devanagari final results; Obtainium run [43776497322](https://github.com/Batmancpu/brrrBoard/actions/runs/43776497322) and debug run [43776497364](https://github.com/Batmancpu/brrrBoard/actions/runs/43776497364) are in progress. Dictate ignores caller language extras and does not report detected language, so English-only use still requires Dictate's own Active language = English and Follow keyboard language = off. Debug run 43774564294 built and signer-verified its APK, then failed at its automatic unit-test gate; earlier baseline is 13/180 failures. No tests were manually run. Reno10 geometry/tint/icon acceptance, Emoji behavior, swipe-library review, and final full-chat audit remain open.

## Product target

Make brrBoard a reliable, light, offline-first keyboard for the owner's OPPO Reno10 5G. Its visual language and common interactions should closely follow the iPhone keyboard shown in the owner's two current reference frames and current iOS 27 documentation. “Smart” means accurate, quick, predictable, and unobtrusive. It does not mean adding an AI service or feature clutter.

Preserve package/signing/update identity, the existing Frosted Glass renderer, and the working owner-confirmed interactions. Use Android's EditorInfo and system window semantics where the host app or OS owns behavior. Do not claim hardware acceptance from source or CI results.

## Verified starting point and recovery limits

- Historical v1.0.19 is the requested starting point. Current signed release is [v1.0.37](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.37), source [a655882](https://github.com/Batmancpu/brrrBoard/commit/a655882050ef39a0f7ad7471ba4f4382ed785a71), Obtainium run [43774564238](https://github.com/Batmancpu/brrrBoard/actions/runs/43774564238), SHA-256 `fd29dcf69ccfb5b061004f422a5e2787f0b2314497661ea25387e2d7492c4bfe`. Main has shipped brrPersonal, Smart Emoji Recents, learned-history reset, and global-incognito mic guards. Current English-only result guard is commit [283b179](https://github.com/Batmancpu/brrrBoard/commit/283b17987d025fa2ca869df817755be37723eb4b); signed CI is pending.
- The remote branch list does not contain the old codex/hinglish-voice-ios-icons draft. The shared-chat record says that draft had a 252-word Hinglish seed, icon/theme edits, and dictation changes, but it was not built or Reno10-verified. The prior chat's private working tree is unavailable, so its exact diff and the exact 252 entries cannot be recovered from GitHub. Reconstruct only the useful decisions and data; do not claim those edits survived.
- The release already contains measured IME-boundary reporting, default-height calibration, footer parity for Emoji/Clipboard/toolbar panels, 300 ms mic-hold switching, 123 tap-to-Symbols/hold-to-numpad, the three-choice Emoji hold arc, and OS-owned swipe dismissal. These are source facts, not Reno10 acceptance. v1.0.31 includes brrPersonal; signed v1.0.33 integrates Smart Emoji ranking into Recents, corrects double-counting, and records accepted Emoji strip candidates.
- InputView.onMeasure() currently keeps the footer visible in typing, Emoji, Clipboard, and toolbar modes. LatinIME.onComputeInsets() subtracts main_keyboard_frame bottom padding when reporting the top of the visible keyboard surface. The owner reports a useless bottom inset/gap. First trace actual WindowInsets, frame padding, visible height, and host-content boundary together; do not simply zero an inset that ColorOS or gesture navigation still needs.
- The v1.0.19 source used SpeechRecognizer.createOnDeviceSpeechRecognizer(), bypassing the configured system recognizer. Batch 1 replaced that with SpeechRecognizer.createSpeechRecognizer() and shipped it as v1.0.20. The owner reports on Reno10 that the mic now reaches Dictate, but the transcript is Hindi. Dictate's current DictateRecognitionService.onStartListening() ignores its recognizer Intent and starts RecognitionSession with Dictate's saved activeInputLanguage; therefore brrBoard can request English but cannot force Dictate to change its private preference. brrBoard now requests the selected English region (en-US/en-GB) or en-IN for other keyboard locales. For the Dictate route to produce English, Dictate's app must have Active language set to English and must not follow a hi-Latn keyboard language. No Hindi-only output should be marked fixed until the owner verifies it. brrBoard remains without INTERNET permission.
- DictionaryFacilitator and AndroidSpellCheckerService already provide word-level spelling checks using the keyboard's dictionaries. This is not full grammar proofreading. Apple's iOS 27 Writing Tools Proofread is an Apple Intelligence writing feature; do not imply it is ordinary keyboard prediction or add a network/AI proofreading feature.
- DictionaryFacilitatorImpl already has a contextual/spatial suggestion path using ComposedData, NgramContext, locale dictionaries, and the native proximity model. The previous proposed fixed 0.35/0.30/0.20/0.15 score was not validated. Prefer improving and measuring this existing decoder over adding a parallel hand-weighted engine.
- The user-provided iOS 26/27 comparison video is not visually available in this browser session; the owner said to stop trying it. Use the two images in this chat, the visible shared-chat transcript, and current primary sources. Do not claim frame-by-frame video review.
- Do not publish the raw screenshots or any private typed text. The two current images show a Messages keyboard with a three-slot prediction row, a highlighted center candidate, a separate compact text/format control on the right, standard QWERTY rows, and Emoji/mic controls at opposite lower corners. They are visual references, not proof of every iOS 27 state.

## Reno10 sizing reference and measurement method

OPPO's India specification lists the Reno10 5G panel as 2412×1080, 6.7 inches, 394 ppi. Apple lists iPhone 16 Plus as 2796×1290, 6.7 inches, 460 ppi. Approximate panel dimensions from those published values:

| Measure | Reno10 5G | iPhone 16 Plus | Difference |
|---|---:|---:|---:|
| Physical width | 1080/394 = 2.74 in | 1290/460 = 2.80 in | Reno about 2.3% narrower |
| Physical height | 2412/394 = 6.12 in | 2796/460 = 6.08 in | Reno about 0.7% taller |
| Portrait pixel aspect | 2412/1080 = 2.233 | 2796/1290 = 2.167 | Reno about 3.0% taller per unit width |

The iPhone 16 Plus is the closest published 6.7-inch comparison found. These ratios are a starting point only: ppi and pixel counts do not reveal Reno10 runtime dp density, display-size setting, font scale, ColorOS navigation mode, keyboard height setting, or safe/mandatory gesture insets. Implement responsive dp/inset geometry and validate the actual device; do not hard-code a pixel screenshot ratio. Keep the existing 345/932 iPhone 16 Plus simulator measure labelled as an iOS 26.5 simulator result, not an iOS 27 specification.

## Exact interaction and layout target

### Board frame and footer

- Keep the visible outer board height and bottom alignment stable across alphabetic, Symbols, decimal/numeric, enlarged numpad, Emoji, Clipboard, and toolbar panels. Let each panel use its internal area instead of adding/removing an invisible navigation gap.
- Remove visual dead space reserved for an unused navigation bar while respecting Android's actual IME window, ColorOS insets, and mandatory system gestures. The board should reach the intended screen edge without making footer controls feel crushed against it.
- Put Emoji at lower left and microphone at lower right. Balance their visual centers and bottom margins against the reference; maintain practical touch targets, with 48 dp as the Android accessibility baseline when layout permits. A glyph may be visually smaller than its hit area.
- Keep the existing rounded Frosted Glass surface and economical blur. Do not add Liquid Glass or animate the board with the user's finger during dismissal. Android/ColorOS owns the hide animation.
- The keyboard must report the host-content boundary from measured window/frame/inset values so app content never remains under the keyboard.

### Prediction strip and toolbar

- Keep three predictable candidate positions: left, center, right. Keep the center geometrically centered even when a compact formatting/accessory button occupies a separate trailing slot.
- Highlight the center candidate only when the current decoder is confident; otherwise use the normal neutral candidate style. Preserve typed text until the user taps a replacement.
- A candidate can be a word or, when context supports it, an Emoji. Show at most three relevant Emoji alternatives in the strip, each requiring an explicit tap; an Emoji mapping must never turn a valid slang/cuss token into a text suggestion.
- Keep the right-side formatting/accessory action visually small and separate from the three candidates. Do not let an empty toolbar produce a tall blank band. Maintain one stable panel boundary while using available panel content efficiently.

### Keys and gestures

- Match key spacing, cap shape, labels, symbol hierarchy, and icon stroke weight to the provided references using the current responsive keyboard layouts.
- Alphabetic 123 tap remains persistent Symbols; holding 123 from alphabetic mode opens the enlarged numpad. Capture the starting keyboard mode on touch-down; sliding off cancels the hold without inserting a symbol.
- Keep tap-to-Emoji and the owner-confirmed Emoji hold/drag/release arc with Emoji, Clipboard, and Settings. There must not be a competing popup menu.
- Audit alternate-symbol long-press popups and repeat/delete behavior across letter rows and symbols. Follow iOS where documented and preserve Android editor-specific Search/Go/Send/Next/Done/URL/email/numeric behavior where the host editor specifies it.
- Preserve swipe-down dismissal, but trigger only once after an intentional gesture; a short release keeps the IME open. No board translation, blur suppression, or spring-back.
- Emoji and Clipboard use one return-to-letters control when appropriate, stable outer height, responsive category/search/recent rows, and cells that retain usable hit targets. Use dynamic columns based on available width and accessibility/display scaling, not a fixed count inferred from a photo.

### Microphone and input-method switching

- Mic tap uses SpeechRecognizer.createSpeechRecognizer() to bind Android's currently selected RecognitionService. With the owner's Dictate service selected, Dictate's configured provider/API handles audio and returns final text through the standard callback while brrBoard stays on screen. Tapping while active stops; processing/cancel states remain on the mic/accessibility controls. Allow up to 30 seconds for the configured provider to return its final result after stop/end-of-speech.
- Mic hold remains a distinct input-method switch. Keep its threshold and cancellation behavior deliberate so a short tap never accidentally switches keyboards.
- Do not use createOnDeviceSpeechRecognizer(), EXTRA_PREFER_OFFLINE, the ACTION_RECOGNIZE_SPEECH popup, or Dictate's separate voice-input IME for mic tap. The owner wants Dictate's configured provider (which may use its cloud API); brrBoard itself must retain no INTERNET permission and must not store or request the provider's API key. The voice-input IME replaces brrBoard, while the standard RecognitionService route keeps it visible. Confirm that the selected system service is Dictate and that its final transcript returns correctly on the Reno10.
- Do not invoke recognition in password, incognito, or fields carrying IME_FLAG_NO_PERSONALIZED_LEARNING. Because the selected Dictate provider may send audio over the network, keep these guards in brrBoard, disclose the handoff in the privacy policy, and do not imply the voice path is on-device.

### Appearance, tint, and icons

- Exactly two automatic base appearances: the owner's configured OG Frosted Glass in system dark and system light. Add one optional tint setting, not four separate theme presets.
- When tint is enabled, use the Android wallpaper/Material You expressive source color and map it to the nearest color in a small Apple-inspired action palette using a perceptual color-distance calculation plus contrast checks. Apply it only to action/highlight keys (for example Shift, Delete, 123, and the editor action). Keep glass, ordinary letter keycaps, and panel surfaces neutral.
- Recalculate on editor/theme/wallpaper changes and clear recycled Drawable filters so URL-field tint cannot leak into the next normal editor.
- Redraw only keyboard-owned icons that actually mismatch: Emoji, mic, Shift, Delete, Return/editor action, layout/clipboard/settings shortcuts, and the compact text/format accessory. Use original vector assets; do not copy Apple's proprietary SF Symbols files. Keep accessible labels and hit targets.
- Keep emoji characters on the system-supported rendering path. The supplied AppleColorEmoji-Linux.ttf is about 116 MB; its host repository says the MIT license covers code but excludes Apple's emoji artwork. Do not bundle it in the public APK. The keyboard cannot force other apps to render Apple glyphs; each host app uses its own emoji font.

### brrPersonal dictionary, spelling, and emoji ranking

- Use the exact display label brrPersonal (lowercase brr, uppercase P, remaining letters lowercase). Keep English US and UK resources, and do not spend this milestone polishing unrelated locales.
- The former local draft's exact 252 entries are unavailable in GitHub. Reconstruct a finite North-Indian Romanized Hindi/Hinglish lexicon rather than inventing that it survived.
- Candidate data sources for review: IIT Gandhinagar's COMI-LINGUA (CC BY 4.0; code-mixed annotation and normalization, not itself a ready-made keyboard dictionary); AI4Bharat Aksharantar mined transliteration data (CC0) and its manually curated portion (CC BY); Unicode CLDR annotations for Emoji names/keywords. Preserve required attribution and distinguish sources by license. Avoid copying social-media captions, handles, or user text. Cross-check emerging slang terms from multiple current public sources and store only the terms/needed metadata, not source posts.
- Choose the final vocabulary size from measured coverage, baseline dictionary overlap, APK budget, and held-out text results. Do not claim 4,000–8,000 is optimal before measurement. Keep a compact, sorted offline asset with a bounded index and no scan of the full lexicon in the keypress path.
- Give words distinct behavior flags: spelling-valid; gesture/tap-decoder candidate; text-prediction eligible; auto-correction eligible; preferred spelling/alias; and optional Emoji tags. Cuss/slang should count as correctly spelled and should not appear as unsolicited text suggestions or autocorrection targets. They may remain eligible for explicit gesture decoding only when spatial evidence is strong enough to preserve a natural swipe path; verify resulting candidate UI. Emoji mappings can remain eligible when that word is typed.
- Reuse the existing dictionary/proximity/NgramContext decoder and tune with a held-out English/Hinglish phrase suite. Do not add a neural model or arbitrary linear ranking weights. Measure top-1/top-3 accuracy, false autocorrections, latency, asset size, and memory before changing scoring.
- Personal learning stores bounded counts and only the minimum context needed for ranking (for example accepted word and bounded previous-word association), not messages. Provide a clear/reset path. Confirm existing field/privacy flags are honored by both typing and spell-check services.
- Smart Emoji ranks from the current token, CLDR names/keywords, and local use counts/recency, with a maximum of three candidates. It stays offline and does not change the Emoji panel's outer height.

## Batches and acceptance

| Batch | Work | Acceptance/checkpoint |
|---|---|---|
| 0 — Handoff and plan | Replace README prose; add this plan; update AGENTS.md, PROJECT_CONTEXT.md, and the conversation record with latest owner decisions, source state, and what is unavailable. | GitHub checkpoint; a new chat can identify the release, exact latest goal, blockers, and next source audit. |
| 1 — Source trace | Trace frame padding and actual WindowInsets path; editor-driven tint refresh; current dictionaries, spell-check, candidate filtering, mic/provider route, gesture conflicts, and current build/permission/signing setup. | Source map and confirmed-vs-hypothesis list recorded; no speculative fix marked complete. |
| 2 — Geometry and interactions | Correct only confirmed IME-boundary/footer/panel-height defects; tune bottom controls; maintain all panels' outer height; preserve 123, Emoji arc, Android action keys, and native hide animation. | Relevant XML/source validation and signed CI; physical Reno10 checks clearly pending. |
| 3 — Dictation and suggestions | Route mic tap through the system-default RecognitionService inline so the owner's configured Dictate provider is used; retain mic-hold input switching; refine candidate strip and compact accessory. | CI build; verify lifecycle/focus/private-field guards; on Reno10 confirm Dictate stays selected, brrBoard remains visible, and final text returns with the configured provider. |
| 4 — Themes and icons | Reduce modes to system-following OG dark/light plus optional action-only tint; fix stale tint; redraw only mismatched keyboard icons with original vector assets. | Contrast/resource/source checks and CI build; visually confirm on Reno10. |
| 5 — Dictionary and swipe | Build compact brrPersonal data from checked sources; separate spelling validity from prediction/autocorrect; add deterministic contextual Emoji candidates; validate GlideLib rights and compatibility or keep user import / choose a redistributable alternative. | Held-out phrase/gesture measurements, attribution, privacy checks, APK-size budget; no claim of proprietary swipe support without license and runtime evidence. |
| 6 — Release | Build signed APK using existing workflow; verify package/signing/update identity, no network permissions, artifact checksum, and relevant test status. | Release link plus exact CI result; owner confirms install/update, layout, swipe, dictation, gestures, and dictation privacy on Reno10. |

## Research references

- OPPO India, Reno10 specifications: https://www.oppo.com/in/smartphones/series-reno/reno10/specs/
- Apple, iPhone 16 Plus specifications: https://support.apple.com/en-in/121030
- Apple iOS 27, predictive text: https://support.apple.com/en-ca/guide/iphone/iphd4ea90231/27/ios/27
- Apple, predictive Emoji in Messages: https://support.apple.com/en-in/102507
- Apple iOS 27, Writing Tools Proofread: https://support.apple.com/en-in/guide/iphone/iph6f08da1d2/27/ios/27
- Android SpeechRecognizer APIs; createSpeechRecognizer() uses the system default RecognitionService, whereas createOnDeviceSpeechRecognizer() selects a separate on-device route: https://developer.android.com/reference/android/speech/SpeechRecognizer
- DictateKeyboard issue #359 and maintainer's explanation of the three voice-input mechanisms: https://github.com/DevEmperor/DictateKeyboard/issues/359
- DictateKeyboard RecognitionService implementation: https://github.com/DevEmperor/DictateKeyboard/blob/main/app/src/main/kotlin/dev/patrickgold/florisboard/dictate/recognition/DictateRecognitionService.kt
- Android, IME insets: https://developer.android.com/reference/android/inputmethodservice/InputMethodService.Insets
- Android, no-personalized-learning editor flag: https://developer.android.com/reference/android/view/inputmethod/EditorInfo#IME_FLAG_NO_PERSONALIZED_LEARNING
- Android, 48 dp accessibility touch targets: https://developer.android.com/guide/topics/ui/accessibility/apps
- IIT Gandhinagar, COMI-LINGUA: https://github.com/lingo-iitgn/COMI-LINGUA
- AI4Bharat, Aksharantar: https://huggingface.co/datasets/ai4bharat/Aksharantar
- Unicode CLDR, Emoji names and keywords: https://cldr.unicode.org/translation/characters/short-names-and-keywords
- Google Research, language-model personalization for touchscreen typing: https://research.google/pubs/effects-of-language-modeling-and-its-personalization-on-touchscreen-typing-performance/
- Google Research, transliterated mobile keyboard decoding: https://research.google/pubs/transliterated-mobile-keyboard-input-via-weighted-finite-state-transducers/
- Apple emoji font source/license note: https://github.com/samuelngs/apple-emoji-ttf
- Current repository code baseline: https://github.com/Batmancpu/brrrBoard/tree/main

## Proofreading and the compact accessory

- The supplied iOS 27 reference's small right-side control is the Writing Tools access point. Apple's Writing Tools Proofread is a system Apple Intelligence feature; it checks grammar and spelling on selected text. It is not ordinary predictive typing.
- brrBoard currently has Android SpellCheckerService integration for word-level spelling suggestions. Android exposes SpellCheckerService/SentenceSuggestionsInfo, but this source does not implement grammar analysis. Keep the compact accessory visually separate and useful for brrBoard's toolbar; do not claim that it performs grammar proofreading. No cloud/AI proofreader is in scope.

## Progress log

- [x] Batch 0: handoff, personalized README and agent guidance committed.
- [x] Batch 1: default RecognitionService route plus English request shipped in v1.0.21. CI signed/published the APK. Owner reports Dictate path works but output remains Hindi until Dictate's own active language is set to English.
- [x] Batch 1b: v1.0.26 corrects selected-service copy and blocks Devanagari-script final results with a Dictate setup hint. It is not full English-language detection; Romanized Hindi and Dictate's saved Active language remain unresolved.
- [ ] Batch 2: trace and correct any confirmed geometry/interactions defects, including visible bottom gap and stable panel boundaries.
- [x] Batch 3a: center autocorrection highlight from existing mWillAutoCorrect signal shipped in signed v1.0.22; Reno10 visual acceptance remains pending.
- [x] Batch 3c source fix: `SettingsValues` disables `mShowsVoiceInputKey` whenever `mIncognitoModeEnabled` is true, including global Always Incognito; `LatinIME` gates `VOICE_INPUT` dispatch on that same flag. Signed in v1.0.36; `InlineDictation.start()` adds a second guard in v1.0.37.
- [ ] Batch 3b: validate text/Emoji suggestion behavior in the three-strip slots, preserve center confidence/word autocorrect, and keep the right accessory independent. Permit multiple relevant Emoji candidates only with explicit taps and no Emoji autocorrection.
- [x] Batch 4a: system-following OG light/dark and optional Apple-palette action-key tint shipped in signed v1.0.23; Reno10 visual acceptance remains open.
- [x] Batch 4b source: original Shift outline/filled/Caps Lock glyphs plus safe saved-icon migration shipped in signed v1.0.25; Reno10 visual acceptance is pending.
- [x] Batch 5a source: brrPersonal uses the existing native decoder, pinned PHINC baseline capped at 6,000 total entries, and curated valid-only slang/profanity plus Emoji shortcuts. Normal completion/autocorrect hides those terms; deliberate swipe decoding retains them. Build-time generation adds no runtime network permission. Signed v1.0.31 is published; dictionary accuracy, latency, and Reno10 acceptance remain open.
- [x] Batch 5b-source: added a confirmation-protected clear-history action in Text Correction settings. Signed v1.0.35 clears only local UserHistoryDictionary word/word-pair learning and preserves brrPersonal, Android's dictionary and Emoji history.
- [ ] Batch 5b-validation: confirm learned-word removal and subsequent relearning on Reno10; verify sensitive-field and no-personalized-learning exclusions remain honored. Do not treat the reset button's build as device acceptance.
- [x] Batch 5c source: integrated the existing local recency/use ranker into the Recents grid, up to three responsive rows, refreshed after use, preserved incognito, fixed double-counting, and recorded accepted Emoji strip selections once. Signed v1.0.33; Reno10 acceptance and contextual text-strip behavior remain open.
- [ ] Batch 5d: evaluate the owner-verified GlideLib upload for license, provenance, ABI/API compatibility and safe packaging before considering default swipe-library integration.
- [ ] Batch 6: signed APK/source/release checks and full request-by-request retrospective. Reno10 hardware acceptance remains the owner's.


## Dictate language safety note

The selected RecognitionService may be a network-backed provider. brrBoard requests an English locale. Source commit 283b179 requests Android's API 34+ language detection and refuses confident non-English final tags; it also refuses Devanagari final candidates. This is a result-safety gate, not a way to control Dictate: the current Dictate RecognitionService ignores caller language extras and returns no language-detection callback. Dictate's saved Active language remains authoritative, and Latin-script Hindi cannot be reliably detected by brrBoard. English-only use still requires Dictate → Languages → Active language = English, Follow keyboard language = off, and Reno10 verification.

The owner's issue [#359](https://github.com/DevEmperor/DictateKeyboard/issues/359) and the maintainer's reply confirm the key route distinction: `SpeechRecognizer` calling Dictate's `RecognitionService` can keep brrBoard visible; calling Dictate's voice-input IME replaces the keyboard, and `ACTION_RECOGNIZE_SPEECH` opens an activity. This project uses the first route. The issue does not expose an API for brrBoard to control Dictate's private Active language.


### Current continuation note — 2026-11-15

PHINC is a CC-BY-4.0, 13,738-pair Romanized Hinglish/English social-media corpus from 2020; it provides a finite baseline, not 2026 trend frequencies. The generator compares source and English-translation token counts and retains at most 6,000 total entries including owner-requested manual rules. India-facing 2026 reports were used only to discover candidate terms such as “ate,” “delulu,” “bussin,” “cooked,” “rizz,” and “aura farming”; they are not corpus frequency evidence: [NDTV slang guide](https://www.ndtv.com/offbeat/confused-by-ate-bussin-and-clock-it-your-ultimate-guide-to-60-gen-z-slang-terms-11841844), [Naidunia slang guide](https://www.naidunia.com/lifestyle/news-decoding-popular-gen-z-internet-slangs-of2026-8461890). Multiword expressions are not modeled as unigram dictionary entries. The owner must decide whether the selected terms match their actual North-India usage after device acceptance.

The owner reports the installed APK routes through Dictate but returns Hindi. Source commit 283b179 adds a language-metadata guard and an English-only setup message; signed CI is pending. Do not mark English-only accepted until the owner sets Dictate's Active language to English, turns off Follow keyboard language, and verifies on Reno10. The full request-by-request retrospective remains required.


## CI and Smart Emoji source audit — 2026-11-15

The first brrPersonal signed-build attempt failed before compilation because PHINC's pinned CSV headers are `Sentence` and `English_Translation`; the generator is being corrected to map `Sentence` as Romanized source text. Obtainium's changed-path filter now needs to include `tools/**` so generator-only changes trigger release CI. No APK was published by failed run [43766256522](https://github.com/Batmancpu/brrrBoard/actions/runs/43766256522).

Source review found an existing local emoji-use recorder and recency/frequency ranker (`EmojiPalettesView.AdaptiveEmojiEngine`) with a cached top-10 result, but no call site presenting it. It does not satisfy the requested Smart Emoji behavior yet. Also, `addRecentEmoji()` records and then delegates to `addRecentKey()`, which records again; fix this before reusing the ranker. Integrate it into a measured compact row, retain typed-word emoji predictions and existing recents, and preserve incognito behavior.


## Latest source checkpoint — 2026-11-15

Signed v1.0.31 confirms the PHINC header fix and brrPersonal inclusion. Obtainium run [43766685860](https://github.com/Batmancpu/brrrBoard/actions/runs/43766685860) succeeded; asset SHA-256 is `ee8211339f62cc17e47cb1af4ff4ddbbda5cf945470d0089948a82af517c1947`. The debug APK build and signing succeeded in run [43766685891](https://github.com/Batmancpu/brrrBoard/actions/runs/43766685891), but 13 of 180 automatic unit tests failed with the known currency/parser, URL/autospace, and emoji-recognition baseline. No tests were manually run.

Smart Emoji source work reuses the Recents tab rather than adding a new category control. It ranks the first up to three rows by existing local frequency/recency metadata, invalidates the 12-hour cache on each recorded use, counts each accepted Emoji once, and uses the same usage path for Emoji prediction-strip acceptance. Signed v1.0.33 verifies this source batch; Reno10 appearance/quality checks remain open. Incognito skips adaptation and does not record use. The separate three-slot text/Emoji suggestion behavior remains to be reviewed.

The owner reiterates that Dictate must return English only. Issue #359 and current Dictate source show that the RecognitionService uses Dictate's saved Active language and ignores caller language extras. brrBoard requests Android language detection where available and rejects confidently tagged non-English or Devanagari results, but this cannot override Dictate; Latin-script Hindi remains hard to identify. Keep device acceptance open after Dictate → Languages → Active language = English and Follow keyboard language = off. The full post-v1.0.19 audit remains required.


## Smart Emoji correction and release — 2026-11-15

The initial Smart Emoji attempts failed at `EmojiCategory.java:287` due to unresolved `TextUtils`. Commit [30cd3c6](https://github.com/Batmancpu/brrrBoard/commit/30cd3c69e6bd25d95e008914db5a3bd257fc0288) replaced it with a null/empty check. Obtainium run [43769819497](https://github.com/Batmancpu/brrrBoard/actions/runs/43769819497) signed and published [v1.0.33](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.33), SHA-256 `15b65ce33695a7cbfa0f27de9202bc59776871af25cf35517a8f0c7ca4b36d49`. Debug run [43769819448](https://github.com/Batmancpu/brrrBoard/actions/runs/43769819448) built and uploaded a signed APK; its automatic unit-test gate completed and failed the automatic `:app:testRunTestsUnitTest` gate with the known baseline of 13/180 tests (5 ParserTest, 7 InputLogicTest, 1 StringUtilsTest); no tests were run manually.

The signed feature uses local frequency/recency in the existing Recents grid, ranks up to three rows for the actual dynamic grid width, refreshes the cache after use, fixes double-counting, and skips adaptive order/writes in incognito. Reno10 quality and the separate contextual text-strip behavior remain open.

## Learned typing history reset — signed release checkpoint

Source commit [2ef3e90](https://github.com/Batmancpu/brrrBoard/commit/2ef3e905b8362b3944a643e6e5d21a7277cdc652) added a confirmation-protected Text Correction action calling `PersonalizationHelper.removeAllUserHistoryDictionaries(context)`. Initial workflows [43771462834](https://github.com/Batmancpu/brrrBoard/actions/runs/43771462834) and [43771462845](https://github.com/Batmancpu/brrrBoard/actions/runs/43771462845) failed AAPT because possessive apostrophes in the confirmation string were unescaped. Commit [acacf32](https://github.com/Batmancpu/brrrBoard/commit/acacf329aaf058446d5deb4b463a11f8941cc0ce) removed them. Obtainium run [43772005035](https://github.com/Batmancpu/brrrBoard/actions/runs/43772005035) signed and published [v1.0.35](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.35), SHA-256 `dc4beadfa10a1d13476b5c978bcdebe96231fed2dba9d55da52a3d1c08a40b15`. The reset clears only local learned word/word-pair dictionaries, preserving brrPersonal, Android's system dictionary, and Emoji history. Reno10 clear/relearn behavior remains open.


## English-only final-result safety — source checkpoint

Commit [283b179](https://github.com/Batmancpu/brrrBoard/commit/283b17987d025fa2ca869df817755be37723eb4b) requests Android 14+ language detection from compatible RecognitionServices, rejects confident non-English language tags and Devanagari candidates, and shows an English-only Dictate setup message. Obtainium [43776497322](https://github.com/Batmancpu/brrrBoard/actions/runs/43776497322) and debug [43776497364](https://github.com/Batmancpu/brrrBoard/actions/runs/43776497364) were pending at this checkpoint. Current Dictate source ignores the RecognitionIntent and doesn't forward language tags; brrBoard cannot set Dictate's private language preference. Latin-script Hindi detection and owner-device acceptance remain open.