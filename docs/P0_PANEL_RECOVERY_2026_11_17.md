# P0 Clipboard crash and unified panels — 17 November 2026

## Evidence and precedence

Owner-supplied recording `VID_20261117_112839(1).mp4` (194.14 seconds) inspected locally. At approximately 00:00–00:30 the IME disappears on Clipboard selection and Android shows **“brrBoard keeps stopping”**; a text-field retap does not immediately restore the keyboard. At approximately 00:30–01:45 Toolbar has clipped first/last tiles, an unnecessary Back arrow and Emoji rather than ABC in the footer. At approximately 01:50–02:55 Emoji has two ABC controls and horizontal content movement. This is physical evidence of a crash, not a Java stack trace. Exact installed version is not visible; last verified signed baseline is v1.0.75/`fbbc6b1`, and the crash-producing B14V layout is present in that source.

Comparison with pre-B14V `d6354bf` shows a new callback in `ClipboardHistoryView.initialize()` inside `clipboardRecyclerView.apply { ... }`. Its unqualified `findViewById(R.id.clipboard_header_scrim)` resolves against **the RecyclerView receiver**. But the header scrim is a **sibling** in `clipboard_history_view.xml`; `ClipboardHistoryRecyclerView.setAdapter()` synchronously invokes that callback through `checkAdapterContentChange()` after `clipboardAdapter` is bound. The null result gets `.visibility`, producing a reproducible null-dereference path regardless of whether history is empty or populated. This source-level causal path fits the immediate IME crash; the actual Oppo logcat stack trace is unavailable without the device and must not be invented.

## Corrective source group (P0 + B10V)

1. `ClipboardHistoryView` binds the sibling header directly from the **panel root** before installing the callback; listener uses that exact reference on each empty-state change. `ClipboardPanelLifecycleTest` inflates real panel XML in Robolectric, verifies sibling ownership, and repeatedly detaches/re-attaches a RecyclerView adapter through empty/nonempty cycles. No history DB or sensitive user content is captured in this test.
2. `PanelMaterial` and `panel_material.xml` centralize theme-aware top fade and straight-edged bottom translucent material, with no host-app second blur or screenshot capture. `ClipboardSurfacePolicy` delegates to these shared stops. Toolbar, Clipboard and Emoji use the same source; theme color is refreshed on each panel open. The bottom layer is **simulated glass/occlusion**, not genuine live internal backdrop blur. Content still scrolls behind the fixed material; sharp controls are above or outside it.
3. Toolbar now has a fixed centered title, scrollable four-column grid with accessible first/last reachability padding, and no upper-right Back arrow; the pinned functional tools are retained. The persistent footer shows ABC in all three panels, and tapping it sends `KeyCode.ALPHA` directly.
4. **B10V supersedes the old horizontal B10**: Emoji uses row-major virtualized `RecyclerView.VERTICAL`, with rows constructed from the existing category keyboards, preserving keys/variants/Recents/Smart/B11–B13. Horizontal category gestures are discriminated from vertical scroll by axis dominance, distance and velocity; at most one category advances per gesture. Tapping category icons works unchanged. The first visible row is remembered per category for the current in-memory panel instance; saved page preferences remain a bounded initial fallback. No artificial horizontal snap. The duplicate inner ABC in both Emoji layouts becomes the existing `emoji_search` key, routed to the existing search UI; bottom-left persistent ABC remains.
5. Transitions use restrained 160 ms fades and honor Android's disabled animation setting. The system controls swipe-down IME dismissal; no IME translation added. Old signing key, package, themes, action tint, B07V rounded icons and B11–B13 suggestion behavior are untouched.

## Automated gates and environmental limits

- Baseline GitHub debug run `44034657924` and original production signer `44034657851` independently reported terminal **success**; that proves prior source/CI only, not Clip runtime acceptance.
- Local source diff hygiene: `git diff --check`; JSON/XML resource parse and source tests should precede push.
- This scratch environment has **no adb, Android SDK, javac or Gradle distribution**, and no connected Reno10 device. Do not claim emulator GPU effects, actual live blur, TalkBack experience or Oppo crash recovery have been validated locally.
- Add/retain Robolectric Clipboard panel callback test, pure Emoji row/index and gesture tests, footer-mapping test, and material alpha invariant test. The complete Android unit suite and both main-branch CI workflows must reach terminal success before any original-signed consolidated release is considered complete.
- CI run IDs, exact source SHA, v1.0.76+ release asset digest and signer identity are **pending** until their actual results are available. Do not replace them with guesses.

## Oppo Reno 10 5G acceptance (after signed consolidated release)

1. Open Clipboard from normal keyboard and footer arc; repeat 10 cycles with empty and populated clips, hide/reopen the IME, then repeat in landscape. Verify no crash/dialog and focus restores in one tap.
2. Check plain text, very long text, content URI image, mixed history, pin/delete/Undo/Clear. Confirm image paste only on apps supporting rich content and no raw private clip leaks.
3. Toolbar: fixed title; no Back arrow; all tiles reachable by vertical scroll, including last row; persistent ABC goes straight to normal letters. Try rapid Toolbar↔Clipboard↔Emoji transitions.
4. Emoji: vertical in-category scroll, one-category left/right swipes and icon taps, diagonal gestures that stay vertical, bottom search opens query field, only one ABC, Recents/Smart/skin tones/variants. Check portrait and landscape.
5. Recheck OG Frosted Glass, iOS Clear and iOS Tinted in light/dark conditions, reduced motion, large font and TalkBack; note any lower-glass seam or interaction occlusion. Physical-only items remain **pending** until owner evidence arrives.

## Boundaries and non-goals

No external-app bitmap sampling, proprietary Apple assets, new permissions or AI network usage; no B16–B19, no changed signing identity, no B13 new-candidate injection. The visual approximation intentionally favors stable typing performance over expensive per-frame blur. Preserve future diagnostics without logging copied content or private text.
