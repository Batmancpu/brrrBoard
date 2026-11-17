# P0 — live internal glass, category paging and Emoji Search (2026-11-17)

## Scope, references and precedence

Owner's 17 November assignment supersedes the older B10V *release-time* category switch and the B14V/P0 simulated `PanelMaterial` gradient. The source of truth remains current main; this is a corrective batch, not a B17 glide rewrite.

All **three owner recordings were available and inspected from sampled frames**:

- `VID_20261117_132807(1).mp4`, 37.41 s: Clipboard scrolling, duplicated ABC (action-row key and global footer), abrupt masking, Emoji grid and lower black region.
- `VID_20261117_133840(2).mp4`, 41.84 s: title `Keyboard toolbar`, translucent tile overlap with title, pronounced gray/black bands, Emoji category replacement, Search showing only the first two typing rows and an unused dark field.
- `ScreenRecording_11-17-2026 13-47-25_1(1).mp4`, 9.83 s: orange message moves behind a fixed ChatGPT header on iPad; the local header warms as content passes while labels remain sharp. This is **behavioral reference**, not an asset for distribution.

Baseline main: `41643b2840635cf1bf1be709008721102ff9f887` (B17A synthetic, test-only glide). Last documented original-signed verified release: `v1.0.78` from B16B; do not infer B17A was signed. Prior P0 `v1.0.76` had source/CI-signed crash recovery but physical Clipboard re-entry was never accepted.

## Architectural change implemented in source candidate

### One source, three visual regions

`PanelGlassOverlayView` uses the **real panel scrolling view** as its rendering source. With Android 12+/hardware acceleration, it records the relevant source pixels into a reusable `RenderNode`, applies HWUI `RenderEffect` blur to those pixels, and composites a theme veil over them. It does not blur an empty overlay, capture the host app, save bitmaps or create new Emoji/card views per scroll frame. The sharp central scroll, glass copies and fixed controls follow the *same view/scroll offset*. The top uses stronger progression for title legibility, and the bottom starts at a clean horizontal boundary. This does introduce extra GPU recording/overdraw; it must be profiled on Oppo rather than called free.

`PanelMaterial` supplies common theme parameters and source binding to Clipboard/Toolbar/Emoji. It expands the **real** scroll viewport by the 48 dp footer height in the non-clipping panel layout, so source contents can move beyond the action row toward the global ABC/mic footer. `BoardFooterView` adds a shared live-glass backdrop under its sharp controls, with `InputView` binding the currently visible panel source. The fixed glass overlays consume touches; foreground controls are later siblings. Legacy non-HWUI/Android below 12 has a theme-material fallback, **not** actual live blur.

When the Clipboard panel closes, its top/bottom GPU display lists are released; the global footer drops the previous panel source when normal typing resumes. `RenderNode.endRecording()` now runs in `finally` to avoid a poisoned recording state on exceptional draws.

Additional scroll-frame correction (after compiler repair): a generic `OnScrollChangedListener` alone is not guaranteed to observe `RecyclerView` child-offset animation. The active Clipboard `RecyclerView.OnScrollListener`, Emoji vertical row scroll callback, Emoji `ViewPager2.onPageScrolled`, and Toolbar `ScrollView` callback now explicitly invalidate their own top/bottom overlays **and the shared footer's glass** on motion. Each overlay re-records the current source offset on its next hardware draw. This is essential to keep blurred pixels moving with the finger; only physical-device/video acceptance can confirm the perceptual result. These callbacks do not allocate new bitmap/list items.

Source owners:

| Responsibility | Files |
| --- | --- |
| Reusable GPU/internal glass | `PanelGlassOverlayView.kt`, `PanelMaterial.kt` |
| Global footer sharp controls plus internal backdrop | `BoardFooterView.kt`, `InputView.java` |
| Clipboard source/list/header/key action | `ClipboardHistoryView.kt`, `clipboard_history_view.xml`, `clipboard_bottom/*.json` |
| Toolbar source/tile/header | `AccessPointMenuView.kt`, `access_point_menu.xml`, `strings.xml` |
| Emoji source/category pages/search | `EmojiPalettesView.java`, `EmojiCategoryPagerAdapter.java`, `emoji_palettes_view.xml` |
| Search body height contract | `SharedPanelGeometry.kt` |

### Emoji continuous horizontal paging

Prior B10V `RecyclerView.VERTICAL` swapped one adapter's category on `ACTION_UP` and could never show the neighbor during drag. The new horizontal `ViewPager2` retains adjacent whole-width pages, each holding a separately virtualized **vertical** `RecyclerView` using the existing row-adapter/keyboard. Native pager motion follows finger position, snaps at rest and shares category state/selected tabs. It retains in-memory first-row positions for pages and updates saved page state. A normal drag is bounded to a neighboring page by ViewPager2's default snapping. First/last bounds remain pager bounds. The old policy source/test is historical compatibility, not the active switch implementation.

### Emoji Search functional recovery

Search body height is measured as the complete alphabet keyboard plus its search field and **one** 56dp result row (capped by available window bounds). Search's results keyboard is constructed with **one row** instead of two, and excess results are horizontally scrollable. A visible `No matching emoji` state replaces the all-black region on genuine zero matches. Reparented main keyboard is no longer explicitly set to alpha 0. Existing key listener still inserts search keystrokes only in local `EditText`, while selecting Emoji commits to the host editor. Old Emoji state and normal IME shell are restored on exit.

### Visual/control repairs

- Toolbar title resource is now `Toolbar` (not `Keyboard toolbar`), with existing fixed centered header/tile actions.
- Clipboard bottom action layout removes the **extra alpha key**; the global ABC remains the return route.
- Emoji bottom search/space/delete action row is inside the full-height grid shell so the live Emoji source continues underneath its fixed key row.
- Original footer `ABC`, Emoji and microphone vectors have stronger optical weight; same control dimensions and callbacks.
- No Apple-owned asset, font, new permission, external capture, signing change or dictionary change was introduced.

## Verification and acceptance boundary

### Initial CI failure and compiler repair

Exact first source tree `c23b9ec7b397659f182a24d3ddcf1650821095ed`, GitHub main commit `83733642c4e16dd10a09f2c6baf6dff6f31f20c1`. Both initial gates **FAILED at compilation**: [debug 44053381015](https://github.com/Batmancpu/brrrBoard/actions/runs/44053381015) `:app:compileDebugKotlin`, [production 44053381018](https://github.com/Batmancpu/brrrBoard/actions/runs/44053381018) unsigned build. Log showed `AccessPointMenuView.kt:66:40 Cannot infer type for type parameter T` and `:66:79 Unresolved reference parent`: a generic `findViewById(R.id.access_point_scroll)` call with no expected type was unnecessarily used instead of the already-typed `scroll` field. One-line follow-up changes to `scroll.parent as View`; no test or certificate gate was weakened. This is a source compile correction only; replacement CI still pending.

Source-only checks to run before committing: `git diff --check`, JSON/XML syntax verification, exact remote-main lease, and code/hierarchy tests (`SharedPanelGeometryTest`, `EmojiPanelCompositionTest`, prior Clipboard lifecycle test). Local `./gradlew --version --offline` fails because Gradle 8.14.4 is absent and the network cannot reach `services.gradle.org`; Android compilation and all unit tests must run on the **existing full GitHub debug+original-signing CI** before publishing. No APK/commit/workflow success is asserted here before it is observed.

Physical Oppo 10 5G must still verify source/GPU behavior: yellow/red item truly traverses under action keys and ABC/mic without hard seam, upper title readability, touch exclusion including TalkBack virtual Emoji keys, portrait/landscape/insets, drag threshold/fling/vertical arbitration, complete Search bottom rows, insertion/focus, Clipboard image and text lifecycle, 10+ reentry cycles, three themes, FPS/overdraw/memory. Shader/RenderNode redraw and view overflow were not tested on physical hardware. **Do not mark this group visually accepted because CI passes.**

Separate remaining known issues: weak real glide predictions (B17), Dictate provider audio capture (B18), and broader on-device typing/IME inset acceptance. Any new build failure is to be repaired without weakening unit/signing gates; record the corrective commit and final run IDs in this file.
