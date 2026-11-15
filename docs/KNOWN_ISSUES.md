# brrBoard Known Issues & Audit Notes

This is the current engineering watchlist. Items are not fixed until CI, source evidence, or Reno10 use establishes the relevant behavior.

| Area | Status | Finding |
|---|---|---|
| Dictate English-only output | **Owner device check required** | Owner reports the selected RecognitionService reaches Dictate but returns Hindi; brrBoard's requirement is English-only. Dictate ignores caller language extras and follows its own saved Active language. brrBoard rejects Devanagari-only final output and gives the exact setup path. Set Dictate → Languages → Active language = English and turn off Follow keyboard language. Romanized Hindi cannot be reliably identified from a transcript alone. |
| brrPersonal dictionary | **Signed v1.0.31; device acceptance pending** | Pinned PHINC-derived seed capped at 6,000 entries and curated valid-only slang/profanity rules with Emoji shortcuts are built into v1.0.31. The corrected source generator recognizes PHINC's `Sentence` header. The design uses the existing native decoder; device ranking/latency remain unverified. |
| Reno10 layout/insets | **Owner device acceptance required** | Confirm host-content boundary, bottom spacing with no visible navigation bar, key hit targets, stable height across letters/symbols/Emoji/Clipboard/toolbar, and ColorOS blur/tint transitions. Source measurements cannot substitute for the actual ColorOS build and display scaling. |
| Suggestion strip | **Partially implemented; quality check required** | The UI uses three candidate slots and the decoder's existing mWillAutoCorrect signal for center emphasis. Validate English/Hinglish relevance and emoji/clipboard interactions on device; no unvalidated ranking weights. |
| Smart Emoji | **Signed v1.0.33; owner device acceptance required** | Corrected source shipped in [v1.0.33](https://github.com/Batmancpu/brrrBoard/releases/tag/v1.0.33). The existing Recents grid adaptively prioritizes up to three width-based rows, refreshes after use, avoids double-counting and records accepted Emoji strip candidates once. Reno10 appearance/quality and contextual text-strip ranking remain open. |
| Learned history reset | **Corrected source checkpoint; signed CI pending** | The confirmation-protected Text Correction action clears only local UserHistoryDictionary word/word-pair learning and preserves brrPersonal, Android's system dictionary, and Emoji history. Initial workflows failed AAPT parsing on unescaped apostrophes in the new resource; latest source removes them. No APK was produced by the failed runs. |
| Swipe library | **Not bundled / rights and compatibility unresolved** | Owner reports personal verification of the uploaded GlideLib build. The supplied archive's license/provenance and native ABI/API compatibility still need review before redistribution in a public APK. |
| Android proofreading | **Platform scope** | Android's installed spell checker can provide word-level spelling assistance. Do not claim Apple's grammar-level Writing Tools Proofread behavior without a distinct, verified local implementation. |
| Automated debug tests | **Known baseline failures** | Debug run [43769819448](https://github.com/Batmancpu/brrrBoard/actions/runs/43769819448) built, signer-verified and uploaded an APK, then failed the automatic `:app:testRunTestsUnitTest` gate with 13 of 180 baseline failures: 5 ParserTest, 7 InputLogicTest, and 1 StringUtilsTest (currency/parser labels, URL/autospace, and Emoji recognition). This matches the previous baseline. No tests were run manually. |
| Asset licensing/size | **Do not bundle** | The supplied Apple emoji TTF is about 116 MB and Apple artwork is excluded from the upstream repository's MIT license. It is not in the APK. |

## Acceptance evidence

1. **Build evidence:** automatic signed CI succeeds and the artifact is non-empty.
2. **Install/update evidence:** owner verifies installation and in-place update on Reno10.
3. **Runtime evidence:** the owner exercises the affected path on the actual device.
4. **Regression evidence:** report existing automatic CI results; no issue is marked fixed solely from compilation.
