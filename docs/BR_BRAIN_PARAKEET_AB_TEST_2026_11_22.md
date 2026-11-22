# BR Brain Parakeet A/B test (next update; not released)

## What the test does

The Voice Lab records one maximum-12-second mono 16 kHz PCM take in volatile
memory. **Compare both on same audio** makes a copy of that exact take and runs
the currently installed Parakeet 110M and optional Parakeet Ultra 0.6B INT8
sequentially, cold-loading and releasing one recognizer before loading the
other. The first model alternates between comparison trials to reduce order
bias. Ultra runs in a private child process so a native crash or model-load OOM
does not take down the keyboard process.

The result card reports each transcript, word error rate (WER) against an
editable reference phrase, model-load/decode/engine-total timings, end-to-end
wall time, real-time factor, and sampled peak process PSS. A user-initiated
**Copy comparison results** action copies transcripts and measurements only;
it never copies audio. Audio and in-app result state are not written to files
or sent over the network. The existing 110M remains the default dictation
model and is never removed or replaced by the test.

## Model/package controls

- The Ultra export is pinned to sherpa-onnx maintainer revision
  `402e1190ce85b9793118ee223db21fb0d12f52db`. Four assets total 670,576,835
  bytes (about 640 MiB) and are size/hash checked at fetch, APK verification,
  and private installation.
- The ordinary update and Obtainium release workflow never contain Ultra. A
  dedicated, original-signed A/B test APK is produced as a GitHub Actions
  artifact for this feature branch only. It contains both pinned model packs
  so fresh and upgraded installs can compare them. The workflow verifies and
  signs the APK but has no release-publishing step.
- On device, the BR Brain lab offers a user-confirmed install only when the
  Ultra pack is present in that APK, checks available storage, verifies the
  copied assets, and keeps the 110M untouched if installation fails.
- The build pins sherpa-onnx 2.0.2, the documented runtime candidate for this
  export. This candidate has not yet passed an Android build or Reno10 device
  acceptance in this workspace.

## How to exercise it after a test APK is explicitly issued

1. Open BR Brain → Voice & Noise Reduction (Voice Lab).
2. Pick English or Hinglish and edit the reference so it exactly matches the
   words you will say.
3. Record one clean take, then tap **Install Ultra test model** and confirm.
4. Tap **Compare both on same audio**. Wait for both cold runs to finish.
5. Review both transcripts and WER/speed/memory results. Repeat with new takes;
   model order alternates. Copy results only if you want to share the text.

The speech model is English-trained. Hinglish scores are useful as a device
comparison but are not a language-quality guarantee. Results from other
devices or public corpora do not predict accuracy, speed, or memory on the
owner's Reno10. In particular, the host FLEURS benchmark must not be presented
as a Reno10 result.

## Current state and release gate

Implementation is on feature branch `feature/parakeet-ultra-brain-abtest`, not
main. Its prerequisite debug build and full Android unit suite passed at exact
source `97e5b981e246c29a7d1958db92b89b5a7ec10a89` in run
[44994076639](https://github.com/Batmancpu/brrrBoard/actions/runs/44994076639).
The branch now builds a separate installable A/B artifact with the original
signer and the current production version code. It updates the existing
`com.mangoloads.board` app in place, preserving settings and the installed
110M model; the existing 110M pack is also included for fresh installs. The
artifact job uploads only to Actions and never creates a release. Its exact
APK size, hash and download link will be recorded after that job succeeds.
Physical Reno10 install, memory, thermal, English/Indian-accent WER, latency
and A/B comparison remain unmeasured. ContextLM stays disabled under its
separate failed quality gate.

The test APK is intentionally large. Installing Ultra needs roughly 640 MiB
plus 64 MiB safety headroom in app storage, in addition to APK space. A fresh
install also needs about 130 MiB plus safety headroom to install the 110M
model. An existing install reuses the already verified 110M copy. Any
low-storage refusal leaves existing model files and settings untouched.

## Emoji layout artifact

No emoji file is needed. On the installed app, open Emoji Browse once, then go
to **Settings → BR Brain → Emoji layout report → Copy geometry report** and
paste that text into the conversation. The report is content-free view geometry
intended to locate the top blank band; it does not include typed text or
clipboard contents.
