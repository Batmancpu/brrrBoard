# brrBoard Known Issues & Audit Notes

This is the current engineering watchlist. Items are not fixed until CI, source evidence, or Reno10 use establishes the relevant behavior.

| Area | Status | Finding |
|---|---|---|
| Dictate English-only output | **Owner device check required** | brrBoard uses the selected system RecognitionService and requests English. Dictate ignores caller language extras and follows its own saved Active language. v1.0.29 rejects Devanagari-only output and shows the exact Dictate language setting path. Set Dictate → Languages → Active language = English and turn off Follow keyboard language. Romanized Hindi cannot be reliably identified from a transcript alone. |
| brrPersonal dictionary | **Source batch awaiting signed CI / device acceptance** | The current batch adds a pinned, attributed PHINC-derived seed, capped at 6,000 total entries, plus a curated valid-only slang/profanity rules asset with emoji shortcuts. It uses the existing native spatial decoder; text suggestions are filtered but deliberate swipe results remain eligible. |
| Reno10 layout/insets | **Owner device acceptance required** | Confirm host-content boundary, bottom spacing with no visible navigation bar, key hit targets, stable height across letters/symbols/Emoji/Clipboard/toolbar, and ColorOS blur/tint transitions. Source measurements cannot substitute for the actual ColorOS build and display scaling. |
| Suggestion strip | **Partially implemented; quality check required** | The UI uses three candidate slots and the decoder's existing mWillAutoCorrect signal for center emphasis. Validate English/Hinglish relevance and emoji/clipboard interactions on device; no unvalidated ranking weights. |
| Smart Emoji | **Not implemented** | Owner requested up to three local rows with typed-word relevance and personal emoji use/recency. |
| Swipe library | **Not bundled / rights and compatibility unresolved** | Owner reports personal verification of the uploaded GlideLib build. The supplied archive's license/provenance and native ABI/API compatibility still need review before redistribution in a public APK. |
| Android proofreading | **Platform scope** | Android's installed spell checker can provide word-level spelling assistance. Do not claim Apple's grammar-level Writing Tools Proofread behavior without a distinct, verified local implementation. |
| Automated debug tests | **Known baseline failures** | Latest debug CI compiled, signer-verified and uploaded an APK but failed the automatic :app:testRunTestsUnitTest gate with 13 of 180 failures. No tests were run manually. |
| Asset licensing/size | **Do not bundle** | The supplied Apple emoji TTF is about 116 MB and Apple artwork is excluded from the upstream repository's MIT license. It is not in the APK. |

## Acceptance evidence

1. **Build evidence:** automatic signed CI succeeds and the artifact is non-empty.
2. **Install/update evidence:** owner verifies installation and in-place update on Reno10.
3. **Runtime evidence:** the owner exercises the affected path on the actual device.
4. **Regression evidence:** report existing automatic CI results; no issue is marked fixed solely from compilation.
