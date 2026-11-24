# Stabilization batch 1 — offline voice cancellation and safe Send

Date: 2026-11-24. Base: `d7e4199cfe7e09fc4c0129adf538f258fcc58c9c`, released as v1.0.259 with tested continuous voice chunks.

## Confirmed source gap
`BoardFooterView.updateHoldCancelTarget()` played the special important-action confirmation only when the finger entered a drag-Cancel target. The Cancel status label used ordinary `feedback(view)`; accessibility Cancel and other explicit paths did not consistently use the special haptic. The callback already distinguishes explicit `USER_CANCEL` from lifecycle/editor `OTHER`; route feedback through this single owner.

## Implementation scope
- `OfflineBoardDictation.finish()`: trigger the existing DND-aware important-action confirmation only once on active explicit user discard. Preserve discard animation and do not claim a haptic on normal stop, completed recognition, stale editor or failure.
- `BoardFooterView`: offline Cancel label does not play a duplicate ordinary key-tick; session exit owns its haptic.
- `OfflineDictationAutoSendPolicy`: check requested hold-to-talk, valid destination, current privacy mode and actual IME `SEND` action. Caller still commits final valid transcript before attempting editor action.
- Add JVM regression cases for action masking and all explicit guards.

## Unchanged safeguards
Preserve original-signature in-place updates, `com.mangoloads.board`, no Internet permission, installed private Parakeet 110M/Ultra models, brrClean, 170-second microphone cap, streaming PCM and composing guards, incognito final-only behavior, and no transcript persistence.

## Verification states
Source: `30e86d270a979945efda9da828e93fd6f39ee876`. Exact-SHA full debug/unit CI: [45328763013](https://github.com/Batmancpu/brrrBoard/actions/runs/45328763013) **success**. Original-signed release: [45328762985](https://github.com/Batmancpu/brrrBoard/actions/runs/45328762985) **success**, [v1.0.260](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.260) contains the exact commit. APK: 63,544,172 bytes, GitHub SHA-256 `cb4a694da22e3ecd7e0bcf21ecc4be1f55d2851adccc1d1853b052f762576c04`. Oppo Reno10 physical interaction: pending. Existing unreleased private gesture branch deliberately not merged.

## Manual device checks after a successful release
In a normal editor, start tap-dictation; Cancel with the label, then via mic during decode, and verify the same important haptic. Long-press mic, drag into Cancel and release; only an explicit discard deletes interim text. Press Stop: do not see discard animation/haptic; the transcript remains and does not auto-send. Hold-to-talk in a Send-capable editor should send only once with opt-in/setting enabled; Go/Search/Done, changed focus or incognito must not auto-send. Recheck two-minute uninterrupted dictation, no-network permission and installed Ultra discovery. Accessibility and DND must be checked physically.
