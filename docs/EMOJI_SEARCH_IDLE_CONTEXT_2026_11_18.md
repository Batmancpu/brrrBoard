# Emoji Search initial suggestions and compact top-gap follow-up — 2026-11-18

## Owner report and identified source conditions

The owner sees an empty 56dp Emoji result row beneath the Search field and above the typing keys until entering text. That behavior is **deterministically caused** by `EmojiPalettesView.searchEmoji`: before this change it called `showEmojiSearchResults(query, List.of(), false)`, then immediately returned for `query.isBlank()`. It never asked the existing local contextual scorer for recommendations. No screenshot inference is necessary for that specific empty-row cause.

The compact Browse category-gap issue is **independent**. v1.0.115 source `c1fd0555` moved the optional 70dp Recents Smart picks stack from a top-level sibling *before* the grid to an overlay *below* the categories. That removes one verified source-level cause of vertical displacement, but owner has not yet confirmed v115 on the Oppo. This follow-up also removes a remaining 8dp decorative inset between the fixed category header and Emoji row zero. `clipToPadding=false` preserves scrolling under the fixed live top glass; the optional 70dp Recents Smart picks continue to reserve space only **within** their category grid, never above category icons. These corrections are not proof there is no other device-dependent offset.

## New Search-open flow (offline, transient)

1. Before focusing the internal Search `EditText`, `enterSearchMode` looks up the **host** `InputConnection`. It checks `EmojiContextEligibility`: only ordinary normal/short/long message text editors; never incognito, passwords, email, URI, no-suggestions or no-personalized-learning fields. It obtains at most 160 preceding characters only on eligible fields.
2. `EmojiSearchContextPolicy.safeSuffix` keeps the latest at most 80 Unicode code points and rejects numeric/identifier-like strings. The input is processed transiently, never logged, stored as history, transmitted or included in typed suggestions.
3. Synchronously, existing manually reviewed phrase rules (e.g. `happy birthday` → 🎂🎉, `love you` → ❤️) lead the row, followed by adaptive recent Emoji. `EmojiSearchIdleResults.merge` fills unoccupied cells with ten generic Emoji, so the row **never starts blank**, including fresh installs and privacy-protected editors. Privacy-protected fields show generic choices only.
4. On the existing low-priority single Describe worker, the immutable bundled CLDR posting index finds specific recent subjects. `recentContextTokens` scans from the **end** so a long sentence ending with “pizza” is not lost to the eight-token query cap. Weak generic words like “tonight,” “order,” and “planning” are ignored. On UI callback (with generation guard), results merge in order: reviewed rules → specific indexed context → recents → default choices, duplicate-free, max ten. The first row is immediately usable while this optional refinement finishes.
5. Typing into Search still uses the existing 24-result offline Describe engine. Clearing to blank re-displays initial recommendations; leaving Search cancels outstanding work and clears transient state. No AI agent, new permissions, model, HTTP client or extra corpus.

## Executable evidence and remaining gates

- Pure-JVM compile of actual `EmojiContextIndex`, `EmojiDescribeNormalizer`, `EmojiDescribeAliases`, `EmojiDescribeIndex`, `EmojiSearchContextPolicy`, `EmojiSearchIdleResults` using JDK compiler module: **success**. Read all 3,944 CLDR entries from the exact bundled `emoji_cldr48_en.tsv`.
- Corpus outputs: “Let's get pizza tonight” → 🍕; “We need coffee” → ☕; “Planning a beach holiday” → 🏖️; “Let's watch a movie tonight” → 🎥,🍿; “We need dog food” → 🐕; long suffix “...let's order pizza” → 🍕. “Let's meet tomorrow” has no confident context hit; generic/recent fallback still fills the row. Reviewed `I love you` gives ❤️ even though the literal corpus contextual token alone doesn't.
- New unit contracts: `EmojiSearchIdleResultsTest` (nonempty, prioritized, deduplicated and capped), `EmojiSearchContextPolicyTest` (safety/bounds), real corpus context cases in `EmojiDescribeBundledCorpusTest`, compact category placement in `EmojiBrowseHeaderMountTest`.
- Android/Robolectric full CI, original production signer, exact APK SHA, and Oppo visual/runtime acceptance: **pending at authoring time**. Never label phone acceptance from unit tests.

## Oppo acceptance checklist

1. In ordinary chat, type `happy birthday`, then tap Emoji → Search without entering anything: 🎂 should be in the result row immediately. Repeat after `Let's get pizza tonight`, waiting a moment for offline refinement; 🍕 should be toward the front. Repeat after `We need coffee` → ☕.
2. Clear a typed Search query using X; the result row should fill again without waiting for input. Confirm explicit `man with glasses` → 🥸 and `smiley with sunglasses` → 😎; unsupported `flying dog` may correctly return no result.
3. New/empty text field should still show a usable default row. Password/incognito fields show only generic results; no personalized usage/context.
4. Open compact Emoji categories Smileys, Recents (with and without Smart picks), other categories, then Expanded/Back and Search/ABC. Categories should begin at the top edge, first row must be complete immediately below header, and scrolling should blur beneath header and bottom control row.
5. If compact gap remains, capture a **new recording on this exact version** showing first open, current category, and entry/exit; source inspection cannot provide the Oppo view-coordinate trace.

Smooth Grow/Collapse height animation remains a separately requested follow-up; do not bundle it with these corrections.

**Subsequent CI checkpoint:** source `b71dabe82e8fb1efca227a3100be40dfcbbe84e8`, Android debug #211 (`44255489574`) SUCCESS, original-signer production #116 (`44255489601`) SUCCESS. Signed v1.0.116 APK: https://github.com/Batmancpu/brrrBoard/releases/download/v1.0.116/brrBoard.apk ; SHA-256 `83f54206d6c43679dec8c95283e1eb2851adb15a7cb7c431fca6b4654a38de85`. All physical Oppo checks listed above remain OPEN. The planned animation is developed in the *next*, separate source batch: `docs/EMOJI_HEIGHT_MOTION_2026_11_18.md`.
