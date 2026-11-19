# Batch 2: confidence pill, toolbar interaction, and numeric long press

## Baseline and intended behavior

- Starting source: `f9c7640ea6654c0aa8c5beb787f7e62a613b7af9`, original-signed `v1.0.139` (Batch 1 green), on `main`.
- Owner's photo: center suggestion `except` carries a mauve/lavender rectangle even when the keyboard tint is unrelated. Default should be neutral gray, with a separate option to use the user's current action tint.
- Toolbar: the expanded Access Point scroll view replaces the suggestion strip. The old strip-only swipe listener therefore never observed touches from the expanded panel. Its 72dp overlay also cut the first tile row at 72dp despite a 50dp initial top inset. The extra 56dp scrolling padding at the bottom looked like a duplicate blur band above the already-live ABC/mic footer.
- `123`: press opens Symbols; a deliberate hold should open the existing large native numpad, then return to alphabet cleanly when leaving numpad.

## Production patch

- `SuggestionStripLayoutHelper`: neutral-gray center confidence highlight with >=4.5 text contrast by default. Opt-in `PREF_CONFIDENT_SUGGESTION_MATCH_TINT` reads the active action-key tint, preserving hue and adjusting luminance if required. Decoder-approved automatic correction remains the **only** activation gate; suggestion order and edit behavior are unchanged.
- `TextCorrectionScreen`, `Settings`, `Defaults`, `strings`: accessible `Use keyboard tint for confident prediction` switch in **Settings → Text correction → Suggestions**; off by default. Existing theme reload triggers a visual refresh.
- `InputView`: when the expanded toolbar is visible, the entire toolbar panel is a possible downward swipe origin; while its list is scrolled away from the top, the list retains scroll ownership. Uses touch slop, horizontal-gesture rejection, child `ACTION_CANCEL`, an explicit limited-distance dismissal and `requestHideSelf` without custom surface translation. The original strip swipe remains.
- `ToolbarGlassFrameLayout`: the sharp list clips below the solid title, not below the entire progressive blur ramp. The live overlay continues compositing moving pixels from the same ScrollView and the footer continues sampling the same source.
- `panel_material.xml`: reduced the artificial toolbar bottom padding from 56dp to 12dp. No change to the genuine shared footer or approved Emoji/Clipboard blur.
- `PointerTracker`: held `SYMBOL_ALPHA` cancels tracking the original Symbols key, releases it, then toggles the full numpad with an alphabet return route. Ordinary quick taps and sliding symbol selection are unchanged.

## Verification and remaining device acceptance

- Local source checks: `git diff --check`, 11 Python harness tests; Android/Kotlin compile, Robolectric and exact signing must pass in **both** GitHub workflows before a new APK is presented.
- Focused Android tests: confidence grayscale/contrast and optional readable hue; toolbar single live scroll source with no second full-footer spacer.
- On Oppo Reno 10 5G: test `123` quick/hold/slide/release; full toolbar swipe from title/tiles/open space at list top and downward scroll when away from top; toolbar's first tile and last tile visibility without bars at 60fps; dark/light/high-contrast theme pill and toggling tint with both 3- and 5-chip configurations; Emoji and Clipboard Browse/Search clip/return/footer continuity.
- The exact physical-screen geometry, glow/blur rendering and gesture feel **cannot** be inferred from CI; owner screenshots/recording are the acceptance gate. No screenshot/video content or clipboard query is added to the repository.
- This batch does not replace the offline native prediction engine, alter the Dictate provider, grant network permission, change package identity, or modify original signing.
