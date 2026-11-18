# Describe an Emoji — offline engine, code-level implementation plan

**Status at original writing:** DESIGN ONLY. **Superseded 2026-11-18:** owner explicitly authorized implementation; actual code and tests tracked at [Describe implementation](DESCRIBE_EMOJI_IMPLEMENTATION_2026_11_18.md). Retain this document as the original proposal, not the current hold.
**Owner:** brrBoard (`com.mangoloads.board`), `main` only, Oppo Reno 10 5G.
**Sequence:** finish separate Emoji-only tonal glass release first; begin this
engine only after the owner approves the behavior and acceptance criteria.

## 1. Recovered request and strict boundaries

The earlier backlog explicitly called this feature **"offline CLDR/typo-tolerant
Describe an emoji engine"**. Full prior chat phrasing is not accessible, so
the following UX is an informed proposal, **not** an invented prior approval:
the user types a short natural description ("happy dog", "crying with joy",
"red heart with arrow") in the existing Emoji Search field and receives
relevant actual Unicode Emoji, instantly, entirely on-device. The feature is
search-on-demand, not an AI agent watching or rewriting normal typing.

Owner constraints: no AI inference model, API, cloud text transfer, per-query
network, new permission, startup slowdown, persistent query history, or
additional latency to normal keyboard touch/swipe/prediction. Preserve the
existing search keyboard, expanded browse, approved glass/footer and original
signing identity. Keep any future emoji-to-description popup *out of this batch*
unless separately requested.

## 2. Source inventory / starting baseline

| Existing component | What it does | Required change |
| --- | --- | --- |
| `app/src/main/assets/emoji_cldr48_en.tsv` | 450,626-byte UTF-8, 3,944 Emoji rows; English CLDR48.2 names and keywords, Unicode17 sequences | Reuse as sole primary corpus; no user text added |
| `EmojiContextIndex.java` | Lazily parses TSV to an immutable `emoji→(name, keywords)` map; `search()` scans every emoji on every query, ranking exact/substring/all-term hits | Preserve as fallback and add read-only iteration or immutable projection for building fast lookup |
| `EmojiPalettesView.java` | Search `TextWatcher` synchronously invokes `searchEmoji()`, rebuilds one-row `DynamicGridKeyboard`, optionally appends existing dictionary suggestions | Route nonempty descriptions through cancelable background engine; render only on UI thread; retain fallback |
| `EmojiContextScorer.java` | Separate carefully reviewed contextual Smart Emoji triggers, private-editor guards | **Do not change**; its automatic-typing confidence policy is distinct from intentional Search |
| `tools/generate_emoji_context_index.py` | Reproducible, version-pinned Unicode/CLDR extraction with asset hash checks | Reuse provenance and fixtures; don't fetch Unicode data at runtime |
| `EmojiContextIndexTest`, `EmojiContextSearchTest` | Current exact keyword/name and input-limit unit tests | Preserve unchanged; add Describe-specific golden/typo/negative/latency tests |

Current lookup is deterministic and offline but has **no bounded edit-distance
correction or semantic alias layer**; it uses `String.contains` over all rows
on the main thread. This is the performance/quality seam to improve.

## 3. Proposed classes and interfaces (new files when authorized)

```
emoji/EmojiDescribeNormalizer.java
    QueryTokens normalize(String text, Locale locale)
    // Immutable normalized terms + original phrase + bounded modifiers.

emoji/EmojiDescribeLexicon.java
    static EmojiDescribeLexicon from(EmojiContextIndex base, AliasTable aliases)
    // Read-only int emojiId -> sequence/name/keyword fields; token postings.

emoji/EmojiDescribeMatcher.java
    List<DescribeHit> search(EmojiDescribeLexicon lexicon, QueryTokens query, int limit)
    // Candidate retrieval, typo fallback, deterministic weighted scoring.

emoji/EmojiDescribeEngine.java
    void searchAsync(String query, long generation, Consumer<List<String>> result)
    // Single bounded executor, lazy one-time index load, cancellation/stale gating.
    void cancelActive(); void close();

emoji/EmojiDescribeAliases.java
    // Reviewed aliases from a versioned TSV; never learned from user input.

tools/generate_emoji_describe_aliases.py (optional build-time validation only)
    // Validate alias cycles, conflicts and license; no runtime generation.
```

Prefer **reusing the existing 450 KB asset** and constructing an immutable
token-to-primitive-`int[]` posting index once, *off the UI thread*, per process.
At 3,944 records, a disk SQLite database, FTS migration or duplicate large
index asset is not justified until benchmarks prove the in-memory approach
insufficient. Build a sorted token lexicon and postings only from CLDR names,
keywords and reviewed aliases. If cold-start parsing is too slow on Oppo,
stage a later reproducible compact binary posting index generated at build
time with a version/hash check; do not add this complexity preemptively.

**Data ownership:** exactly one `EmojiDescribeLexicon` per process, immutable
after construction. No writable database, user query cache on disk, remote
telemetry or editor content captured. Search query state remains ephemeral in
the same in-IME Search input and is cleared by existing exit/session behavior.

## 4. Tokenization, synonyms and typo handling

1. Clamp description to 80 Unicode code points and at most eight meaningful
   tokens. Empty input keeps the existing blank-result behavior. Preserve
   original string only for exact phrase ranking during the active request.
2. Normalize *query words*, **not the Emoji sequences**, with Unicode NFKC,
   `Locale.ROOT` case folding/lowercase, Unicode word boundaries (`BreakIterator`
   available at minSdk24), whitespace/punctuation handling and a small safe
   plural/stemming table. Avoid substring traps such as `cat` matching
   `education`, and handle apostrophes/combining characters consistently.
3. Index CLDR short-name tokens with greater weight than broad CLDR keywords.
   A short phrase may match a full keyword; retain phrase order and proximity.
4. Version a small **human-reviewed** synonym/alias map. Examples *to test*,
   not automatically accepted mappings: `joyful → happy`, `weeping → crying`,
   `birthday → cake`, `bday → birthday`, `loo → toilet`. Test ambiguity before
   adoption. Add locale-specific aliases (e.g. Hindi/Hinglish) only with vetted
   dictionaries and synthetic acceptance cases; do not pretend English CLDR
   covers the user's full language preference.
5. Exact and prefix matches run first. For unmatched nontrivial words, retrieve
   nearby lexicon terms via a token trigram/prefix candidate map, then apply
   **bounded Damerau-Levenshtein** (single adjacent transposition included):
   max distance 1 for length 4–5, max 2 for length 6+, none for length ≤3.
   Cap comparisons/candidates per term to avoid keystroke spikes; fuzzy
   candidates rank strictly below genuinely exact matches.
6. Parse explicit attributes (`red`, `heart`, `woman`, `man`, `skin tone`,
   `hair`, direction, flag region) as hard/soft modifiers only when CLDR or
   an existing Unicode-qualified sequence supports them. **Never synthesize**
   arbitrary ZWJ/VS16/skin-tone combinations or show unsupported glyphs.

## 5. Search/ranking pipeline

```
query from Emoji Search
    → normalize/tokenize (bounded)
    → postings union for recall; weighted intersection for precision
    → phrase/name/keyword exact matches
    → reviewed alias matches
    → typo recovery only for still-unmatched tokens
    → Unicode-supported Emoji filter and stable dedup
    → deterministic score and top 24 (never host-editor autocorrect)
```

Proposed baseline score (requires golden-test calibration, not final tuning):

| Evidence | Relative boost |
| --- | ---: |
| Full CLDR short name equals query | +1000 |
| Ordered query phrase occurs in short name | +650 |
| All meaningful tokens represented | +250 |
| Exact short-name token | +110 each |
| Exact keyword token | +70 each |
| Reviewed synonym token | +45 each |
| Bounded typo-corrected token | +15 each |
| Explicit unsupported/contradictory modifier | strong penalty / reject |

Give partial-result recall a lower score than complete matches. Exact/phrase
matches must dominate synonymous/fuzzy guesses. Use original CLDR order and
the exact normalized Emoji sequence as deterministic ties; optionally add a
*tiny* existing Recents tie-breaker only after semantic relevance. A query
that truly has no credible candidate returns the existing "No matching emoji"
state rather than inventing an answer. Never use this ranking to inject
unreviewed Emoji into ordinary text suggestions.

## 6. Integration / threading — exact call sites

1. `EmojiPalettesView.setupSearchField.afterTextChanged()` updates the private
   query as today and increments a monotonically increasing `mSearchGeneration`.
2. Empty query: on the UI thread clear existing results immediately, no worker
   request. Nonempty query: optional 40–60ms debounce, dispatch to a dedicated
   single worker executor (index load and search never on the IME UI thread).
3. `EmojiDescribeEngine` lazily initializes the immutable lexicon once on
   first Emoji Search entry (or warm it after the emoji panel is visible, not
   on normal typing startup). Subsequent queries use postings; no disk IO.
4. Worker returns `List<String>` only. `Handler(Looper.getMainLooper())` posts
   to `EmojiPalettesView`, which checks generation, `mSearchMode`, current query
   and view attachment before touching `DynamicGridKeyboard` or ScrollView.
   Stale results are silently discarded.
5. Extract current display logic from `searchEmoji` into
   `applyEmojiSearchResults(List<String>)`; keep the same Emoji parser/keyboard
   key construction, horizontal one-row result layout, accessibility labels,
   Back/ABC/reparenting and bottom glass. Never mutate Views from the worker.
6. When engine cannot initialize or its safe timeout fails, fall back to
   `EmojiContextIndex.search()` and optional locale Emoji dictionary as today;
   avoid blocking the main thread with a repeated full index scan in normal
   operation. On `exitSearchMode`, editor switch, IME teardown and forced
   layout change, bump generation and cancel pending work; close only on
   view/IME lifecycle teardown, not on each keypress.
7. Default to at most 24 ranked results for this describe mode to prevent
   allocating a 96-result-wide virtual keyboard on every typed prefix.
   Preserve existing emoji selection, skin-tone popup and Recents update path.

## 7. Performance, privacy and safety gates

**Proposed measured targets, not measured claims:**

- APK overhead: reuse 450 KB CLDR asset, with only a small reviewed alias map;
  no models, embeddings, runtime SQLite migrations or new dependency.
- Steady index memory: target under 4 MiB; measure actual heap and allocation
  counts on Oppo before release.
- Warm query: target p95 under 15ms on device for 1–8 token descriptions;
  cold index loading under 150ms on worker, never the IME input thread.
- UI: no dropped normal typing/swipe frames; stale results never overwrite a
  newer query; no keyboard resize/blur change while typing.
- Privacy: search text exists only inside the transient search session. No
  network, logging, analytics, personalization training, clipboard reads,
  other-app EditorInfo reads, new permission or query persistence.
- Character safety: preserve fully qualified Unicode sequences and tone/gender
  variants, use parser validity and system-supported glyph checks; never
  rewrite the host editor text until a user taps an actual Emoji.

**Unit fixtures:** ≥40 name/phrase exact queries, ≥40 typo/transposition
queries, ≥40 synonymous multi-token descriptions, ≥30 negatives/ambiguous
and contradictory modifiers, plus punctuation, NFKC, diacritics, emoji
sequences, variants, languages, stale generations and 80-codepoint limits.
Always include `laughing with tears → 😂`, `red heart → ❤️`, `dog face → 🐶`,
`bday cake → 🎂`, and unknown words returning no invented result (rankings
must be checked against the actual pinned data rather than assumed).

**Android acceptance:** first use cold and warm; quick typing/deletion;
Search→Back→Expand→Search; category switches; empty/no-match UI; landscape;
TalkBack; clipboard/Toolbar isolation; keyboard reopening; OEM/Oppo long-press
modifiers; 10-min sustained typing profiling. Run Android debug/unit and
original-signer GitHub Actions independently. Physical result required before
the owner accepts relevance and perceived speed. No changes until approval.

## 8. Rejected alternatives and why

1. **LLM/cloud search:** introduces network/privacy/latency/availability
   dependencies and a high operating cost; contradicts the owner's directive.
2. **Bundled neural embeddings/NN runtime:** can add memory, model size,
   cold-start/CPU cost and opaque errors. Not warranted for only ~4k CLDR
   records and straightforward natural descriptions. Re-evaluate *only* if
   a measured non-ML quality ceiling justifies it and the owner opts in.
3. **SQLite FTS4/Room:** officially supported and excellent for large text
   corpora, but adds on-device index persistence/queries/migration and does
   not by itself solve synonyms or edit-distance ranking. A small immutable
   in-memory postings index has fewer moving parts for 3,944 entries.
4. **Naive per-row Levenshtein each keystroke:** O(all terms×all Emoji) work
   on every letter; typo correction must run only on bounded lexicon candidates.

## 9. Trusted primary sources used in the design

- Unicode [UTS #51 Emoji](https://www.unicode.org/reports/tr51/) for qualified
  Emoji sequences and language-dependent short names/annotations.
- Unicode [UTS #35 CLDR annotations](https://unicode.org/reports/tr35/tr35-general.html)
  and [Emoji names and keywords](https://cldr.unicode.org/translation/characters/short-names-and-keywords),
  including the explicit distinction between names and search keywords.
- [CLDR releases](https://cldr.unicode.org/index/downloads), pinned v48.2
  and its exact repository data, rather than mutable HEAD.
- Unicode [UAX #29 text segmentation](https://www.unicode.org/reports/tr29/)
  for word boundaries; Android
  [`BreakIterator`](https://developer.android.com/reference/android/icu/text/BreakIterator),
  available from API 24 (brrBoard's minSdk).
- Android [coroutine/background work guidance](https://developer.android.com/kotlin/coroutines)
  on keeping lengthy work off the UI thread.
- [Android Room FTS4](https://developer.android.com/reference/androidx/room/Fts4)
  and [SQLite's FTS4 specification](https://www.sqlite.org/fts3.html) for
  evaluating—but not prematurely adopting—a database solution.
- [Unicode data license FAQ](https://unicode.org/faq/unicode_license.html),
  and this repo's pinned provenance/license in `docs/EMOJI_CONTEXT_DATA.md`.

## 10. Owner decision before implementation

Confirm that "Describe an Emoji" means **type a phrase and find Emoji** in
the existing Search UI, not automatically suggesting Emoji while normal text
is being typed or an Emoji-to-English explanation popup. If confirmed, begin
with an isolated *engine + golden tests* batch (no UI); then wire Search UI in
a second gated main release, preserving the signed keystore and prior panels.
