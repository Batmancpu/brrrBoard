# brrBoard — adaptive text/swipe/Emoji + instant panel switch: implementation handoff

> **Superseded for the next GO:** The owner subsequently reported that an intended swipe word can be **absent from every suggestion** and authorized a ≤20 MB offline encoder plus optional app-specific context. See [brrBrain v2 — independent decoder and context handoff](BRRBRAIN_V2_DECODER_AND_CONTEXT_HANDOFF_2026_11_18.md), whose architecture and revised T0–T6 now govern. Retain this old document only for historical source analysis and detailed Emoji/height semantics; its native-top-three-only learning limit is no longer sufficient.

Status: **DESIGN ONLY — NO RUNTIME CODE CHANGED BY THIS DOCUMENT.** Prepared 18 November 2026 (India). The next request "GO — implement the adaptive typing/Emoji/snap handoff" authorizes this implementation, tests and one original-signed release on main. Do not publish or claim a runtime fix merely because this plan exists.

## 0. Owner request, exact visible failures, and acceptance goal

The owner reports on a real Oppo Reno 10 5G:

1. While composing `happy` or `birthday`, Emoji appear, but after pressing Space (or Enter) suggestions, including Emoji, disappear. With the cursor after `happy `, the keyboard should predict useful **next words** (e.g. `birthday` when the model supports it) and allow appropriate post-word Emoji.
2. Clicking an Emoji in the suggestion strip while `happy` is unfinished replaces the whole typed word with the Emoji. **Never replace text with an Emoji** for an ordinary Emoji suggestion tap. Preserve the typed word and append Emoji using the normal insertion/spacing policy.
3. When a swipe gesture is decoded as the wrong word and the owner explicitly chooses or corrects another word, future similar gestures should favor that correction, with careful decay and false-positive guards. Also learn normal vocabulary and next-word habits from accepted text.
4. Learn which Emoji the owner actually chooses, including contextual usage after words, and reuse this for Emoji Search, Emoji Browse/Recents, and the strip. One shared Emoji knowledge/ranking system.
5. Search/Expanded Emoji and Clipboard Search height grow/collapse currently look janky, especially return to ABC. Owner accepts **instant one-tap snap** instead of a poor spring transition. Preserve system-driven keyboard show/hide animation, approved live glass, footer and native geometry.
6. **Two engines, not three**: existing native text+swipe decoder/dictionaries with a tiny adaptive feedback layer; existing CLDR+reviewed Emoji rules with one adaptive metadata layer. No separate AI or duplicate corpus. Lightweight View presentation is not another engine.
7. The work must be offline, small, private, low latency, original-signing-key compatible, main-only, and must not modify Dictate/voice code currently owned by another agent.

The latest verified functional baseline at planning time is signed **v1.0.129**, commit `e905624632ee70dd0b344930402b9d0c0ca229e2`; debug workflow `44355485307` and Obtainium signer workflow `44355485966` both succeeded. Source CI does **not** validate Oppo screen behavior or prove that the predictor has become more accurate. Before work, refresh HEAD/release and compare for concurrency.

Historic P0–P6 references belong to prior mixed batches; the repository's open ledger is `docs/ACTIVE_REMAINING_BATCHES_2026_11_18.md`, and the smart improvement proposal is identified SK-1A–SK-1H elsewhere. Do not claim that all prior roadmap or device-acceptance items are complete. The stages T0–T6 below are **new handoff stages**, not retrospective completion claims.

## 1. Confirmed source inventory and causal analysis

### Native text and swipe

- `latin/Suggest.kt:getSuggestedWordsForNonBatchInput` already calls `getNextWordSuggestions(ngramContext, ...)` when the WordComposer is not composing; it normally returns INPUT_STYLE_PREDICTION. The native dictionary remains authoritative.
- `latin/settings/Defaults.kt` has `PREF_BIGRAM_PREDICTIONS = true` and personalized suggestions enabled by default. `SettingsValues.needsToLookupSuggestions()` and `InputLogic.performUpdateSuggestionStripSync()` can still suppress/neutralize a strip depending on actual editor and settings. Do not assume an owner setting is at its default.
- `Suggest.kt:getNextWordSuggestions` caches by `NgramContext` (up to 128 keys). The v1.0.129 change invalidates the cache when a main dictionary loads/reloads, **not** after a user-history word/bigram is learned or unlearned. An empty or old result can persist. This is a plausible stale-prediction cause, **not yet a confirmed on-device diagnosis**.
- `inputlogic/InputLogic.java` already calls `performAdditionToUserHistoryDictionary` on accepted word commit; `DictionaryFacilitatorImpl.kt:addToUserHistory` updates the preferred locale's `UserHistoryDictionary` with forgetting curve via `updateEntriesForWord`. This already learns normal words and preceding-word relations. Do not duplicate it.
- `InputLogic.onStartBatchInput / onEndBatchInput / onUpdateTailBatchInputCompleted` and `InputLogicHandler.updateTailBatchInput` drive swipe candidates through the existing imported glide/native decoder. The top candidate becomes composing text. `WordComposer.isBatchMode` and `mAutoCommitSequenceNumber` identify gesture state and reject stale callbacks. `InputLogic.onPickSuggestionManually` distinguishes manual word picks, and gesture Backspace already calls `unlearnWord(... EVENT_REJECTION)` for a composing swipe word; `DictionaryFacilitatorImpl.unlearnFromUserHistory` removes a unigram on rejection but deliberately does not remove on every ordinary Backspace.
- There is **no reliable transaction binding a specific incorrect swipe choice to a specific later corrected word**. Generic Backspace may mean a typo, formatting, or deletion; do not interpret it alone as a confirmed intended replacement.
- `latin/touchinputconsumer/GestureConsumer.java` is a stub in this source tree; the working pipeline is batch input in `InputLogic`, `InputLogicHandler`, `WordComposer`, `InputPointers`, and the bundled imported decoder. Do not build the feature around callbacks in that stub.
- Old `GestureDataGathering`/SQLite research collection was intentionally retired and purged in earlier releases; do **not** resurrect raw gesture-point logging, contribution prompts, or uploaded word/trace samples.

### Current Emoji and strip

- `EmojiContextScorer` owns a reviewed English/Hinglish phrase map. `EmojiDescribeIndex/Engine` owns the pinned CLDR48.2 corpus, async Search and custom aliases. v1.0.129 promotes only **whole-phrase** reviewed rules in Search to protect semantic descriptions (for example "laughing so hard I am crying" -> 🤣); typing can use suffix matches.
- `SuggestedWords.mEmojiContextHint` carries a bounded previous-word hint for **non-prediction** suggestions with multiple native Emoji candidates. `SuggestionStripView.setSuggestions` currently reranks only existing Emoji side entries and asks `EmojiPalettesView.AdaptiveEmojiEngine.getFastRowEmojis` for global usage order; it does not present a contextual Emoji after Space when zero Emoji came from the native prediction result.
- `SuggestionStripView.onClick` passes every suggestion word view to `Listener.pickSuggestionManually`. `LatinIME.pickSuggestionManually` calls `InputLogic.onPickSuggestionManually` even when `suggestionInfo.isEmoji()`; that method `commitChosenWord` replaces the active composing word. This is the **confirmed source explanation** for the owner's "happy becomes Emoji" report. It also records Emoji Recents **before** the editor commit succeeds.
- `EmojiPalettesView.addRecentKey` and `addRecentEmoji` record use; `AdaptiveEmojiEngine` currently persists global `lastUsed`/`freq`/`burst` in SharedPreferences and sorts a 48-item fast row. It does **not** store a safe topic→Emoji association. It creates a fresh Java `Thread` for each Emoji use, parses/serializes JSON, and can synchronously refresh cached rankings on a strip call. Consolidate, do not add a second Emoji preference engine.
- `InputLogic.addToHistoryIfEmoji` already stores eligible word→Emoji transitions in the native history under restricted conditions (last composed word, enabled bigram, non-incognito, non-slow editor). Reuse this n-gram learning and avoid double counting when an Emoji is selected via panel, strip, smart chip, Search or persistent row.

### Mode-height motion

- `EmojiPalettesView` uses `mHeightMotion`, `mBodyHeightChangePending`, `mAnimatedBodyHeight` and `launchBodyHeightMotion` to spring/clip for 260ms. A delayed collapse path `collapseForFooterAlphabet` waits `EmojiPanelHeightMotion.DURATION_MS` before switching to ABC. Search/Expand callbacks and posted `restoreEmojiCategoryStrip` can overlap.
- `ClipboardHistoryView.kt` duplicates the height spring/clip mechanics in `launchHeightMotion` and `collapseSearchForFooterAlphabet`, including a 260ms delayed ABC. Rapid transitions can leave intermediate visual states even if the expensive full-IME remeasure was reduced in v122–v123.
- `SharedPanelGeometry` and `KeyboardWrapperView` already determine native and expanded viewport sizes. Keep those as the only geometry owners. No speculative pixel padding, child alpha fades, or new RenderNode blur logic.

## 2. Chosen architecture and tradeoffs

**Text:** retain the native `DictionaryFacilitatorImpl + UserHistoryDictionary + Suggest + WordComposer` pipeline. Add a compact event-based correction policy (not a decoder or new language model). Its tiny learned confusion pairs, if enabled, may *rerank only decoder-produced word candidates*; accepted words also strengthen the native history. An optional aggregate spatial calibration experiment is separately gated by demonstrated replay improvement; do not promise it in the first signed rollout.

**Emoji:** retain `EmojiDescribeEngine / EmojiDescribeIndex / EmojiContextScorer` and fold contextual acceptance counts into the already existing `AdaptiveEmojiEngine` metadata. CLDR meaning + curated triggers stay authoritative; personal signals only influence eligible ties/side choices. Emoji Search, panel Smart picks, Recents and strip read the same immutable rank snapshot. No Clipboard contents are stored or scanned to train the model.

**UI:** remove custom height interpolation and delayed footer waiting. Switch modes immediately, compute final size once, request a single endpoint layout, clear clip/translation/alpha state. Leave Android/ColorOS's own IME entrance/exit insets animation entirely alone. If device still flickers on window resizing, diagnose a host/window issue; do not reintroduce a spring as the default.

Why this over a neural model/online sync/new gesture library: the existing native model combines spatial and lexical evidence, while reviewable online feedback/forgetting is simpler, smaller and local. Research supports personalized language/spatial weighting but does not guarantee improvement for every interaction. There is no defensible "100% better after every use" claim; use held-out accuracy and no-regression gates.

## 3. T0 — Reproduce and instrument the real bugs before tuning scores

1. At fresh branch HEAD, snapshot exact `main` and `docs/KNOWN_ISSUES.md`; list active workflows. Preserve concurrent Dictate changes. No checkout/reset/force push.
2. Add **test-only** fake editor/IME flow: `happy` composing -> Space -> `happy ` empty typed word -> `birthday` next-word native candidate; `happy birthday ` after second Space -> relevant Emoji. Assert `mInputStyle`, `NgramContext`, settings flags, number of text/Emoji slots, pending handler requests and toolbar visibility.
3. Instrument **development-only** transition counters (not typed text): candidate request count, cache hit/miss, empty native results, final visible strip count, stale-generation drops, Emoji tap insertion type, body-height endpoint. Do not log strings, app package, editor contents, gestures or per-user dictionaries. Remove or keep debug-only behind build flag.
4. Establish baseline on an emulator with a real offline dictionary, plus the owner on Oppo: default vs personalized-on, punctuation/Space/Enter, no-suggestions input, password/incognito, Telegram/WhatsApp/notes. Distinguish **no prediction generated** from **generated but hidden by toolbar/pinned-key UI**. Do not fake native top-3 output with authored strings.
5. Freeze synthetic authored fixtures for 20 normal next-word contexts, 20 protected names/URLs, 30 gesture confusion pairs and 25 Emoji contexts. Actual native baseline outputs are required before calling a ranking win.

## 4. T1 — Fix the two reported suggestion semantics first

### T1A. Emoji tap means insert, never "pick replacement word"

- Extend the `SuggestionStripView.Listener` action contract with an explicit Emoji-insert action (or tag a typed action type), and branch in `SuggestionStripView.onClick` on `SuggestedWordInfo.isEmoji()`. Preserve the existing path for text, punctuation and application-defined completions.
- Add a dedicated `LatinIME` entry and `InputLogic` transaction that: (a) validates the active editor/session and Emoji eligibility; (b) completes the active composing word **as typed** without taking the Emoji as a correction; (c) follows existing `mSpaceState`/`SettingsValues.mInputAttributes.mShouldInsertSpacesAutomatically` to insert at most one space when appropriate; (d) commits the Emoji using the existing `onTextInput`/normal Emoji text route; (e) updates shift, predictions and Recents once **after** successful commit. Do not simply call `commitChosenWord(emoji)`.
- Exact expected examples: `happy` + Emoji tap => `happy 🎉` (spacing governed by actual prefs/editor); `happy ` + Emoji tap => `happy 🎉`, not `happy  🎉`; selected unrelated host text must not be silently replaced unless the user explicitly requested replacement; no extra space at start/after opening punctuation; multi-codepoint Emoji/skin tones/ZWJ intact; cursor restored after Emoji.
- Distinguish Emoji strip picks from deliberately replacing highlighted host selection; if a selection exists, require the same behavior as the regular Emoji palette insert path and include a UI test. Do not duplicate sound/haptics.
- Route `LatinIME.pickSuggestionManually`/`EmojiPalettesView.addRecentEmoji`/`KeyboardSwitcher` persistent row through one acceptance receipt to prevent a failed/double insertion incrementing use twice.

### T1B. Persistent post-Space predictions and contextual Emoji

- In `Suggest.kt` and `InputLogic.performUpdateSuggestionStripSync`, honor `mBigramPredictionEnabled` and `needsToLookupSuggestions`; ensure successful Space/Enter normal commits schedule a fresh `INPUT_STYLE_PREDICTION` with the correct previous-word n-gram, without blocking the IME. Do not synthesize predictions in password/URL/no-suggestion fields.
- Identify why `happy ` is empty on Oppo: native dictionary result vs cache vs `retrieveOlderSuggestions`/neutral strip vs pinned toolbar in `SuggestionStripView.updateSuggestionContainersVisibility`. Fix the observed branch based on T0, not guessed ordering.
- Add a **selective history revision / cache invalidation** contract. On successful `DictionaryFacilitatorImpl.addToUserHistory(word, ngramContext)` and explicit unlearn, invalidate the affected `Suggest.nextWordSuggestionsCache` context (or tag entries by dictionary revision). Do not evict per keystroke; mark one generation only at word-commit boundaries. Ensure stale async callbacks cannot put old results back after an invalidation.
- With zero native next-word candidates, the strip must not show stale unrelated text. If a useful low-cost deterministic fallback is needed, first benchmark the existing dictionaries; only then add a tiny reviewed **text dictionary** pair seed or a native personal-history fallback through `DictionaryFacilitator`. Do **not** invent a second text prediction engine or assert that "happy" always predicts "birthday" for every locale.
- Give `SuggestedWords.mEmojiContextHint` a safe **post-Space** projection of the last completed one or two words when `resultsArePredictions`; currently it is empty for predictions. Use `EmojiContextEligibility` and `NgramContext` already in the background query; no additional `InputConnection.getTextBeforeCursor` call per key.
- Allow **at most one** Emoji accessory chip after Space (high-confidence reviewed topic or well-supported personal usage), even when the native next-word list contains zero Emoji. This chip is a UI action with an existing CLDR-supported Emoji, not a fabricated decoder text suggestion. Preserve up to two readable next-word text choices in the three-slot strip and do not steal the center word/prediction. If context is ambiguous `happy `, prefer general high-confidence positivity or no Emoji; do not prematurely force 🎂 when the user may mean "happy new year".
- After `happy birthday `, the exact phrase is eligible to show 🎂/🎉; after `happy `, an ordinary word prediction like `birthday` can coexist with an optional generic Emoji chip. Search's exact-phrase ranking regression from v1.0.129 remains covered.
- Enter versus Space: Enter may end a sentence or submit the editor; only keep predictions when the keyboard remains active and the editor supports suggestions. Never override IME_ACTION_SEND/SEARCH/DONE or leak prior editor context across focus changes.

## 5. T2 — Add verified swipe→correction learning to the existing text engine

### Signal definition and lifetime

- On `InputLogic.onStartBatchInput` initialize a generation/locale/session token. On finalized `onUpdateTailBatchInputCompleted` capture only the chosen decoder word and at most the top 3 distinct lexical candidates, **not pointer x/y/time arrays**. Hold in RAM until the user accepts, explicitly picks an alternate, reverts/deletes and replaces within a small bounded interaction window (proposed 8 seconds or next two word commits), changes focus/locale, starts new gesture, or enters private field.
- The gesture's initial top word is **not assumed to be intended**. Explicit alternate suggestion pick is strongest positive feedback; replacing a rejected composing swipe word with a complete manually typed/suggestion-picked word in the same editor is medium evidence; continuing normally past next separator is weak acceptance. Ordinary Backspace alone is negative evidence against acceptance, **not proof of an intended replacement**. Canceled gesture, fast second swipe, cursor movement, paste, remote app text changes and selection changes kill the sample.
- Use `WordComposer.isBatchMode`, `InputLogicHandler.mInBatchInput` and `mAutoCommitSequenceNumber`/IME session generation to avoid late async hits. Never learn from `getGestureSuggestedWordsForExternalInput`, Clipboard Search's embedded alphabet, Emoji Search's embedded alphabet, accessibility-generated completions, or app-specified suggestions.
- When a confirmed pair (decoder `wrong` -> user-chosen `right`) is valid: feed the **right word** and its preceding n-gram into the existing user-history dictionary **once** (avoid double credit from `commitChosenWord`); call the existing explicit EVENT_REJECTION unlearn path for the rejected swipe if appropriate; store a small bounded confusion **weight**, not a new dictionary or raw gesture. Only use a pair if language/keyboard layout consistent and both words are reasonable single lexical words, not protected code/URL/profanity-sensitive input.

### Adaptive rerank policy

- Add a tiny locale-scoped `GestureCorrectionFeedback` value/policy helper (pure deterministic score/math), owned by the native text pipeline. Persist at most 256 distinct `(locale, wrong, right)` pairs with count, lastUsed and explicit-correction confidence in the existing preference/learning settings layer; cap normalized words to 2–24 Unicode code points, use a versioned schema, prune lowest/oldest on background executor. This is learned **feedback metadata**, not another candidate generator, and it must be visible/resettable in Settings.
- Bound a candidate-only bonus: e.g. a monotonic confidence based on `min(3, confirmedCorrections)` plus exponential aging (30-day half-life), with overall cap so a strong native spatial/lexical difference still wins. Start by reranking only alternative strip candidates; permit automatic top-1 promotion **only** after >=3 independent confirmed corrections for that pair and a held-out test proving no top-1 loss on protected/common contexts. Under low native confidence or ambiguous touch path, keep the decoder's safe first word.
- The owner can correct an earlier adaptation: an explicit contrary pick reduces or reverses the pair count; edits cannot permanently poison a common word. No unbounded reinforcement from automatic predictions. Do not reward a word simply because the keyboard predicted it.
- Word choices not returned in the native top-3 cannot be conjured from the same swipe geometry; repeat use can make them available via existing `UserHistoryDictionary` and optional user dictionary. This plan **does not promise to recover an arbitrary absent candidate** without retraining/replacing the native spatial decoder.
- Normal keyboard tap-typed words continue learning through `performAdditionToUserHistoryDictionary`, which already implements n-grams/forgetting. Fix cache freshness and duplicate/false learning; do not add a separate per-key tap logger.

### Optional T2B spatial thumb calibration, evidence-gated OFF in initial rollout

- The owner's thick-thumb Y→O→U explanation is physically plausible, but the "Google library" imported into brrBoard is **not proven to be the complete current Gboard decoder or editable/licensable**. First audit `GestureLibraryImport.kt`, native ABI/install/signature, `InputPointers`, `WordComposer`, `DictionaryFacilitatorImpl.getSuggestions`, and `docs/B17A_GLIDE_REPLAY.md`. Do not substitute a binary, decompile proprietary code, or assume Gboard internal training can be ported.
- If native replay demonstrates a systematic device/user offset, experiment **only on a transient copy** of `InputPointers` before the decoder JNI call, with aggregate per-region (not per-word) x/y offsets computed from *explicitly confirmed* gestures. Derive matching/alignment cautiously using authored word key centers; discard raw path immediately and store only aggregate count/mean/capped variance per key cluster and layout orientation. Cap adjustment to a small fraction of key width; warm up on at least 30 verified gestures per cluster, reject outliers, decay/rollback. Do not alter keyboard drawing or touch targets.
- This spatial experiment is **not approved for default shipping** until A/B replay improves real top-1 and p95 with no regressions/ABI/privacy problems. A lightweight lexical feedback layer is the safe initial feature; spatial tuning is a separate measured flag **within the same text engine**, not a third engine.

## 6. T3 — Context-sensitive Emoji adaptation inside the existing engine

### Accepted use: one source of truth

1. Add a single `recordAcceptedEmoji(emoji, safeContext, eventId)` entry to the existing Emoji subsystem, called **after** the editor successfully receives the Emoji. Route Palette/Recents/Smart picks/Emoji Search/strip/persistent row through it; remove pre-commit double-recording. When distinct actions can trigger the same hook, dedupe by transient insertion ID rather than text matching.
2. Validate the complete Emoji sequence against bundled `EmojiContextIndex` or `mEmojiCategory.findEmojiKey`; preserve skin tone, variation selector, flags and ZWJ family as actually chosen. Use neutral sequence only for semantic cluster lookup; retain last explicit variant preference for display/selection.
3. Make non-private EditorInfo and current `SettingsValues` eligibility a prerequisite: `EmojiContextEligibility.isEligible`, `mUsePersonalizedDicts` and new Emoji-learning setting; no password/OTP/email/URI/no-suggestions/no-personalized-learning/incognito. If the user disables personalization, stop learning and forget learning metadata according to their choice.
4. Prefer the **existing** `InputLogic.addToHistoryIfEmoji` word→Emoji n-gram path for ordinary last-word usage. Extend its commit coverage only after proving no double write, stale context or multi-Emoji sequence bug. This already gives arbitrary user-word adaptation through the native history when legal; do not create a second literal phrase database.
5. Use the active last completed 1–2 words **only in volatile memory** to compute a canonical reviewed topic ID (for example GREETING, BIRTHDAY, THANKS, LOVE, FOOD, NIGHT) from `EmojiContextScorer`. Persist topic ID→Emoji counts, not typed sentences, words, package names, clipboard entries or conversation windows. Ambiguous/unrecognized text updates only global Emoji recency; no invented topic.
6. Add bounded topic associations under the existing `AdaptiveEmojiEngine` JSON metadata, with schema `version:2` and backwards-compatible reader for old `lastUsed/freq/burst`. Initial suggested cap: 128 global Emoji and 256 topic+Emoji pairs; prune by exponentially decayed use, recency and age. Keep JSON size bounded and tolerate malformed/old content without silently destroying old Recents.
7. A short-lived Emoji delete/undo (for example an immediate Backspace after insertion within 3 seconds, unchanged editor/cursor) can cancel a pending adaptive increment; delayed deletion must not become a permanent negative preference. Two successive identical Emoji taps should count as two **actual** insertions, not one due to same string.

### Selection and ranking

- Keep `EmojiDescribeIndex` CLDR annotation relevance and exact Search semantic priority. Let personalized topic/global weights break ties **inside** the qualified result set; never turn "crying" into first result for "laughing so hard I am crying" or let weak personal history mask direct Search intent. User custom aliases remain stronger than personalization.
- Keep `EmojiContextScorer` reviewed trigger map as the topic vocabulary. No new JSON corpus, AI tokenizer, background NLP model or network request. Do not scan complete Clipboard text; Clipboard Search stays its own history filter.
- The strip, Emoji Search, Smart row and Recents read **the same** `AdaptiveEmojiEngine` snapshot, but display policies differ: strip may show one contextual Emoji action; Search prioritizes explicit query semantics; Smart panel may show multiple; Recents remains strongly recency-biased.
- Suggested bounded score: `baseReviewedOrCLDR + min(15, 6*ln(1+topicCount)) + min(5, 2*ln(1+globalCount))` in a local rank projection after eligibility and corpus filtering. These weights are **hypotheses**; freeze fixtures and tune downward if irrelevant Emoji displace correct text/meaning. Avoid modifying native text decoder scores. User's last Emoji should be noticeable but not monopolize every context forever.
- For `happy `, ambiguous positive topics must not select one of birthday/Diwali/new-year by fiat. With repeated explicit owner choices, personal adaptation may favor the user's actual preference; otherwise show a generic supported positive Emoji or no Emoji while keeping native text predictions.

### Performance repair built into this same Emoji work

- Replace `AdaptiveEmojiEngine.recordEmojiUsage`'s `new Thread(...)` per press with one bounded app-private serial executor already used for Emoji background work if available, or exactly one lazy single-thread worker. Use coalesced writes (suggested 250–500ms debounce) and flush at safe panel/session lifecycle edges; never block IME event, never spawn a worker per tap.
- Keep an `AtomicReference`/volatile immutable ranking snapshot computed off-UI-thread. `SuggestionStripView.setSuggestions` should read at most a small already-ranked array/list in O(1) and rerank at most the visible Emoji candidates; never synchronously parse a 3944-entry JSON/CLDR index or run `getRankedEmojis` on the keystroke path. On a cold cache, use stable old Recents until async refresh completes.
- Invalidate one local topic/global ranking revision after accepted Emoji; notify panel/strip only if still in same EditorInfo generation and visible, avoiding RecyclerView shifts while user presses. Existing `addPendingKey / flushPendingRecentKeys` stays in charge of visual reordering.

## 7. T4 — Remove janky grow/collapse, make one-tap transitions atomic

**Default chosen behavior: instant snap, no custom spring.** This is directly authorized by the owner's "fix properly or remove it" instruction and avoids native IME-window resizing vs inner clip competing on ColorOS.

### Emoji

- In `EmojiPalettesView.java` remove the `ValueAnimator mHeightMotion` state machine, `prepareBodyHeightMotion`, `launchBodyHeightMotion`, clip/top-inset, `mAnimatedBodyHeight`, generation and `mShrinkClipAwaitingLayout` coupling. In mode handlers `enterSearchMode`, `exitSearchMode`, `collapseExpandedBrowse` and Search→Expanded, change the child visibility/state and call a **single final** `requestLayout`. Do not interpolate `onMeasure`. Preserve `SharedPanelGeometry.emojiSearchBodyHeight/emojiExpandedBodyHeight`, categories, pager and two-row compact viewport.
- Replace `collapseForFooterAlphabet(Runnable)`'s `postDelayed(..., 260ms)` with direct state reset/ABC callback in the same logical user action; no artificial blank waiting period. Cancel pending Search/Expand posted restores by generation when switching editor/keyboard. Ensure the host's `KeyboardSwitcher` action remains the single switch owner.
- Reset stale render state on every transition and detach: `setClipBounds(null)`, `setClipChildren(false)` as appropriate, `alpha=1` for visible children and no leftover `translationY`. Do not remove carefully approved top/bottom glass or force a second backdrop tint. `revealWithoutFlash` may retain its no-fade visibility semantics.
- Retain `EmojiPanelHeightMotion.java` only if a different active consumer needs its bounds test; otherwise delete unreachable animation helper/tests once both Emoji and Clipboard are migrated. Do not leave a 260ms constant controlling ABC delays.

### Clipboard

- In `ClipboardHistoryView.kt` remove `heightMotion`, `prepareHeightMotion`, `launchHeightMotion`, `animatedBodyHeight`, clip/translation animation listener and 260ms delayed `collapseSearchForFooterAlphabet`. Search to browse/back/ABC becomes immediate endpoint geometry with exactly one layout invalidation. Reset `searchHeader.translationY` and `clipboardRecyclerView.translationY` to 0 and clip to null.
- Preserve horizontal clipboard card lane, safe Search alphabet reparent/return, typed query, pinned cards, undo, contextual clips and full smart glass. Never accidentally expose cards beneath QWERTY during Search→ABC. Do not change local Clipboard retention or permissions.
- For an active frame with two quick actions (Search→Back→ABC or Expanded→Search→ABC), use a simple mode generation token to discard stale worker callbacks, not any duration-based tap lock. The final mode and touch listeners must agree immediately. Verify `PointerTracker.switchTo` receives the current keyboard once.

### Geometry and platform behavior

- Keep `SharedPanelGeometry` and `KeyboardWrapperView.getLastTypingPanelViewportHeight` as the source of normal/expanded height. If needed, consolidate the final endpoint computation into one helper shared by Emoji and Clipboard without altering approved measured values. No fixed arbitrary 40/48/70dp offsets.
- Android's `WindowInsetsAnimation` coordinates the host app and IME windows; it is not an API for reliably animating a third-party IME's **internal Emoji height**. Leave system show/hide to ColorOS and avoid installing another insets animation callback solely for mode switching. If system imposes a residual outer-window transition, measure it on Oppo and keep internal state instant.
- Reduced Motion / animator-duration-scale=0 must have exactly the same snap behavior. Accessibility focus and touch targets remain available in the first frame. Do not animate for landscape/floating/split or when detached.

## 8. T5 — Consent, privacy, data model, backup and controls

### User-facing settings

- In `latin/settings/Settings.java` + `Defaults.kt` define `PREF_LEARN_SWIPE_CORRECTIONS` and `PREF_LEARN_CONTEXTUAL_EMOJI`. In `TextCorrectionScreen.kt` show "Learn from corrected swipes" and "Learn contextual Emoji choices", with short plain descriptions explaining on-device learning and that changing these controls does not send text anywhere. Default both **OFF** on upgrade until the owner deliberately enables them. This avoids silently expanding collection even though existing personalized dictionary defaults to ON.
- Add a small reset action "Forget swipe/Emoji adaptations" with independent confirmation and a clear "Clear all learned typing history" interaction. The existing `CLEAR_LEARNED_TYPING_HISTORY` must reset swipe-pair feedback **and** relevant native user history; documented Emoji counters should either be included in a clearly titled "all personalized learning" reset or have a separate explicit reset. Do not erase saved clipboard items, custom Emoji aliases or user dictionary entries when user only resets adaptive weights.
- Disabling `PREF_KEY_USE_PERSONALIZED_DICTS` must immediately disable *both* new adaptive learners. Incognito and `IME_FLAG_NO_PERSONALIZED_LEARNING` override settings. Neither setting grants permission to use passwords, email addresses, URLs, banking/OTP or app editor metadata.

### Persistence / limits / compatibility

| Data | Persistence | Cap | Delete behavior |
| --- | --- | --- | --- |
| Native word+bigram history | Existing `UserHistoryDictionary` per locale | Existing native forgetting/limits | Existing clear personalized history |
| Confirmed gesture confusion pair | Tiny versioned learning metadata in existing preferences layer | 256 pairs; prune/decay | Forget swipe adaptations / all learned data |
| Emoji global count/recency | Existing `pref_adaptive_emoji_metadata` | 128 actively scored, preserve original saved Recents | Forget Emoji adaptations; keep unrelated aliases |
| Emoji reviewed topic ID→sequence | Nested v2 fields of the same Emoji metadata | 256 associations, no raw words | Forget Emoji adaptations |
| Last swipe candidates and editor context | RAM only; expire/cancel | 3 candidates, 8 seconds | Focus switch, cancel, incognito, IME destroy |
| Raw touch points/sentence/clipboard/app name | **Not persisted or transmitted** | 0 | N/A |

- Store no Internet permission, no remote model, no usage telemetry, no screenshots. Never log candidate words or context in production. Hashing predictable words does **not** make it private; prefer topic IDs or existing native history.
- Introduce a `version` field in Emoji metadata and separately versioned swipe metadata; old preference JSON remains readable; malformed JSON disables the new layer and keeps the keyboard usable. Preserve user backups with explicit schema and migration tests, or exclude ephemeral adaptability by documented user choice. Under no circumstance allow import of external JSON to execute code or provide unbounded collections.
- Per-locale words cannot be allowed to contaminate another language; no naive lowercasing for Turkish dotted I, accents or Hindi. Use locale-aware normalization and NFC code point length limits; preserve codepoints and Unicode Emoji ZWJ. If current bilingual native group is uncertain, do not train a pair.
- Cap retained metadata in bytes and operations, not only element count. Prototype budget: total <64KB for new adaptive metadata and no new packaged binary model. Exact budget to be measured in CI on maximum-size fixture, not assumed.

## 9. T6 — Test architecture, acceptance and release gates

### Test-only additions

- `app/src/test/java/helium314/keyboard/latin/inputlogic/EmojiSuggestionInsertionTest.java`: composing `happy`, after `happy `, selected host text, multi-codepoint family/skin tone, no duplicate space, mode switch, disabled suggestions. Use fake `InputConnection` and assert final text, cursor, native history calls and no wrong-word replacement.
- `app/src/test/java/helium314/keyboard/latin/PostSpacePredictionTest.kt`: normal next-word candidate after Space, empty native results, cache invalidation after history update, stale async callback rejection, Enter/action semantics, clipboard/pinned-toolbar coexistence and multiple locales.
- `app/src/test/java/helium314/keyboard/latin/GestureCorrectionFeedbackTest.kt`: confirm vs ordinary delete; three confirmations vs one; same locale only; consent, incognito, canceled/tail interleaving, no feedback from unrelated word, decay/pruning, conflict reversal, candidate-only rerank, no out-of-list injection.
- `app/src/test/java/helium314/keyboard/keyboard/emoji/AdaptiveEmojiContextTest.java`: v1 metadata migration, context topic+Emoji counts, global fallback, search direct-match precedence, different skin tones, dedup receipt, rapid delete undo, no private-field update, size caps.
- `app/src/test/java/helium314/keyboard/keyboard/emoji/EmojiPanelSnapTest.java` and `clipboard/ClipboardPanelSnapTest.kt`: switch ABC↔Emoji Search↔Expanded↔Browse and Clipboard Search↔ABC under fast taps/rotation. Assert single final height, no lingering clip/translation/alpha, zero 260ms wait and correct `PointerTracker` target.
- Extend `SuggestTest.kt`, `InputLogicTest.kt`, `EmojiContextScorerTest.java`, `EmojiDescribeBundledCorpusTest.java`, `SuggestionStripVisualPolicyTest.java` and existing `EmojiContextEligibilityTest`. Preserve v1.0.129 regression that "laughing so hard I am crying" must rank 🤣.
- Extend `tools/evaluate_ngram_predictions.py` and `docs/B17A_GLIDE_REPLAY.md` *only for authored synthetic fixture comparisons*, never capture the user's messages, raw swipe traces or clipboard. Build a test-only native decoder adapter before asserting improved top-1/top-3 on actual outputs.

### Required reproducible behavior matrix

| Action | Required outcome |
| --- | --- |
| Type `happy` without Space | Normal word slots; optional relevant Emoji action; **tap Emoji appends, does not replace happy** |
| Type `happy ` | Fresh native next-word options (if model has them), not stale/hidden; relevant Emoji action eligible after Space |
| Type `happy birthday ` | Strong contextual 🎂/🎉 eligible without losing useful text predictions |
| Tap Emoji after word with and without space | Exactly one Emoji; never double-space or erase unfinished word |
| Tap text prediction after `happy ` | Insert predicted word with normal spacing, not Emoji insertion semantics |
| Swipe "you" but explicitly pick another word | Record valid correction once; candidate rank shifts on repeated confirmed identical context; no automatic promotion on first mistake |
| Backspace unrelated old text | Never create a swipe correction pair |
| User deletes Emoji immediately | Cancel pending increment only for same insertion/session |
| Password/incognito/no-learning/email | No adaptive writes, no persistent context, no context-derived Emoji |
| Search "laughing so hard I am crying" | 🤣 first; exact custom alias still higher priority |
| Search→Back→ABC, Expanded→ABC x10 fast | Immediate snap, no blur flash, ghost page, 260ms delayed return or stale touch target |
| Landscape, rotate, animation scale 0, light/dark | Correct endpoint height, unchanged glass and footer, correct focus |
| Consent OFF / reset / import old backup | Old typing works; old Recents/aliases preserved; new learning fully inert or safely removed |

### Performance and quality gates — fail closed

1. Both GitHub workflows (debug full Android unit suite + original signer full suite) must finish **SUCCESS**; no bypass of failures. Use `git diff --check`, existing B16A/B16B Python tests, native ABI/signature verification and APK SHA. No temporary signer; preserve `com.mangoloads.board` and monotonic Obtainium release tags.
2. On-device or emulator instrumentation: compare the same baseline vs changed authored gesture traces, not subjective anecdotes. Track exact top-1/top-3, false replacement/false suggestion rate, accepted correction count, next-word coverage and per-session prediction freshness. Accept a learning score change only if **no protected-case regression**, at least unchanged aggregate top-3 on held-out fixtures, and a reproducible benefit for confirmed corrections. If no benefit, ship semantics/snap fixes and leave learned score promotion disabled.
3. UI: an Emoji tap must never destroy a composing word in any test. No multi-space insertion or stale/past-editor context. Target no new expensive per-key work. Measure word-tap, final swipe decoding, panel change p50/p95 and frame pacing on actual Oppo; propose ≤2ms additional p95 CPU on a swipe-completion feedback path and zero database/JSON writes on UI thread. Adjust only after real baseline measurement.
4. Use Android `Perfetto FrameTimeline` or `adb shell dumpsys gfxinfo com.mangoloads.board`, and optionally debug-only `JankStats`; do not add a production analytics SDK. Check 60/90Hz frame budgets, thermal/battery and background executor queue under 100 rapid taps. No claim of 60fps without trace.
5. Owner installation acceptance: share exact original-signed APK, confirm same package/signature installs over v1.0.129, and request a 15–30s Oppo screen recording of `happy`, `happy + Emoji`, `happy + Space + birthday`, Emoji Search→ABC, Clipboard Search→ABC and a few repeated swipe corrections. Confirm current release version in Settings before closing a visual issue.

### Atomic commit and handoff checkpoints

Implementation can be completed in one owner-approved **work session**, but save small main-only commits (no feature branches), each after passing locally available tests:

1. T0 diagnostic fixtures and documented baseline.
2. T1 Emoji append + post-Space suggestions/cache freshness; stop on incorrect text mutation.
3. T4 instant Emoji/Clipboard snap + remove delayed ABC; device visual check if available.
4. T2 text/swipe confirmed feedback and T3 Emoji adaptive metadata with T5 settings/reset. Keep ranking optional behind settings until quality gate.
5. T6 full quality/performance/privacy suites, original signer, signed release, owner acceptance.

**Check main HEAD immediately before every push** and use non-force expected-SHA update. If Dictate/automation commits while work is underway, compare changed paths, rebase only nonconflicting source hunks and preserve their code. Never rewrite history, edit voice/mic/Dictate files, commit private logs, add trackers or change the signer. Append exact request, changed paths, commands, CI job IDs, failure/fix, version/sha and remaining Oppo QA to `AGENTS.md`, `PROJECT_CONTEXT.md` and `docs/KNOWN_ISSUES.md` after each verified stage.

## 10. Primary-source research and adopted lessons

1. **Google Research, "Spatial model personalization in Gboard" (2022):** individual touch offsets/covariances can give modest real-world improvements; avoid per-user overfitting, consider data quantity and key clustering. Adopt a **measured optional aggregate spatial stage**, not shipping direct raw-path personalization. https://research.google/pubs/spatial-model-personalization-in-gboard/
2. **Google Research, "The Machine Intelligence Behind Gboard":** keyboard decisions combine spatial evidence with an n-gram/lexical transducer, and reverted corrections/manual picks are useful but imperfect supervision. Adopt confirmed positive/negative feedback at the existing decoder boundary. The article does not grant access to or license to copy Gboard internals. https://research.google/blog/the-machine-intelligence-behind-gboard/
3. **Google/CHI 2015, "Effects of Language Modeling and its Personalization on Touchscreen Typing Performance":** personal LM context can improve simulated touch typing; benchmark and don't overclaim real device gains. https://research.google/pubs/effects-of-language-modeling-and-its-personalization-on-touchscreen-typing-performance/
4. **Adhikary & Vertanen, Interspeech 2023, "Language Model Personalization for Improved Touchscreen Typing":** contextual adaptation outperformed simple unigram cache in a simulation; models need pruning, and actual long-term behavior requires user study. A bounded n-gram/recency layer fits our speed/privacy constraints better than an extra neural model. https://www.isca-archive.org/interspeech_2023/adhikary23_interspeech.pdf
5. **AOSP LatinIME UserHistoryDictionary / DictionaryFacilitatorImpl:** existing native decaying n-gram vocabulary/history is the integration point. https://android.googlesource.com/platform/packages/inputmethods/LatinIME/+/fa1e65cb3a5dcce6299a6dd067cee95720107307/java/src/com/android/inputmethod/latin/personalization/UserHistoryDictionary.java
6. **HeliBoard FAQ and background gesture-data documentation:** recognizes that hidden swipe intent is uncertain even after a correction; raw background traces are sensitive. Adopt explicit confirmed corrections and no raw trace retention. https://github.com/HeliBorg/HeliBoard/wiki/FAQ and https://github.com/HeliBorg/HeliBoard/wiki/Background-Gesture-Data-Gathering
7. **Android EditorInfo / InputConnection official docs:** honor `IME_FLAG_NO_PERSONALIZED_LEARNING` and real composing/commit/selection contracts; editor IPC can fail, and retrieving text incurs process calls. https://developer.android.com/reference/android/view/inputmethod/EditorInfo and https://developer.android.com/reference/android/view/inputmethod/InputConnection
8. **Android performance/JankStats/IME animation docs:** measure frame time with platform tools, and distinguish host app insets animation from the IME's internal panel switch. https://developer.android.com/topic/performance/vitals/render and https://developer.android.com/topic/performance/jankstats and https://developer.android.com/develop/ui/views/layout/sw-keyboard

## 11. Next-agent "GO" exact prompt

> GO — implement `docs/ADAPTIVE_TYPING_EMOJI_AND_SNAP_HANDOFF_2026_11_18.md` stages T0–T6 in brrBoard, preserving the two unified native text/Emoji engines, main-only guarded commits, privacy opt-ins and signed Obtainium pipeline. Fix the confirmed Emoji suggestion tap replacement and post-Space prediction/Emoji visibility before adding feedback learning; remove the janky 260ms Emoji/Clipboard height springs and delayed ABC. Keep Dictate untouched. Use real decoder/CI evidence, document each checkpoint in AGENTS.md/PROJECT_CONTEXT.md/KNOWN_ISSUES.md, deliver the signed APK and distinguish CI success from physical Oppo acceptance. Do not ship optional spatial-calibration top-1 rewrites unless held-out replay proves benefit.

This plan is deliberately **actionable without further product-design questions**. If a proposed feedback score harms real top-1, fail closed to the baseline decoder and still ship the independently validated insertion/prediction/snap fixes. The user may later choose a more aggressive spatial experiment with explicit privacy and device benchmarks.

