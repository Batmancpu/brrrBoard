# B11 Emoji context data provenance and scoring contract

**Snapshot:** 2026-11-17; Unicode Emoji **17.0** qualified sequences, English **Unicode CLDR 48.2** annotations and derived annotations. The package includes `app/src/main/assets/emoji_cldr48_en.tsv`, an offline-generated, UTF-8, 3,944-entry reference index. Broad CLDR keywords are **search metadata only**; they do not themselves authorize automatic contextual predictions.

Sources and pinned Git blob SHA-1s:

1. `unicode-org/cldr` tag `release-48-2`, `common/annotations/en.xml` — `fbf0e693ebafbae2dedf90f97b7ad141769941d9`.
2. Same tag, `common/annotationsDerived/en.xml` — `8ba77741a2ff1e02e2be911b8a3766dc114f1d43`.
3. This repository's `tools/make-emoji-keys/src/main/resources/emoji/ucd/17.0/emoji-test.txt` — `e42fdef494399d2a05b9b75930a3231fe81b2436`.

All downloaded upstream annotation text has SPDX `Unicode-3.0`; the full license and copyright notice are packaged as `app/src/main/assets/emoji_cldr48_license.txt`. The curated trigger list in `EmojiContextScorer.java` is independently authored, stored in code under GPL-3.0-only, and deliberately narrower than CLDR's general-use synonyms. No Apple font, assets or proprietary symbols are redistributed.

Deterministic offline regeneration (not run at application startup or in a release build):

```bash
python3 tools/generate_emoji_context_index.py \
  --annotations /path/to/cldr48.2/common/annotations/en.xml \
  --derived /path/to/cldr48.2/common/annotationsDerived/en.xml \
  --emoji-test tools/make-emoji-keys/src/main/resources/emoji/ucd/17.0/emoji-test.txt \
  --output app/src/main/assets/emoji_cldr48_en.tsv
```

The script refuses inputs with mismatched upstream Git blob IDs. Fully qualified Unicode sequences remain intact, including VS16, skin tones, flags and ZWJ families. Only the CLDR lookup identity removes FE0F for matching the upstream annotation key; displayed/committed Emoji are never rewritten.

`EmojiContextIndex` lazily reads this fixed asset into an immutable map. `EmojiContextScorer` uses an immutable deterministic curated phrase/alias allowlist, bounded suffix normalization, a confidence threshold, exact word boundaries, deduplication and stable score/Unicode ordering. It refuses explicit sensitive/private field requests and email/URL/code-like context. It never persists, logs or rewrites text; B12/B13 callers must separately enforce real `EditorInfo`/incognito/no-learning rules and stale-editor revision checks before any UI exposure. The R3 evaluation requires 30+ synthetic positives and 30+ negatives/private cases plus memory/latency inspection before enabling any unreviewed term expansion.

This is **source-level provenance, not physical relevance acceptance**. B11 adds data/scorer only; B12 owns Smart grid integration, B13 optionally owns suggestion-strip merge. Neither source compilation nor correct CLDR metadata alone proves contextual suggestions feel relevant on Oppo Reno10 5G.
