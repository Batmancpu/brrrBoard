# brrBoard Emoji Search: tone-family ranking + natural phrases (2026-11-19)

## Owner request and five-minute workflow gate

The owner reported that searching `thumbs up` wastes nearly the entire Emoji
Search row with six skin tones. Plain descriptions must show the *current
selected default* tone once; a complexion phrase before or after the Emoji name
must retrieve the requested tone, and an intentional request for `skin tones`
must reveal the variants. The Describe an Emoji engine should also understand
natural phrasing more reliably. **No Liquid Glass or unrelated changes.**

- Hold began 2026-11-19 15:39:20 UTC, HEAD
  `e7a10db40fda4e918fef0ac6ac743b4af786a8d2`.
- Nine roughly 30-second GitHub HEAD/Actions checks through 15:44:21 UTC:
  **zero queued/running workflows and unchanged main** throughout.
- At start, both exact-HEAD debug and original-signed Obtainium workflows
  were terminal SUCCESS. Work started only once the full quiet gate expired.
- A separate voice-model commit sits after the older motion revision.
  Publish the Emoji changes *on top of that newer commit*, with a non-forced
  SHA lease. Preserve voice source, model-bundling workflows and original signer.

## Implementation and contract

1. `EmojiSkinToneSearchPolicy`: Unicode-scalar grouping removes only modifier
   codepoints U+1F3FB–U+1F3FF for comparison. The entire original Emoji
   sequence, gender, ZWJ and presentation selectors remain intact. Group
   variants by their genuine bundled CLDR family; **never synthesize** Emoji.
2. `EmojiDescribeIndex`: rank an at-most-240-candidate pool before grouping,
   otherwise variants consume the 24 visible slots before unrelated matches.
   Plain searches keep one canonical neutral sequence per family; the UI maps
   it to the saved `PREF_EMOJI_SKIN_TONE`. Specific color/complexion words
   select the exact existing Unicode shade in either word order; explicit
   `skin tones`, `shades`, or `variants` show the family, including neutral.
3. Protect non-complexion searches. Exact CLDR names such as `black heart`,
   `white heart`, `brown square`, and `light bulb` outrank incidental skin-tone
   interpretation. An unknown color request may fall back to literal CLDR
   lookup but must not fabricate a matching Emoji.
4. `EmojiPalettesView`: applying results, contextual idle suggestions and
   dictionary fallback uses one selected tone/family when there is no explicit
   qualifier. Explicit shade results bypass the preferred-tone replacement;
   even a non-default result can still open its existing long-press family
   picker. Preserve the asynchronous generation/touch gate, stable search
   viewport, popup hitboxes and no-flash result morph.
5. `EmojiDescribeAliases`: small, reviewed spoken descriptions such as
   `someone giving a thumbs up` or `giving a thumbs down` map to their known
   CLDR subject. The parser also supports complexion descriptions such as
   `tan`, `pale`, `brown`, `deep`, `medium light`, and `medium dark` without
   a model, user history or an internet request.

| Search | Expected first/available result |
| --- | --- |
| `thumbs up` | Only the configured default/neutral `👍` family in Search |
| `dark thumbs up`, `thumbs up dark` | `👍🏿` |
| `brown thumbs up`, `thumbs up medium dark` | `👍🏾` |
| `light thumbs up` | `👍🏻` |
| `thumbs up yellow`, `thumbs up neutral` | `👍` regardless of saved default |
| `thumbs up skin tones` | Six actual CLDR entries `👍 👍🏻 👍🏼 👍🏽 👍🏾 👍🏿` |
| `dark woman doctor` | `👩🏿‍⚕️` (not an invented composite) |
| `black heart`, `brown square`, `light bulb` | `🖤`, `🟫`, `💡` (ordinary colors) |

## Verification and release constraints

- Pure-JVM `EmojiSkinToneSearchPolicyTest` and real 3,944-row asset
  `EmojiDescribeBundledCorpusTest` cover order independence, diversity,
  natural phrases, exact colors, unknown phrases, ZWJ and variation selectors.
- Offline Java-21 `jdk.compiler` compiled all pure-index classes; a temporary
  full-asset probe returned the expected results for 24 example queries.
- Android compile, all unit tests, generator/prediction gates and original-signer
  Obtainium workflow must pass on the **new exact commit** before an APK is
  considered ready. If they fail, repair and repeat. No custom signer or
  package change, no internet permission, no voice/brrSpeak mutation.
- Physical Reno10 acceptance: default tone neutral and each selected shade;
  rapid query/backspace, tone before/after words, long-press variant popup,
  Emoji Search empty/no matches, dark/light theme, Accessibility and 60/120 Hz.

Unicode source: https://www.unicode.org/reports/tr51/ — the five emoji modifier
codepoints define skin tone, not arbitrary color-coded symbols or hair style.
