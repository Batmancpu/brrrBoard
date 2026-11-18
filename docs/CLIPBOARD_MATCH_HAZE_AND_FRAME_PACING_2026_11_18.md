# Clipboard Search — measured row, match-centered preview, accent haze, and frame pacing

## Release gate status and immediate palette correction

Focused source `78115ce7708d71d8939cc70ca7100c0d74e79401` passed original signer `44282918004` and published [original-signed v1.0.122](https://github.com/Batmancpu/brrrBoard/releases/download/v1.0.122/brrBoard.apk), SHA-256 `d067270dfdefdfcfec245777d3e5d9e7aa0f0e23049925a130d6cf65e4e0c8f9` (38,118,508 bytes). Debug `44282918098` first failed exclusively at 25 Robolectric `MavenArtifactFetcher` runtime SDK downloads; the failed-job rerun `134731958824` ended **SUCCESS**, including unit, B16A/B16B checks, on the exact same source. A subsequent audit found that `SPECIAL_KEY_BACKGROUND` is a neutral modifier color on the default palette, not the user's accent. The *next adjacent candidate* changes Clipboard card match, query text glow and haze to `ACTION_KEY_BACKGROUND`, the same accent role as Enter, and explicitly uses `repo.maven.apache.org/maven2` for Robolectric runtime test fetching. No production source or test bypass and no OEM-frame-rate claim. This next candidate still requires its own full CI, signer and phone validation; the historical candidate wording below is preserved as a dated pre-release snapshot.

Owner request: 18 November 2026, screenshot of signed v1.0.121 running on Oppo Reno10 5G. **This is a source candidate; Android CI, original-signed next APK, real frame timing and visual acceptance are pending.**

## Starting verified release

- `main` `2abb323a003329a817ad943e2ca5b665ee616288`, signed [v1.0.121](https://github.com/Batmancpu/brrrBoard/releases/download/v1.0.121/brrBoard.apk), SHA-256 `19c8f79ccf1eeed834771636baf94fb2a9afce3da8510f78d921b9bb1dba5b8d` (38,118,508 bytes).
- Complete debug/Android tests [#216](https://github.com/Batmancpu/brrrBoard/actions/runs/44279097296) SUCCESS and independent original signer [#121](https://github.com/Batmancpu/brrrBoard/actions/runs/44279097332) SUCCESS. Owner reports horizontal scrolling and Search/ABC work, but transition fluidity is still poor and a blank vertical area remains above the alphabet.

## Verified source causes, not guesses about the device GPU

1. `ClipboardHistoryView.applySearchResultsLayout(true)` hard-codes a 96dp RecyclerView at 48dp below the field. `ClipboardAdapter.searchCardHeight()` hard-codes an 80dp card. The Search panel independently reserves the native alphabet typing height **plus** a 160dp expansion and docks the alphabet at the **bottom**. Therefore its cards cannot consume the available space. This explains the screenshot's dead band even when the panel itself expands correctly.
2. `ClipboardHistoryView.launchHeightMotion()` uses `ValueAnimator.ofInt`, calls `requestLayout()` on **every animation frame**, and its `onMeasure()` propagates the changing exact height into the entire view subtree (RecyclerView cards, `MainKeyboardView`, glass/RenderNode/host wrapper). Android's rendering guidance identifies unexpectedly costly Layout/Record View#draw frames as jank sources. This is a concrete avoidable workload; no on-device frame trace was supplied, so it does **not** prove that every dropped frame has this cause.
3. `ClipboardAdapter` previously bound the first 1,000 characters of a copied item and limited Search to three lines. Thus a legitimate substring match late in a long note was invisible even while search returned the correct card.
4. `clipboard_search_clear` was a separate sibling of the Search EditText with visibility toggled `GONE`/`VISIBLE`. Every transition changed the field's weighted width and required another layout. It also looked visually detached from the field.

## Coherent correction

- Search RecyclerView becomes a **single horizontal lane filling the measured space** from the 48dp header to the actual reparented alphabet keyboard top. A keyboard-height layout listener updates the row's bottom margin on layout/rotation/keyboard mode changes, and cards stretch to this lane. The normal Clipboard grid remains unchanged.
- `ClipboardSearchPreview.window()` is a bounded view-only source→visible substring mapping with case-insensitive selected range, Unicode-surrogate boundary hygiene, leading/trailing ellipsis and up to 136 original UTF-16 units. Full source clip, pin and paste behavior stay intact. The matching source characters become accented, bold, softly shadowed and highlighted in the card; the search field adopts the accent when matches exist.
- The clear button is now **inside the same search glass capsule** with stable reserved text padding and composited alpha/scale/translation entry/exit; its visibility no longer changes header width. A small accent-gradient haze behind the typed query pulses briefly on real matches; reduced-motion/disabled animations retain a static state. This is an original Android approximation, not an Apple proprietary Liquid Glass shader or macOS system cursor implementation.
- Clipboard height motion measures the larger endpoint immediately on grow, keeps the current larger endpoint during collapse, and animates only `clipBounds` and contained view translations. Collapse performs **one final** `requestLayout()` when settling, rather than a full layout on every frame. Cancel/detach/rapid mode-switch resets clip/translation generation. No change to the native IME backdrop blur, Emoji-only dark/light tint, original footer, sign key, or host editor text.

## Source references

- Apple [Liquid Glass matched geometry transition](https://developer.apple.com/documentation/swiftui/glasseffecttransition/matchedgeometry): related glass elements derive geometries in a shared container; our single-capsule X is an Android-inspired approximation.
- Apple [system text cursor during dictation](https://developer.apple.com/documentation/appkit/adopting-the-system-text-cursor-in-custom-text-views): native macOS text cursor features blue glow and trailing fade; we apply only decorative search preview haze, no false claim of iOS system integration.
- Android [slow rendering and FrameTimeline](https://developer.android.com/topic/performance/issues/render) and [ViewPropertyAnimator](https://developer.android.com/develop/ui/views/animations/prop-animation): layout cost and composited property transitions; real Reno10 Perfetto FrameTimeline still needed to quote FPS.

## Automated/phone gates

1. `ClipboardPanelLifecycleTest`: horizontal lane `MATCH_PARENT`, 48dp header offset, normal grid restoration, clear X inside unchanged weighted capsule.
2. `ClipboardSearchPreviewTest`: match late in a long note is visible, selected text preserves original case, no query/no hit has no highlight, Unicode emoji boundaries intact.
3. Full Android unit/debug workflow and independent existing original-signer workflow must both succeed before an APK link. No local Android SDK/Gradle installed in this workspace.
4. Oppo: Search idle, first typed character, clear morph, matching and nonmatching words, long copied passage, live paste contents, horizontal swipes, ABC exit, 3-color themes, light/dark/landscape, accessibility reduced motion. Video at 30fps cannot certify 60fps; use Perfetto FrameTimeline to measure p50/p95 actual UI/RenderThread frame durations. Do not claim 30/60fps without traces.

## Next work after the release

User separately prioritized previously planned #9 contextual Emoji suggestions, #8 offline Describe relevance, and #12 conservative dictionary/next-word evaluation. Their preliminary pure Java source is intentionally **unstaged and unreleased** during this narrow Clipboard quality batch so a visual correction does not silently tune ordinary typing. Revisit after Clipboard APK passes both gates, preserving prior exact context/history. Remaining Emoji/other-panel spring issues require independent physical testing and may need the same endpoint-clip architecture.
