# Dictionary and offline context update

## Native dictionary and candidate safety

The repository's v202 policy reads the shipped English tries but cannot write a v202 file. This update uses the approved additive fallback. It keeps both original English binaries byte for byte, including terminal flags, shortcuts, raw bigram records, and locale selection. The public native supplement still uses the existing on-device writer and native decoder. The reviewed delta is in `app/src/main/assets/brrPersonal_modern.tsv`. Its `prefix` entries receive native completion with a matching prefix and may be offered after a supported phrase. Entries marked `valid` remain spelling-valid. The seven entries moved from the Indian English validity list gain prefix or contextual eligibility. Their automatic replacement remains blocked for the supplement. The existing PHINC seed and its notice remain in the build.

Only the derived public supplement uses the new v5 filename. The app continues using the existing filesystem and preference keys for imports, device dictionaries, native history, user shortcuts and swipe learning. No data-copy migration is needed for those stores. A preexisting production ContextLM opt-out is read exactly as stored, while the distinct experimental neural switch retains its existing value and default.

Apostrophe normalization deduplicates equivalent straight and curly surfaces while preserving the selected surface. Five reviewed full omissions can become automatic in ordinary eligible English if autocorrection is enabled, local validity confirms the target, and the user has no existing valid spelling or explicit shortcut for the literal. `were`, `well`, `its`, `lets`, `ill` and the other reviewed ambiguous aliases retain their automatic literal. They may appear as manual choices with contextual or native evidence. The commit path repeats the safety policy at the separator. Refreshed candidates must match the active editor generation, request and selection. The optional context pass never runs in the separator refresh.

The model observes the existing three-word native snapshot on the existing decoder worker. It works on eligible tap suggestions and next-word rows, and only rearranges existing manual alternatives on completed swipe. Live swipe does not run the model. The native swipe winner, indices, editor and rejected candidate remain fixed. Protected and host-suppressed editors skip the model. Local word removals and active-dictionary validity are checked before new model words are offered. History participates through the existing native results when the user enables it. The context model has no editor-text storage or persistent context cache.

## Reproduction and checks

The model is a fixed, quantized backoff table with English and mixed channels. Training observations use Tatoeba CC0 English, PHINC, COMI-LINGUA and human HinGE rows. The build retains common candidate vocabulary from the native inventory and reviewed Roman Hindi and modern lexical additions. Short windows require multiple occurrences. The input normalization rejects identifiers such as URLs, email addresses, handles and digit strings, and the model distributes counts and weights with lexical n-grams. No source sentence files are bundled. Near-duplicate grouping uses a deterministic first-two/last token key plus exact deduplication. It cannot identify every shared translation family. See [license and dataset revisions](../app/src/main/assets/licenses/br-offline-context.txt) and [input and output fingerprints](../tools/dictionary/replay_report.json).

Download the four pinned inputs and verify their digests in the replay report. For Tatoeba, retain the exact CC0 export matching the digest, since the public download URL advances. From the repository root, with Python 3.11+, a Linux C++17 toolchain and a JDK:

```bash
bash tools/dictionary/build_native_replay.sh /tmp/brr-native-replay
for locale in US GB; do
  /tmp/brr-native-replay "app/src/main/assets/dicts/main_en-${locale}.dict" > "/tmp/brr-${locale}.tsv"
  /tmp/brr-native-replay "app/src/main/assets/dicts/main_en-${locale}.dict" \
    tools/dictionary/tap_fixtures.tsv app/src/main/assets/brrPersonal_modern.tsv > "/tmp/brr-tap-${locale}.tsv"
done
python3 tools/dictionary/prepare_context.py --tatoeba PATH_TO_CC0_TAR_BZ2 \
  --phinc PATH_TO_PHINC_CSV --comi PATH_TO_MT_TRAIN_CSV --hinge PATH_TO_HINGE_CSV \
  --us /tmp/brr-US.tsv --gb /tmp/brr-GB.tsv --counts /tmp/brr-counts.tsv --output /tmp/brr-context.tsv
cmp /tmp/brr-context.tsv app/src/main/assets/contextlm/production.tsv
python3 tools/dictionary/verify_context.py --native-taps /tmp/brr-tap-US.tsv /tmp/brr-tap-GB.tsv
```

CI regenerates the packaged table and held-out report when the production model changes, verifies the output against the pinned fingerprints, and archives the aggregate count snapshot. The archived counts can reproduce the packaged table offline with `prepare_context.py --from-counts`; the raw-corpus route repeats the independent held-out measurement.

The holdout evaluator starts from the native stock prediction rows for the unchanged binaries and compares them with the compact model's prediction proposal. These are stock-only host results. Installed-user history, imported dictionaries, actual Android suggestion slots and swipe geometry are outside those measurements.

| Replay | Held-out contexts | Stock Recall@3 | Proposed Recall@3 | Change |
| --- | ---: | ---: | ---: | ---: |
| US stock on English | 23,326 | 14.16% | 17.40% | +3.25 pp |
| UK stock on English | 23,326 | 0.00% | 4.92% | +4.92 pp |
| US stock comparator on mixed/Roman Hindi | 3,965 | 3.86% | 4.49% | +0.63 pp |

Top-1 increased in each stock comparator. UK ships without the US stock bigrams, so the UK result is a cold stock comparison. The mixed corpus includes translated data. The authored Indian English and Roman Hindi phrase fixtures supplement the public holdout, whose inputs do not provide locale-balanced typing sessions. Native host tap replay used the shipped tap decoder and a synthetic QWERTY geometry. On each stock English dictionary, the modern/contraction slice improved from 7/27 to 26/27 at rank three with the overlay. All 21 common-word and dialect controls kept their top candidate. The replay exposes raw candidate word, score and flags for authored fixtures. It does not deliver Android strips or type into editors.

Pending physical verification covers update over both APK editions, local history/import retention, Space/Enter/punctuation and undo in host editors, live and completed swipe with supported imported libraries and without a library, protected editor handoff, visible strip taps, and warm/cold latency and PSS on the owner device and a second phone. The optional context computation cooperatively falls back at 5 ms. CI and the host timings do not establish actual handset latency or accuracy.
