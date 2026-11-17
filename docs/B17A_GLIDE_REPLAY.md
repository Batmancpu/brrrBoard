# B17A — Glide decoder replay boundary (synthetic-only)

Date: 17 November 2026. Owner authorized the next batch while testing the Oppo P0 panel release. Branch: `main` only. **This is not a replacement glide library or a claim of improved swiping.**

## Stable starting gate

- B16B [`55933d2d9cc9a41e000e72a59c3f63105ce85523`](https://github.com/Batmancpu/brrrBoard/commit/55933d2d9cc9a41e000e72a59c3f63105ce85523) has complete debug `44047326546` and original-signing production `44047326573` **terminal SUCCESS**, public [v1.0.78](https://github.com/Batmancpu/brrrBoard/releases/download/v1.0.78/brrBoard.apk) SHA-256 `71685e6742210b05dd73c2083f6bb303257d154e64bf13e3b8394448c7c7ea22`. No ranking or swipe runtime behavior changed in B16B.
- Physical Reno10 acceptance of P0 panel v1.0.76 is still pending. The actual device Clipboard exception stack trace was not available; the source-level wrong-scope RecyclerView lookup was fixed and automated tests passed. Do not equate either later signed build with phone acceptance.

## B17A implementation boundary

`app/src/test/java/helium314/keyboard/keyboard/internal/GlideReplayHarness.java` is compiled exclusively into the **local Java unit test** source set. It introduces `Fixture(id, locale, layout, width, height, canceled, gestureEligible, expected, points)` with normalized finite coordinates, monotonically increasing elapsed pointer times and up to 256 sampled points. The only decoder dependency is an injected `Decoder` interface returning at most three unique, bounded candidates and non-negative measured microseconds. Gesture eligibility and cancellation are authoritative; canceled/non-glide traces are skipped without calling the decoder. The runner reports eligible/decoded/skipped/no-result counts, exact top-1/top-3 and nearest-rank p95. Zero measurements yield p95 `-1` and cannot masquerade as a fast decoder.

`GlideReplayHarnessTest.java` provides five authored-synthetic JUnit 4 tests for single decoder dispatch and top-1/top-3, portrait/landscape geometry and null results, duplicate IDs or malformed coordinates/timelines, invalid response candidates/latency, and cancellation/normal-drag non-dispatch. All fixture words and coordinates are invented; no text, clipboard, typed history, IME settings, telemetry or network data is read. The harness is **not** an Android IME instrumentation test, and its fake decoder timing is not on-device latency or real suggestion quality.

No changes are made to the production pointer pipeline, original imported Google/GlideLib binary, `LatinIME`/`InputLogic`/`Suggest` selection, candidate insertion, app permissions or APK assets. Existing swipe works as before, including its owner-reported inaccurate suggestions. The compiled test fixture is never shipped as runtime code.

## Next technical evidence, before runtime replacement

1. Audit existing importer and installed native library licensing/provenance (keep the currently working import in place). Evaluate an independently cleared, pinned Apache-2.0 candidate (e.g. relevant [FlorisBoard source](https://github.com/florisboard/florisboard)) only after specific source/component rights and assets are verified; do not copy Google/Gboard or another proprietary model.
2. Add a **test-only adapter** from these normalized fixtures to one actual decoder at a time. Capture authored paths at known layout geometry and locale. The same traces and layout dimensions must be replayed against baseline and candidate to permit comparison. Author own non-private Hinglish, close-shape, repeated-letter, short/long, cancellation and off-key strokes. Make fixture sampling independent from human editor text and never store unconsented real strokes.
3. Gate on real decoder outputs: exact top-1/top-3 on disjoint traces; false-positive ordinary drags; canceled/swipe-to-space interaction; case, 123, Delete and landscape; p50/p95 after touch-up, native loader/ABI behavior, memory/frames and offline privacy. Check source provenance and four-ABI/16 KB loader constraints before any packaged native change.
4. Only if measured and cleared, design a separately reviewable integration with a rollback switch, original signing, CI and physical Oppo acceptance. Until then **do not automatically switch a running user gesture to an untested decoder**.

## Automated and physical gate matrix

| Evidence | B17A | Status |
|---|---|---|
| Java test harness dispatch/validation/top-3 | Five JUnit 4 synthetic tests | Source staged; CI pending |
| Android unit suite and APK package unchanged | Both debug and production workflows | Pending |
| Original production signer and in-place upgrade | Pinned signing action | Pending |
| Actual imported decoder exact output/quality | JNI replay adapter / held-out traces | **Not measured** |
| Warm/cold swipe p95, jank and memory | Android emulator/owner Oppo | **Not measured** |
| v1.0.76 panel crash and material visuals | Oppo Reno10 owner test | **Not physically accepted** |

## Resume and acceptance protocol

After a guarded main-only commit, wait for both full workflows to terminal outcome, collect exact job failures if present, and verify signed tag/asset hash. Replace this pending section with exact commit/run IDs/hash. A signed release of **test-only code is an engineering checkpoint**, not a reason to require the owner to retest/install another APK or a promise that swipe recognition is better. Keep the more important v1.0.76 physical Clipboard/Toolbar/Emoji report open for next corrective work.
