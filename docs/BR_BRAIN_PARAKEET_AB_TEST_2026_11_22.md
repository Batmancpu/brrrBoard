# Offline voice model selection and Parakeet A/B comparison

## Keyboard model selection

Open **Settings → Everyday tools → Offline voice** to choose the verified model
used by keyboard dictation. Parakeet Ultra 0.6B is selected by default when
installed; Parakeet 110M remains selectable as the smaller, lower-memory option.
If a selected pack is unavailable, the keyboard safely falls back to the other
verified installed model. Selecting a model applies to the next dictation.

The same screen has one **Voice cleaning · brrClean** switch. It is on by default;
turning it off selects raw audio. A failure in brrClean falls back to the original
audio. The Voice Lab's per-trial noise buttons are independent and do not change
the keyboard preference.

## Same-audio comparison

Tap **Compare voice models** on the Offline voice page. The Voice Lab records one
maximum-12-second mono 16 kHz PCM take in volatile memory and runs installed
110M and Ultra sequentially on that same take. The first model alternates between
trials to reduce order bias. Ultra runs in a private child process so a native
crash or model-load OOM does not take down the keyboard process.

The lab displays each transcript, reference-based word error rate (WER),
model-load/decode/engine-total timings, end-to-end wall time, real-time factor,
and sampled peak process PSS. Transcripts stay on screen only. There is no
Voice Lab result-copy action and no audio file, transcript log, or network path.

## Model files and release packaging

- Ultra is the pinned sherpa-onnx maintainer INT8 export. Four model assets total
  670,576,835 bytes (about 640 MiB), with size and SHA-256 checks at fetch,
  packaging, and private installation.
- Model files remain in GitHub. Normal updates omit them after the one-time
  model update; verified copies in the app's private no-backup storage survive
  APK updates. The release workflow keeps an explicit `bundle_ultra_model`
  option for a future update only when explicitly requested.
- A model can be installed only when the current APK contains its pinned files.
  Installation checks free storage, verifies each file, and preserves existing
  installed models if it cannot complete.

## How to exercise the models

1. Open **Settings → Everyday tools → Offline voice**. Confirm Ultra is selected
   and brrClean is on.
2. Tap **Compare voice models**.
3. Set the reference to exactly match a short phrase, record it once, and tap
   **Compare both on same audio**.
4. Review both transcripts and their accuracy, latency, and memory results.
   Use the same phrase and conditions for later comparisons.
5. For the complete device audit, return to **Settings → Device checkup** and
   follow all seven checks. One final button copies the whole privacy-safe text
   report. It includes only numeric A/B results, not the transcripts, reference,
   typed test text, audio, or clipboard contents.

## Device evidence and remaining limits

The owner-supplied Reno10 A/B run showed Ultra WER of 12% versus 25% for 110M on
that one phrase. Ultra used more memory and took longer to cold-load/decode; this
is one device/take, not a general accuracy guarantee. The v1.0.238 Reno10 report
marked Emoji spacing, suggestions, panel glass, and A/B as good, and recorded a
toolbar issue. The owner then said the reported items are fixed. The next
checkup revision adds explicit checks for model selection, voice cleaning, and
important DND haptics. ContextLM remains disabled because its corrected
held-out quality gate did not improve the native baseline.

The initial A/B artifact came from source
`5997430406b91edf1e2a1682d6a8bacae67581b4`, verified by exact-SHA Android CI and
the production signing workflow in run
[44999224034](https://github.com/Batmancpu/brrrBoard/actions/runs/44999224034).
That test artifact is historical; production model packaging is governed by the
current release workflow and remains lightweight by default.
