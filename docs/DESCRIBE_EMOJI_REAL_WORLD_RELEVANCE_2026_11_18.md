# Describe Emoji: real-world relevance revision — 2026-11-18

## What the owner actually reported

The 77.5-second v1.0.113 screen recording searches phrases including `man with a dog`, `man with glasses` and `smiley with sunglasses`. Literal single words produce results but conversational compositions often show an empty result row. The owner asks for **indirect real-life descriptions**, not trivial exact-name demonstrations, and a strictly local fast engine without AI model or network. v1.0.114 is installed but was not the source build in this clip.

## Baseline reproduced with the genuine bundled corpus

Locally compiled the pure Java `EmojiContextIndex`, `EmojiDescribeNormalizer`, `EmojiDescribeAliases` and `EmojiDescribeIndex` using JDK 21's compiler module, parsed `app/src/main/assets/emoji_cldr48_en.tsv` (3,944 CLDR48 / Unicode17 sequences), and executed 19 everyday descriptions. **Baseline v1.0.114 returned no results for all 19.** This is an actual offline engine test; no hardware/UI claims.

## Targeted code design

1. Query-only conversational stopwords (e.g. `I am`, `someone`, `because`, `need`) are ignored **only in multiword query text**; CLDR asset tokens and standalone word searches keep their meaning.
2. Reviewed concept synonyms cover everyday terms such as `smiley → smile/face`, `man → person`, `hurts → broken`, `throwing → vomit`, `sunny/holiday → beach`. The existing exact CLDR postings remain stronger than aliases.
3. A tiny local idiom normalizer handles high-confidence compositions: cannot see → blind, no energy → tired, born today → birthday, keeping a secret → shushing, blind person helped by dog → guide dog, silly mistake → grimacing. It never creates an Emoji code point. The existing all-term-match gate still refuses arbitrary unrelated composites.
4. Existing lazy immutable inverted index, single cancellable background worker, 80-code-point and 8-token limits, 24-result UI cap, optional custom TSV overrides, and privacy boundaries remain as before; no new file/assets/permissions.

## Reproducible before / after (entire real CLDR, top result)

| Real-life description | Previous | New result |
| --- | --- | --- |
| smiley with sunglasses | none | 😎 |
| smily glasses (owner's recorded spelling) | none | 🥸 (🤓 is second) |
| man with glasses | none | 🥸 |
| laughing so hard I am crying | none | 🤣 (😂 is second) |
| I can't stop laughing | none | 🤣 |
| I am sick and throwing up | none | 🤮 |
| person who cannot see and walks with a cane | none | 🧑‍🦯 |
| a dog helping a blind person | none | 🦮 |
| party because someone was born today | none | 🎉 (🥳 is second) |
| love that hurts | none | 💔 |
| I have no energy and need sleep | none | 😪 (😴 is second) |
| I made a silly mistake and feel awkward | none | 😬 |
| I am really nervous about this | none | 😅 |
| someone keeping a secret | none | 🤫 |
| take a break and have a cup of tea | none | 🍵 |
| need to use the bathroom | none | 🚽 |
| sunny and happy beach holiday | none | 🏖️ |
| man with a dog | none | **no result**, no such composed Unicode Emoji |
| flying dog | none | **no result**, not a supported Emoji |

Pure Java probe results were observed locally; the new first/second result assertions live in `EmojiDescribeBundledCorpusTest` for Android CI. The corpus and Android rendering can still produce variant sequences / different artwork. Real Oppo Search entry, typing, text insertion, stale-result cancellation, custom overrides and latency are device acceptance gates.

## Sources / provenance

- Unicode CLDR Emoji short names and search keywords: https://cldr.unicode.org/translation/characters/short-names-and-keywords
- UTS #35 CLDR annotations have separate short-name and search keyword fields: https://unicode.org/reports/tr35/tr35-general.html
- Official CLDR 48 release included Unicode 17 Emoji names and search terms: https://cldr.unicode.org/downloads/cldr-48

No model, HTTP client or external runtime data source added. New version/release only after standalone debug unit gate and original signing succeed; earlier glass-gap release must finish its gates first so CI isn't canceled.
