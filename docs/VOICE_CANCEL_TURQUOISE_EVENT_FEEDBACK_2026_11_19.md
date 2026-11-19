# brrBoard offline microphone: event-correct discard and turquoise decoding

> **Superseded visual motion:** The user approved a subsequent silhouette-shaped
> red underlight and descending white-bin-to-white-mic 520ms cancel morph.
> Refer to [MIC_SHAPED_GLOW_DISCARD_MORPH_EMOJI_VIEWPORT_2026_11_19.md](MIC_SHAPED_GLOW_DISCARD_MORPH_EMOJI_VIEWPORT_2026_11_19.md)
> for current appearance. The exit-policy and privacy requirements below remain valid.

## Owner's updated contract

- The English ASR model is already bundled in normal brrBoard updates. Do **not**
  introduce a separate LLM, model download during keyboard use, or extra voice app.
- Cancel must dismiss the visible `Cancel` status immediately; show a tasteful,
  brief trash/discard effect near the mic only when the user actually canceled an
  active recording or pending decode.
- While audio is being decoded, show **no** `Transcribing privately on device…`
  or other footer status text. The stationary mic surround and subtly pulsing
  **turquoise mic glyph** are the indicator; retain the accessible description
  and accessible Cancel action.
- On successful insertion, normal completion and silent lifecycle cleanup must
  leave the footer empty and never play the cancellation animation.
- No Liquid Glass, unrelated input/theme change, internet permission, model
  replacement, app ID/signing changes, or brrSpeak handoff.

## Exact state ownership

| Real event | Internal state | Footer status text | Mic treatment | Discard feedback |
| --- | --- | --- | --- | --- |
| Idle | `IDLE` | None | Existing themed mic | Never |
| Start/capture | `STARTING` / `CAPTURING` | Existing tap-to-Cancel while recording | Blue recording glyph, fixed ring, glyph pulse | Never |
| Stop and decode | `DECODING` | **None**, cleared synchronously | Turquoise (`#009F97` light, `#4DE5D1` dark), soft glyph-only pulse | Never |
| Explicit Cancel in active session | Exit from active phase | **None**, cleared synchronously | 390 ms trash-glyph rise/dissolve, then idle mic | **Once** |
| Empty/stale cancel | Already `IDLE` | None | Idle | Never |
| Successful decode/commit | `DECODING` → `IDLE` | **None** | Idle | Never |
| Error | Failure → `IDLE` | Short scoped error only | Idle | Never |
| Editor change/IME hide | Lifecycle → `IDLE` | None | Idle | Never |

`OfflineBoardDictation.cancelFromUser` alone emits `USER_CANCEL`. Normal
`cancel()` and all success/error/editor cleanup emit `OTHER`. The pure
`OfflineDictationFeedbackPolicy` requires `USER_CANCEL` **and** a previously
active phase. Every exit still increments `generation`, cancels the native
request, closes capture, and discards buffers as before. A stale callback
cannot insert text after cancellation.

`BoardFooterView.showDictationProcessing` removes the old status and its click
handler and switches tint before restarting its existing, bounded mic-only
ValueAnimator. `showDictationStatus` rejects any late message during processing.
The circle stays geometrically fixed, and the processing glow comes from the
mic glyph, not the outer ring. All accessibility actions remain explicit.

The trash icon reuses the project's original `ic_bin_rounded` vector in a
non-interactive 48dp overlay aligned to the existing microphone. The icon
enters over 101 ms, settles briefly, rises by 5dp and dissolves by 390 ms;
the idle mic crossfades back at the end. If system animations are disabled,
show a stationary trash icon for 220 ms. New recording or footer removal cancels
any lingering animation and restores the mic immediately. The overlay never
changes the keyboard's measured size or intercepts mic touches.

## Model investigation and on-demand release

Existing pinned model: NVIDIA Parakeet TDT-CTC 110M English, INT8 ONNX with
sherpa-onnx. Four asset files total **136,490,421 bytes (~130.2 MiB)** from
`tools/offline-voice-model.lock`; hashes checked during debug and signed
release. Both `.github/workflows/brrrboard-build-debug.yml` and
`.github/workflows/obtainium-release.yml` build the model into **each** APK,
also supporting the existing manual `workflow_dispatch` trigger. No new model
download workflow or package split is needed.

Official candidate sources:

- NVIDIA Parakeet card: https://huggingface.co/nvidia/parakeet-tdt_ctc-110m
- Whisper.cpp models: https://github.com/ggml-org/whisper.cpp/blob/master/models/README.md
- Whisper.cpp runtime and memory notes: https://github.com/ggml-org/whisper.cpp
- sherpa-onnx supported ASR models: https://github.com/k2-fsa/sherpa-onnx

Whisper Base English is ~142 MiB, but switching also changes runtime and RAM
cost; that disk size alone does **not** establish better accuracy or latency.
Other compact multilingual ONNX options likewise lack a fair device-specific
comparison with the pinned Parakeet. Keep the working model. Only change the
model in a future separate version if a licensed, offline, <=250 MB packed
model beats it on recorded short English/Indian-accent, punctuation, noise,
silence, timing, CPU/memory, thermals and battery tests on Oppo Reno10 5G.
Require measured quality improvement, not model-card marketing.

## Verification and deployment gates

1. Pure Kotlin unit assertions: explicit-active cancellation gate, no idle /
   success / error / lifecycle animation, bounded 390 ms glyph keyframes,
   turquoise tint distinct from recording blue and legacy red.
2. Build-time Python contract: the offline microphone is still the only live
   route, processing never calls the old visible status, cancel is explicit,
   normal APK still bundles verified weights without network permissions.
3. Android compile, whole unit suite, both exact-SHA CI gates, old pinned
   signing identity and normal Obtainium release required before delivery.
4. Physical Oppo Reno10 check: stop/cancel from capture and mid-decode,
   cancel with screen reader, successful insertion, tiny/no speech, rapid
   repeated taps, app/editor switch during work, hide keyboard, background /
   foreground, dark/light themes, reduced-motion, and long dictation. Measure
   no stale status, duplicate insertion, icon clipping, or mic touch loss.

No physical-device acceptance is claimed by source-only/CI verification.
