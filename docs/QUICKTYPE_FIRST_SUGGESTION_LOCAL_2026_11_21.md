# First-keystroke QuickType delivery and stable strip — release verification ledger

## 2026-11-21 v1.0.207 release and owner-reported remaining motion failure

The preceding QuickType source was published on `main` as `709b01b4ff434b37525171f79e28d988c3c50a0d`. Exact-SHA debug workflow `44871798905` and original-signed Obtainium workflow `44871798882` completed **SUCCESS**, and signed release `v1.0.207` was published. This is Android CI/signature evidence, not Oppo animation acceptance. Owner subsequently reported that initial words should appear with the keyboard **without** an entrance transition, and *every later replacement* should animate, including unrelated pairs and words with only one shared character, across three-slot/chip updates. The prior v207 glyph plan deliberately suppressed those transitions. The new source fixes this design gap and synchronously seeds safe, generic English starting words for eligible empty prose fields without waiting for an asynchronous decoder. Real native candidates are still authoritative; the generic row is not a model prediction or autocorrect. Exclusions: privacy, secure/non-prose/non-English fields, app completions, inline autofill, clipboard, hardware keyboard, unknown/non-zero caret positions, disabled predictions. The exact current iOS 27 timing is **not publicly documented or frame-measured**; 164ms remains a reference-inspired approximation until high-FPS iPhone/Oppo verification. No APK claiming this newer behavior is accepted yet.

## 2026-11-21 owner GO: remote Android build and signed update

The owner now explicitly said "Android build is pending, so go ahead." This
supersedes the earlier no-GitHub-upload boundary **for the QuickType repair**.
Publish only by fast-forwarding the existing `main` branch; wait for Android
debug compilation/full unit tests on the exact pushed SHA, then the normal
original-certificate Obtainium signed release (which requires that same debug
run to be green). The last independent signed release is v1.0.206 from
`c4e2882`; its successful CI did **not** test these keyboard changes.
Even if CI and signing pass, first-key suggestions, rapid Space/Backspace,
three-slot animation, chips, Emoji, secure editors, RTL, accessibility and
latency on the user's real Oppo must still be confirmed on-device. Record the
actual source SHA, CI run IDs, signed tag and failures below after observation.

### Remote verification evidence

Pending publication; local Gradle wrapper 8.14.4 unavailable. Do not mark
debug, Obtainium or Oppo accepted before external results are verified.

## Owner instruction and exact boundaries

Earlier on 2026-11-21 (IST), owner requested a **ten-minute GitHub Actions watch**, followed
by implementation if no active work, continuing to poll for CI success. Prior
explicit **no GitHub upload / app installation** remained in effect during this
keyboard-specific local phase. Preserve the separate BR Brain, Parakeet,
GTCRN, native dictionary, original package/signing key, and offline-only policy.

Observed the live Actions tab for more than ten minutes, including initial
idle, new `b5d5e82` debug/release runs, debug failure at the release-notes
gate, and the consequent release failure. They were both terminal at the end
of the watch. New corrective `c4e2882` debug and Obtainium runs appeared after
local implementation started; these belong to the separate workflow/privacy
cleanup. They do not test or certify this unpublished QuickType source.

Rebased prior local motion commit `57098fa` on top of remote `b5d5e82`
without losing the independent privacy-log and workflow changes. Its new local
commit was `4496489` before this additional work; a later fresh-main reconcile
must preserve any newer remote changes without force-pushing.

## Root cause observed in source

- Blank editor startup previously reset the strip without scheduling initial
  context prediction except when resuming a touched word.
- Typing updates waited up to 40 ms on UI for a decoder thread. A result that
  arrived after timeout had no subsequent main-thread delivery.
- `SuggestedWords.getWordCountToShow()` excludes the typed entry. With only
  that entry available, `SuggestionStripView` hid the content.
- Beginning-of-sentence predictions were suppressed in favor of pinned keys
  even when genuine words were available.
- `SuggestionStripView.setSuggestions()` detached and rebuilt all word and
  divider children on every update, including common three-slot transitions.
- Main dictionary readiness cleared the next-word cache but refreshed the
  current editor only when a specific load-wait message was pending.

## Implemented design (local, not Android-compiled)

1. On eligible keyboard-open sessions, schedule an initial context lookup.
   Respect users' suggestion, bigram, hardware keyboard, application completion,
   incognito, secure-editor and external accessory gates.
2. On a normal first typed character, publish an immediate, display-only
   literal prefix when no real candidate is ready. This neither fabricates a
   completion nor installs a new autocorrect; touching it cannot duplicate
   already typed content. This mechanism also covers subsequent typed-only
   dictionary results.
3. Regular per-letter decoder requests run on the existing non-UI handler
   without blocking the keyboard UI and return on the UI handler. Each result
   checks editor generation, monotonic request version, exact typed prefix and
   both expected selection endpoints before display. Old results are dropped;
   empty results do not erase an active literal-prefix fallback.
4. Preserve the pre-existing bounded 40ms correction refresh **only** at an
   explicit Space/Return commit boundary to avoid accepting an old correction.
   If still missing, commit the literal typed word rather than a stale result.
5. Upon dictionary availability, post a fresh current-editor lookup to UI,
   after invalidating the next-word cache.
6. For ordinary three-slot word updates, update the attached word TextViews
   and dividers without `removeAllViews`. Retain full rebuild for one-word,
   punctuation, RTL direction changes, chip mode, oversized candidates and
   debug overlays.
7. Keep latest suggestion fully visible from its first painted frame. Additional
   Latin glyph painting safety checks reject symbols, digits and cases where
   individual-glyph measurement or common ligature pairs risk native shaping.

## Verification and blockers

- `git diff --check`: passed.
- `python3 -m json.tool docs/releases/notes.json`: passed.
- New pure Java `SuggestionDeliveryPolicyTest` covers editor/session, prefix,
  cursor and obsolete-request rejection. Extended
  `SuggestionStripVisualPolicyTest` covers a one-letter typed fallback.
  Updated `SuggestionGlyphMotionTest` protects text-shaping fallback.
- `./gradlew --offline --no-daemon :app:testRunTests`: **blocked before
  compilation**; Gradle 8.14.4 wrapper distribution is absent, and the
  distribution host is unreachable from this environment. No Android test
  result, APK, original signature or Oppo device result is claimed.

## Remaining proof and rollout gates

1. Fresh-main source reconcile with any concurrent `main` movement.
2. Full Android compile, unit/Robolectric and exact-SHA debug CI, then original
   signer Obtainium CI **only on explicit upload/release authorization**.
3. Physical Oppo Reno10 5G: cold keyboard open, first `t`, `th`, fast typing,
   autocorrect immediately followed by Space, delete/backspace, cursor moves,
   repeated field switches, context after Space, clipboard/autofill priority,
   gesture typing, Emoji Search, incognito, password, RTL, TalkBack and chips.
4. Observe first-visible word and p95 keystroke-to-candidate timings, not just
   animation duration, without logging any editor text.

Stop on unintended inserts, stale suggestions, privacy/field violations,
accessibility regressions, layout crashes or performance regression. Never
conflate source acceptance with a signed or phone-verified release.
