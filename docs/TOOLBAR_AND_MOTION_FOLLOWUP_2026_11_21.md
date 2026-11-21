# Toolbar layout, suggestion motion, and remaining gates

Date: 21 November 2026  
Baseline: original-signed v1.0.230 (`78acc83e`)  
Status: source changes for the four-column Toolbar and Toolbar-footer Settings are in progress; no new build or release is claimed yet.

## Implemented in this source change

- The full Toolbar is being changed to a four-column, row-major grid with a three-row minimum viewport and vertical overflow scrolling. Tiles are taller with larger icons and no gray icon-card fill.
- Settings is promoted to grid position one and remains there after drag reorder or pinned-key insertion. The footer slot that normally holds dictation opens Settings while the Toolbar panel is active; the regular typing footer still starts dictation. The footer has a Settings content description and equivalent accessibility action.
- Drag-edge autoscroll and drop targeting are being converted from horizontal coordinates to the vertical viewport, including insertion positions in additional rows.
- Existing suggestion motion was audited before adding animation. `SuggestionWordMorph` already performs finite, interruptible transitions for changed visible suggestions and the confidence pill, including five-chip rows; initial keyboard-open suggestions intentionally appear without an entrance. Reduced-motion and complex-script fallbacks remain enabled. Exact iOS 27 model/timing is not public, and there is no measured Oppo acceptance trace, so changing this path without evidence risks making typing feel slower.
- The source already contains a content-free `BrrImeBounds` geometry probe and same-width compact Emoji viewport handling. No device trace is available to isolate the remaining top band; do not add guessed padding or claim it fixed.

## Remaining items from the broader plan

1. **Emoji top band — physical diagnosis still open.** Install the current signed build on the Reno10, open Emoji Browse once, and capture `adb logcat -d -s BrrImeBounds:I | tail -n 1`. Compare screen coordinates of the measured frame, wrapper, Emoji panel, category strip, and first row; fix the first boundary that diverges beyond its real parent inset. The earlier candidate cause was a ColorOS measured IME frame taller than the alphabet-plus-strip sum. The latest code reuses an observed same-width frame, but that does not prove which boundary causes the remaining band on the owner's current device.
2. **Smart Emoji lifecycle and quality.** Contextual phrase ranking, CLDR metadata and opt-in local personalization already exist, and redundant ranking on the strip hot path was removed in v1.0.230. Audit editor/cursor generations and cancellation across panel open, editor switch, rapid typing and privacy states; move remaining context acquisition off view construction where safe; benchmark held-out English/Hinglish/name/negation cases and device latency/allocations. Never insert a suggestion automatically.
3. **ContextLM.** Keep disabled and unbundled. Existing held-out results lost to the native baseline and selected native-only fusion. Reopen only after independent held-out candidate-quality gains, privacy/provenance review, and actual-device CPU, memory, thermal and latency gates.
4. **Parakeet Ultra.** Keep the current offline Parakeet TDT 110M. Benchmark the exact 0.6B/Ultra export against matched consented English/Hinglish audio on the Reno10, including WER/CER, load/stream latency, RSS, heat, battery and APK/storage impact. Parameter count is not model file size; current official quantized export is hundreds of MB. Do not replace or bundle it until it wins and licensing/provisioning gates pass.
5. **Apple-inspired motion acceptance.** The current candidate implements visible suggestion replacements, retargeting, confidence-pill transitions, search-result reveal and reduced-motion behavior. Verify fast replacements, empty-result disappearance, chips, tap identity, RTL/Emoji shaping, TalkBack and frame cost on the Reno10; compare with a calibrated high-frame-rate reference if exact timing is requested. Apple does not publish its private keyboard ranking model or current animation timing.
6. **Device performance and reliability.** Gather privacy-safe p50/p95/p99 key-to-first-visible-suggestion, Emoji open/rank, toolbar scroll and voice start/finalization measurements on the target phone, plus thermal/repeated-switch and crash checks. Emulator/CI results do not establish ColorOS blur or microphone behavior.
7. **Release gate for this patch.** Run full Android compile/tests and static gates on the exact final SHA, then the original-signer release workflow. Verify package, signer, monotonically increasing version, APK checksum and GitHub release asset before calling it shipped.

## Current evidence boundary

The source animation is an Android implementation inspired by documented Apple behaviors; it is not Apple's private model or an exact timing reproduction. Repository geometry tests and the existing bounds logger cannot substitute for the owner phone. ContextLM and the Ultra voice model remain explicit evidence-gated future work, not missing code that should be enabled speculatively.
