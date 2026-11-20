# Owner repair — immediate Emoji, synchronized incognito motion, top alignment, premium color

Date: 20 November 2026. Main only. Starting SHA: `85e40dfa4ac1de49470b5bd88a732f37ee62eea4` (documentation tip); app source previously `3a9f59902d5e8b044d1561815010eca0c14d2f77`, signed v1.0.171. Owner specifically says Emoji/ABC now feels almost immediate but the large empty band persists and manual double-tap privacy artwork/ mic shield seemingly disappeared. Screenshots and user observations are **not** CI, timing telemetry or a new model benchmark.

## Batch A — Source repair

- `BoardFooterView.emojiTapUp` retains immediate first-touch dispatch. On the recognized second tap that first opened Emoji, switch back to alphabet before manually toggling privacy; retain the three short privacy-confirmation haptics. A quick double tap that originally started from ABC is consumed separately, per `FooterEmojiTapPolicy`.
- The shared `FooterIncognitoEmojiDrawable` instance is temporarily replaced in the ImageButton by `ic_board_abc` during Emoji browsing. The erroneous `!returnToLetters` test in `syncEmojiPrivacy` disabled its `setIncognito(..., animated=true)` precisely then. The mic shield comes from `setFrameListener` and thus also jumped instantly. Allow the single animator based on actual footer visibility, not the transient icon. A nonanimated layout re-render must leave a correctly running same-target animator alone, per the existing drawable guard.
- Preserve protected/password editor refusal, triple manual haptics, one 760ms enter/700ms exit clock, reversal continuity, 112ms protected mic denial, reduced-motion setting, touch exploration and instant panel tap behavior. The visual outcome and whether ColorOS drops a second touch remain Oppo acceptance checks.
- Added pure `FooterIncognitoMotionTest` guard for the visible-footer animation decision. No private content logging, storage changes, model, dictionary, package or signer changes.

## Batch B — Persistent band (planned, not claimed fixed yet)

Current XML nests the fixed tab strip at the top of `emoji_grid_shell`; the alphabet strip/persistent row are hidden for Browse. But the root `main_keyboard_frame`, `keyboard_view_wrapper`, and `emoji_palettes_view` are wrap-content, bottom-gravity containers. Previous fallback height calculations and pre-hide typing snapshots did **not** eliminate the owner's band. Inspect real view rectangles without logging input, then make Browse's visible pane top-anchored to the established parent keyboard viewport rather than relying on an inferred offset. Keep Search, Expand, one-handed, landscape, footer hitbox, glass and first Emoji row visible. The physical gap remains unaccepted until owner tests the shipped signed build.

## Batch C — Premium palette (planned separately)

`FooterPrivacyPalette.TURQUOISE` is still `#2BCCC9`; this is not the owner's requested gold. Replace only privacy face/lens reveal and outline shield with muted premium metallic-gold shades, keeping their exact shared motion clock, and preserve turquoise solely for voice processing. Recording mic should be a deeper polished crimson while idle remains theme-neutral. Do not change any speech model, ASR mode or permission. Check legibility in light/dark themes and color-blind/reduced-motion accessibility.

## Batch D — Independent validation and delivery (pending)

- Source/static: pure policy tests, Java/Kotlin Android test gate, `git diff --check`, contract tests.
- GitHub: `main` only, exact code SHA debug/full unit workflow SUCCESS, original signing workflow SUCCESS, release asset target exact SHA, APK size/hash.
- Real Oppo Reno10: double-tap on/off, 3 tiny haptics, gold reveal + simultaneous mic shield, long protected mic denial, instant Emoji/ABC first taps, no accidental privacy on routine navigation, top category flush, first emoji rows not clipped, Search/Expanded/rotation/one-handed, offline voice, learned vault survives in-place update. Cannot be inferred from CI.
