# B16B — Next-word quality comparison harness (synthetic-only preflight)

**Terminal source/automation checkpoint (2026-11-17):** Main commit [`55933d2d9cc9a41e000e72a59c3f63105ce85523`](https://github.com/Batmancpu/brrrBoard/commit/55933d2d9cc9a41e000e72a59c3f63105ce85523). Complete debug [44047326546](https://github.com/Batmancpu/brrrBoard/actions/runs/44047326546) and production original-signer [44047326573](https://github.com/Batmancpu/brrrBoard/actions/runs/44047326573) both terminal SUCCESS, including Android unit/build and required Python comparison tests. Public original-signed [v1.0.78 APK](https://github.com/Batmancpu/brrrBoard/releases/download/v1.0.78/brrBoard.apk), SHA-256 `71685e6742210b05dd73c2083f6bb303257d154e64bf13e3b8394448c7c7ea22`. This validates the harness, **not** any prediction gain or Oppo physical behavior. The following historical pending-CI statements describe the pre-commit stage and are now superseded. Next B17A isolated gesture/decoder replay checks are documented in [B17A_GLIDE_REPLAY.md](B17A_GLIDE_REPLAY.md).

Date: 17 November 2026. Owner requested continued development while testing the P0 v1.0.76 panels. Branch `main` only. Do **not** treat manually authored candidate lists as real dictionary output or report their scores as prediction quality.

## Verified starting boundary

- [B16A source `0ed3fe3`](https://github.com/Batmancpu/brrrBoard/commit/0ed3fe364a955ad28b0cbd52cbea52407d0140d6), complete debug/full Android/Python suite [44044335856](https://github.com/Batmancpu/brrrBoard/actions/runs/44044335856) SUCCESS and complete production/full Android/Python/original signing [44044335909](https://github.com/Batmancpu/brrrBoard/actions/runs/44044335909) SUCCESS.
- [Originally signed v1.0.77](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.77) targets exactly `0ed3fe3`; APK SHA-256 `419a96a8626b3726d0876583f6e60baa6c11acfdba8a089b7c40df557e59d284`, `versionCode` 77, same `com.orion.frostkeys.brrrboard` and pinned original signing identity.
- Generated `brrPersonal.tsv`: **5,981 entries, identical** SHA-256 `26de07d1825ce75c8121104a97829b6e414aa179fd8cfe322e7e72123c77ff0a` from GitHub debug job logs; B16A did not silently retune the seed.
- All real Oppo v1.0.76/v1.0.77 panel and typing acceptance remains pending, and the original P0 stack trace was never available.

## B16B harness and security contract

`tools/evaluate_ngram_predictions.py` accepts one JSONL authored *synthetic* candidate comparison; it does not read the keyboard, text fields, logs, clipboard, user history, or native dictionary. The schema is limited to: `id`, `origin` (must be `authored_synthetic`), `split` (train/development/holdout), `category`, `expected`, `baseline` and `proposed` arrays (max three unique short tokens), and `protected` Boolean. An unknown field, including a raw sentence or editor context, is an error. Duplicate IDs are rejected globally across splits.

`evaluate()` reports counts of exact top-1 and top-3 reference matches, any previously reachable top-3 item lost, and any changes to protected categories (`proper_name`, `url_email`). The *offline comparison* gate passes only with no top-1/top-3 aggregate loss, no individual baseline top-3 loss and zero protected candidate changes. It always reports `production_quality_verified=false`, because a synthetic comparator cannot prove actual decoder output, UI speed, or false autocorrection.

`tools/test_evaluate_ngram_predictions.py` runs five synthetic harness-correctness checks: gain counts; reject any lost top-3 despite aggregate gains; protect names/URLs; disjoint split accounting; reject real-context fields/duplicate IDs/non-synthetic origin/unprotected names. Local `python3 -m unittest discover -s tools -p 'test_evaluate_ngram_predictions.py' -v` passed **5/5**. Both GitHub CI workflows now gate on the same checks after Android tests and B16A tests, before original signing. **New B16B CI result pending** until a new main commit triggers them.

## What is *not* concluded or changed

- No candidate lists captured from actual `Suggest`/native dictionary, no real accuracy measurement, no p50/p95, no actual lexical frequency edits and no claim that any ranking proposal improves ordinary typing. Current `Suggest.kt` rank ordering is unchanged by B16B.
- The next honest engineering experiment is an **emulator/host-side decoder replay** using solely authored synthetic previous-word sequences, compare frozen baseline native outputs against a narrowly isolated candidate scorer, then measure on Android hardware. No user/private text collection or unconditional n-gram asset addition.
- If that runner or licensed corpus cannot be provided, leave B16B ranking unchanged and record evidence gap. User has already said ordinary typing is satisfactory; preserving behavior is preferable to unmeasured tuning. **B17 glide quality** is the separately owner-reported visible problem and may proceed via a source-only replay harness without overwriting B16.

## Handoff

Source scope: two new `tools/` files, two workflow test steps, documentation. No new APK behavior. After guarded `main` source push, monitor both new workflows through terminal state, retrieve logs for any failure and correct without bypass, record exact version/hash/signer and source SHA. This checkpoint's automated tests validate the evaluator, **not** the predictor. Keep v1.0.76 P0 physical QA and v1.0.77 B16A independent.
