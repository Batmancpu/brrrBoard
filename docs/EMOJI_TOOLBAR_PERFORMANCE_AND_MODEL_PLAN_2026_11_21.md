# Emoji, Toolbar, Performance, and Model Work Plan

Date: 21 November 2026  
Starting source: `c91c9b9645c835a69675857e4fd897f43bff1cdc`  
Scope: owner-directed implementation after the current v1.0.223 release. The goal is a stable, fast, visually coherent keyboard, with no loss of local privacy or existing user data.

## Current state and verified findings

- Smart Emoji already exists. `EmojiPalettesView.updateContextSmartPicks` reads at most 160 characters before the cursor and feeds `BrrSmartEmojiEngine`, which combines reviewed phrase cues, CLDR metadata, recent use, and opt-in encrypted selection patterns. This is local contextual ranking, **not a ContextLM**. It only presents suggestions; selecting one follows the normal insertion path. It currently performs editor-context acquisition and ranking while preparing the UI, so responsiveness and editor-generation safety need a focused audit.
- ContextLM code/evaluation groundwork exists, but the model is not shipped or active. The latest holdout evaluation rejected it because recall lagged the native baseline; the prior engineering ledger records fusion weight 0.0. The next stage is a reproducible quality and integration program, not turning on an unproven model.
- Several prior fixes already removed independent Emoji gap causes: hiding the outer strip before measurement, moving category tabs into the grid shell, removing decorative row padding, retaining same-width native viewport measurements, and top-anchoring Browse. The current screenshot still shows the symptom. The present source audit therefore treats the remaining gap as **not yet physically isolated**. Instrumented, content-free bounds/measure traces across the parent frame, wrapper, Emoji root, fixed category strip, and first row are required; then correct the first divergent parent/child boundary. Do not add another guessed spacer or offset. No host text or screenshot pixels should be logged or persisted.
- The Toolbar screenshot matches the current `GridLayout`/vertical `ScrollView` and `menu_tile_item.xml`: four large card-like actions per row, 56dp icon cards, generous margins, vertical scrolling, and a 50dp content inset after the title. This wastes both width and panel height. Rework it as a compact horizontal rail with visible labels, no large gray card backgrounds, preserved touch/accessibility targets, existing enabled/order preferences, and functioning drag reorder.
- Toolbar and Clipboard already share `PanelGlassOverlayView` top and bottom overlays. The top overlay samples only in-panel scrolling content and applies a progressive alpha mask; `PanelMaterial` is shared. Older Android/OEM fallback is material tint, not real live blur. The screenshot’s upper blurred band can also be the IME window backdrop effect, independent of the panel overlay. Keep the host backdrop, internal content blur, and crisp controls as separate layers; verify visually on device.
- Toolbar overlay currently records a `RenderNode` source during draw. Reuse recording while content/scroll/size are unchanged, and invalidate from existing source-change/scroll notifications, to reduce GPU recording and frame work without caching screenshots or copying sensitive content.
- Existing voice is offline Parakeet TDT 110M INT8 with pinned sherpa-onnx and user-private model installation. The exact larger candidate already investigated in this repository is **Moondream Parakeet Ultra**, based on NVIDIA Parakeet TDT 0.6B v3. The official sherpa-onnx maintainer INT8 export currently reports a **671 MB** repository with a 652 MB encoder, 11.8 MB decoder, 6.36 MB joiner and tokens; this confirms the storage cost is 600-some MB, not merely a 600M parameter label. The upstream model/export pages identify CC-BY-4.0 and 25 languages; that does not establish Hindi/Hinglish accuracy or target-phone latency. Keep this a separately benchmarked feasibility candidate, never an automatic replacement. See the [Moondream model card](https://huggingface.co/moondream/parakeet-ultra), [maintainer INT8 export](https://huggingface.co/csukuangfj/sherpa-onnx-nemo-parakeet-tdt-0.6b-ultra-int8), and [NVIDIA NeMo ASR model documentation](https://docs.nvidia.com/nemo-framework/user-guide/latest/nemotoolkit/asr/models.html).

## Work sequence and acceptance criteria

### 1. Pin down the Emoji gap at its geometry source

1. Extend and use the existing one-shot/content-free `BrrImeBounds` trace to capture screen-relative rectangles of `main_keyboard_frame`, `keyboard_view_wrapper`, `emoji_palettes_view`, `emoji_grid_shell`, fixed category strip, pager and visible first Emoji row. Include parent padding and the owner-level mode/width/window-inset information needed to interpret it. Never record editor text, category search text, clipboard content, or screenshots.
2. Exercise compact Browse with Smart row shown/hidden; initial open, repeated toggle, Alphabet→Browse, Search→Browse, Browse→Search→Browse, Expanded→Browse, portrait/landscape, one-handed, height scale, toolbar hidden/visible, footer visible/hidden, and window inset changes.
3. Fix the first responsible measurement/layout contract, not the downstream child. Keep Browse top anchored to its true available frame body; keep Search and Expanded as separate size contracts; clamp at the parent spec; reject stale-width snapshots. Remove dead compatibility offsets only when tests prove they are unused.
4. Acceptance: categories begin at the Emoji pane’s top edge within the intentional header boundary; first content row follows categories/Smart row without a blank band or overlap; no clipped rows, footer, or controls in the matrix. Software tests do not substitute for owner Reno10 visual acceptance.

### 2. Smart Emoji: reliable context and efficient UI

1. Trace every call site and eligibility rule (password/private/incognito, app/editor changes, empty fields, cursor movement, rapid switching). Bound context to the existing short suffix, keep it RAM-only, and associate requests/results with editor identity plus a monotonically increasing generation.
2. Move context acquisition/ranking off latency-sensitive view construction where practical; keep the previous valid/native rows stable while a request runs. Cancel or discard stale work on cursor/editor/session changes. Avoid per-open view churn and repeated preference/file reads; cache immutable reviewed indexes and bound any worker queue.
3. Consolidate suggestion ranking/deduplication across typing strip, Smart Emoji row, and idle Emoji Search while respecting each surface’s eligibility and capacity. Do not insert automatically. Learn only on explicit Emoji selection; preserve encrypted opt-in personalization, erase semantics, and normal Recents.
4. Add held-out phrase tests (English, Hinglish, names, negation, sensitive fields, punctuation, empty context) and measure first-result latency, allocations, stale-result rate, and UI jank. ContextLM may contribute only as a confidence-gated candidate scorer after it passes the gates in section 6.

### 3. Compact Toolbar rail and panel material

1. Replace vertical action grid with a one-row horizontal scroll rail. Remove filled large gray icon cards; use compact icon/label treatments with clear pressed/focus states. Retain comfortable minimum touch targets, localized labels/content descriptions, haptics, enabled-key ordering, drag reorder, pinned-key transfer, and all current action routing.
2. Remove redundant top/bottom content insets that create dead space while maintaining title separation and clearing the shared ABC/mic footer. Ensure drag insertion/reordering uses horizontal coordinates and autoscrolls while dragging.
3. Share Clipboard’s progressive top mask and panel content sampling; remove any independent static toolbar ribbon/background that duplicates the window backdrop. Increase/reduce the existing Clipboard header fade only from measured screenshots, not by adding another blur layer. Preserve a clear fallback for devices without Android RenderEffect support and honor high contrast/reduced motion.
4. Acceptance: title and rail do not overlap; initial actions are visible; overflow is horizontal; drag works; no gray card grid; labels/touch targets remain accessible; blur tracks content without seams, duplicate dark slabs, flicker, or footer clipping.

### 4. Performance and reliability audit

1. Profile and inspect hot UI paths: keyboard open/first key, suggestions, Emoji switching/rendering, toolbar/clipboard scroll, voice start/stop, preference reload, editor connection calls, image/font work, allocations, and repeated layout passes. Separate cold-start from steady-state and 60/90/120Hz budgets.
2. Reuse draw/render state and immutable prepared data; avoid full-panel redraw/invalidation where a clipped region is sufficient; bound executors/queues and cancel obsolete work. Keep expensive file/model initialization out of input callbacks. Avoid caching editor, clipboard, or microphone payloads.
3. Audit lifecycle/races: editor generation, stale async prediction and Emoji result, recycled view click tags, view detach, configuration/IME insets, voice cancellation and child process failure. Keep defensive fallback paths and user data migration intact.
4. Run focused regression tests, full Android unit suite, static checks and exact-source CI. Report measured data and unverified physical gates separately.

### 5. Voice candidate: Parakeet 0.6B v3 feasibility and migration plan

1. Identify the exact upstream checkpoint/export and runtime format that works with the pinned Android sherpa-onnx. Record source revision, model/tokenizer/config hashes, CC-BY-4.0 attribution obligations, export/conversion provenance, and whether each required derivative is permitted. No unverified community conversion is trusted.
2. Build a non-shipping offline benchmark candidate outside the default APK. Measure archive/download and extracted size (quantized candidate likely remains hundreds of MB; parameter count is not file size), peak RSS, cold-load time, real-time factor, thermals, battery, cancellation, streaming/chunk behavior, and 1/2/4GB-class memory behavior on the actual Oppo Reno10.
3. Compare WER/CER and named-entity/code-switch accuracy on a consented, owner-reviewed English/Hinglish/Indian-accent test set with matched audio against current Parakeet 110M. Publicly reported European-language support does not prove Hindi/Hinglish coverage. Keep test audio local, opt-in, RAM-only, and eraseable.
4. Plan user-visible model selection/download only after install/update, offline operation, attribution, storage budgeting, checksum/signature, interruption/resume, low-storage rollback, model compatibility, and existing 110M fallback are proven. Do not replace or delete the current working model until the larger candidate wins quality and device gates. No new network microphone/text path; any model download must be explicit user action with transport and integrity checks.

### 6. ContextLM integration plan and release gate

1. Preserve the native decoder as the immediate source of truth. Reproduce the held-out evaluation with immutable train/dev/test splits and report candidate recall, top-1 accuracy, calibration, English/Hinglish/code-switch and privacy cohorts. Compare native-only, current neural candidate, and fusion weights without training/test leakage.
2. Audit model/data provenance, license, privacy risk, encrypted personalization interaction, model size, ABI/runtime/operator set, CPU/RAM/thermal use, and p50/p95 latency on the actual device. ContextLM never receives raw app/field identity or unbounded editor text.
3. Run shadow scoring only after candidate quality beats/meaningfully complements native; log aggregate metrics only, never text. Then enable an opt-in, confidence-gated rerank of native top-K while native candidates remain instantly visible and rollback is immediate. Retain user personalization and do not activate in protected fields.
4. Smart Emoji integration may consume only the same bounded, eligible context snapshot and vetted semantic features; it cannot introduce free-form generation, silently type an Emoji, or delay ordinary keypresses. Require its own held-out Emoji relevance and latency win.
5. If no model passes, keep the existing reviewed phrase/CLDR/native path and report ContextLM as not activated. A neural label or inert adapter is not an integration result.

### 7. Broader blind-spot checklist

- RTL, Hindi and other Indic input, system font scale, screen readers, touch exploration, large touch targets, color contrast, reduced motion, high-contrast/system theme, and low-end GPU/OEM fallback.
- Emoji Unicode 15/CLDR availability, unsupported glyph fallback, skin tones/ZWJ sequences, Recents ordering, no-result state, and privacy/password/incognito suppression.
- Keyboard panels under navigation gesture/3-button insets, split/floating/one-handed layouts, landscape, display cutouts, rapid mode transitions, and small-height devices.
- Toolbar actions preserve existing semantics and order; pinned rows remain reachable; drag/drop handles touch exploration; labels do not truncate to ambiguity.
- Clipboard and recovered transcripts keep current privacy boundaries; blur never captures host-app pixels or becomes a screenshot/clipboard cache.
- APK and update compatibility, original package/signing certificate, model checksum/rollback, user settings and encrypted data preservation, release notes, and offline operation.

## Batch tracking

| Batch | Scope | Completion evidence |
|---|---|---|
| A | Plan + baseline audit | This document and source audit complete; root cause awaits device trace |
| B | Emoji geometry diagnosis and repair | Existing one-shot `BrrImeBounds` probe is already wired on Emoji entry; it reports content-free bounds. Actual Oppo boundary and corrective source remain **open pending device measurement**. |
| C | Horizontal compact Toolbar and progressive material/performance | **Source candidate implemented:** horizontal compact rail, no large cards, less dead inset; shared top fade now begins translucent and decays progressively. CI/drag/device review pending. |
| D | Smart Emoji latency, cancellation, quality and robustness | Initial source candidate now reuses one bounded editor-context query/ranking pass for Smart row + Recents. Async lifecycle guards, held-out quality and profiling remain open. |
| E | ContextLM evaluation/integration decision | Reproducible holdout report; activated only if it wins gates |
| F | 0.6B Parakeet feasibility | Export/license/device benchmark; model migration only if it wins gates |
| G | Full verification and delivery | Exact-SHA CI, original signature/release if gates pass, release notes, device checklist |

## 21 November execution checkpoint

Plan and initial UI batch are now in the working tree. `AccessPointMenuView` uses a one-row `HorizontalScrollView` with transparent 32dp icons and compact text labels; touch selection, the existing enabled ordering, and GridLayout-backed drag/drop code remain connected. `PanelGlassOverlayView` and the `PanelMaterial` fallback both use an immediately translucent, monotonic top fade shared with Clipboard and Toolbar. Layout XML parses and `git diff --check` passes. `:app:testDebugUnitTest` could not start: the Gradle 8.14.4 wrapper distribution is absent and network sockets are blocked in this environment. No APK was built or signed here.

The exact Emoji gap remains unpatched: source already has the prior geometry fixes and actual window/frame/widget rectangles have not been captured from the Reno10. The existing one-shot `BrrImeBounds` probe now also includes the pager, first visible Emoji row, and frame padding, without reading or logging key/text content. A device readout is the input for Batch B, not another guessed offset. The source now obtains the bounded Emoji context once and shares one ranked list between the Smart row and Recents ordering, removing the prior duplicate editor call and ranking on Emoji open. This preserves the synchronous native path; async cancellation and model work remain later gates. All new source changes remain unbuilt at this checkpoint.

The final report must identify completed software, published APK/release if CI and signer permit it, and owner-only Reno10 checks. Never claim device acceptance from repository tests.
