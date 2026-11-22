# ContextLM and Parakeet Ultra quality-gate re-evaluation

Date: 22 November 2026
Baseline app release: original-signed v1.0.231. Follow-up containing the tooling/report path: original-signed v1.0.234 from source `4d42fa598a6fd1686f6682dce8dee6e121958418`, exact-SHA debug `44982800637` and signer `44982800542` successful. APK is 64,985,587 bytes, SHA-256 `1d42db4fd79845f936ad20bb8bd59e2aba7225b06b716b74075300fcae9d5db6`.
Purpose: continue the owner-approved implementation, record actual model evidence, and keep every result distinct from Reno10 acceptance.

## Emoji geometry and Smart Emoji implementation

The unresolved Emoji top gap is now collectable without ADB. After opening the keyboard and switching to Emoji Browse once, the latest content-free measurement is saved asynchronously in the app's `noBackupFilesDir`. In **Settings → BR Brain → Emoji layout report**, one tap copies the report; it contains only allow-listed geometry, inset, panel-search, keyboard-mode and toolbar-mode fields. Copy is user initiated, and an Erase button removes the local sample. It rejects unknown fields and nonnumeric values. The remaining root-cause repair still depends on the owner's Reno10 measurements; this change does not guess an offset.

Smart Emoji no longer makes a synchronous `getTextBeforeCursor` Binder call or reruns its scorer as the Emoji panel opens. The eligible typing pipeline computes a capped list from its existing bounded n-gram context and stores only the ranked Emoji snapshot on `SuggestedWords`; the panel reuses that snapshot only for the identical `EditorInfo` instance. Protected/incognito eligibility, explicit Emoji insertion, and user-controlled learning behavior remain in force.

## ContextLM re-evaluation

Reproducible data preparation is provided by `tools/contextlm/prepare_public_corpora.sh`. It pins the COMI-LINGUA and HinGE revisions, exact sizes, and SHA-256 values; public corpus files stay outside Git and the APK. A source audit found that the candidate ranker's training helper invented random per-user history/personal features, although evaluation and production had to pass zero for those fields. The train helper now keeps these features zero because no consented user-history labels exist in the public corpus.

The neural ContextLM remains **off and unbundled**. The prior model's held-out n-gram proxy showed lower Recall@3 than the n-gram/native proxy (tn 20.20% vs 23.19%; mt 32.24% vs 36.17%; Hinglish 13.25% vs 16.87%). The proxy is not the Android native decoder. The corrected 1-epoch, 80,000-pair training run removed synthetic-history features and scored a fixed 3,000-case untouched corpus holdout. Its neural Recall@3 remained lower (tn 22.43% vs 23.83%; mt 35.01% vs 36.86%; Hinglish 23.19% vs 23.91%); top-1 tied or lost by 0.07 percentage points. A fixed-seed 3-epoch experiment worsened the same holdout (tn 19.99%, mt 32.63%, Hinglish 18.84% Recall@3), so additional epochs are not a fix. Development-only calibration selected a neural fusion fraction of **0.0**; holdout fusion exactly matched the n-gram baseline, providing no measured neural improvement. The candidate-recall quality gate correctly remains red. Actual native decoder candidates, phone latency, and privacy/upgrade checks remain required.

The rerun is not being called complete on the strength of an attractive synthetic top-1 score. The production gate remains actual held-out native-candidate Recall@3/top-1, false autocorrection, target-device p95/memory, and opt-in/privacy acceptance.

## Parakeet matched benchmark

`tools/voice/benchmark_parakeet_pair.py` runs both models through sherpa-onnx 2.0.2 greedy CPU decoding on the identical local 16 kHz mono WAV/reference manifest, applies the same Unicode/case/punctuation normalizer, reports WER and latency, and computes paired utterance-bootstrap intervals. It prints aggregates only. Audio, transcripts, and hypotheses stay local.

The 300-clip paired run used randomly selected, unique English FLEURS test clips (6,744 reference words), with both models decoded on every same clip. Ultra INT8 WER was **6.05%**, current app Parakeet 110M INT8 WER **9.56%**; paired-bootstrap relative WER change was **−43.9% to −29.3%** (seed 2827, 5,000 resamples). On this Linux host with four CPU threads, Ultra's p50/p95 decode times were **2.962/5.372 seconds** versus **0.851/1.495 seconds** for the 110M model; observed RTF was **0.316 vs 0.090**. Ultra improves US-English read-speech accuracy while taking about 3.5x longer in host CPU RTF. This is not Indian-accent, spontaneous speech, Android runtime, or Reno10 evidence. FLEURS test set: [Google FLEURS](https://huggingface.co/datasets/google/fleurs).

Pinned models used: current app `tools/offline-voice-model.lock`; official sherpa-onnx Ultra export revision `402e1190ce85b9793118ee223db21fb0d12f52db`, with a 652,282,345-byte encoder, 11,845,274-byte decoder, 6,355,277-byte joiner, and 93,939-byte tokens file. The Ultra export's license is CC BY 4.0. Encoder/decoder/joiner/tokens SHA-256 values for the shipped 110M model are `0f35509ddeb9b39002fb077d979a9fe74f06eb0bc4dd5c34f512f82e5111d657`, `f7c331c5504c2e593c76ed22b728e3f554af6c4a383dde862e719ced08b1da19`, `bf7dff69e9f2cdbe9943d70da358f38b361c115ba0105bae7e908e0d6ec782f6`, and `450e56bd2f036fe5b6aa821865838cc5aa9d8b0106134ce9a9ba0664abe6cd10`; Ultra's are `b43072e1b721f18a3df03fcfeb763ff7c4debdf29843598a3aba2f703e2780a3`, `1fab98fe6c12aded87d2da66272cc9e148d0a0044ce3850a12fe56302ec4a922`, `8a71aaccfdba3d451775507a889afd2257d2e754463fd81095a8c8f290f924c2`, and `d58544679ea4bc6ac563d1f545eb7d474bd6cfa467f0a6e2c1dc1c7d37e3c35d`. A matched FLEURS result alone cannot clear the North Indian English / Reno10 latency and memory gate. Therefore no model switch, 671 MB APK, or 110M deletion is claimed in this source candidate.

## Remaining device-only acceptance

1. Install the verified app update over the current Reno10 installation; confirm settings and existing local model state.
2. Open Emoji Browse, then use the one-tap copy action above. Paste the geometry report into the active conversation. Use it to isolate the first parent/child gap and implement the corresponding layout fix.
3. Visually inspect Emoji/Toolbar/Clipboard material, safe-area behavior, accessibility scaling, reduced motion, touch exploration, Toolbar reorder, and the prior Apple-style suggestion motions.
4. Run same-audio Indian-English / Hinglish voice comparison and measure cold load, p50/p95 decode, RSS/PSS, thermals, and long-recording behavior on the Reno10.
5. Record owner acceptance only after those measurements; CI and a Linux benchmark cannot stand in for the device.
