Warning: truncated output (original token count: 23204)
Total output lines: 473

# Post-v1.0.19 Reno10 keyboard plan

## 2026-11-16 — autonomous implementation B00 checkpoint

The owner now authorizes implementation across the published dependency-ordered plan and requests continuous, recoverable progress while unavailable for approximately 4–5 hours. This latest direction supersedes the older planning-only instruction preserved below. **B00 is partial:** latest main is `cb1a35d6b506b2214bb19e56d62e50d8b80e48b6`; its diff from source baseline `3ee9da9e9b2a0b4a8c42b1e27d93f4686ff65b21` is documentation-only. Release v1.0.57 remains latest (37,949,189 bytes; SHA-256 `a57da3ad314f4f6e21dd64cf18996c5e215134d631d93f4a3a396e75b32bdbfd`). Debug job 133178974688 built and signer-verified an APK, then its test gate failed 13 of 180 tests: 5 currency parser, 7 URL/autospace, 1 Emoji classification. Local Gradle execution is blocked before configuration by a network failure downloading Gradle 8.14.4. This workspace has no `adb`; Reno10 baseline remains unverified.

**B01 result:** source commit [e4a4a01](https://github.com/Batmancpu/brrrBoard/commit/e4a4a01ce13edf7020335cb802c4687c165db841) on `brrrboard-audit-b01-currency-20261116`, starting from `7ab4bc570b18ad79bd5e4d8761ae910f10de2174`. It makes parser test locale/currency explicit, restores test preference state and cache, and adds configured-₹ coverage without touching product defaults. CI run [43848172243](https://github.com/Batmancpu/brrrBoard/actions/runs/43848172243) built/signer-verified/uploaded the debug APK; 181 tests completed, all five prior ParserTest failures resolved, and the new rupee test passed. Eight pre-existing failures remain (7 URL/autospace; 1 Emoji classifier), so the workflow test gate failed and prerelease publication was skipped. Local tests are blocked because Gradle 8.14.4 could not be downloaded. B00 Reno10 checks remain open. **Next exact action — B02:** isolate URL/autospace test policies, preserving production defaults and assertions, then run focused and full CI before B03. All four continuity records are updated in the implementation commit and this result checkpoint.

## B02 — InputLogic URL/autospace fixture triage (in progress)

Starting checkpoint: `2df304cdcd98b7f08bb912f8974fcd93631b24d5`. Inspection of `Defaults.kt`, `SettingsValues.java`, `Settings.java` and `InputLogicTest.reset()` confirmed product defaults enable URL detection and autospace; clearing preferences does not disable either policy. Four failing test scenarios asserted disabled-policy behavior without explicit false values. `InputLogicTest.kt` now writes autospace=false before editor initialization in `autospace` and `autospaceButWithTextAfter`; it writes URL detection=false before editor initialization in the initial email scenario and URL-disabled selection scenario. Expected assertions and production defaults remain unchanged. Three other failing cases already set URL detection=true (`urlDetectionThings`, URL history separator stripping, and intermediate URL commits), so they remain candidate runtime regressions if CI confirms them. This edit is in progress and not yet validated or committed. Next: diff-check and CI on audit branch, inspect any residual failure, then test focused methods/class and full suite before marking B02 complete. Local Gradle download remains blocked; no `adb` for device checks.

## B01 — currency parser fixture isolation (complete; CI has 8 unrelated baseline failures)

Starting commit: `7ab4bc570b18ad79bd5e4d8761ae910f10de2174`; ending source commit: `e4a4a01ce13edf7020335cb802c4687c165db841`. Changed test path: `app/src/test/java/helium314/keyboard/KeyboardParserTest.kt`; fake IDs now use an explicit US English subtype, the fixture pins `custom_currency_key` to blank, `@AfterTest` restores/removes the previous value, locale cache is cleared at fixture boundaries, and `currencyKeyUsesConfiguredRupee` validates explicit rupee preference. Product code/default remained untouched. `git diff --check` passed. Local Gradle did not run because 8.14.4 could not be downloaded. Remote CI run [43848172243](https://github.com/Batmancpu/brrrBoard/actions/runs/43848172243), job [133233579041](https://github.com/Batmancpu/brrrBoard/actions/runs/43848172243/job/133233579041), built and signer-verified the APK and uploaded its artifact. `testRunTests` completed 181 tests: all five parser/currency failures were fixed, the new configured-rupee assertion passed, and the same seven InputLogic plus one Emoji failures remain (8/181). The final gate failed and the debug prerelease step was skipped. No B02 changes are included yet; next is the URL/autospace fixture contract and CI repair.

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

- **Already implemented:** the three static files recovered from the saved FrostKeys backup are bundled by default as `app/src/main/assets/dicts/main_hi-Latn.dict`, `emoji_en-IN.dict`, and `emoji_hi-Latn.dict`; loader defaults and the release workflow's byte checks include them. Their contents match the backup's files. Do not copy them a second time. Keep `UserHistoryDictio…11204 tokens truncated…ntrolled. |
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
