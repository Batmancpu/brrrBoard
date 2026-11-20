# BR Brain — Parakeet-era intelligence and dashboard implementation contract

> **20 Nov 2026 voice-crash hotfix:** Earlier GTCRN native-enhancement/route telemetry caused or coincided with an owner-reported IME crash 1–2 s after mic tap. For the next release, live capture and Parakeet engine revert to pre-GTCRN code, and BR Brain accurately says “GTCRN filtering paused”. GTCRN stays packaged but inactive. The 0.6B Ultra switch is deferred pending measured North Indian WER, Oppo RAM and latency. This does not change the separate neural ContextLM plan.


> **20 Nov 2026 voice enhancement update:** Owner GO authorizes a separate bundled-GTCRN noise-filter integration and a BR Brain voice diagnostic card; this does not authorize or imply trained English/Hinglish neural ContextLM weights. See [GTCRN audio contract](GTCRN_AUDIO_ENGINE_IMPLEMENTATION_2026_11_20.md). North Indian English WER and Oppo/mic routing validation remain open.

> **Owner GO resumed on 20 Nov 2026.** Previous planning freeze is historical. First implementation candidate adds release-safe, RAM-only native typing/nextword/swipe/Emoji latency metrics and truthful live BR Brain dashboard; **trained bilingual neural ContextLM weights are still absent**. See [function-level implementation plan](BR_BRAIN_FUNCTIONAL_CODE_LEVEL_IMPLEMENTATION_PLAN_2026_11_20.md), AGENTS.md and handoff for exact code, CI and later provenance/validation gates.

> **Latest owner decision (20 November 2026): planning-only hold for the *next* app-code batch.** Earlier GO produced existing Smart Emoji source candidates, but no further source/model/APK changes are authorized until a fresh GO. The detailed [function-level implementation and benchmark contract](BR_BRAIN_FUNCTIONAL_CODE_LEVEL_IMPLEMENTATION_PLAN_2026_11_20.md) supersedes this document's historical “wait for GO” vs “GO” status notes for subsequent BR Brain work. The bilingual neural model is untrained/unbundled, original Parakeet 110M/sherpa stays, and code-level targets are unproven until exact-SHA CI and Oppo tests.

Date: 20 November 2026 · Project: brrBoard (`Batmancpu/brrrBoard`, `main`) · Status: **PLAN ONLY — WAIT FOR OWNER'S EXPLICIT INTELLIGENCE GO**

## 0. Owner's decision and freeze line

The owner has **paused the Parakeet Ultra/0.6B model migration**. First improve the *intelligence around the existing, installed* **Parakeet 110M TDT INT8** and the keyboard's existing native dictionaries; redesign and rename the existing `brrBrain · Learning dashboard` user-facing screen to exactly **`BR Brain`**. After an explicit new **GO for BR Brain intelligence**, implement, test against the unchanged 110M model, obtain an original-signed app release only after exact-SHA CI, and request actual Oppo Reno10 5G acceptance. **No application source, model weight, Android runtime, APK, release, workflow or phone installation is authorized by this planning batch.** The display name is **not yet changed in the installed app**.

An Ultra release is a **separate, later approval**; its prior [one-time full-APK private migration plan](ULTRA_OFFLINE_VOICE_INTEGRATION_AND_MIGRATION_PLAN_2026_11_20.md) remains a historical active design but is **ON HOLD**. Do not delete Parakeet 110M, prepare its replacement, migrate assets, change its sherpa-onnx v1.13.3 runtime, alter Obtainium distribution or require network access as part of the BR Brain implementation.

### User-visible outcome to build **after** GO

1. **Typing:** preserve high-quality native dictionary predictions, learned word frequency and native user history, while improving *safe* preference ordering for intentionally typed words, Indian English and accepted corrections. Never quietly overwrite names/identifiers because a fuzzy guess seems plausible.
2. **Swipe:** use the existing encrypted geometric correction memory, stronger verified outcome feedback and conservative candidate presentation. No raw trails on disk, no extra unlicensed CTC glide model.
3. **Blue offline voice:** preserve Parakeet 110M raw recognition and 16-kHz PCM/private worker path; apply safe casing and approved, auditable vocabulary **after** recognition. Offer a correction/learning interaction for clearly observed user edits, with explicit consent to save. The engine does **not** acoustically retrain from typing; dictionary adaptation is a *lexical/post-processing* capability. Do not blindly change homophones or words merely because another entry is frequent.
4. **Emoji/context:** reuse one existing CLDR/reviewed-context path; boost already eligible choices, never replace typed text or read the clipboard merely to infer context.
5. **BR Brain:** one truthful, premium privacy-first screen showing live proven learning/evidence by mode, user controls and granular deletion, with `Not measured` where evidence does not exist.

## 1. Verified baseline in repository as of this plan

| Surface | Current source and behavior | Gap / protected invariant |
| --- | --- | --- |
| Blue offline STT | `latin/OfflineBoardDictation.kt` -> `voice/OfflineVoiceEngine.kt`: private 16-kHz PCM; pause-window segmentation; sherpa-onnx native transducer, 2 threads; transcript formatted by `VoiceTranscriptFormatter.kt`; `LatinIME.onPrivateVoiceTextInput()` inserts without transcript learning and refreshes suggestions. | Voice does **not** currently learn post-dictation corrections. Existing `sameEditor` guard and editor/IME cancel paths must remain; keep voice audio/transcripts off disk/logs/network. `VoiceTranscriptFormatter` is exact-word casing only and lacks personal dictionary input. |
| Existing voice model | Private installed Parakeet **110M TDT INT8**, ~136 MB from historical build-time bootstrap; sherpa-onnx **v1.13.3**. | No model/runtime download, replacement, deletion or hotword decoding assumption in this phase. Blue offline vs separately selected red brrSpeak/possible online provider must stay distinct. |
| Text prediction | `Suggest.kt`, `DictionaryFacilitatorImpl.kt`, `NgramContext.java`, native dictionaries, `personalization/UserHistoryDictionary.java`, Android user dictionary, `dictionary/BrrPersonalDictionary.java` and `brrPersonal.tsv`; next-word LRU keyed by an immutable context. `SmartCorrectionSafety.kt` and valid-only/profanity gates. | Native decoder stays candidate authority; new logic is a small ranking/safety adapter, never a second generative dictionary. Any new learning must respect settings/eligible editors, existing native forgetting, and dictionary invalidation. |
| Swipe | `brain/SwipeCorrectionLearner.java` records at most **64** key-skeleton -> explicitly corrected-word associations; two corrections required, geometry check, ambiguous tie suppressed, suggestion **alternative only**; session swipes/corrections/**offers** counted. AES-256-GCM `BrainLearningVault.java` with AndroidKeyStore, private no-backup file; no plaintext fallback. | Offered != accepted; no real swipe accuracy denominator. Do not replace top decoder result based on a learned pair. Preserve vault and old migration logic. |
| Emoji | `Suggest.kt` combines native suggestions and reviewed contextual Emoji, with `EmojiContextEligibility` filtering. `pref_adaptive_emoji_metadata` records selection counts in preferences. | Emoji preferences are **not** covered by the swipe AES-GCM vault; do not claim all learning is encrypted. Keep insert-vs-replace behavior and category/keyboard affordances. |
| Performance | `IntelligenceMetrics.java` counts DEBUG-only RAM-only timing buckets for next-word/Emoji/swipe; no identifiable strings; not surfaced in release dashboard. | No measured North Indian English WER, true prediction accuracy, accepted recall, native result latency or field-device p95 yet. |
| Dashboard | `settings/screens/BrainDashboardScreen.kt` shows swipe/Emoji counts, up to 8 observed swipe pairs, vault status, `Prediction accuracy: Not measured yet`, refresh and swipe-only erase slider. `SettingsNavHost.kt` route `brain_dashboard`; `MainSettingsScreen.kt` dictionary entry description references brrBrain. | User-facing display title is currently `brrBrain · Learning dashboard`, **not** `BR Brain`. Preserve route ID/preference keys and current data without reset. |

Do not treat older engineering plans or synthetic tests as live device accuracy. Confirm all signatures on fresh `main` before editing after GO; other agents may have changed these paths meanwhile.

## 2. Exactly one intelligence decision layer, not another model

Implement a **model-agnostic `BrainCoordinator`** with scoped adapters. It receives **already computed candidate sets / transcript outputs**, applies safety and a bounded preference adjustment, and returns either the original result or a permitted display-only alternative. It must not own the native language dictionary, replace native candidate generation, fetch outside content, retain full editor history, or run LLM/cloud inference. In the held Ultra future, only the `VoiceRecognizerAdapter` changes; BR Brain contracts/events/storage/dashboard continue unchanged.

```text
Native Suggest + n-gram/UserHistory + brrPersonal --\
Native gesture candidates + correction memory ------> BrainCoordinator + PrivacyGate
Parakeet 110M final transcript ---------------------/      | safe scores / suggestions
Existing CLDR/Emoji context -----------------------/      v
                                              existing IME insert/strip
                                                       |
                                           confirmed user choices only
                                                       v
                                             bounded local BrainEvents
                                                       |
                                     selective encrypted learned vocabulary
                                                       |
                                      BR Brain read-only evidence snapshot
```

The diagram denotes **conceptual** seams, not a new production API. `BrainCoordinator` never sends content to Settings/dashboard; the latter receives sanitized aggregate snapshots and separately authorized reviewable personal tokens.

### Contracts and authority

- `BrainEligibility.evaluate(EditorInfo, SettingsValues, isIncognito, secureAppState, generation) -> {maySuggest, mayLearn, mayVoice, reason}`. Default **deny** on null/stale editor, password, hidden/visible passwords, `IME_FLAG_NO_PERSONALIZED_LEARNING`, secure-app/incognito, `TYPE_TEXT_FLAG_NO_SUGGESTIONS`, email/URI/search/code/identifier contexts and restricted editor variants. Reuse `EmojiContextEligibility`, `InputAttributes`/secure-app policy, not a softer duplicate. A user temporarily disabling protected-app incognito must not override an app's no-learning request or password. Voice availability and learning permission are separate: dictated text may still be committed into an allowed editor while **learning** is prohibited.
- `BrainContextSnapshot` is an immutable, **ephemeral** 0–2 preceding native n-gram words plus locale, editor generation, boundary flags, and source; no app package/history in persisted storage. Get native context from an already available `NgramContext`, **never issue an extra editor surrounding-text read**. Maximum 160 UTF-16 code units; discard on editor/language/privacy/connection switch.
- `BrainCandidateArbiter.rankExisting(source, candidates, context, evidence)` preserves the engine's candidate set (except existing reviewed Emoji side candidates), typed literal, scores/safety veto, caps/locale and top valid candidate invariants. It may adjust an **alternative** ordering only if the source supports it, with deterministic tie break and hard score cap. An invalid/sensitive field => untouched candidates. Prevent personal words that are `valid-only` or `noSuggestionWords` from proactive promotion; no duplicate decoding/reentrancy.
- `BrainEvents` describes `source = TYPE | SWIPE | BLUE_VOICE | EMOJI`, `editorGeneration`, `eligible`, `attempt`, `displayed`, `selected`, `explicitCorrection`, `undo`, `rejected` and coarse latency. Only event **counters** cross to the dashboard; never persist keystrokes, raw transcript, audio, editor text, app identity or gesture coordinates. Avoid counting `displayed` as `selected` or `explicitCorrection` as confirmed model error automatically.
- `BrainLearningRepository` wraps *existing* data: native UserHistory still owned by personalization; swipe stays `SwipeCorrectionLearner`; Emoji stays its approved local frequency store; new voice spelling/pair memory is an independent tiny versioned encrypted file. There is **no migration or wipe of existing native dictionaries or swipe ciphertext** merely for the dashboard.
- `BrainSnapshotProvider.snapshot()` produces copied immutable mode snapshots on explicit Settings refresh / lifecycle resume, not on every keypress. UI binds only this provider. `modelStatus` is read from the actual `VoiceModelInstaller` and runtime, never inferred from a planned roadmap.

### Priority/safety ordering (no bypass)

1. Privacy/editor/session eligibility and user's per-feature toggles.
2. Exact typed text, protected tokens, user undo/reject and native valid-only/no-suggest bans.
3. Existing decoder confidence, layout/locale and known stable candidates.
4. Repeated **confirmed** local user choices with a confidence cap; ambiguous associations => show nothing new.
5. Recency/context and Indian English vocabulary as a *small* deterministic tie break; no blanket forced replacements.
6. Emoji as a distinct, optional insertion action.

When any input is stale, missing, ambiguous or corrupt, **return the unmodified current native/Parakeet output**. No learning loss on failed vault writes. Invalidation: editor-generation change, dictionary load/reload, language/layout change, privacy switch, secure grant revocation, pref change, background/IME hide and model swap. The adapter must remain memory- and deadline-bounded.

## 3. Text and dictionary intelligence — exact behavior

Keep `Suggest.getSuggestedWordsForNonBatchInput()` / `getNextWordSuggestions()`, `DictionaryFacilitatorImpl`, Android `UserDictionary.Words`, `BrrPersonalDictionary` and `UserHistoryDictionary` as independent native sources. Introduce **no redundant word model**. `BrainTextRanker` is post-candidate **alternative reranking** only; the native top correction threshold and `SmartCorrectionSafety` are an absolute upper bound. Start with weight 0 in shadow tests; enable only after on-device evidence supports a particular scoring delta.

Suggested deterministic policy for candidates already returned by native decoder:

```text
if !eligibility.maySuggest or candidateSet.isEmpty: return native
preserve typed literal, native auto-correction verdict, valid-only/no-suggest guard
for each permitted non-primary candidate:
  bonus = min(0.08 * clamp(confirmedPersonalUse,0,3), 0.24)
        + min(0.04 * clamp(recentNativeContextMatches,0,2), 0.08)
  never cross an unsafe auto-commit threshold or invert a strong native winner
stable-sort only genuinely near-tied alternatives (score margin pre-registered)
return candidates with source/provenance unchanged
```

This is a proposed **normalized, bounded heuristic**, not an asserted native scoring scale. Native scores must be inspected and calibrated first; if they are not comparable across sources, use ordinal/tie-only logic rather than apply the example equation. Do **not** write duplicate history from raw typing: use existing native accepted-word and manual-pick hooks, prevent double counting on voice insertion, swipe completion, correction undo, cursor movement or composing restarts. Native UserHistory tracks typed usage over time; a voice-specific map is a separate explicitly confirmed record.

Examples to test: `Saket`, `Rohini`, `Noida`, `Ghaziabad`, `UPSC`, `NCERT`, Hinglish place/name spellings rendered in Latin script and legitimate uncommon surnames; never force these as corrections to acoustically unrelated words. No unsolicited dictionary changes from a static Indian-English lexicon; prebuilt reviewed `brrPersonal` entries remain only a candidate/validity supplement. Preserve user-selected locale, UK/US spelling and casing exactly.

## 4. Swipe intelligence — extend evidence, preserve current rescue

Keep `SwipeCorrectionLearner.suggest()` alternative-only, 64-entry bound, two **distinct explicit** corrections, geometry plausibility and equal-evidence ambiguity rejection. Add reliable `BrainEvents` linkage from a displayed candidate ID to a chosen candidate ID only within the same editor generation and active gesture. Require a genuine user selection to count `selected`; backspace/undo counts only when correlated to that committed candidate, not generic editing. Record coarse per-session `swipes`, `recallOffers`, `recallSelected`, `recallUndone`, `confirmedCorrections`, `ambiguousSuppressed`, `vaultStatus` without trails/tokens in metrics. Existing encrypted pairs remain separately reviewable and selectively erasable.

Do not start a CTC/CTC-decoder swipe replacement, add a new neural weight asset, or change the actual gesture path under this GO; that requires a new license, ABI/latency/accuracy evaluation and separate permission. The term **CTC** is a model decoding method, **not** an automatic "learn what I type" component. Swipe confidence gets better only to the degree measured in replay/real traces, not by a dashboard number.

## 5. Blue offline voice intelligence — current Parakeet 110M first

**Source seam:** existing `OfflineVoiceEngine.transcribe()` -> segment join -> `VoiceTranscriptFormatter.format()` -> `OfflineBoardDictation.finishCapture()` checks current editor -> `LatinIME.onPrivateVoiceTextInput()` (which intentionally avoids transcript training). Insert an explicitly new `VoiceLexicalAdapter` **after transcription and before the same-editor commit**; keep the pinned recognizer unchanged. An optional post-commit `VoiceCorrectionReceipt` lives only within a confirmed eligible editor and is erased on change/cancel/privacy switch. All transforms run off the UI thread unless a tiny display-only action.

### Phase V1 safe formatting (ship candidate)

- Preserve the current exact-token brand/acronym casing (`UPSC`, `NCERT`, `ISRO`, `OPPO`, `WhatsApp`, etc.), word count, punctuation, email/URL, digits and recognized language. Add only user-approved *exact orthographic* canonicalizations for Latin-script tokens, e.g. `saket` -> `Saket` when the user has saved that casing, without changing any phoneme or token boundaries. Honor per-locale canonical forms.
- The built-in personal dictionary and native user-history can propose a **visible optional** alternative for a matching token; *mere presence/frequency must not silently replace raw Parakeet output*. If ASR hears `socket` rather than `Saket`, do not assume the user's place name. Preserve mixed-language text and the current Devanagari/no-commit safety until separately tested; do not pretend 110M is a Hindi code-switch model.
- Maintain exact model-output provenance for test harness: `rawTranscript`, `formattedTranscript`, `userAcceptedTranscript` exist **only as transient test/active-editor values**, never on disk or analytics. Both baseline and post-pass outputs are assessed on the **same** recorded consented local test set.
- No unconditional punctuation insertion, phrase rewriting, homophone correction, profanity censoring or "smart" paraphrase without user control and measured evidence. A harmless uppercase normalization is different from an unverified lexical substitution.

### Phase V2 explicit voice correction learning (behind user-controlled toggle)

- After a blue voice commit into an eligible normal prose/chat editor, create a bounded **RAM-only** receipt: editor generation, cursor anchor, time (max **15 seconds**), up to **32 Latin-script token identifiers** in a temporary buffer for correlation. Prefer only the latest dictated token/word if precise span is known. **No raw audio** retained after decode; no persistent full phrase. Cancel on cursor move outside the span, selection across other text, another dictation, app/editor/locale switch, incognito, secure-app state, failed commit, or timeout.
- If a directly related subsequent manual edit **unambiguously** changes one dictated word, show a nonblocking "Remember `correctWord` for voice?" action. Merely pressing backspace, sending an SMS, copying from clipboard or changing unrelated text must not count. **Do not save until positive user confirmation** (and never inspect another app's document for this purpose). If Android editor APIs cannot prove correlation safely, disable passive detection and expose an explicit `Teach voice spelling` action instead.
- Persist at most **128** user-confirmed Latin-script `{observedToken, preferredToken, caseForm, locale, confirmations, lastApproved}` records, each 2–32 chars, bounded 16–32 KiB ciphertext, sanitized (no URL/username/password/number/phrase), expiration/recency prune, ambiguity tied => no auto-applied replacement. Store in `noBackupFilesDir` as e.g. `brr_voice_lexicon_v1.aesgcm`, authenticated domain/version header using AndroidKeystore AES-256-GCM via `BrainLearningVault` compatible extension/new isolated wrapper. `AtomicFile`, authenticated readback, bounded size, revision and serial writer; **never plaintext fallback**; if key unavailable keep session-only memory and report status. It must not mutate the preexisting swipe vault or native UserHistory.
- Approved casing may apply next time on **exact same normalized token**. For unequal observed/preferred spellings use a visible correction chip first, and require **at least two distinct confirmations** plus a held-out zero-harm test before considering any strictly matching auto-replacement; default release behavior is **suggest-only**. Make undo immediate, one-tap disable and selective single-pair deletion. Never accept learned changes from red brrSpeak as evidence of blue Parakeet acoustic performance.
- Typed personal words should be present in native suggestions immediately under native dictionary rules; they can populate the **candidate vocabulary** for visible voice alternative chips, but **not** create an automatic ASR mapping until the voice user confirms one. This precisely explains "learning from what I type" without falsely claiming the acoustic weights train themselves.

### Hotwords, beam search, VAD and future Ultra

The checked-in `OfflineRecognizerConfig` for v1.13.3 presently specifies no beam/hotword file; it uses sherpa-onnx defaults. Upstream sherpa-onnx supports `modified_beam_search` and hotwords for compatible transducer builds, but this **does not establish compatibility, stable ABI, or accuracy with this pinned Android NeMo TDT**. No implicit on-device dictionary-as-hotwords switch in this GO. Prototype **only in an isolated benchmark branch/test fixture** after verifying the exact pinned Java config, transducer/tokens and native behavior; promote only if North Indian English WER and memory/latency improve without regression. Do not upgrade sherpa here; the documented v2.0.2/TDT fix and Ultra migration are held. Likewise preserve existing lossless `VoiceSegmenter` pause windows; Silero VAD is **evaluation-only** and may be adopted later only with proven no-word-loss, extra memory/license/package audit. CTC, LM shallow fusion and contextual biasing are alternate decoding techniques, *not* required dependencies of the BR Brain data layer.

### No new model dependency — interface contract for later swap

```
VoiceRecognizerAdapter.decode(pcm16, generation) -> FinalTranscript(text, locale, source=BLUE_OFFLINE)
VoiceLexicalAdapter.propose(finalText, eligibleContext, approvedLexicon) ->
    DisplayTranscript(originalText, safeExactCaseText, optionalAlternativeChips)
VoiceOutcomeObserver.confirmChoice(receipt, explicitUserAction) -> aggregateEvent + optional encrypted word pair
```

There is no assumption that sherpa returns token confidence, n-best candidates, timestamps or probabilities. If they aren't exposed by the installed runtime, those fields **do not exist** and must not be fabricated. Later Ultra integration replaces only the recognizer adapter and optionally supplies validated n-best metadata; user-approved lexical data persists untouched.

## 6. BR Brain dashboard — exact design and navigation

**Display name everywhere:** `BR Brain` (capital B/R, space, capital B), never `PR Brain`, `brrBrain · Learning dashboard`, or a stale duplicate. Preserve the internal screen route constant `SettingsDestination.BrainDashboard = "brain_dashboard"`, existing `SwipeCorrectionLearner` store, prefs, accessibility/back behavior and Settings deep links. Change the Settings dictionary tile subtitle to `Personal words, dictionaries & BR Brain`; add a clearly labeled `BR Brain` destination if the new Settings IA allows it, without duplicate destinations. Respect the new pearl/midnight-sapphire Settings design and Material dynamic scaling, contrast, text zoom, TalkBack and touch targets.

### Intended screen (SPECIFICATION PREVIEW — not yet in APK)

```text
< Back                          BR Brain
Your private, on-device language intelligence

[ Local & private ] [ Learning: On / Paused ]
  Parakeet 110M · Blue voice offline · Ready/Unavailable (actual state)
  No voice audio saved · No cloud BR Brain sync

BR Brain activity                  (Refresh)
  [ Typing ]  [ Swipe ]  [ Voice ]  [ Emoji ]
  Show actual source, scope (this session / persisted) and time window.

  Typing   Native dictionary active · accepted manual words (if instrumented)
  Swipe    # attempts · # confirmed edits · # recall offered / chosen
  Voice    # blue dictations committed · # voice alternatives offered / chosen
           # explicit saved voice spellings; show "Not measured" until instrumented
  Emoji    # recorded selections · # distinct Emoji · reviewed favorites

Learning you can review
  [ Swipe word-pairs ]  [ Voice spellings ]  [ Personal dictionary link ]
  Per-entry: provenance, confirmations/ready label, Remove (explicit consent)
  Never show raw captured phrases/audio/keystroke history.

Accuracy & reliability
  Accuracy: Not measured [until a reference corpus exists]
  Offers and selections: shown with correct numerator/denominator
  Storage: Swipe vault verified | Voice vault verified/empty/unavailable

Privacy & control
  Learn from swipe corrections [existing toggle]
  Remember approved voice spellings [new toggle; default OFF pending consent]
  Personalized dictionaries [existing toggle and destructive-off consent]
  Erase swipe pairs [slide] · Erase voice pairs [slide] · Clear history [existing flow]
```

**Premium visual intent:** calm compact privacy hero, mode chips, quiet distinct metric tiles, truthful empty states and a separate clearly dangerous erase region. Use the existing Settings design tokens/glass/rounded surfaces rather than inventing an isolated mini-app, a bright toy dashboard, high-frequency animation or dashboard background polling. Dynamic height, scroll, 200% font, large controls and TalkBack labels. Screen launch must not initialize the 136-MB recognizer just to read model installation state.

### Exact dashboard data rules

| Evidence item | Valid numerator / denominator | Scope and source | Safe display before event instrumentation |
| --- | --- | --- | --- |
| Swipe attempts/corrections | Actual `recordSwipeAttempt`/explicit `recordCorrection` counts | Session; `SwipeCorrectionLearner.snapshot()` | Existing counts, labeled **this session** |
| Swipe recall **offered** | Actual `suggest()` non-null offered event count | Session | Existing count, explicitly **not accepted** |
| Swipe recall **chosen** | User picked an offer / that offer actually shown | Session, new event linkage | `Not measured yet` |
| Swipe stored pairs/ready | Actual valid encrypted map and `count >= 2` | Persistent store | Existing live counts & status |
| Voice commits | Successful blue same-editor text insertions / completed blue decode callbacks eligible for commit | Session, new source-specific event hook | `Not measured yet` |
| Voice corrections | Explicitly user-confirmed voice correction / eligible voice correction prompts | Session/approved encrypted map, distinct | `Not measured yet` |
| Emoji selection/favorites | Stored Emoji chosen frequencies / distinct entries | Existing SharedPrefs, lifetime of that store | Existing counts; **not AES-vault encrypted** |
| Typing saved vocabulary | Only words/accepted candidates exposed truthfully by native provider | Native storage; **do not fake total** | `Native learning active` or `Not measured yet` |
| "Suggestion acceptance" | Chosen counted offers / all eligible **actually displayed** offers | Domain + session; not all keyboard words | Only after robust event IDs |
| "Prediction accuracy" | Correct top-1 or top-3 / all labelled held-out reference decisions | External on-device consented benchmark; not passively inferred from keyboard telemetry | **Not measured yet** |
| Voice WER | (substitutions + deletions + insertions) / reference word count | Explicit labelled held-out test set, 110M baseline vs BR Brain output | **Not measured yet** in normal app |
| Latency | p50/p95 from true elapsed samples, environment+sample count | Debug/test; never private strings, no fake device p95 | No number unless measured |

Zero denominator shows `—`, never `0%` or `100%`. `Offers` must be actual strip-rendered offers, not merely internally generated options; distinguish decoded/completed/committed and canceled dictations. Session counters reset at IME restart and are not lifetime totals; selectively persisted counters require an explicit audited schema and privacy reason. `Learned` describes word-level approved adaptation, not model weight training. Report vault state separately per data owner; no blanket "fully encrypted keyboard" label. A Settings navigation destination cannot be used to bypass privacy toggles.

### Controls and deletion semantics

- The existing swipe erase slider still calls `SwipeCorrectionLearner.clear()` only. Deleting voice-approved entries only deletes the new voice encrypted store and corresponding in-RAM state, never swipes, native dictionaries, clipboard, installed model or Emoji by default.
- `Erase all BR Brain learning` should **not** exist initially: categories span different owners and may include irreversible native dictionary removal. If owner later requests it, define an explicit per-store checklist plus high-friction confirmation, idempotent partial-error recovery and precise descriptions.
- Editing/reviewing typed personal words navigates to existing `PersonalDictionariesScreen`, not a fabricated mirror. Disabling `PREF_KEY_USE_PERSONALIZED_DICTS` currently deletes native user history via `LatinIME.refreshPersonalizationDictionarySession()`; retain its existing slide-warning and **never** toggle it from an innocent dashboard overview card.
- On vault authentication failure: show `Unavailable — learning only this session`, preserve ciphertext bytes, no replacement plaintext and no destructive auto-reset; dashboard may show current-session counts separately. Review/list features are local and never enter crash reports/logs.

## 7. Implementation map — explicit target files (only after GO)

| Batch | Create/change | Exact integration and guard |
| --- | --- | --- |
| A — baseline & contracts | `latin/brain/BrainEligibility.kt`, `BrainCoordinator.kt`, `BrainEvents.kt`, `BrainSnapshotProvider.kt` (new); tests under `app/src/test/.../brain/` | Root privacy/editor-generation checks, stable snapshots and event ID correlation; attach zero-write feature flags default OFF and synthetic deterministic tests. No model, native decoder or persisted schema rewrite. |
| B — dashboard rename/shell | `settings/screens/BrainDashboardScreen.kt`, `MainSettingsScreen.kt`, if appropriate `SettingsNavHost.kt` display-only wording; localized `res/values/strings.xml` and existing translations | Exact `BR Brain` title and Settings link; keep route constant unchanged and compile-safe imports/resources. Show current actual swipe/Emoji stats and explicit `Not measured` Voice/Text slots until B/C events exist; preserve erase slider. UI tests/screenshot review for sapphire/pearl/dark/large-font/TalkBack. |
| C — text/swipe event attribution | `latin/Suggest.kt`, `latin/InputLogic.java`, `latin/LatinIME.java`, `latin/brain/SwipeCorrectionLearner.java`, optionally `DictionaryFacilitatorImpl.kt` and `NgramContext.java`; new `BrainTextRanker.kt` | Attach only existing source suggestion IDs/manual pick/undo hooks; no extra native dictionary; add true offered/selected/undo attribution. Shadow score before production reranking. Keep caches/locale/editor and privacy invariants. |
| D — voice formatting & approval | `latin/voice/VoiceTranscriptFormatter.kt`, `OfflineVoiceEngine.kt` result handoff, `latin/OfflineBoardDictation.kt`, `LatinIME.java` + possibly `InputLogic.java`; new `VoiceLexicalAdapter.kt`, `VoiceCorrectionReceipt.kt`, `VoiceLearningRepository.kt` | Decode still pinned Parakeet110M/v1.13.3. Same-editor and protected-field check **before** lexical access/learning. Canonical casing then visible offers and opt-in explicit correction approval; no raw transcript write. Any post-pass async callback must revalidate editor generation and original connection. |
| E — vault & settings | Extend `brain/BrainLearningVault.java` compatibly or add independent authenticated voice vault; `brain/VoiceLearningRepository` and UI, relevant `Settings`/`Defaults` pref; no change to `brr_swipe_corrections_v2.aesgcm` format | AES-GCM domain/version, noBackup AtomicFile, bounded data, authenticated readback, no plaintext fallback, targeted deletion. Default new voice-learning switch OFF until onboarding consent; separate existing prefs and no destructive side effects. |
| F — Emoji evidence | Existing `Suggest.kt`, `EmojiContextScorer`/CLDR ranking, `BrainEvents` | Reuse eligibility and reviewed candidates, add displayed/selected counter correlation if reliable; no duplicate Emoji dictionary or cross-editor context leaks. |
| G — QA, release, handoff | `app/src/test/...`, existing test harnesses, (if accepted) `tools/` benchmark fixtures, docs root/index | Fail-closed policy regression and private-content non-retention tests, exact real-Oppo measurement, Android debug+unit and original-signed release **only once explicitly approved**; record exact SHA, version, signer hash, result and physical acceptance separately. |

These names are proposed, **not existing committed classes**. Confirm current Java/Kotlin signature, Gradle/Test APIs, package paths and code owners before writing them. Batches A–G form one owner-approved BR Brain development goal, but deployable milestones must be independently verified and must not partially ship a feature displaying fabricated learning.

## 8. Deterministic test, benchmark and privacy gates

### Pure/JVM instrumentation and negative cases (required)

1. **Eligibility matrix:** ordinary text/chat yes; password and visible password, PIN, email, URL, code, `IME_FLAG_NO_PERSONALIZED_LEARNING`, incognito, secure app, unknown editor, denied personalization **no persistent learning**. Verify persisted bytes unchanged and immediate receipt wipe across editor/restart/IME hide.
2. **Native candidate invariants:** user's literal text always accessible, safe auto-correction veto, sensitive identifiers/paths/URLs/handles/exact case untouched, banned/no-suggest valid-only not proactively promoted; no candidate duplication; after-Space next-word/Emoji remains; no stale LRU after dictionary, language or privacy reload.
3. **Swipe:** two explicit confirmed corrections before offer; equal evidence skip; corrupt/invalid keys skip; actual strip displayed vs merely generated vs chosen vs undo counters; gesture coordinates never serialized; old vault bytes remain readable.
4. **Voice:** single/multi 11–15s windows, punctuation/digit/URL fidelity, names/casing, long speech, cancel while decoding, reused recognizer unload, PCM array wiped, red/blue provider separation, Devanagari current guard, no text insertion in changed editor. Correction receipt from old editor or after 15s **must not** save. No automatic spelling-changing guess from typed dictionary; first explicit-approved exact case may apply, nontrivial map suggest-only.
5. **Vault:** fresh create, serial revision supersession, concurrent clear/flush, corrupt/tampered/wrong domain/wrong AndroidKeyStore key, device lock/key failure, size bound, authenticated round-trip, `AtomicFile` rollback, exact per-domain erase, no plaintext, no stale learning resurrection. Preserve existing swipe encryption and native dictionary on dashboard upgrade/uninstall-safe app update.
6. **Dashboard:** true snapshot provenance, explicit `Not measured`, zero denominators, session reboot resets, actual installed 110M status without model loading, real per-mode data absence, privacy state, working refresh, per-domain slide deletion, TalkBack/contrast/font scaling and no crash in empty vault.
7. **Security static checks:** no `INTERNET`/`ACCESS_NETWORK_STATE` permission added, no remote model/lexicon fetch, no external telemetry/crash payload of content, no PCM/transcript/keystroke/clipboard logger, no external storage/backup of voice learning, no unencrypted fallback. Verify microphone permission remains existing-only and no other app/IPC in blue path.

### Held-out North Indian spoken English evaluation: **do not claim a winner without this**

- Consent-based **locally processed** test set target: at least **400** short and long English utterances from **20+** speakers with Delhi/UP/Haryana/Punjab/Rajasthan-relevant accent variability; balanced female/male and natural speech where feasible; indoor quiet and common room/road/ceiling-fan noise, slow and fast, proper nouns, numbers, Indian place/institution names, casual grammar, complete sentence punctuation. Separate **speaker-disjoint** calibration vs held-out test; do not tune on the final test. Written references authored by consented speakers. No uploading the users' actual messages or private recordings, no unconsented background collection. If insufficient speakers, report *exploratory*, not final accuracy.
- Replay **identical, locally consented** PCM through (1) raw current Parakeet 110M, (2) existing exact-casing formatter, (3) BR Brain proposed exact-casing and optional consented lexical rules. Keep dictated content and speaker IDs out of GitHub. Compute case-insensitive WER and CER for acoustic recognition, plus case-sensitive exact spelling, named-entity correctness, number/URL fidelity and false-replacement count. Separate `raw`, `post-pass`, and `user-confirmed` outcomes; do not confuse human edits with ASR accuracy.
- **Promotion guard:** default no worse than baseline by **0.5 absolute WER percentage points** overall and **1 point** in any sufficiently sampled accent/noise subgroup (thresholds are proposed acceptance gates, not current results); **zero unintended replacement** of protected tokens in negative corpus; useful relative WER/identifier precision improvement on the held-out set to turn on optional substitutions. If no measurable improvement, ship only case normalization + transparent manual chip and leave scorer shadow/disabled. Use per-speaker error tables (nonidentifying aggregates), not a single flattering sample.
- **Live Oppo budget targets (to verify, not claimed):** text/swipe incremental ranker p95 <= **2 ms** off native suggestion critical path, voice lexical pass p95 <= **15 ms** incremental (excluding ASR), incremental steady RAM <= **2 MiB**, no extra native model and no visible keyboard frame regression. Record cold/warm memory, battery, 16-kHz end-to-end decode time, cancellation and 15–30s speech, IME animation/scroll. If not met, disable optional layer. Never run profiling that persists private user text.
- **Typing/swipe quality gates:** replay an authored Indian-English word/name/identifier set with native results recorded from the *real decoder*, not synthetic-only fixtures; compare top-1, top-3, accidental auto-corrections, manual accept rates and after-space predictions. A replay harness with synthetic fake candidate arrays verifies control logic only, not language quality. Stratify common vs uncommon words; never sacrifice stable everyday top1 for names. Any regression => turn off optional reranker.

### Release and real-world acceptance

After explicit GO, for each candidate deployment: fetch current `main`; guard expected parent; run locally available pure tests/static contracts; write app source with smallest meaningful batches; run **both** full Android debug/unit and original-signer Obtainium workflows on the **exact final app-source SHA**, wait terminal, verify signed package `com.mangoloads.board`, certificate continuity, APK name/hash and release link. A doc-only commit or green workflow for another SHA is not a signed install. Ask owner to install from the original trusted signed Obtainium path and report actual Oppo behavior before marking real-world acceptance. Do not purge the installed 110M; this BR Brain upgrade reuses it.

## 9. Proposed owner demo after BR Brain GO (what "see it in effect" means)

1. Open Settings → Dictionary & learning → **BR Brain**. Verify native sessions and previous saved swipe/Emoji records appear unchanged. `Voice` initially shows actual ready state for Parakeet 110M and either `Not measured` or valid session events; never an imaginary 99%.
2. Type an Indian name/term twice and observe native Suggestions/history handling; show that a URL, UPI/email/handle or password is not silently changed and no protected-field learning increments. Trace no raw input in logs.
3. Make a swipe mistake, explicitly correct it on two eligible separate attempts, then observe the learned alternative on subsequent similar swipe. In BR Brain, show `offered` vs `chosen` differ unless chosen; erase only swipe pairs through slide; retain voice/native dictionary.
4. Use blue mic and dictate “I went to Saket yesterday” or “UPSC NCERT OPPO” (speaker-provided reference). Compare raw vs formatted local test output; demonstrate **exact casing** without pretending ASR is trained. Manually approve a **single voice spelling** via a visible chip. Next same exact token uses approved case; mismatched words remain untouched by default.
5. Switch to incognito, password, flagged no-learning editor, another app/editor, or cancel dictation. Confirm no new persistent pairs and no stale transcript insertion after editor switch. Back in BR Brain, sensitive content never appears as history.
6. Later, with a **separate Ultra GO**, swap only `VoiceRecognizerAdapter`, measure fresh same-corpus WER and activate safe migration. The BR Brain dashboard/learning stays intact and the old model is purged **only after Ultra READY**, under the separate full-APK plan.

## 10. Boundaries, risks and decision record

- **Accuracy risk:** an ASR transcript string is not a probability distribution. A dictionary word appearing somewhere does not prove it was spoken. Every silent lexical substitution risks worse Indian English recognition; default to approved casing plus visible alternatives.
- **Privacy risk:** passive post-voice edit inference can accidentally learn unrelated app content. Require a precise same-session receipt + positive approval or no passive inference. Distinguish *on-device* from *encrypted*: swipe vault is encrypted, Emoji usage prefs and native history have their own persistence. Never advertise zero OS/network capability as an absolute confidentiality guarantee; blue path has no app network permission, red brrSpeak is separate.
- **Performance risk:** the recognizer is large and running under IME lifecycle. Do not synchronously launch recognizer from dashboard/IME rendering or make a dictionary lookup during a held UI frame. Keep bounded, cached and cancellable work.
- **Release risk:** parallel agents commit to `main`. Use fresh-head lease; preserve unrelated concurrent changes and never force-push, change signer, app ID, permissions, assets or distribution for this phase. A docs commit is not an Android update.
- **Deferred scope:** Ultra model, sherpa-onnx upgrade, first-APK 0.6B bundling, model purge, new VAD/beam/hotword deployment, CTC gesture/neural decoder, server sync, LLM rewriting, automatic app profiling, unsupervised audio training and new broad data export/backup.

**NEXT ACTION: WAIT.** No implementation, tests against an altered APK, model install/removal or signed release until the owner explicitly says **GO for BR Brain intelligence/dashboard**. Then reread this plan against live main, implement and empirically validate each batch. The separate Ultra model migration remains on hold until its own later GO.

## 11. Primary references for implementation checks

- Android `EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING`: https://developer.android.com/reference/android/view/inputmethod/EditorInfo#IME_FLAG_NO_PERSONALIZED_LEARNING
- Android `UserDictionary.Words`: https://developer.android.com/reference/android/provider/UserDictionary.Words
- sherpa-onnx transducer decoding/hotwords **upstream capability**, not verified on pinned brrBoard TDT runtime: https://github.com/k2-fsa/sherpa-onnx/blob/master/sherpa-onnx/python/sherpa_onnx/offline_recognizer.py
- Internal design and evidence: [Eight-stage intelligence/privacy](EIGHT_STAGE_INTELLIGENCE_PRIVACY_2026_11_20.md), [One dictionary/Emoji pipeline](SMART_DICTIONARY_UNIFICATION_2026_11_18.md), [Existing brrBrain dashboard history](BRRBRAIN_LEARNING_DASHBOARD_2026_11_19.md), [Encrypted learning vault](BRRBRAIN_SECURE_LEARNING_VAULT_2026_11_19.md), [B16B n-gram evaluation caveats](B16B_NGRAM_EVALUATION.md), [Ultra separate migration plan](ULTRA_OFFLINE_VOICE_INTEGRATION_AND_MIGRATION_PLAN_2026_11_20.md).

## 12. 20 November 2026 online research upgrade — ONE sentence-aware LM before swapping swipe

**Owner follow-up:** Research better offline keyboard-intelligence engines across sources and ask whether a *really smart next-word predictor that knows the sentence* would help tap and swipe more than replacing the gesture engine. **Answer: yes, conditionally; a small neural contextual language model is the highest-leverage first experiment.** We have **not** proven that the current swipe decoder is deficient. This revises the original heuristic-only recommendation above: keep the native fast path and add a genuinely trained, **optional** sentence-aware model for *next-word generation and candidate reranking*, after measuring actual candidate recall. It remains **PLAN ONLY**, with no new model artifact permitted until BR Brain GO and license/benchmark clearance. The larger Parakeet Ultra **voice** model is still held separately.

### Grounded code diagnosis

- `Suggest.kt:getSuggestedWordsForBatchInput()` calls native `mDictionaryFacilitator.getSuggestionResults(... SESSION_ID_GESTURE ...)`, then `preferNextWordSuggestion()` using the native `getNextWordSuggestions()` result. That function currently requires personalized dictionary setting ON, main-dictionary top word, at least two swipe candidates, next-word score **>=170**, and an existing candidate within roughly **93%** of the top swipe score before it can promote a contextual word. It cannot invent a missing word and has no long-sentence semantic understanding. This is direct code evidence, **not** a device quality metric.
- `Suggest.kt:getNextWordSuggestions()` queries the **native dictionary's empty-word** n-gram suggestions, caching by locale/layout/input style/context/privacy flags. `NgramContext.java` holds bounded previous words. Existing user history feeds the native path, but this is not a full-sentence neural predictor. It also cannot learn every proper noun just because it appeared once in an ASR transcript.
- Classify every **consented labelled** swipe trace *before selecting a decoder*: **A — target absent from native top-8** = gesture/lexicon candidate-recall problem; **B — target in top-8 but not top-1** = ranking/context problem; **C — target correctly top-1 but changed after insertion** = insertion/undo/editor bug; **D — next-word empty/weak after Space** = language prediction/cache/dictionary problem. A and B are separable. Add purely aggregate counters to the test harness, not private user traces in the app/dashboard.

### Why this is a research-backed architecture

1. Google's *Neural Search Space in Gboard Decoder* (EMNLP Industry 2024) describes the same compositional issue: touch/lexicon candidates plus an n-gram language model with too-short context. Their neural LM improved Words Modified Ratio by a reported **0.26–1.19%** across tested locales, but decoding latency increased; the research does **not** give a brrBoard/Oppo or Indian English benchmark. Primary paper: https://aclanthology.org/2024.emnlp-industry.93/ .
2. FUTO Keyboard ships a **hybrid AOSP dictionary + English ~36-million-parameter Llama-architecture contextual LM** for word prediction and autocorrection (model described as experimental, QWERTY/English, potentially slow on lower-end devices). Its source is under **FUTO Source First**, not a blanket GPL permission to copy its keyboard/transformer engine into brrBoard. Its own docs say local LoRA finetuning is **disabled by default** due unproven effectiveness; saved typing text/fine-tuned weights introduce significant privacy risks. Study the **architecture**, do not copy protected model/code. Primary: https://gitlab.futo.org/keyboard/keyboard-wiki/-/wikis/Keyboard-LM-docs ; https://docs.keyboard.futo.tech/settings/textprediction ; https://docs.keyboard.futo.tech/troubleshooting/faq ; https://github.com/futo-org/android-keyboard/blob/master/README.md .
3. FUTO separately released **FUTO Swipe**, a GPLv3 C++/ExecuTorch decoder with a trained **~635k universal encoder + ~304k QWERTY-English decoder + ~1.5M English causal DFSMN ContextLM**, the latter **~6.25 MB FP32** and capable of *next-word prediction and candidate reranking*. Source/library: https://gitlab.futo.org/keyboard/swipe-library ; model card: https://huggingface.co/futo-org/futo-swipe . The model weights use **FUTO Model Weights License 1.0**, including **visible product attribution**, and require a precise supply-chain/license/redistribution review; there is no unconditional authorization to bundle them. `hungry_jellyfish` is the established published ContextLM; `chosen_planarian` is a November 2026 successor candidate in the official model repository, not yet characterized on our keyboard. Reuse **one** compatible ContextLM for both text and gesture only if its vocabulary/API/scoring and license support both use cases; **do not run two independent LMs in parallel in production**.
4. On FUTO's own 20k-swipe English/QWERTY emulator subset, FUTO Swipe reports **7.38% top-1 error** versus **13.12% for HeliBoard + Google library**, and **4.19% top-4 error** versus **7.63%**. This is **developer-published**, not a direct brrBoard/Indian-English device comparison. It makes FUTO Swipe a strong *candidate if and only if* our failure category A is significant, not an automatic replacement. Primary: https://github.com/futo-org/android-keyboard/releases/tag/v0.1.29 ; research: https://arxiv.org/abs/2606.25247 ; model weights licensing: https://huggingface.co/futo-org/futo-swipe/blob/main/LICENSE.md .
5. Peer-reviewed Samsung research *On-Device Neural Language Model Based Word Prediction* demonstrated a **7.40 MB** compressed RNN-LM with **6.47 ms** average prediction on an older Samsung high-end handset; useful feasibility evidence, not a ready-to-integrate public model or guaranteed Oppo result. Primary: https://aclanthology.org/C18-2028/ . FlorisBoard's Apache-2.0 NLP core is still alpha and documents high trie RAM and immature ranking, so changing the entire native text dictionary to it is unjustified: https://github.com/florisboard/nlp .

### Frozen recommended neural-context implementation after GO

**One hybrid, not one replacement:** `native AOSP/HeliBoard touch + lexicon` **plus** `BR Brain Neural ContextLM` **plus** `confirmed personal-word memory`. All three feed a **single arbiter**. Keep `VoiceRecognizerAdapter` (Parakeet 110M) separate. `Neural ContextLM` is an abstract interface, not a commitment to ship FUTO binaries without legal and device review.

```text
Eligible editor's committed words (RAM-only, last 12–24, no host scan)
                 -> context model (off UI thread; recompute after word boundary)
                 -> top next-word predictions + scores for candidates
                                        |
Native typed/swipe candidates --------> gated fusion (no blind new auto-commits)
                                        |
                             existing suggestion strip/gesture flow
```

*Context capture:* maximum **24 committed tokens / 160 UTF-16 units** from this IME's own actual accepted output only, excluding URLs, passwords, app IDs and clipboard. Clear on editor/focus change, cursor jump, manual selection/paste, locale/layout or privacy switch, incognito, no-learning and IME hide. Use immutable generation-stamped snapshots; never read an entire host-editor sentence on a timer or persist raw context. 24 words is a memory **cap**, not a guarantee the chosen LM effectively attends to all of them; benchmark its true usable receptive field.

*Model selection gate:* First benchmark one **tiny 1.5M-class DFSMN/causal word-level ContextLM**, with the FUTO published model as a candidate **only if licensing and Android ExecuTorch dependencies are cleared**; then test a compatible self-trained/permissively distributable larger neural LM in shadow mode **only if** next-word recall and real context disambiguation remain inadequate. The 36M FUTO Transformer is an architectural reference, not bundled. Freeze a concrete model ID, authoritative upstream revision, asset hashes, licenses, vocabulary/normalization, memory and exact Android-runtime version **after** a pre-implementation offline bake-off. Do not invent the model choice, speed or training rights.

*Runtime:* Native next-word suggestions render instantly as before. ContextLM updates **only on completed word / swipe gesture**, never one expensive inference per touch/key; background single-worker, cache on `(editorGeneration, locale, committedContext, dictionaryRevision, privacyFlags)`, exact generation check on arrival. If context scoring misses the UI deadline or device pressure, skip this update, show native suggestions without flicker. No recognizer launch, no GPU context in the keyboard frame. Model load is lazy, bounded and cancellable. Prefer incremental state for long-context computations if model supports it. Set test target incremental p95 <= **15 ms** on Oppo (not claimed), cap resident LM memory only after measurement, and use thermal/battery and cold-start checks. If a model cannot meet this, **do not ship it**.

*Fusion:* Keep top-1 native candidate unless the neural model assigns high contextual probability to an **already valid native candidate** with a pre-registered safe score margin; no auto-commit after marginal disagreement. The neural LM may add genuinely predicted next words from its *verified English vocabulary* after Space, but manual chips must be visibly distinct from the native auto-correct path and honor dictionary bans and personally approved words. If a target is not in the swipe decoder's top-8, the separate FUTO Swipe decoder study is triggered; a pure reranker cannot fix absent candidates. Avoid mixing incomparable raw model logits and native scores; calibrate via held-out paired traces with normalized probability or learned rank fusion and make confidence failures fall back.

*Learning:* Native UserHistory/Android dictionary and explicitly approved `BR Brain` correction pairs update a small local word/pair preference overlay immediately. No private-text corpus or local LoRA weight training. This is enough to prefer habitual Indian-English words without the training-data extraction privacy risk noted in FUTO docs. Learning affects candidate ranking and context scoring only, **not** acoustic Parakeet110M weights.

*Evaluation in a single decision sheet:* labelled source-native swipe top-1/top-4/top-8 candidate recall; share of failures A vs B; native-only versus neural rerank top-1/top-3; next-word recall@3, keystrokes saved and false auto-correct/undo rate; Indian place/surname/acronym precision; long-context contrasts (`I missed the morning ___` vs `I need to use my ___`) and context-independent proper names; p50/p95, CPU, battery, RAM, hot/cold load, privacy zero-retention. All scores come from real candidates and consented test text, never synthetic-only data or dashboard offers count. Decision: **ship the tiny context layer first if it improves B/D and meets latency; replace swipe decoder only if A remains substantial**. Dashboard BR Brain can show genuine user-chosen suggestions; no invented predictor accuracy.

**WAIT FOR GO.** This online research is a documentation-level architecture refinement. No neural LM, FUTO weights, ExecuTorch, model downloader, Parakeet change, old-model purge, APK build or signed update has been implemented in this research batch.

## 13. OWNER GO — shared English/Hinglish intelligence and Smart Emoji integration (20 Nov 2026)

**Authorization change:** The owner now explicitly said **GO** to create BR Brain and to route Emoji choices into **all three** UI consumers: (1) text-suggestion strip, (2) Smart/Recent Emoji at the Emoji page's opening category, and (3) the blank Emoji Search row. The new engine must learn choices over time with different apps and editor fields, never blindly infer from popularity. This GO **does not** unfreeze Parakeet Ultra / replace Parakeet 110M, does not authorize cloud inference or collecting private text, and does not waive exact-SHA tests/original signer/Oppo acceptance. Prior plan-only line is superseded for BR Brain code but remains historically true for earlier batches.

### 13.1 Evidence-backed model decision: separate trained ContextLM from the shipping heuristic learner

The target next-word neural model remains **BR ContextLM-Hinglish v1**, approximately **1.5M-class tiny causal DFSMN with a dedicated English + Romanized Hindi vocabulary**, intended to share bounded context between text next-word and native swipe-candidate reranking. It is a **target to train**, not an existing pretrained file and not already integrated. FUTO's 1.5M English single-language ContextLM provides proven compact architectural feasibility, *not* a ready-made Hinglish model or guaranteed brrBoard performance. FUTO's QWERTY-English swipe error numbers are developer-only; Reddit reports praise the newer English swipe while still reporting contractions, casing and non-English problems. The FUTO Model Weights License requires visible “powered by FUTO Swipe” attribution if its weights/derivatives are used. **No FUTO code/weights or other unlicensed weights are bundled in this batch.**

Rejected as drop-in: **Keyboard-SLM** (genuine tiny C++ interpolated n-gram with Hinglish personalization, but 2026-11-06 early alpha, zero stars/releases, no root license file; its ~95µs/40–48% self-reported are not independent Android/Hinglish benchmarks); **IndicSwipe v2** (MIT, 41 stars but no releases and is primarily a gesture decoder, not next-word brain); **Lexo** (small 500+ installs/19 Google Play ratings, premium proprietary app with much larger named open weights; not an importable validated model); **L3Cube-HingCorpus/HingGPT** (large real Romanized Hinglish corpus but CC BY-NC-SA 4.0; non-commercial/no unreviewed production weight transfer); **Qwen 0.6B/1.5B+**, too large for per-word keyboard work on the Oppo; and standard FUTO ContextLM English weights alone, which lack Roman Hindi coverage.

**Data rights for separate neural training gate:** COMI-LINGUA (peer-reviewed EMNLP 2025, Hindi-English code-mixed Roman/Devanagari, CC BY 4.0) + human-created HinGE (CC BY 4.0) + only verifiably distributable English conversational text with preserved attribution and source registry. Split by source/speaker/thread before training; de-duplicate, evaluate Romanized Hindi spelling variants, Indian names, code switches, false autocorrect and 100% offline behavior. Never import Twitter/HingCorpus NC data, privately typed text, clipboard history, WhatsApp exports or Parakeet transcripts for model training. Do not equate code-mixed corpus benchmark size with keyboard NWP examples. Freeze weights hash, vocab, license inventory and model-specific Android CPU/ORT compatibility **only after** a reproducible trained file and paired Oppo tests exist. Quantized model budget **target** <=8 MiB weights, prediction p95 <=15ms at committed-word boundaries, resident RAM/CPU/thermal measured rather than asserted. There is no trained/validated file today, so this substep remains **PENDING** even though the engine GO has been given.

### 13.2 GO batch 1 — implemented Emoji-adaptive core and exact consumers

New files:

- `app/src/main/java/helium314/keyboard/keyboard/emoji/BrrEmojiPatternModel.java`: bounded (144 associations / 45-day age), deterministic word-suffix (last one to three normalized tokens, up to 160 preceding chars), SHA-256 phrase fingerprints, confidence decay and separate global vs per-app/per-field hashed scopes. Does not serialize sentences, package names, raw field IDs, keyboard audio or clipboard. Has JUnit tests of Romanized Hindi/English, app/field separation, late expiry, malformed records, boundedness and round trip.
- `app/src/main/java/helium314/keyboard/keyboard/emoji/BrrSmartEmojiEngine.java`: **one** shared offline ranking API merging reviewed `EmojiContextScorer` cues + confirmed locally learned Emoji choices + existing adaptive Recents; minimum confidence for suggestion-strip insertion; one background writer, private no-backup `AtomicFile` sealed with existing `BrainLearningVault` AndroidKeystore AES-256-GCM; no plaintext fallback, no model/network download. `forgetLearnedPatterns` erases contextual learning while retaining normal Recents and counts.
- `EmojiPalettesView`: selected Emoji updates existing use counters and the new learner; the Smart row and Recents top ordering use one shared ranker; empty Emoji Search immediately shows the same personal/contextual picks plus safe defaults, then optional background CLDR semantic enrichment. Preserve existing category/scroll session logic, Search keyboard, row heights, tint/skin families and selection path.
- `Suggest.kt` after-Space suggestions and `SuggestionStripView.kt` existing decoder Emoji-side candidates call that same ranker. They never use learned Emoji as text autocorrect or an auto-commit, and existing two text slots remain reserved. `LatinIME` warms encrypted data off the key path.
- `BrainDashboardScreen.kt`: rename user-facing title to **BR Brain**, show measured association count and provide a slide-confirmed **Erase Smart Emoji patterns** action. This erases only the new contextual pattern file, not existing Recents, normal dictionary, correction patterns or Parakeet.

**Privacy/security constraints:** All learning/ranking is gated by `EmojiContextEligibility`: reject incognito, `IME_FLAG_NO_PERSONALIZED_LEARNING`, passwords, no-suggestions, email/URL/code/non-prose/unknown variations. A host editor snippet is requested **only at Emoji selection** or explicit Emoji page/Search entry, not on each keypress; normal strip gets its already-generated n-gram hint, with no new host Binder read. A transient max-160 UTF-16 suffix reaches the bounded worker, then only hashed suffix associations encrypted at rest, never exported. Fingerprints are pseudonymous, *not guaranteed anonymous*; encryption, TTL, retention bound, erase UI and no backup are mandatory. The existing separate explicit undelivered-voice Clipboard recovery rail remains unrelated: do not feed it into BR Brain. Reset rendering context on editor generation/secure-field changes, never show personalized candidates in protected editors. Any IO/crypto fault must fall back to reviewed rules/standard Recents without text logging or crash.

**Correctness contract:** The same ordered recommendations should be observed in (a) Emoji Smart/Recent, (b) empty Emoji Search and (c) live strip when eligible and semantically relevant. Ineligible editors show no personal suggestions, but ordinary non-personalized Emoji search can still use defaults. After Space, at most one Emoji manual chip is added *after* the strongest two text predictions; while composing, the ranker only reorders decoder-supplied Emoji chips. Never silently insert an Emoji or replace a dictated/swiped word. Varying app and field hashes change ranking after repeated local selection, with global fallback. Other GUI bugs (gap, page layout, animation, Oppo geometry) must not be regressed.

**Tests required before calling shipped:** `BrrEmojiPatternModelTest` and all existing Emoji/Search/behavior tests; exact-commit Android debug and Obtainium Actions status; R8/minSdk/runtime compatibility; APK sign/certificate, package `com.mangoloads.board`, stored 110M voice model and upgrade continuity unchanged; physical Oppo Reno10 5G three-surface/real Hinglish scenario, app/field switches, private-field no-learn, no-network/airplane, memory/latency and save+erase tests. Developer observation is **not** a measured 90%+ accuracy claim. A successful GitHub commit is not a successful device install.

### 13.3 Follow-on GO batch (not falsely marked complete)

To make the **whole typing engine** genuinely sentence-smart, next implement/trial the trained BR ContextLM-Hinglish v1 in shadow mode against existing native next-word and swipe paths; only promote if Hindi-English recall@3, swipe top-1 conditional on top-8 availability, false correction rate and Oppo p95/thermal all beat native without regressions. Formal benchmarks/consented samples are still missing. The current GO allows engineering this path, **not** shipping guessed weights or stripping Parakeet. Any implementation must preserve instruction and release handoffs after each material batch.

**Research registry (checked 20 Nov 2026):** https://swipe.futo.tech/ ; https://huggingface.co/futo-org/futo-swipe/blob/main/LICENSE.md ; https://github.com/futo-org/android-keyboard/releases/tag/v0.1.29 ; https://www.reddit.com/r/Android/comments/1ue0fzq/futo_keyboard_swipe_update/ ; https://github.com/vikaskolanu/Keyboard-SLM ; https://github.com/AI4Bharat/Indic-Swipe-v2 ; https://huggingface.co/datasets/LingoIITGN/COMI-LINGUA ; https://aclanthology.org/2025.findings-emnlp.422/ ; https://huggingface.co/datasets/LingoIITGN/HinGE ; https://github.com/l3cube-pune/code-mixed-nlp ; https://onnxruntime.ai/docs/tutorials/mobile/ .

### 13.4 Second source batch: no invisible learning holes, privacy switch and correct menu label

A selected Emoji in the typing strip can be accepted before the Emoji palette view has ever been constructed. In `LatinIME.pickSuggestionManually`, call the shared `BrrSmartEmojiEngine.learn` in that **panel-null branch**, exactly once after insertion, while the existing panel-present path continues through `EmojiPalettesView.addRecentEmoji`. Keep the original adaptive frequency and actual Recents intact; no duplicated event when the palette exists. This is especially important to learn from contextual Emoji chips, not only deliberate Emoji-page taps.

Honor the user's `PREF_KEY_USE_PERSONALIZED_DICTS` toggle: when OFF, **do not learn or surface saved personal app/field patterns**, but allow nonpersonal reviewed phrases; no cloud fallthrough. A blank Search still has generic defaults. Include a live stat and confirmed erase in BR Brain. Rename both the Settings navigation title `brr_brain_page_title`, its summary and the breadcrumb label, not just the dashboard composable. Strengthen the pure-Java bound test with distinct alphabetic contexts. These changes are **small implementation refinements, not proof that a neural bilingual text model has been trained**. User-facing signed release and Oppo acceptance remain explicit separate gates.
