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
  export. Exact-source Android debug/full unit CI passed on the feature branch;
  the device's runtime, latency, memory and WER acceptance still require the
  Reno10 comparison.

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
main. The exact-source debug/full Android suite passed for source
`778d63c4cd4a0d2d0c27f72c2d5646bd6fc6cc62` in run
[44995213924](https://github.com/Batmancpu/brrrBoard/actions/runs/44995213924).
The A/B artifact was built as package `com.mangoloads.board`, production
versionCode 236, and verified against the production signing certificate. Its
APK is 869,514,509 bytes with SHA-256
`d35a1c4252f055d8fc63238ec847e068b1f9dbbbbc133efaec061bd34947b352`.
[Download the 14-day Actions artifact](https://github.com/Batmancpu/brrrBoard/actions/runs/44995213924/artifacts/14743318266)
(artifact ZIP 606,500,553 bytes; ZIP SHA-256
`f1f6e6af32b31f640d64759bf71edcd41ac32405e751e3fe5ee96bcae3acb94e`). This
is not a GitHub/Obtainium release. A later source-only workflow fix adds
smaller checksum-verified chunks because the connected artifact downloader
caps a single download at 512 MiB; that exact-source build is running on
`e9c6392fe642586e57f86532793eb908371f17c1`.
Physical Reno10 install, memory, thermal, English/Indian-accent WER, latency
and A/B comparison remain unmeasured. ContextLM stays disabled: after fixing a
training-candidate mismatch and expanding to 250,000 pairs, its new fixed-seed
holdout still tied top-1 and lost Recall@3 against the proxy in every language
family. Detailed evidence is in `QUALITY_GATE_REEVALUATION_2026_11_22.md`.

The test APK is intentionally large. Installing Ultra needs roughly 640 MiB
plus 64 MiB safety headroom in app storage, in addition to APK space. A fresh
install also needs about 130 MiB plus safety headroom to install the 110M
model. An existing install reuses the already verified 110M copy. Any
low-storage refusal leaves existing model files and settings untouched.

## Emoji layout artifact

No emoji file is needed. After downloading the four checksum/part artifacts,
extract the three part ZIPs into the same folder and run
`bash tools/join-ultra-abtest-apk.sh <part-folder> <output.apk> <APK-SHA256>`.
Use the SHA-256 file from the run (or the exact APK hash recorded above); the
script refuses to leave an output APK if the bytes do not match. On the
installed app, open Emoji Browse once, then go
to **Settings → BR Brain → Emoji layout report → Copy geometry report** and
paste that text into the conversation. The report is content-free view geometry
intended to locate the top blank band; it does not include typed text or
clipboard contents.
