# BR ContextLM-Hinglish v1 — personalized integration execution ledger

**Owner authorization:** 21 November 2026 — monitor `main` workflows, require a continuous five-minute period with no Actions runs, then begin ContextLM implementation; meanwhile audit efficiency, privacy, bugs and crashes. This GO supersedes the recent ContextLM plan-only boundary, not the independent voice/QuickType workstreams.

## Confirmed preimplementation gate

- GitHub repository: `Batmancpu/brrrBoard`; `main` HEAD at gate: `7d856d619d753347a22e75610ed496a79d32994a`.
- Initial live 21 Nov check: both debug run `44882039159` and original-signed Obtainium run `44882039152` were `in_progress`. Debug ended `success`; signed ended `success` and published `v1.0.214`.
- At **07:55:52 UTC** the first idle poll had zero active runs. Follow-up checks including Actions' `in_progress`, `queued`, `waiting`, `pending` and `requested` filters stayed zero through **08:01:00 UTC**. Five-minute uninterrupted idle condition satisfied. Fresh `main` HEAD confirmed at gate. Local implementation began afterward.
- Reference shipped APK before this work: `v1.0.214`, **70,900,211 bytes**, original `com.mangoloads.board` signing lineage. **None of this ContextLM work is in that APK.**

## Architecture decision: the user's old learning must be an actual LM input

Native AOSP/HeliBoard suggestions remain immediate and authoritative for typed literal, correction, swipe candidate pool and sensitive editor policy. A new read-only adapter accepts the already-computed native candidate list with score, source and n-gram context; it exposes candidate features **before neural scoring**. Existing private `UserHistoryDictionary` binary files stay intact, and no app-wide enumeration/export/migration/extra keystroke capture is allowed. Only `Dictionary.TYPE_USER_HISTORY` and explicit `TYPE_USER`/user shortcuts are personal model signals; contacts/apps/host completions are excluded from neural input. Off/incognito/no-learning editor means no personal features and no stale cache reuse. Model outputs are only ranked probabilities; native autocorrect safety cannot be bypassed.

Target self-trained causal DFSMN: 5 memory layers, 128 channels, 4 taps, tied candidate embeddings and a four-feature personally conditioned score head. INT8 tensor storage with explicit shapes, scales, manifest SHA-256. A **pure-Java CPU reference** avoids another large dependency/JNI crash surface, but mobile latency/RSS has **not** been measured. Never call reference execution approved until target-phone and failure-injection tests pass. A production coordinator will use one bounded background worker, immutable editor/cursor generations, hard deadline, no late strip flicker, wipe on privacy transition and a native-only circuit breaker. Do not run it while a voice decode/memory-pressure event threatens keyboard responsiveness.

## Sources and training prerequisites

- `COMI-LINGUA`, research-preview CC BY 4.0 dataset, pinned Hugging Face revision `0214cb358e59e36da60e01d3d59ddd24df897749`. Locally downloaded **only** `MT_train.csv` and `TN_train.csv`; train script uses one English and Romanized-Hindi human-annotated translation per source row, plus source and one normalized Hinglish variant. No dataset CSV belongs in the repository or APK.
- `HinGE`, CC BY 4.0, pinned revision `e25a10382d9adb77f3a4f03b721fbd0a43c0de4c`. The human-generated Hinglish list and English parallel sentence are used; verify subcorpus IITB parallel-source rights separately before redistributing derived weights.
- Verify all annotations' source rights, Llama-derived outputs, train/eval partition independence, public-source PII filtering, attribution and model-card provenance before any bundled weights. Personal phone text, contacts, clipboard, voice/audio, app labels and private dictionary are **never training data**.

## Local source work, no GitHub push as of current ledger

- `brain/BrPersonalContextFeatures.java`: bounded `NgramContext` + 12 native candidate feature input; direct learned-history / user-dictionary bits, native score/rank, contacts/app/host exclusions; **no persisted private history copy**.
- `brain/BrContextLmInt8.java`: bounded asset reader, SHA-256 checks, strict dimensions/architecture, CPU-only reference evaluator; no packaged model yet.
- Tests `BrPersonalContextFeaturesTest.java` and `BrContextLmInt8Test.java`: personal OFF, editor protection, inherited history, contacts exclusions, corrupted model and model-head use of learned features.
- `tools/contextlm/train_dfsmn.py`: fully local corpus parsing/partitioning, candidate training, checkpoint, quantized export and manifest. `tools/contextlm/check_quantized_parity.py` verifies INT8-vs-float scores. `tools/contextlm/evaluate_candidate_recall.py` provides only an **offline 4-gram proxy**, never claims native IME quality or Oppo latency.

## Actual experimental evidence / failed gates

1. Initial experiment used random negative words. About **1.134 million parameters**, `weights.int8` **1,134,216 bytes**, quantized score top-1 agreement **99.61%** against float reference over 512 generated cases; this only tests export parity.
2. The random-negative-trained model appeared strong on its **synthetic** dev candidate sets (top-1 73.33%, ablation without personalization 68.27%), but failed a more realistic held-out n-gram candidate comparison: among sampled `tn` Hinglish examples, **model Recall@3 13.42% vs n-gram 23.23%**; among `mt`, **22.89% vs 36.67%**. HinGE sample was small (68 cases) and also worse. **This is a model-training failure, not a shipping quality success.**
3. Corrective training samples **hard candidates from observed offline n-gram continuations** instead of random words. After three epochs, synthetic development top-1 reached **30.47% vs synthetic native proxy 14.53%**, but the more honest held-out ranking STILL FAILED: `tn` 20.20% vs n-gram 23.19%, `mt` 32.24% vs 36.17%, `hinge` 13.25% vs 16.87% (Recall@3; 802/915/83 cases). These dev and held-out sample distributions differ; the dev percentages must never be represented as true predictor improvement.
4. `calibrate_fusion.py` tested native+neural blending, selecting weight only on 1,600 dev examples, then scoring an independent 1,600-case held-out proxy. **The best dev blend was 0.0 neural / 1.0 native**, 31.50% Recall@3, and the untouched holdout native baseline was 30.625%; any positive neural fraction reduced overall dev quality. This is a direct evidence-based STOP for enabling the current neural scorer. The training-export manifest is explicitly marked **NOT FOR APK** and the Java runtime loader rejects unapproved manifests. There are still **no pretrained weights in the app**.
5. Repeated 512-case quantized-parity check on the corrected hard-negative model: **100% top-1 agreement** versus floating-point reference, worst score delta **0.02717**. This only tests numerics; the model still fails held-out ranking and real native/Oppo criteria.
6. Local Gradle 8.14.4 cannot bootstrap because `services.gradle.org` is unreachable; JVM/Android compilation and the 500+-test suite require exact-SHA GitHub debug CI. No emulator/adb or actual Oppo access exists in this environment. Do not claim physical installability or latency until evidence.

## Safe APK-size optimization discovered while model remains gated

The settings/search icon face was `material_symbols_rounded.ttf`, a **15,080,092-byte** four-axis variable font; both callsites explicitly used exactly `FILL=0`, `GRAD=0` (default), `opsz=24` (default), `wght=400`. Instancing that face at the same exact coordinates using FontTools `varLib.instancer` produces a **~1.7 MB fixed font**, while retaining all 6,593 glyph names and 4,383 mapped code points. All **4,008 mapped outlined glyphs** compare equal at the original default-axis locations; the `rlig` ligature feature is retained. The two callsites now use the fixed face without unsupported variation flags. Original source SHA256 `619eaa2f2e270723cf0cd06d2aba126ade66fee94e380c3e6a61e2e496279e0c`; transformed file SHA256 `ecd70522170cd8c953f197e1c88fc998ea7864c3b090e39485536a1c5a4cb8d0`. Raw file saving is **~13.4 MB**, but real compressed APK-byte savings remain unmeasured until signing; UI differences must still be checked on Oppo. No language, font glyph, voice model, dictionary, animation, signing or package content was removed. Font license remains as originally bundled; review attribution if required.

## Crash / stability / efficiency pre-mortem

| Failure | Preventive implementation and test |
|---|---|
| Late inference reorders or makes strip blank | One latest-generation result only; no model on keypress; reject same-field cursor changes, locale/layout, composing state and timeout. Native results always render immediately. |
| JNI/runtime crash brings down IME | Prefer bounded pure-Java reference first and limit load to a background thread; verify hashes, dimensions, OOM guard and fail closed to native. Do not load a new unproven JNI engine in `LatinIME`. |
| GC/jank/thermal spikes | Limit batch and token/candidate counts, warm after commit, cap active queue at one, reuse buffers if profiling demands; pause during voice decode, secure editor and keyboard hide. |
| History not inherited or accidentally erased | Existing `UserHistoryDictionary` queried directly; no migration/export; synthetic prior-version install/upgrade fixture, OFF/toggle and model ablation tests; no deletion of old dictionaries. |
| Sensitive app/contact leakage | Strict prose `EmojiContextEligibility` gates, `mUsePersonalizedDicts` OFF removal; exclude contacts/apps/host-completion source from model, prevent logs/telemetry/network. |
| APK bloat | Compare original-signed byte size with **70,900,211 B** baseline; INT8 raw model ~1.14 MB; pure Java runtime candidate avoids four-ABI ORT libraries; first reduce a redundant 15 MB variable icon font to its identical-style static 1.7 MB instance. Measure the actual signed asset difference and verify Settings icons. No language/voice feature deletion. |
| Offline quality regression | Training must pass real native candidate Recall@3 and English/Indian English/Hinglish holdouts; the current first trial failed. Do **not** package or enable a neural model just because synthetic accuracy improves. |
| Build/release failure | Main-only, normal fast-forward expected-head update; update release notes and all handoffs; require exact-SHA full debug/unit success then original-signed Obtainium, APK provenance/permission/size check and device acceptance. Never reuse an old release artifact. |

## Next actions

1. **STOP live ContextLM activation**: both trained models failed public held-out proxy comparison, and dev-calibrated fusion selected native-only. Explore larger/cleaner curated data, authentic decoder-supervised examples and calibrated personal-history behavior until measured improvement is real.
2. Build/test the independent exact-style static-font APK reduction and inert fail-closed scoring scaffolding in same-source Android CI, with no model bundled and no change to displayed predictions. Produce signed installer only after source/unit/privacy/asset gates; identify this as a **preparatory release**, not a model-improved release.
3. Validate Java INT8 runtime golden vectors and executor benchmarks; only once real candidate quality passes, integrate async shadow scoring using live native features without visual changes or privacy regressions.
4. Confirm model licensing/provenance, build compact next-word index only if candidate recall improves, and require same-source original-signed size, privacy and real Oppo p95 before making any neural ranking active.

**Unverified now:** JVM compilation, model real-quality gain, target-phone latency, final signed APK size, install/update, and any user-visible ContextLM improvement. No files pushed to GitHub yet.
