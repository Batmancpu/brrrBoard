# 2026-11-18 — One text dictionary pipeline, one Emoji knowledge base

## Owner's decision

The next smart-keyboard batch must be two coherent capabilities rather than
unrelated add-ons:

1. **Text:** better autocorrect, next-word predictions, locale handling and
   dictionary freshness are one native `Suggest`/`DictionaryFacilitator`
   pipeline. No separate AI model, hidden learning store, cloud request or
   second candidate generator.
2. **Emoji:** Emoji Search, contextual Smart picks and eligible Emoji in the
   suggestion strip share the existing pinned CLDR knowledge plus one
   **reviewed** phrase rule table. There is no new Emoji asset, no separate
   synonym database, and no scraping of Clipboard contents for suggestions.

This small integration is deliberately not a claim that the native language
model or glide decoder has been made measurably more accurate. Their scoring
and binary dictionary payloads are unchanged. Benchmarking actual decoder
output on held-out authored fixtures is still required before rank/frequency
tuning.

## Text changes

- `SmartCorrectionSafety.kt` guards the final existing decoder correction
  decision for strongly structured intentional text: email/URL-like tokens,
  handles, path separators, source identifiers, internal uppercase or dotted
  versions. This only blocks silent replacement; the native suggestions remain
  available for manual selection. Existing user auto-correction settings,
  BrrPersonal valid-only terms, typed-word choice, Space and Backspace/undo
  contracts remain authoritative.
- `Suggest.kt` keeps the same candidate retrieval and next-word cache. The
  output flag is gated by the conservative policy and existing native result.
- `LatinIME.onUpdateMainDictionaryAvailability` and `resetSuggestMainDict`
  invalidate cached next-word results when the underlying main dictionary
  becomes ready/reloads, avoiding pre-load candidates outliving that transition.
  No per-key cache invalidation or UI-thread blocking was added.

## Emoji changes

- `EmojiContextScorer.java` still holds the *only* reviewed English/Hinglish
  typing-trigger table. Its new bounded search projection exposes those same
  Emoji in an explicitly requested Search. Typing continues to demand the
  existing stricter score and will only rerank Emoji **already emitted by the
  decoder**, never synthesize a new text-strip replacement.
- `EmojiDescribeIndex.java` retains the pinned, lazy CLDR index and existing
  bounded inverted-token/fuzzy logic. Search merges exact reviewed contextual
  rules with the CLDR output and verifies each sequence is present in the
  existing Emoji corpus. Explicit user-defined Search aliases retain priority
  in `EmojiDescribeEngine`. No extra index is built, and the identical CLDR
  blob is reused.
- `Suggest.kt` attaches a bounded, in-memory previous-token suffix *only if*
  more than one existing Emoji candidate is present and the current settings
  are non-incognito. It uses already-available `NgramContext`, not an editor
  Binder/surrounding-text query. `SuggestedWords` carries the hint through to
  `SuggestionStripView.kt`; the existing
  `SuggestionStripLayoutHelper.prioritizeEmojiSideSuggestions` preserves it
  when it copies candidates. The typed/autocorrect center slot and all
  non-Emoji candidates remain untouched.

## Privacy, performance and regression contract

- App remains offline and uses only existing assets. No INTERNET permission,
  telemetry, clipboard-content parsing, per-key persistent cache, keyboard
  height/glass/footer updates or Dictate changes.
- Password/no-learning/incognito editor eligibility remains guarded by
  `SettingsValues` and `EmojiContextEligibility`; no long editor text is read
  beyond the existing n-gram context. Context hint is ephemeral, bounded at
  160 UTF-16 units, absent from ordinary predictions, gesture/app-specified
  suggestions and empty results.
- Normal Emoji Search and every custom shortcut still use the existing
  off-UI-thread `EmojiDescribeEngine` singleton with its stale-request
  generation guards; suggestions from the native decoder remain fast and
  no new worker thread was added.
- No claim of 60 fps or physical Oppo acceptance until owner tests it.
  Test multiword “happy birthday”, “good luck”, English/Hinglish, unrelated
  phrases, protected editors, actual typed candidate slots, Emoji Search,
  landscape, Clipboard Search, swipe/undo and original themes.

## Verification checkpoint

Initial local `git diff --check` passed. Both existing pure Python B16A and
B16B suites passed (5/5 each). The Android Gradle wrapper/JDK compiler and
SDK are not available in this sparse local execution environment; Android
unit/compile and original-signed APK require both GitHub Actions workflows.
Record their exact run IDs, terminal status, resulting release target,
certificate and SHA-256 after any source commit. Do not claim device-accepted
behavior merely from green CI.

### CI correction checkpoint

Main commit `8c390dae38ec4db536d6a2da9401c3607ac964af` compiled but its
debug workflow `44353959830` failed one of 310 Android tests:
`EmojiDescribeBundledCorpusTest.realLifeDescriptionsUseMeaningRatherThanOnlyLiteralEmojiNames`
at the natural query `laughing so hard I am crying`. Root cause: Search promoted
the suffix-only low-confidence contextual `crying` rule above the existing CLDR
semantic `🤣` result. The follow-up correction restricts Search rule promotion
to **whole explicit query matches**. The suggestion strip deliberately retains
its suffix matching, since live editor text is contextual by definition. A
focused regression was added. This must be confirmed by the next full CI runs.

## Follow-up intelligence without multiplying engines

The remaining substantive “better dictionary” work is an **evidence-gated**
experiment on the *same* native pipeline: frozen synthetic English/Hinglish
previous-word contexts, actual decoder top-3 output, protected names/URLs,
before/after correction false positives and Oppo p95 latency. Reuse
`tools/evaluate_ngram_predictions.py` and its strict no-loss gate. If no
reproducible improvement appears, keep present frequencies and native order;
never market an unmeasured change as a smarter model.

