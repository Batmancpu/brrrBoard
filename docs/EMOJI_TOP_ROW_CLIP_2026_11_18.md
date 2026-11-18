# brrBoard: top Emoji row crop — 2026-11-18

## Owner evidence and scope

- Installed app about screen reads `1.0.108` on Oppo Reno 10 5G.
- Photo: category strip is sharp; below it is an empty-looking dark region, followed by the first Smileys row cut horizontally at its top while the following row is fully visible.
- Prior video: compact Browse behaves differently after Emoji Search → Back; A1 made Browse height deterministic but did not resolve this owner's reported visual defect.
- **Only this issue is authorized in the next release.** Defer Expand/upper glass/icon/Clipboard/typing changes. Preserve approved moving lower Emoji and shared footer glass completely.

## Code-path finding

`EmojiPalettesView.onMeasure()` computes the Browse body height, but it does not control the *pixel* anchor inside the nested vertical lists. `EmojiCategoryPagerAdapter.onBindViewHolder` reconstructs a category `RecyclerView` from `SparseIntArray` row numbers and requests `LinearLayoutManager.scrollToPositionWithOffset(savedRow, 0)`. There was no post-layout assertion that saved **row zero** actually starts at the viewport's existing 8dp top padding. Search/Browse reparenting and changes of ViewPager height can therefore leave the leading row partially outside the visible viewport. This is the precise source-level gap addressed; a device layout trace is still necessary to establish it as the entire visual cause, especially for the empty-looking region above the cutoff.

## Minimal source change

- Add a one-time post-bind/post-layout check for first-visible row zero at idle; reanchor only when its actual top differs from RecyclerView top padding by more than a rounding pixel.
- Leave deliberately scrolled rows (saved index greater than zero) and active vertical gestures untouched.
- Remove the listener on recycle/rebind to avoid stale holders; retain existing 8dp top and 50dp+footer bottom insets, ViewPager2 and all glass draw/clip paths.
- Add small deterministic `EmojiRowViewportPolicyTest` for cropped, excess-gap and correctly aligned first rows.

## Required gates / no false acceptance

1. Verify source diff, compile and run Android unit tests using the existing debug workflow.
2. Pass the original-signer production/Obtainium workflow, inspect exact source tag, package `com.mangoloads.board`, APK digest and certificate guard.
3. Owner installs the new APK on Oppo Reno 10 5G; capture first opening, tab switching, vertical scroll/reopen, Search → Back, portrait and landscape. The first Emoji row must be entirely visible when the list is at its top; no artificial empty block; bottom moving blur unchanged.

**Status:** source candidate. CI, signer and physical acceptance not yet established. If the photo remains unchanged, request a short screen capture and inspect actual view-coordinate instrumentation before changing more surfaces.
