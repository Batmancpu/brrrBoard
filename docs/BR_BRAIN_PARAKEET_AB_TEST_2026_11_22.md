# BR Brain Parakeet A/B comparison — v1.0.237

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
- Original-signed v1.0.237 is the one-time production update that contains
  both pinned Parakeet packs in one APK. The app continues to use 110M for
  default dictation; Ultra is an explicit Voice Lab install/comparison option.
  Later releases omit these large weights unless the owner asks to bundle them
  again. The older Actions test artifact remains available as a separate
  historical validation path.
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

## Published production update

The combined original-signed APK is published as [v1.0.237](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.237), from merge commit `ed336594e4dea63d8e1a0494d14acb74fda6e09b`. Exact-source Android debug/full-suite run [45001350457](https://github.com/Batmancpu/brrrBoard/actions/runs/45001350457) and original-signer release run [45001350481](https://github.com/Batmancpu/brrrBoard/actions/runs/45001350481) both succeeded. The single `com.mangoloads.board` APK (versionCode 237) is [brrBoard.apk](https://github.com/Batmancpu/brrrBoard/releases/download/v1.0.237/brrBoard.apk), 869,526,797 bytes, SHA-256 `7b347ff9b8b065eae43552635565503103fcd1c2025fdfd1f2f2673981c11a8b`. GitHub's asset metadata reports the same SHA-256. No multi-update installation sequence is needed.

The release includes the SHA-256-pinned 110M and Ultra packs. Ultra stays optional in BR Brain Voice Lab and does not replace default dictation. Future releases omit the large speech packs unless explicitly requested. This update's download is about 830 MiB and Ultra needs about 700 MiB of app storage. ContextLM remains disabled because its corrected fixed-seed holdout failed the Recall@3 gate. Reno10 visual, voice accuracy, latency, memory, and thermal acceptance remain device-only and are not claimed as CI results.

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
