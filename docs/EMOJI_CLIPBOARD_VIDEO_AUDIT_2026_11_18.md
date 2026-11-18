# Owner 70-second keyboard recording — focused visual audit (2026-11-18)

## Evidence and release boundary

The supplied 512×1140 / 30 fps / ~70.4-second recording contains a regular
alphabet keyboard (~0–8 s), Compact Emoji (~9–13 s), Describe Emoji Search
(~14–22 s), Expanded Emoji with rapid category swipes (~22–38 s), a return to
Search (~38–46 s), and Clipboard/Search/ABC (~47–69 s). Video frames show
behavior on the owner's device at recording time, but **do not establish the
exact installed version**. No connected Oppo, frame-time trace, logcat or
current-release acceptance is available. Inspect the entire video, not just
the supplied stills. Do not reproduce personal host/clipboard content in logs.

The earlier Clipboard Search/clear/match-preview update is already **signed
and published as v1.0.123** from source `e0676c94c34d2663505374fa4b38a2943603e855`;
debug workflow `44285278898` and original-signer `44285278884` both passed.
Signed installer:
https://github.com/Batmancpu/brrrBoard/releases/download/v1.0.123/brrBoard.apk
(SHA-256 `50cba9ae28d0a184edf4de580749cd2c219d2fa7b322db0b0e9aca73099c490b`).

## Confirmed source issues and this focused candidate

1. **Compact Emoji top blank strip**, visible above the category tabs near
   9–13 s, and absent in Search and Expanded. At the root
   `main_keyboard_frame.xml`, `persistent_emoji_row_scroll`, its divider and
   `strip_container` precede `KeyboardWrapperView` in a `LinearLayout`.
   Those elements are intentionally detached/hidden for in-panel Emoji tabs,
   but the old defensive hide was inside *child* `EmojiPalettesView.onMeasure`.
   Android measures the *parent's preceding siblings first*; this can reserve
   an obsolete 40/48dp row on the current pass. `RoundedKeyboardFrameView`
   now enforces the panel's zero-external-header invariant **before** calling
   `super.onMeasure`, not by adding a compensating negative margin or shrinking
   the actual Emoji rows. A Robolectric measurement test asserts both Emoji
   removal and ordinary alphabet header preservation. The potential timing
   mechanism is verified in source; its exact Oppo run-time occurrence still
   needs installation and first-open/return-to-Search testing.

2. **Describe Search briefly clears Emoji between letters**, around 18–22 s.
   `EmojiPalettesView.searchEmoji` removed all `DynamicGridKeyboard` keys on
   **every** nonblank query before asynchronous CLDR indexing completed.
   Search now retains the last measured Emoji row dimmed while the bounded
   offline worker searches, gates all touch and accessibility actions on old
   keys, and atomically replaces results on the latest guarded callback. After
   a no-match answer it briefly displays safe idle fallback Emoji while the
   new query is pending. `syncSearchQueryFromField` also suppresses duplicate
   re-dispatch from the Search keyboard's synchronous `TextWatcher` event.

3. **Height transitions show low-fluidity/glow risk**, particularly Compact →
   Search, Search ↔ Expanded, return/ABC. The previous
   `EmojiPalettesView.launchBodyHeightMotion` changed an exact measured body
   height and called `requestLayout()` on **every ValueAnimator frame**;
   `ViewPager2`, row lists and the IME host were remeasured repeatedly.
   The new bounded 260-ms spring uses an alpha-independent **bottom-anchored
   clipping reveal** (render the maximal viewport once, change only
   `clipBounds` during animation, one final shrink remeasure). Generation
   cancellation, accessibility reduced-motion and available-height limits
   remain. No physical FPS improvement is claimed without FrameTimeline.

4. **Rapid multi-category paging** near 25–35 s shows partially incoming
   columns and temporarily empty inter-page space. Some content is naturally
   sparse in Recents, so a single half-page frame is not a proven blank-data
   bug. Still, source previously cached only one adjacent RecyclerView page;
   the bounded ViewPager2 offscreen cache is now two neighbors, and nonadjacent
   *tab taps* jump directly rather than animating through offscreen categories.
   Direct finger swipes remain a standard ViewPager2 snap/scroll path.

5. **Clipboard Search** around 49–63 s displays taller horizontal cards,
   and ABC at the end returns to typing. The recording does not demonstrate
   a selected Clipboard query substring or accent match; the focused v123
   logic must still be tested with deliberate queries and on the currently
   installed build. No unrelated Clipboard code is changed in this batch.

## Verification before declaring a fix

- Robolectric `RoundedKeyboardFrameEmojiHeaderTest`: with Emoji visible,
  external 40dp + 1dp + 48dp rows are GONE **in the same measurement**, while
  alphabet mode leaves all three visible.
- Existing `EmojiPanelHeightMotionTest`, Emoji Browse composition,
  contextual Search regression and ordinary keyboard/Clipboard unit tests.
- Full Android debug CI and **original-keystore production signer** must be
  terminal green, release tag and asset must point to the exact source commit;
  keep separate unstaged semantic #9/#8/#12 files out of the commit.
- Phone: initial Compact top edge/category/first row; return from Search,
  Expanded, Recents and another category; type/delete/clear a phrase quickly
  and verify no empty result flash **or stale Emoji insertion**; scroll both
  directions, tap nonadjacent tabs, ABC exit, Clipboard Search and return;
  portrait/landscape, light/dark, reduced motion, TalkBack, 60 Hz frame times.
- Preserve original app ID/key, live host backdrop blur, Emoji-only tone filter,
  glass tabs/scroll underlays and separate ABC/microphone footer.

## Unresolved / no overclaim

The exact Oppo FPS, in-place Search/Expanded window resizing, vibration/audio
and the final top-gap optical result are unverified without the owner's next
recording or connected device. No changes are attributed to Clipboard data
from the video. The user's separate uncommitted intelligent Emoji search work
remains deliberately excluded from this visual-only batch.
