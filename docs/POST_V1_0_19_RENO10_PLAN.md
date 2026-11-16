# Post-v1.0.19 Reno10 keyboard plan

## 2026-11-16 — autonomous implementation B00 checkpoint

The owner now authorizes implementation across the published dependency-ordered plan and requests continuous, recoverable progress while unavailable for approximately 4–5 hours. This latest direction supersedes the older planning-only instruction preserved below. **B00 is partial:** latest main is `cb1a35d6b506b2214bb19e56d62e50d8b80e48b6`; its diff from source baseline `3ee9da9e9b2a0b4a8c42b1e27d93f4686ff65b21` is documentation-only. Release v1.0.57 remains latest (37,949,189 bytes; SHA-256 `a57da3ad314f4f6e21dd64cf18996c5e215134d631d93f4a3a396e75b32bdbfd`). Debug job 133178974688 built and signer-verified an APK, then its test gate failed 13 of 180 tests: 5 currency parser, 7 URL/autospace, 1 Emoji classification. Local Gradle execution is blocked before configuration by a network failure downloading Gradle 8.14.4. This workspace has no `adb`; Reno10 baseline remains unverified.

**Next exact action — B01:** make currency parser fixture state deterministic (en-US, explicit blank custom currency) without changing the app’s `₹` default; preserve coverage for custom rupee and currency popup labels. Run focused and full tests in CI because local Gradle is unavailable. Do not proceed to B02 until the B01 evidence is recorded. All four continuity records must be committed with each batch. See [current engineering handoff](ENGINEERING_HANDOFF.md#b00--implementation-baseline-2026-11-16).

## B01 — currency parser fixture isolation (in progress)

Starting commit: `7ab4bc570b18ad79bd5e4d8761ae910f10de2174` (B00 documentation checkpoint). Changed only `app/src/test/java/helium314/keyboard/KeyboardParserTest.kt`: fake parser IDs now use an explicit US English subtype; the test fixture sets `custom_currency_key` to blank so legacy USD expectations exercise locale fallback; setup saves previous preference state, teardown restores/removes it and clears the locale-info cache; new `currencyKeyUsesConfiguredRupee` checks the existing custom-₹ path. No product source or `Defaults.PREF_CUSTOM_CURRENCY_KEY` changed. `git diff --check` is the only local validation so far; local Gradle cannot download 8.14.4, so CI is required. Exact B01 ending commit and focused/full test outcomes will be appended after the `brrrboard-audit-*` workflow result. Next: run/inspect focused parser tests and the full `testRunTests` suite; fix any failing fixture or test-order behavior before B02.

**R6 — handoff verified.** The documentation-only architecture phase is complete. A later authorized implementation should begin with B00 and the current CI triage before changing product features. See the [engineering handoff](ENGINEERING_HANDOFF.md#start-here).

**R5 — dependency-ordered implementation master plan.** The plan is ready for a later implementation session. R6 will verify links, source immutability, recovered history and the remote checkpoint chain. See the [engineering handoff](ENGINEERING_HANDOFF.md#start-here).

**R4 — Clipboard, inline autofill and Dictate integration audit.** The mic-to-settings branch and Clipboard safety seams are mapped. R5 will order test repair, editor safety, interaction and late release gates. See the [engineering handoff](ENGINEERING_HANDOFF.md#start-here).

**R3 — local language engine, Smart Emoji and glide research.** Current Smart is usage-ranked Recents and the default build lacks a working gesture factory. R4 will trace provider integrations and privacy boundaries. See the [engineering handoff](ENGINEERING_HANDOFF.md#start-here).

**R2 — Reno10 and iOS visual reference audit.** Original media was sampled where available. R3 will trace the local ranking and swipe engine before choosing evaluation gates. See the [engineering handoff](ENGINEERING_HANDOFF.md#start-here).

**R1 — source architecture and test triage.** R2 will measure the visual reference and specify shared geometry. Future implementation begins with the test baseline. See the [engineering handoff](ENGINEERING_HANDOFF.md#start-here).

**Current research checkpoint.** See [engineering handoff R0](ENGINEERING_HANDOFF.md#r0--repository-baseline-on-16-november-2026) for the current planning-only scope, newer v1.0.57 baseline and status of the new implementation plan.

**Status (2026-11-16 UTC):** Latest signed release is [v1.0.56](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.56), source [8463fc8](https://github.com/Batmancpu/brrrBoard/commit/8463fc80c1f8695c0f0b11075db25d5e0d3434db), Obtainium run [43829494372](https://github.com/Batmancpu/brrrBoard/actions/runs/43829494372), 37,949,189 bytes, SHA-256 `1340be9ef8ba3b85cc080900d74d743b072c8ccd8b99d10d846de227ef276f25`. The 123 key now taps to persistent Symbols and slides to one momentary symbol before returning to letters. Debug run [43829494384](https://github.com/Batmancpu/brrrBoard/actions/runs/43829494384) built, signer-verified and uploaded its APK, but the 180-test gate found the same 13 failures: 5 parser/currency, 7 URL/autospace, 1 Emoji recognition. No new failure was introduced; no Reno10 test was run.

## Current implementation checkpoint — 123 momentary-symbol selection

Source commit [8463fc8](https://github.com/Batmancpu/brrrBoard/commit/8463fc80c1f8695c0f0b11075db25d5e0d3434db) removes the owner-specific 123 long-press branch that opened an enlarged numpad and suppressed dragging. A normal tap still switches to persistent Symbols. A slide from 123 can choose one symbol; the existing KeyboardState momentary alpha/symbol state returns to letters after sliding input finishes. Ordinary alternate-key popup handling remains unchanged. Obtainium run [43829494372](https://github.com/Batmancpu/brrrBoard/actions/runs/43829494372) built, signed and published [v1.0.56](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.56), 37,949,189 bytes; SHA-256 `1340be9ef8ba3b85cc080900d74d743b072c8ccd8b99d10d846de227ef276f25`. Debug run [43829494384](https://github.com/Batmancpu/brrrBoard/actions/runs/43829494384) built and signer-verified the APK and uploaded artifact [14208306869](https://github.com/Batmancpu/brrrBoard/actions/runs/43829494384/artifacts/14208306869); its 180-test gate retained the known 13 failures (5 parser/currency, 7 URL/autospace, 1 Emoji recognition). No manual tests or Reno10 checks were run.

## Current implementation checkpoint — first-use Smart Emoji

The sparkle Smart Emoji tab and local frequency/recency ranker were already present in source; the owner's “I don't see Smart Emoji” report exposed an empty first-use Recents grid, which caused the category to fall back to Smileys. Source commit [8caeb5d](https://github.com/Batmancpu/brrrBoard/commit/8caeb5d963b76a606f636ad9c0e08236bacf9648) now seeds ranked Emoji keys into free Recents capacity on opening, migrates the old Smileys fallback to Smart once, and preserves actual recent Emoji history by keeping starter keys transient and excluding them from saved recents. Ranking is capped at the lesser of grid capacity and three rows, and the ranker is not read in incognito mode. Obtainium run [43825284915](https://github.com/Batmancpu/brrrBoard/actions/runs/43825284915) published v1.0.54. Debug run [43825284952](https://github.com/Batmancpu/brrrBoard/actions/runs/43825284952) compiled, signed, and uploaded the APK; its automatic test gate reports the known 13/180 failures. No owner-device test has been performed.

## Current implementation checkpoint — contextual Emoji side suggestions

Source commit [32d580d](https://github.com/Batmancpu/brrrBoard/commit/32d580dc276cd87e2aef42d707bdcf556b6d530a) uses the existing local usage/recency ordering only when the strip has multiple decoder-provided Emoji candidates and the keyboard is not incognito. SuggestionStripView passes a display copy to SuggestionStripLayoutHelper; the helper moves the best ranked available Emoji into the two visible side slots. It preserves the middle suggestion, the decoder-owned mWillAutoCorrect confidence state, candidate scores, and the original InputLogic suggestion model. It does not invent Emoji or perform semantic mapping beyond Emoji already returned by the decoder/dictionary. Obtainium run [43827417372](https://github.com/Batmancpu/brrrBoard/actions/runs/43827417372) published v1.0.55. Debug run [43827417364](https://github.com/Batmancpu/brrrBoard/actions/runs/43827417364) built, signed, and uploaded; its 180-test gate again found the known 13 failures. Reno10 acceptance remains open.

**Apple reference for the next behavior audit:** The iOS 27 onscreen keyboard guide documents touch-and-hold on a related letter/number/symbol to choose its alternate character, and holding the Emoji or Switch Keyboard key to open Keyboard Settings: [Apple iPhone Guide](https://support.apple.com/guide/iphone/type-with-the-onscreen-keyboard-iph3c50f96e/27/ios/27). Apple also describes tapping an Emoji suggestion to replace the typed term: [Add Emoji, Memoji, and stickers](https://support.apple.com/guide/iphone/add-emoji-memoji-and-stickers-iph69df21ec5/27/ios/27). The screenshots establish visual proportions only; exact Android/ColorOS geometry and gestures still require the Reno10 acceptance pass.


## Ordered implementation sequence for the current owner request

| Batch | Scope and source areas | Acceptance criteria / release gate |
|---|---|---|
| 1. Restore usable Smart Emoji | `EmojiCategory.java`, `EmojiPalettesView.java`, adaptive Emoji ranking | First normal open shows ranked default Emoji without overwriting saved recents; the Smart tab is visible; incognito does not read personalized ranking. **Shipped in v1.0.54.** |
| 2. Contextual Emoji suggestions | `SuggestionStripView.kt`, `SuggestionStripLayoutHelper.java`, existing adaptive Emoji ranker | Keep the center text candidate and its decoder confidence state unchanged; only rearrange Emoji already supplied by the decoder into the two side slots; tapped Emoji inserts/replaces according to current input logic. **Shipped in v1.0.55.** |
| 3. Key interaction and glyph audit | `PointerTracker.java`, `KeyboardState.kt`, `PopupKeysUtils.kt`, keyboard layouts, icon/vector assets | Tap 123 remains a persistent symbols toggle; sliding from 123 selects a symbol and returns to letters; regular key long-press selects its configured alternate; mic/Emoji long-press and popup actions remain intentional. Map every visible icon to its action and replace only mismatched glyphs. Source batch is committed; CI and Reno10 acceptance are pending. |
| 4. Reno10 proportions and visual finish | `InputView.java`, `RoundedKeyboardFrameView.kt`, `KeyboardLayoutSet.java`, keyboard XML/layout resources, `BoardFooterView.kt`, key drawables | Match the supplied screenshot’s three prediction slots, small separate accessory, row spacing, key/footer balance and rounded glass edge. Hold selected height across letters, symbols, Emoji, Clipboard and toolbar; measure ColorOS insets and visible bottom space on the actual Reno10 before changing dp values. Preserve auto light/dark, neutral OG glass, optional action-key-only palette tint and the white top-edge highlight. No screenshot-only claim counts as device acceptance. |
| 5. brrPersonal and swipe typing | `app/src/main/assets/brrPersonal*`, generator/NOTICE, gesture-decoder integration and setup screens | Keep the finite PHINC-derived and curated Hinglish corpus, valid-only slang/profanity, Emoji mappings and local-only learning; evaluate decoder ranking on representative English/Hinglish words, names and slang. Check license, binary provenance, ABI coverage, latency and APK growth. Bundle a redistributable engine by default, then remove the outside-library installer in the same reviewed batch. Do not ship the unlicensed GlideLib binary or claim Reno10 accuracy without owner testing. |
| 6. Dictate, cleanup and acceptance | `InlineDictation.kt`, settings/help copy, build payload and final request checklist | Keep brrBoard open and bind the selected Dictate RecognitionService; validate English configuration, sensitive-field/incognito guards and mic-hold IME switching on Reno10. Confirm three visible language choices, no global key/icon style pickers, no Apple font, no stale tint, and no redundant controls. Build/sign/size-check the installable APK, record the known 13/180 CI baseline separately, and obtain owner acceptance across the device matrix before calling the work complete. |

Do not trim the internal subtype/parser catalog to hide languages from Settings; the full catalog is required by compatibility tests. Do not remove unrelated preferences. Apple’s guide documents alternate-key popups but does not specify ColorOS geometry; use the owner screenshots for visual comparison and Reno10 measurements for acceptance. Each implementation or research batch must update the four handoff files in the same commit and must finish its CI checkpoint before the next source batch starts.

## Product target

Make brrBoard a reliable, light, offline-first keyboard for the owner's OPPO Reno10 5G. Its visual language and common interactions should closely follow the iPhone keyboard shown in the owner's two current reference frames and current iOS 27 documentation. “Smart” means accurate, quick, predictable, and unobtrusive. It does not mean adding an AI service or feature clutter.

## Owner continuation and execution plan — 2026-11-16

The owner supplied a previous-chat transcript and two keyboard reference screenshots, then authorized implementation. This current request supersedes older plan-only instructions and the older preference to keep manual gesture-library import. The target remains the owner's Android phone and ColorOS; screenshots are visual evidence only and do not prove interactions. Do not publish raw screenshots, message text, clipboard content, or learned typing history.

### Batch 0 — Carry the current intent forward

- Keep this plan, PROJECT_CONTEXT.md, docs/PROJECT_CONVERSATION.md, and AGENTS.md synchronized at each small batch. Record exact files, reasons, outcomes, build/CI results, and the next action before starting another batch.
- Correct the earlier “252-word draft unrecoverable” summary: a saved backup contains compiled static dictionaries, even though the editable original list is not present.

### Batch 1 — Verify behavior and remove product clutter

**Implementation checkpoint — correction:** `LanguageScreen.kt` shows only English (US), English (UK), and Romanized Hinglish, and `SubtypeSettings.kt` migrates old extra enabled choices. The internal `method.xml` catalog must stay complete (128 entries) for locale/parser compatibility; the first attempt to trim it caused eight additional CI failures and was reverted. Appearance no longer exposes global key/icon styles, per-icon customization, or theme preset pickers. v1.0.41 carries the correction. The latest run still reports the 13 known baseline tests; do not call the gate green.

- Keep `app/src/main/res/xml/method.xml`'s complete internal subtype catalog for parser/locale compatibility. Restrict only the keyboard choices shown/enabled through `LanguageScreen.kt` and `SubtypeSettings.kt` to English US, English UK, and Romanized Hinglish. Keep app UI translations unless they are specifically keyboard choices.
- In AppearanceScreen.kt, remove the global Key Style and Icon Style selectors and pin one iOS-inspired default. Preserve unrelated settings. Retain only automatic OG Frosted Glass light/dark plus the optional action-key tint; do not add separate theme presets.
- Triage the existing 13 failing automatic tests (parser/currency, URL/autospace, Emoji recognition) as a separate known issue before release; do not report the current red debug gate as green.

### Batch 2 — Keyboard layout, insets, and liquid-glass treatment

- Trace main_keyboard_frame.xml, InputView, LatinIME.onComputeInsets(), BoardFooterView, KeyboardWrapperView, and RoundedKeyboardFrameView.kt. Keep the host-content boundary correct while removing the unwanted bottom gap.
- Use responsive Reno10 dp/inset measurements and the supplied screenshots to tune row/key proportions, footer spacing, mic and Emoji balance, toolbar height, and globe/menu behavior. Compare relative proportions rather than copying iPhone pixel sizes. Keep useful hit targets and editor-specific action keys.
- Trace field-type/theme transitions, frame background ownership, `fitsSystemWindows`, visible nav-bar insets, mandatory gesture insets, and `InputMethodService.Insets`; retain only system-required safe space, then distribute the remaining interior breathing room evenly around the footer.
- Hold the same outer keyboard height and bottom edge across letters, Symbols, numpad, Emoji, Clipboard, and toolbar states.
- Balance footer visuals against the last key row: use 48dp minimum Emoji/mic touch targets, place the glyphs with a 10dp optical bottom inset, and reduce key-body height by the same 10dp so increasing the footer does not increase the keyboard's total height. Keep OS-mandated navigation/gesture insets; never reserve a hidden navigation bar as blank keyboard space.
- Add a subtle, thin white top-edge highlight to the rounded Frosted Glass frame. Keep the glass and letter keys neutral; repair stale action-key tint and map the wallpaper accent only to selected action keys.

**Source checkpoint:** `RoundedKeyboardFrameView.kt` uses the visible navigation-bar inset, clears an inherited child bottom inset, and draws a 1dp neutral white line along the rounded top edge. `LatinIME.onStartInputViewInternal()` reapplies the new editor's frame background after theme loading through `InputView.refreshBoardFrameBackground()`. Footer geometry shipped in v1.0.48: `InputView` retains the lower controls in number layouts, `BoardFooterView` is 48dp, controls retain 48dp hit regions with a 10dp optical inset, and `ResourceUtils.getKeyboardHeight()` subtracts the extra 10dp from the key body. This is calculated from existing source, not Reno10 measurement; phone acceptance is pending.

### Batch 3 — Emoji and suggestions

- **Source implemented in v1.0.41:** `EmojiPalettesAdapter.java` flattens Emoji pages into horizontal page items and `EmojiPalettesView.java` uses page snapping/category state. Reno10 scrolling and page boundary behavior remain unverified.
- **Source implemented in v1.0.41:** Smart Emoji is visible as a sparkle icon with “Smart Emoji” accessibility text on the existing Recents category; it uses the existing local usage/recency ranking. Its discoverability and actual contextual usefulness on the phone still need checking.
- **Source implemented in v1.0.41:** the Emoji footer lower-left button says ABC and dispatches `KeyCode.ALPHA`; the redundant strip back arrow is gone. Preserve the existing hold menu for Emoji, Clipboard, and Settings.
- Review the screen-reader label, visible icon, category position, tap target, horizontal swipe/snap behavior, and recents refresh so Smart Emoji cannot look like ordinary empty Recents. Never change panel height when its ranking updates.
- In the existing three-slot suggestion strip, keep text candidates and current `mWillAutoCorrect` center emphasis. Offer up to three relevant Emoji candidates from the active token, brrPersonal Emoji mappings, CLDR keywords, and local recency/use counts; make each Emoji an explicit tap action and never replace accepted text. Never expose valid-only slang as unsolicited text completion.
- Inspect `Suggest.kt` and `DictionaryFacilitatorImpl.kt` insertion/ordering first: preserve the current decoder's auto-correct choice and three-column layout, but allow up to two relevant Emoji only when token/shortcut relevance is positive. Keep typed text available as a candidate, and keep local-use updates disabled in no-learning/private editors.
- **Current source correction — released:** `EmojiPalettesView.initialize()` always builds category tabs even when `mSecondaryStripVisible` is false; `KeyboardSwitcher.setEmojiKeyboard()` shows the category strip only while the Emoji panel is open; `EmojiPalettesView.restoreEmojiCategoryStrip()` restores it after Emoji search; and `EmojiLayoutParams.kt` reserves the same strip height from the Emoji grid when that row was previously omitted. `KeyboardSwitcher` hides typing candidates before revealing this shared strip so stale words do not flash in the category row. Source commits [7d0f191](https://github.com/Batmancpu/brrrBoard/commit/7d0f191a36a5f9750539e71b49eb7ab1e3e901d8) and [1d45dee](https://github.com/Batmancpu/brrrBoard/commit/1d45deeca4d3dd9e1dbea425ce33ce3c82e70fc5) carry the correction; v1.0.47 is published. The malformed earlier checkpoint's failed CI is recorded above. Reno10 acceptance remains open.
- **Current source patch — contextual strip:** `DictionaryFacilitatorImpl.kt` now preserves a single non-Emoji candidate for a typed word and leaves native-ranked contextual Emoji eligible for the two side positions; `Suggest.kt` moves the first text candidate to the center only when the decoder's first result is Emoji. Swipe input and empty-token predictions retain the two-text preference. The decoder score order/confidence flag is not recomputed, and valid-only brrPersonal terms still suppress all text suggestions/autocorrection. CI is pending; confirm visible relevance on Reno10.

### Batch 4 — Keep supplied offline data, make swipe typing default

- **Already implemented:** the three static files recovered from the saved FrostKeys backup are bundled by default as `app/src/main/assets/dicts/main_hi-Latn.dict`, `emoji_en-IN.dict`, and `emoji_hi-Latn.dict`; loader defaults and the release workflow's byte checks include them. Their contents match the backup's files. Do not copy them a second time. Keep `UserHistoryDictionary.hi-Latn.dict` device-local only.
- Keep the existing native spatial decoder and measure quality before changing ranking weights. Use a finite, license-reviewed vocabulary; separate suggestion-eligible terms from valid-only slang/profanity and Emoji mappings. No internet access in typing.
- Audit the in-tree native gesture decoder path, JNI service loader, build ABI filters, current `JniUtils.sHaveGestureLib` gate, and import-only settings path. Remove the external ZIP/file picker and its setup language only after a compatible redistributable engine is bundled/default; first prove the in-tree decoder can replace the proprietary GlideLib or select a legally redistributable alternative. The owner reports the supplied GlideLib works on their phone, but its public archive has no license and identifies proprietary Google code. Do not ship that binary without redistribution rights.
- Treat the owner's phone check as evidence that the supplied GlideLib runs and that poor output is a vocabulary/ranking complaint. Preserve its known working behavior as a compatibility reference; do not claim the engine itself is defective. Inspect the provided archive's precise license/source and CPU coverage before choosing an alternative. Public distribution requires a license permitting redistribution.
- Measure language-model size, prefix lookup/decoder latency, and APK delta in CI; keep indexes built off the keypress path. Retain the current `brrPersonal` naming, dictionary asset and valid-only flags; keep user learning bounded, on-device, and disabled in private/no-learning fields.
- The separate 116 MB AppleColorEmoji font is not in the backup and is excluded by its upstream artwork-license terms. Keep native/system Emoji rendering unless a redistributable bundled font becomes available.

### Batch 5 — Dictation, privacy, and final acceptance

- `InlineDictation.kt` calls `SpeechRecognizer.createSpeechRecognizer()`, which uses the Android system-selected `RecognitionService`. Dictate's maintainer documents the distinction between this invisible service, the separate voice-input IME (which replaces brrBoard), and the `ACTION_RECOGNIZE_SPEECH` activity (which opens a window) in [issue #359](https://github.com/DevEmperor/DictateKeyboard/issues/359). The owner reports that the mic reaches another offline provider; the likely cause is that Android Voice input still selects that provider while Dictate is installed as a keyboard. Source now checks `Settings.Secure` key `voice_recognition_service`; when it is not Dictate's `net.devemperor.dictate/dev.patrickgold.florisboard.dictate.recognition.DictateRecognitionService`, the mic opens `Settings.ACTION_VOICE_INPUT_SETTINGS` and does not start another engine. CI and ColorOS acceptance remain pending.
- Once Dictate is selected as Android's Voice input service, keep its text transcription through the RecognitionService callbacks and brrBoard visible. Request English but keep Dictate's saved Active language = English and Follow keyboard language = off, because Dictate may ignore caller locale extras. If the OS settings action is unavailable on ColorOS, display the manual path; never switch to Dictate's voice IME or the popup activity.
- Preserve English request/filtering, password/incognito/no-personalized-learning guards, and brrBoard's no-INTERNET rule. Dictate's private Active language can still override caller locale extras; device acceptance remains open.
- Build/sign with the existing package and certificate workflow, report automatic tests accurately, then hand the owner a Reno10 checklist for the states unavailable to CI.

### Acceptance checklist

- Only English US/UK and Hinglish appear as keyboard layouts; other Preferences remain intact except obsolete global style/theme selectors.
- Emoji scrolls horizontally; the Smart Emoji entry is visible; Emoji can return to letters; three-slot text/Emoji predictions remain stable.
- Keyboard outer height, bottom alignment, toolbar, footer, and host-content boundary match across all panel types; the top highlight is visible without tinting the whole surface.
- Static user dictionaries ship by default, personal learned history stays local, swipe typing needs no outside install, and normal typing remains offline.
- Mic uses the configured Dictate RecognitionService inline; owner verifies English output and the final visual/gesture behavior on Reno10.

### Exact next implementation sequence and exit checks

1. **Make Smart Emoji reachable — released in v1.0.47:** always show the Emoji category strip independently of the typing toolbar setting and keep panel height aligned by reducing the Emoji grid's measured area by the strip height only when newly shown. Reno10 confirmation of the sparkle entry and Emoji scrolling is still open.
2. **Reconcile the physical keyboard frame:** trace `LatinIME.onComputeInsets()`, `RoundedKeyboardFrameView`, `InputView`, `KeyboardWrapperView`, `EmojiLayoutParams`, `ClipboardLayoutParams`, and `main_keyboard_frame.xml`; compare total frame, strip, key area, footer and visible system inset for letters, symbols, numpad, number/phone, Emoji, Clipboard and toolbar. The active patch exposes the footer across number layouts, makes the footer 48dp high with 48dp button hit regions, lifts icon glyphs with a 10dp bottom content inset, and compensates the key-body height by 10dp. CI must first confirm compilation; Reno10 must confirm visual spacing. Tune only measured mismatches; exit when all modes share the selected height and host content stops at the keyboard boundary.
3. **Finish color and glyph system:** inspect `KeyboardTheme.kt`, `FrostedGlassHelper.kt`, `KeyboardIconsSet.kt` and every visible key vector; keep neutral glass/letter caps, map Material You to the closest approved Apple-inspired action color only when optional tint is enabled, clear recycled tint/filter state on every editor change, and keep the 1dp top highlight. Exit when URL → text → URL and light/dark transitions redraw correctly in CI/source review and Reno10.
4. **Finish contextual suggestions and dictionary behavior:** trace `DictionaryFacilitatorImpl.kt`, `Suggest.kt`, `InputLogic.java`, `SuggestionStripView.java`, and `brrPersonal` generation. Preserve decoder scores and `mWillAutoCorrect`; rank Emoji only from the active token/shortcut and local Emoji usage; keep at most three strip choices, with explicit Emoji taps; keep swear/slang spelling-valid but out of ordinary text completions/autocorrection. Use existing PHINC-derived data and the bundled backup assets; do not add a second unmeasured weighting formula. Exit when local phrase/shortcut cases and private-field/no-learning behavior pass CI and owner acceptance.
5. **Provide swipe typing by default:** check source/ABI/license and benchmark a redistribution-compatible offline decoder against the owner's GlideLib experience; retain `brrPersonal`/local history as the candidate source. Bundle and enable the chosen engine by default, check APK size and latency, then remove the ZIP/file picker, manual setup copy, and importer-only strings. Do not ship the proprietary Google binary without redistribution rights. Exit when all supported ABIs build and the owner accepts English/Hinglish swipe quality on Reno10.
6. **Close dictation, settings and icon audit:** v1.0.44 now verifies the selected Android RecognitionService and opens Voice input settings instead of silently using another engine. Confirm Dictate is selected, set Dictate's Active language to English with Follow keyboard language off, preserve inline mode and privacy guards, and audit all mic hold/tap, Emoji hold/ABC, 123 hold/tap, action-key, toolbar/accessory and hidden-language states. Exit only on Reno10 confirmation.
7. **Final release and audit:** preserve package, signer, permissions and update identity; report automatic CI and its known 13-test baseline separately from Reno10 results; complete the request-by-request table in this plan and hand over the signed APK only after source/build and owner-device acceptance.

Preserve package/signing/update identity, the existing Frosted Glass renderer, and the working owner-confirmed interactions. Use Android's EditorInfo and system window semantics where the host app or OS owns behavior. Do not claim hardware acceptance from source or CI results.

## Verified starting point and recovery limits

- Historical v1.0.19 is the requested starting point. Current signed release is [v1.0.39](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.39), source [6f308ac](https://github.com/Batmancpu/brrrBoard/commit/6f308ac72b3b14364d0ef0a98542ff8242e3c0d0), Obtainium run [43778036994](https://github.com/Batmancpu/brrrBoard/actions/runs/43778036994), SHA-256 `4451b5cdaa653c95e27cca92f596f13d57fad971097337a049eba40495965865`. Main has shipped brrPersonal, Smart Emoji Recents, learned-history reset, global-incognito mic guards, and the v1.0.38 English-result guard. v1.0.39 redraws the default Return/Delete vectors. Dictate's Active language and Reno10 acceptance remain open.
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
- Apple iOS 27, onscreen keyboard, including touch-and-hold then slide for alternate characters: https://support.apple.com/guide/iphone/iph3c50f96e/27/ios/27
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

- In the supplied screenshot, the compact right-side control is visually consistent with Apple's Writing Tools access point. brrBoard's matching compact icon is `ic_board_toolbar`; it opens the Android toolbar/access-point menu, not Apple's Proofread action.
- The repository contains `AiWritingToolsView.kt` with Proofread/grammar options backed by `GeminiService`. Source tracing found that `LatinIME` currently returns early for `KeyCode.AI_TOOLS`, and `KeyboardSwitcher.onToggleKeyboard(AI_TOOLS)` ignores the request. Treat that Gemini panel as unreachable and not a working feature in this build.
- Android's current SpellCheckerService offers word-level spelling candidates. It does not provide sentence grammar proofreading. Do not enable the Gemini/cloud route or label ordinary spell-check as full Proofread under the owner's offline, no-AI typing requirement. A true no-network writing assistant would require separate product scope; no such feature is claimed here.

## Progress log

- [x] Batch 0: handoff, personalized README and agent guidance committed.
- [x] Batch 1: default RecognitionService route plus English request shipped in v1.0.21. CI signed/published the APK. Owner reports Dictate path works but output remains Hindi until Dictate's own active language is set to English.
- [x] Batch 1b: v1.0.26 corrects selected-service copy and blocks Devanagari-script final results with a Dictate setup hint. It is not full English-language detection; Romanized Hindi and Dictate's saved Active language remain unresolved.
- [x] Batch 2 source: stale editor tint refresh, visible nav insets, and top-edge highlight are present; the 48dp footer/10dp key-body compensation is in signed v1.0.48. Obtainium build passed; debug compile/sign passed but automatic tests reported the known 13/180 failures. Reno10 check is open.
- [ ] Batch 3d source/CI: allow contextual Emoji in the two side suggestion positions while keeping text/autocorrection centered. The current source patch is queued for GitHub CI; Reno10 acceptance remains open.
- [x] Batch 3a: center autocorrection highlight from existing mWillAutoCorrect signal shipped in signed v1.0.22; Reno10 visual acceptance remains pending.
- [x] Batch 3c source fix: `SettingsValues` disables `mShowsVoiceInputKey` whenever `mIncognitoModeEnabled` is true, including global Always Incognito; `LatinIME` gates `VOICE_INPUT` dispatch on that same flag. Signed in v1.0.36; `InlineDictation.start()` adds a second guard in v1.0.37.
- [x] Batch 3b source audit: the current filter preserves ordinary text candidates and the existing center mWillAutoCorrect signal; brrPersonal valid-only slang can surface Emoji candidates without text completion/autocorrection, with explicit taps only. Keep the accessory separate. [ ] Reno10 acceptance of text/Emoji relevance, center styling, clipboard suggestions, and accessory behavior.
- [x] Batch 4a: system-following OG light/dark and optional Apple-palette action-key tint shipped in signed v1.0.23; Reno10 visual acceptance remains open.
- [x] Batch 4b source: original Shift outline/filled/Caps Lock glyphs plus safe saved-icon migration shipped in signed v1.0.25; v1.0.39 redraws the default Return and Delete glyphs. Reno10 visual acceptance remains pending.
- [x] Batch 5a source: brrPersonal uses the existing native decoder, pinned PHINC baseline capped at 6,000 total entries, and curated valid-only slang/profanity plus Emoji shortcuts. Normal completion/autocorrect hides those terms; deliberate swipe decoding retains them. Build-time generation adds no runtime network permission. Signed v1.0.31 is published; dictionary accuracy, latency, and Reno10 acceptance remain open.
- [x] Batch 5b-source: added a confirmation-protected clear-history action in Text Correction settings. Signed v1.0.35 clears only local UserHistoryDictionary word/word-pair learning and preserves brrPersonal, Android's dictionary and Emoji history.
- [ ] Batch 5b-validation: confirm learned-word removal and subsequent relearning on Reno10; verify sensitive-field and no-personalized-learning exclusions remain honored. Do not treat the reset button's build as device acceptance.
- [x] Batch 5c source: integrated the existing local recency/use ranker into the Recents grid, up to three responsive rows, refreshed after use, preserved incognito, fixed double-counting, and recorded accepted Emoji strip selections once. Signed v1.0.33; Reno10 acceptance and contextual text-strip behavior remain open.
- [x] Batch 5d source/license decision: upstream identifies GlideLib as proprietary Google code extracted from OpenGApps; the archived source has no LICENSE and says its ARM64 build is untested. Do not bundle in a public APK. The older manual-import direction is superseded by the 2026-11-16 owner correction: remove the importer and implement a redistributable offline engine by default. Reno10 gesture acceptance remains open.
- [x] Batch 6 source/release audit: v1.0.39 signed APK/source identity and current request-by-request source status are recorded in the full audit below. [ ] Owner Reno10 install/update and runtime acceptance remain open.


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

## Full source audit — 2026-11-16

| Request from the pasted chat | Current source/release state | What remains |
|---|---|---|
| Reno10-fit iOS-inspired frame, no unused bottom navigation gap, stable height across letters/Symbols/Emoji/Clipboard/toolbar | Earlier releases contain measured IME boundary and panel-height work; no pixel-ratio hard-coding. | Owner must confirm ColorOS insets, bottom edge, height, blur, and host-content boundary on the Reno10. |
| iOS-like key icons and visible controls | Shift/Emoji/mic use custom artwork; v1.0.39 redraws the configured Return/Delete resources. Right accessory remains a separate compact toolbar trigger. | Check icon size/stroke and all panel/field states on the device. |
| Auto light/dark OG appearance and optional wallpaper-derived tint | Source has system-following light/dark and optional action-key-only tint; stale-tint source fix exists. | Test URL → normal text → URL and dark/light/wallpaper transitions on Reno10. |
| Three suggestions and confidence styling | Existing native decoder and mWillAutoCorrect center signal are preserved. Three visible slots stay independent of the right accessory. | Validate relevance and confidence appearance with real English/Hinglish use. |
| brrPersonal: finite, useful North-Indian Hinglish; slang/cuss valid but not text-suggested; Emoji shortcuts | v1.0.31 dictionary and current candidate filtering implement the source rules; no runtime network or new ranking equation. | Reno10 accuracy, latency, swipe candidates, and local learning/reset acceptance. The old private 252-word draft cannot be recovered. |
| Smart Emoji using current term and personal use; up to three rows/candidates | v1.0.33 Recents ranker uses local counts/recency; source strip allows explicit Emoji choices without replacing valid-only slang with text. | Check three-slot behavior and actual relevance on device. |
| Swipe typing, using the owner-verified GlideLib by default | Existing private ZIP importer exists; the public upstream archive has no license and labels the Google library proprietary, with ARM64 untested. | Not bundled in public APK. Test private import on Reno10 or provide a redistributable licensed library. |
| Mic tap stays inline and uses purchased Dictate service; English only | v1.0.21+ uses Android's selected RecognitionService. v1.0.38+ requests English and rejects confident non-English/Devanagari results. | Owner reports Hindi output. Set Dictate Active language to English and disable Follow keyboard language. Dictate ignores caller extras and supplies no language metadata; Latin-script Hindi cannot be reliably classified here. |
| Mic-hold switch, no mic in private fields/incognito | Source retains mic-hold input switching and password/no-learning/global-incognito guards; included in signed releases. | Confirm device interactions and privacy behavior. |
| Proofreading icon/action | Android spell-check is word-level. Repository Gemini proofreader is not reachable through the current AI_TOOLS callback and is cloud-based. | No working grammar Proofread is shipped. Do not enable it without explicit agreement on the cloud/API behavior; current keyboard remains local for typing. |
| Apple emoji font by default / lean APK | 116 MB Apple font is excluded because its artwork is not licensed by the source project's MIT code license and the app should stay lean. | System/host app emoji rendering remains platform-controlled. |
| README and context continuity | Personalized README, AGENTS.md, PROJECT_CONTEXT.md, plan, known issues, and conversation log are updated on GitHub. | Keep these checkpoint files current on each future batch. |

The source/release audit is complete. The project is **not yet Reno10-accepted**: user-device verification is required for layout, tint, gestures, suggestions, dictionary quality, and Dictate English output.

### 2026-11-16 implementation log — Batch 1

- Updated `method.xml` to retain the three owner-selected keyboard subtypes, `LanguageScreen.kt` to show only those choices and remove the redundant OS subtype-sync control, and `SubtypeSettings.kt` to clear old extras and migrate existing installations.
- Removed Key Style, Icon Style, individual icon customization, and theme-preset pickers from the appearance settings flow. Pinned key/glyph rendering to the existing default style in `KeyboardTheme.kt`, `KeyboardIconsSet.kt`, and layout-scale defaults in `Settings.java`.
- Validation: XML parsed with exactly `en-US`, `en-GB`, and `hi-Latn`; `git diff --check` passed. Local APK assembly was blocked before source compilation by unavailable Gradle 8.14.4/network access. No tests were run.
- Next: make Emoji category pages horizontal and page-aware; expose Smart Emoji visibly; implement the single ABC return behavior; then correct frame/edge measurements.


## 2026-11-16 correction and current implementation batch

**Subtype catalog correction.** The first language/style commit `ce7b2d5` limited `method.xml` itself to three subtype declarations. Obtainium run [43804893066](https://github.com/Batmancpu/brrrBoard/actions/runs/43804893066) built, signed and published v1.0.40. Its paired debug run [43804893049](https://github.com/Batmancpu/brrrBoard/actions/runs/43804893049) built and signer-verified the APK but failed 21 of 180 automatic tests. Thirteen matched the existing baseline; eight additional failures came from removing internal parser/resource fixtures: two parser-locale cases, three subtype cases and three Hangul input cases. The local source restores the full 128-entry internal subtype catalog from the pre-batch parent. The language settings screen remains filtered to `en-US`, `en-GB`, and `hi-Latn`; the one-time migration still clears previously saved extra enabled languages. v1.0.40 is superseded for device acceptance; do not mistake its signed publication for a passing debug test gate.

**Emoji interaction source batch.** `EmojiPalettesAdapter.java` now flattens category pages into full-width page items; `EmojiPalettesView.java` uses a horizontal `LinearLayoutManager` with `PagerSnapHelper` and stores category/page state during swipes. The Recents category now uses a sparkle icon and the “Smart Emoji” accessibility label. `BoardFooterView.kt` displays ABC and dispatches `KeyCode.ALPHA` while Emoji is open; the old back arrow at the leading edge of the category strip is removed to leave one letters-return control. The top strip is now reserved for emoji category navigation. Obtainium run [43806306816](https://github.com/Batmancpu/brrrBoard/actions/runs/43806306816) published the signed v1.0.41 APK. Debug run [43806467695](https://github.com/Batmancpu/brrrBoard/actions/runs/43806467695) compiled, verified and uploaded the APK but found the existing 13/180 unit-test baseline (5 Parser, 7 InputLogic, 1 StringUtils); restoring `method.xml` avoided the extra subtype/Hangul failures from the first attempt. No tests were run manually. Reno10 behavior is still unverified.

**Next.** Complete the geometry batch: trace actual window/frame/inset reporting and editor-field tint reset, keep outer height aligned across letters, symbols, number, Emoji, Clipboard and toolbar panels, tune bottom breathing room without deleting ColorOS-required gesture insets, and add the narrow white top glass highlight. Then handle shipped static dictionary/Emoji data, licensed bundled swipe recognition, and remaining suggestion/dictation audits. Keep owner-only Reno10 acceptance open.

## 2026-11-16 — frame/inset/tint source batch and corrected data status

The three static assets recovered from the FrostKeys backup are already in the default asset path and are hash-checked by the Obtainium workflow; the earlier sentence saying they still needed integration was inaccurate. The learned `UserHistoryDictionary` from that backup remains private/device-only. The supplied 116 MB Apple emoji font remains excluded; the assets do not include it.

The working source diff now touches `RoundedKeyboardFrameView.kt`, `InputView.java`, and `LatinIME.java`: on API 30+ it reserves the navigation inset only while that system inset is visible, passes no nav-bar inset into child layout, redraws a narrow 1dp white curved highlight for the frosted frame, and reapplies the freshly loaded `MAIN_BACKGROUND` after a new editor field/theme switch. `git diff --check` passed before this checkpoint. No local APK/tests were run. Automatic debug compilation/signing/test gate will run after this source is committed. Reno10 ColorOS spacing, URL → normal → URL tint transitions, and equal panel height still need owner acceptance.

**Next:** commit and check the frame source; then reconcile panel-height formulas and contextual Emoji suggestions, audit/removal of external gesture installer against a redistributable recognizer, and trace selected dictation service. Keep the public GlideLib binary blocked on licensing; do not mislabel the owner-verified private archive as redistributable.


## 2026-11-16 — v1.0.49 and explicit Dictate-service batch

- **Release state:** Obtainium run [43815989435](https://github.com/Batmancpu/brrrBoard/actions/runs/43815989435) published [v1.0.49](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.49) from source `b4faaaac2e5afafcdafb8f4f4a6d1df782b5ef5c`. The debug workflow [43815989319](https://github.com/Batmancpu/brrrBoard/actions/runs/43815989319) built, signer-verified and uploaded its APK; the automatic test task reported 180 completed and 13 failed (5 parser/currency, 7 URL/autospace, 1 Emoji recognition). The gate is red; do not describe the debug workflow as passing. No manual tests were run.
- **Contextual strip:** `DictionaryFacilitatorImpl.kt` now retains one text result for a typed word and allows decoder-ranked Emoji to occupy side slots; `Suggest.kt` moves the first available text candidate into the center when an Emoji ranked first. Decoder scores remain unchanged, while autocorrection is reevaluated against the promoted center candidate. This does not yet use local Emoji counts in strip ranking. The Smart Emoji picker category and Recents ranking exist separately. Phone relevance is unverified.
- **Source audit:** `LanguageScreen.kt` currently exposes only English (US), English (UK) and Romanized Hinglish. The internal locale catalog remains complete for parser compatibility. Appearance no longer exposes global key/icon style selectors or theme presets; unrelated preferences are retained. `EmojiPalettesView` uses horizontal snapped pages and the footer returns to letters as ABC. Those source facts do not establish the owner's reported Reno10 behavior.
- **Current Dictate fix:** `InlineDictation.kt` now reads `Settings.Secure.voice_recognition_service`, verifies the selected component belongs to Dictate and is discoverable for `RecognitionService.SERVICE_INTERFACE`, then passes that exact component to `SpeechRecognizer.createSpeechRecognizer(context, component)`. It will not silently resolve another service. Dictate's separate voice-input IME and its recognition activity remain outside the tap-to-inline path. The recognizer still follows Dictate's saved provider/language settings. CI is pending; do not claim Reno10 verification.
- **Next:** wait for the debug compile/test gate and signed release workflow. Then address tint and alternate-key behavior, complete local Emoji-use ranking, select a licensed/default swipe engine after integration/accuracy review, and finish the whole-request checklist. Keep owner-device checks open.

## 2026-11-16 — v1.0.50 Dictate routing and neutral tint batch

- **Dictate:** `InlineDictation.kt` reads the selected `voice_recognition_service`, checks the exact component is a discoverable RecognitionService in `net.devemperor.dictate`, and binds `SpeechRecognizer` to that component. It keeps brrBoard visible and does not launch Dictate's voice-input IME or recognition activity. Obtainium run [43818042870](https://github.com/Batmancpu/brrrBoard/actions/runs/43818042870) published [v1.0.50](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.50) from [11810ad](https://github.com/Batmancpu/brrrBoard/commit/11810ad0e6778a2a473dbfc005dcc9a54630f131). Debug run [43818042687](https://github.com/Batmancpu/brrrBoard/actions/runs/43818042687) compiled/signed/uploaded but failed the known automatic gate: 180 tests, 13 failures. No tests were run manually.
- **Tint:** frosted defaults were 250% saturation and up to 500% vibrancy, amplifying Material You hue on surfaces/letter keys; night special-key vibrancy was 0%, muting Apple action accents. `Defaults.kt` now sets saturation/vibrancy to 100%. `KeyboardTheme.kt` migrates stored values only when equal to previous shipped defaults and preserves customized values. The optional action tint still maps the system accent through the Apple palette. v1.0.51 release build/signing passed; the debug build compiled and the same 13/180 automatic-test baseline remained. No phone color acceptance is claimed.
- **Current source batch:** the selected Emoji category tab was using raw Android `colorAccent`; it now uses the shared nearest-Apple CIELAB palette mapper. CI is pending and no phone appearance result is claimed.
- **Next:** inspect alternate-key popups and every visible icon, connect local Emoji counts/recency and brrPersonal shortcuts to the strip, then integrate a licensed offline swipe path that can use `brrPersonal`. Keep all Reno10 checks open.

## 2026-11-16 — v1.0.51 neutral tint batch completed

- The tint change is committed in [3cbdab6](https://github.com/Batmancpu/brrrBoard/commit/3cbdab649b7df1fa6efc39d5f9f0e788143e642c). Defaults set frosted saturation/vibrancy to 100%; a one-time migration updates only values matching the prior defaults, preserving other saved values. Optional tint remains restricted to selected action keys and maps the system accent through the existing Apple-inspired palette.
- Obtainium run [43819288817](https://github.com/Batmancpu/brrrBoard/actions/runs/43819288817) published [v1.0.51](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.51), signed APK 51,028,859 bytes, SHA-256 `f52c1ea1031ec20720abc4595f5899495054e4b1710a2f67b2504cdaf4b6ef97`.
- Debug run [43819288789](https://github.com/Batmancpu/brrrBoard/actions/runs/43819288789) compiled, verified the signer and uploaded a 59,266,617-byte APK (SHA-256 `453c9ed21a0448f7f0674283c3e245ff6a61bef7300f265fa0403418b210bbd7`). Its automatic gate retained the 13/180 baseline: 5 parser/currency, 7 URL/autospace, and 1 Emoji-recognition failure. No manual tests were run.
- **Still open:** Reno10 color and layout acceptance. Next source batch: audit alternate-symbol popup behavior and icon mapping, then connect local Emoji use/recency and brrPersonal mappings to the suggestion strip; assess a licensed offline swipe decoder before removing the external import.


## 2026-11-16 — Apple-palette Emoji category highlight

- Source audit found `EmojiPalettesView.setCurrentCategoryId()` was applying the raw Android `colorAccent` to the selected category tab. This bypassed the CIELAB nearest-Apple palette helper already used by the keyboard theme.
- `KeyboardTheme.kt` now exposes that existing mapper to Java, and `EmojiPalettesView.java` uses it for the selected category highlight. No keyboard surface or key tint behavior changes in this batch.
- CI is pending. Confirm the highlight on Reno10 after installing the resulting signed APK. Next audit alternate-key popup behavior and visible icon mappings.


## 2026-11-16 — v1.0.52 palette checkpoint and shipped-dictionary trim

- Obtainium run [43820853475](https://github.com/Batmancpu/brrrBoard/actions/runs/43820853475) published [v1.0.52](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.52) from [34c4179](https://github.com/Batmancpu/brrrBoard/commit/34c4179dc1a867616f474f26d5b5d0df3adc910c), APK 51,028,859 bytes, SHA-256 `c1b8063b9d74abf86fd423d2eb967a74b87b5b266635372cd6408de0d7f570ab`. The selected Emoji category highlight now uses the existing nearest-Apple-palette helper. This release build succeeded; no Reno10 visual acceptance is claimed.
- Debug run [43820853163](https://github.com/Batmancpu/brrrBoard/actions/runs/43820853163) compiled, signer-verified and uploaded its APK, then failed the automatic gate: 180 tests completed, 13 failed (5 parser/currency, 7 URL/autospace, 1 Emoji-recognition). The failure list is unchanged from the known baseline. No tests were run manually.
- **New implementation batch:** `app/build.gradle.kts` filters 16 unsupported main dictionary assets (30,876,181 raw bytes) out of every installable variant. The three visible owner languages remain backed by English US, English UK and Romanized Hinglish. Both English/Hindi Emoji dictionaries remain bundled. All dictionary files stay in source, and `runTests` keeps the full catalog to preserve test coverage. This does not delete user-cached dictionaries. Build-size impact, asset extraction, and language fallback still require CI verification.
- **Next:** wait for release/debug workflows from this commit, confirm the selected dictionary set in the APK, then continue the three-slot local Emoji ranking and alternate-key/icon/layout audit. Keep Dictate provider/language and all proportions as Reno10 acceptance items.


## 2026-11-16 — v1.0.53 installable dictionary payload trim

- Commit [1d9ed8d](https://github.com/Batmancpu/brrrBoard/commit/1d9ed8d476870e3fd0918659b0098ab86a72af4f) changes `app/build.gradle.kts`: all installable variants ignore the 16 main dictionaries not offered in the owner-facing language screen. English US, English UK, Romanized Hinglish and both Emoji dictionaries remain packageable. No dictionary source files, internal subtype catalog, or test fixture was deleted; `runTests` retains the full asset catalog.
- Obtainium run [43822092860](https://github.com/Batmancpu/brrrBoard/actions/runs/43822092860) succeeded and published [v1.0.53](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.53): 37,949,189 bytes, SHA-256 `38817186ea3db60c2937f7afa28ab89e49dff0e6163e351c71d21b8df5be62c6`. This is 13,079,670 bytes (~25.6%) smaller than v1.0.52. The release workflow preserved its exact-byte checks for the shipped Hinglish and Emoji dictionaries.
- Debug run [43822092903](https://github.com/Batmancpu/brrrBoard/actions/runs/43822092903) built and signer-verified a 46,063,050-byte APK (SHA-256 `d7cb84cd406b1d07c5c95b9f708e83c51a956966c4a112928588a11aa40eac5b`) and uploaded artifact [14204702723](https://github.com/Batmancpu/brrrBoard/actions/runs/43822092903/artifacts/14204702723). Its test step completed 180 tests with the same 13 failures: 5 parser/currency, 7 URL/autospace, 1 Emoji-recognition. No manual tests were run.
- **Smart Emoji source audit:** the Smart tab icon/accessibility label already exists in `EmojiCategory.getCategoryTabIcon()` and `getAccessibilityDescription()`; `KeyboardSwitcher.setEmojiKeyboard()` reveals the tab strip and `EmojiPalettesView.addTab()` renders that icon. The optional separate persistent Emoji row is off by default. Existing local frequency/recency ranking feeds only that optional fast row; next connect the ranker to the Smart Emoji category and contextual Emoji slots without changing decoder text scores. Reno10 discoverability/content remains unverified.

## 2026-11-16 — owner-requested audit and implementation sequence

This is the current ordered plan. Every implementation batch is committed together with updates to `AGENTS.md`, `PROJECT_CONTEXT.md`, this file, and `docs/PROJECT_CONVERSATION.md`. CI results get a documentation checkpoint before the next source batch.

### Completed source/release work

- **v1.0.54 Smart Emoji visibility:** populate a useful local first-use grid instead of leaving Smart empty until recents exist; preserve real recents and incognito behavior.
- **v1.0.55 contextual Emoji:** rank decoder-supplied Emoji by local frequency/recency into side candidate slots; leave text/center candidate and decoder scores unchanged.
- **v1.0.56 123 slide:** tap 123 remains a persistent Symbols switch; slide from it selects one symbol and returns to letters through the existing momentary keyboard state.
- **Language/style cleanup:** visible language choices already are English US, English UK, and Romanized Hinglish. Global key/icon style and theme-preset choice screens are already removed.
- **APK trim:** 16 unsupported installable main dictionaries were excluded while source assets, test catalog, main English/Hinglish, and both Emoji dictionaries were retained. Release v1.0.53 was about 25.6% smaller than v1.0.52.
- **Tint baseline:** wallpaper-derived saturation/vibrancy defaults were returned to 100%; optional action-key tint uses a nearest Apple-inspired palette color. Device appearance still needs owner acceptance.

### Batch 1 — remove dead toolbar choices (active)

- Hide AI Tools, GIFs, Stickers, and internal Close History from toolbar-key preferences, persistent-key preferences, custom key-code chooser, access menu, pinned row, and drag/drop.
- Preserve enum names to read older serialized preferences; migrate stale saved selections to supported entries when Settings opens.
- Keep working Settings, Clipboard, editing, Emoji, Voice, layout, and accessibility controls.
- Inspect CI and document its result before Batch 2.

### Batch 2 — map and correct layout/interaction

- Trace touch behavior from key view and pointer tracking through KeyboardActionListener/InputLogic for letters, 123, alternate popups, Emoji, mic, toolbar, and swipe dismissal.
- Use the supplied stills for the three candidate slots, highlighted center correction, separate right utility action, and Emoji/mic bottom-corner placement. Do not treat stills as evidence for unseen Emoji panel transitions or edge blur.
- Measure actual Reno10 and iPad logical dimensions, density, IME/window insets, and user-configured keyboard scale. Correct the outer frame, rows, bottom baseline, and panel transitions from those measurements rather than guessing dp values.
- Make text, symbols, numeric, Emoji, Clipboard, and toolbar panels share the intended outer frame. Remove empty spacer/inset space the owner does not use, while preserving balanced footer breathing room and usable hit targets.
- Keep Emoji pages horizontally swipeable, show one appropriate ABC return control, and verify tap-to-enter/return behavior. Test Smart tab discoverability and maintain a stable frame while panel content changes.
- Check alternate-key long press/slide timing, popup direction/clipping, slide-off behavior, and 123 tap-versus-slide. Change only behavior traced to the actual Android pointer/state path.
- Render the thin top-edge glass highlight, rounded frame, key spacing, three prediction slots and independent utility accessory; compare dark/light captures on device.

### Batch 3 — finish color/icon behavior

- Re-trace editor transitions (URL → text → URL, light ↔ dark, tint on/off) through LatinIME, KeyboardSwitcher, KeyboardTheme, and drawable state. Clear recycled per-key filters and stale accent state.
- Keep glass and alphabet keycaps neutral. Keep only automatic dark and light frosted appearance plus one optional tint switch; remove obsolete preset/style choices, not unrelated useful preferences.
- Derive an action-key accent from system/Material You wallpaper colors and map it to the closest restrained Apple-like palette color with contrast checks. Apply to selected action/highlight keys only, never the whole keyboard.
- Inventory every visible icon across letters, symbols, Emoji, footer, suggestions, toolbar and popups. Match stroke/weight/alignment using original project vector assets; do not copy proprietary SF Symbols.
- Recheck accessibility labels, hit areas, and dynamic-light/dark icon contrast.

### Batch 4 — Dictate service routing

- Follow mic tap/hold from BoardFooterView and key handling through InlineDictation, input method manager and Android RecognitionService selection.
- Ensure tap uses the selected Dictate recognition provider and does not launch or switch to an unrelated keyboard/dictation UI; keep the keyboard shown if inline service API supports it.
- Respect the selected service's actual active language; verify the user's English configuration, Hinglish limits, start/stop/error flows, and private/incognito flags.
- Separate source-confirmable behavior from Reno10-only provider/model configuration and record the exact owner setup needed.

### Batch 5 — brrPersonal + bundled offline swipe

- Inspect the current 6K PHINC-derived Hinglish data and curated entries, data format, ranking path, JNI/native ABI and the external gesture-library installer.
- Curate a finite, license-cleared North Indian Romanized Hinglish vocabulary from reviewed corpora plus manually checked current usage. Keep common English/Hinglish forms and spelling aliases; flag swear/slang entries valid-only so they are not text completions or autocorrections, while permitting an independent emoji mapping.
- Rank suggestions deterministically using decoder confidence/frequency, user-accepted local counts, recent context and keyboard path/distance; cap output at three. Do not override a high-confidence decoder candidate without measured evidence. Update local counters off the per-keystroke hot path, bound storage, and disable learning in password/incognito/no-personalized-learning fields.
- Improve swipe accuracy with the same dictionary and candidate ranking; measure top-1/top-3 accuracy, latency, memory and APK size on Reno10 for ordinary typing and code-mixed phrases.
- The user reports GlideLib works on their phone, but the supplied archive has no redistribution license and contains proprietary Google code. Do not put it into a public APK. Select a license-cleared compatible offline decoder, bundle it in the APK by default, and only then remove the outside-library install flow. If no viable cleared engine exists, report that blocker and keep the working manual route until the user chooses a licensed source.
- Keep the existing static Hinglish and Emoji dictionaries bundled; do not fetch or duplicate them. Do not bundle the 116 MB Apple Emoji font because of the asset license and size.

### Batch 6 — lean release and acceptance

- Audit installable files by runtime reference, license, fallback behavior, test needs, and measured APK impact. Delete only proven-dead package payload, never useful parser/test fixtures or user dictionaries.
- Preserve application ID, signing identity, release/update mechanism, and exact-byte artifact checks.
- Review release/debug workflows and the known 13/180 baseline; report new versus pre-existing failures separately.
- Publish a signed APK/artifact only after the corresponding gates. Ask the owner to verify on Reno10: layout at configured heights, Insets, Emoji/ABC and sideways paging, Smart Emoji relevance, 123/popup behavior, dictation provider/language, swipe quality/latency, tint transitions, and update installation. Keep iPad layout as a separate tablet acceptance profile.

### Acceptance boundary

The source has no access to the owner's phone, actual ColorOS build, Dictate provider/model state, or in-chat video beyond two still frames. No source review or CI run can close those device-specific checks. Do not describe a feature as accepted until the owner reports it tested.
