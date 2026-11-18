# Describe an Emoji — implementation and phone acceptance

**Release verification (2026-11-18):** Source `2f279e6fe2426ea25f89a11534dc904819183b56` passed complete [debug/unit #207](https://github.com/Batmancpu/brrrBoard/actions/runs/44245705860) and [original-signer #113](https://github.com/Batmancpu/brrrBoard/actions/runs/44245705899), both terminal SUCCESS. [Signed APK v1.0.113](https://github.com/Batmancpu/brrrBoard/releases/download/v1.0.113/brrBoard.apk) published targeting that exact source. User's device behavior remains **unverified**. Custom offline shortcuts followed independently in signed v1.0.114. The original source-candidate notes below are historical pre-release status.

**Owner authorization:** 2026-11-18: implement now, separately from the Emoji viewport/blur release the owner is testing.
**Starting source:** `main` `9b04e2e10bfb4f86d176e4d1a780f33ca18cd920`, signed v1.0.112, debug #206 and original signer #112 both succeeded. **This document records a new source candidate, not an already validated APK.**

## Client experience

1. Open brrBoard Emoji, choose the existing **Search** icon. The field says **Describe an emoji**. This is a search within the keyboard: the words are **not** sent to the app's text editor.
2. Enter a phrase such as `laughing with tears`, `red heart`, `heart with arrow`, `dog face`, `bday cake`, `loo`, or `doog face`. The single horizontal search results strip updates as the description changes, within the existing alphabet/search keyboard layout. Results are actual shipped Unicode sequences. The preexisting Emoji popup specification and tap-to-insert route are retained.
3. Typos, short-name words, broad Unicode CLDR keywords, simple plurals, and a small reviewed set of synonyms are supported. Empty input gives an empty results strip; a description with no credible match shows the existing **No matching emoji** indication. Back and ABC return through the existing UI state. Expanded Browse and its header/footer geometry are unchanged.
4. No AI/chat model, cloud request, account, additional permission, search history, learning of descriptions, model download or system keyboard typing interception. This is **English CLDR-based**, not unrestricted English/Hindi understanding or a generator of new Emoji combinations.

## Code-level dataflow

| File | Role / invariants |
| --- | --- |
| `EmojiContextIndex.java` | Existing strict UTF-8 TSV parser and immutable 3,944-record `emoji→annotation` map; new package-private read-only `entries()` for projection. Original legacy `search()` retained for asset-failure fallback. |
| `EmojiDescribeNormalizer.java` | NFKC, root-locale case-folding, Unicode letter/digit tokens, filler removal, conservative plural reduction; clamp query to 80 code points and eight meaningful terms. No disk writes. |
| `EmojiDescribeAliases.java` | Reviewed simple words and exact phrases (`laughing with tears` → 😂, `heart on fire` → ❤️‍🔥, `bday` → birthday, etc.). No learned/user-defined rules. |
| `EmojiDescribeIndex.java` | Build immutable token→primitive posting lists from short names (strong) and keyword fields (weaker). Sorted prefix term map; trigram→term lists for bounded fuzzy candidates; one reusable in-memory instance, no SQLite or second asset. Exact and whole-name hits beat alias/fuzzy. Multiword queries require one jointly matching Emoji, except deliberate phrase aliases. Stable CLDR tie order; at most 24 results. |
| `EmojiDescribeEngine.java` | Lazy `ThreadPoolExecutor` (one background-priority daemon, queue length one, latest pending request replaces old, timed out when idle). Reads/indexes the asset once on explicit Search entry, computes result off the UI thread, dispatches immutable candidates via main `Handler`. No normal alphabet keypath or network use. |
| `EmojiPalettesView.java` | Search `TextWatcher` and existing keyboard edit operations call `searchEmoji()`. Each request increments `mSearchGeneration`, immediately removes previous tappable hits **without rebuilding the search keyboard geometry**, then submits bounded description search. Main-thread callback checks generation, Search state, and current query before display. Existing `DynamicGridKeyboard`, Unicode canonicalization and variant popup remain the renderer; optional local-language emoji dictionary is consulted only when the index finds nothing. Search exit/palette stop invalidates older replies. |
| `emoji_palettes_view.xml` / `strings.xml` | Only the existing search-field hint changes to `Describe an emoji`; no new bar or extra bottom key. |

Index memory is process-local and immutable after build; there is no persisted query, per-editor input read or analytics. The single low-priority worker only exists after the user opens Emoji Search and is allowed to time out when idle. Each query allocates bounded scoring arrays over 3,944 IDs (not a linear 3,944-item string/keyword comparison). Prefix compares at most 48 lexicon terms, typo candidate list at most 96 terms per unknown word; edit distance bound is 1 for length 4–5 and 2 for longer Latin tokens. Unicode asset remains the source of truth, so even a phrase override resolves only if its Emoji exists in the bundled index.

## Automated test gates

- `EmojiDescribeIndexTest`: synthetic fixture, exact/NFKC, natural phrase priorities, aliases, transpositions, typo bounds, no `cat` substring from `education`, no hallucinated unrelated multiword candidate, unknown words/empty query, immutable lists and strict limits.
- `EmojiDescribeBundledCorpusTest`: loads the **real** pinned 3,944-record asset from test working directory, asserts index term count, golden queries, Unicode sequences and 24-result cap.
- Run the existing complete Android Java/Kotlin/Robolectric unit gate and debug assembly (no test skips), plus independent original-signing workflow and published GitHub release. Source/build success cannot certify actual Oppo latency/visual layout.

## Acceptance on Oppo Reno 10 5G

1. In normal Emoji Browse (including compact first row and bottom glass), verify no new layout or fade changes. Owner is independently testing the v1.0.112 viewport/blur fixes; do not claim them resolved by Describe.
2. Open Search; type `laughing with tears` → first 😂, `red heart` → ❤️, `dog face` → 🐶, `bday cake` → 🎂. Try typo `doog face`, then nonsense `blorple`; verify useful results and the existing no-match state respectively.
3. Tap result and verify the **host** receives the correct Emoji, then search again and long-press one with variants. Type descriptions using the on-screen keyboard without outputting those words to the host editor.
4. Quickly type/delete several characters, clear the search, exit to Browse, change editor, and reopen Search. No stale result, insertions from the old query, freeze, or unexpected expanded state.
5. Confirm full alphabet search-keyboard rows and lower ABC/microphone remain usable; search exit, expansion, categories and bottom Emoji-only glass still work. Check both themes, landscape, TalkBack if practical. Quality/latency on device remain **unverified** until owner reports.

## Independent release and rollback

Change only the new Describe Search engine, its UI hint, regression tests and handoff documentation. Do not modify Emoji pager/header/footer material, signing identity/package, or other pending batches. If CI fails, repair and rerun before linking a signed APK. If real device behavior regresses, perform a small forward-fix restoring only Search callpath; retain v1.0.112 as the prior known-green signed package. Keep source and release evidence in this ledger and `AGENTS.md`/`PROJECT_CONTEXT.md`.
