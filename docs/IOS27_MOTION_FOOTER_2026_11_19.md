# Owner-approved brrBoard iOS-inspired motion and footer indicators

Date: 2026-11-19 (India). Scope: second of two owner-requested sequential
commits after verified c4e6527/v1.0.145. No other branches or signers.

## Reference and behavior

Owner supplied a 37.5667-second, 1127-frame/30fps YouTube comparison and a
light-mode keyboard screenshot. Machine-screened all frames, manually reviewed
adjacent prediction changes around 25.2s and 28.6s. The visible suggestion
strip keeps fixed slots while old words fade and new words appear; the center
confidence rectangle changes unobtrusively. These are visual estimates from a
compressed camera recording, not private Apple animation timing APIs. Apple's
HIG Motion calls for concise, cancelable, purposeful effects; Color calls for
adaptive role colors rather than fixed cross-theme hex values.

## Code map

- `KeyboardMotionSpec.kt`: one bounded timing/reduced-animation vocabulary,
  150ms word crossfade, 170ms result reveal, 1600ms full recording pulse.
- `SuggestionWordMorph.kt` and `SuggestionStripView.kt`: preserve the active
  suggestion's tap target and label synchronously; overlay a noninteractive
  short-lived old-word copy, fade the new word in without changing physical
  slots or keyboard height. Reuse bounded ghost views; clear content and cancel
  on replacement/detach/editor mode change/RTL flip. Center correction pill
  remains decoder-approved and gray by default, with owner's opt-in tint.
- `EmojiPalettesView.java`: retain newest-query protection and pending-result
  touch gate, replacing abrupt alpha jumps with short result reveals.
- `ClipboardHistoryView.kt`: reveal matched Clipboard cards and control X
  morph with shared compositor policy, no card clipping/height changes.
- `KlipyPalettesView.kt`: on a successful already-authorized media search,
  reveal returned items without adding a network request or changing providers.
- `FooterStatusRingDrawable.kt` and `BoardFooterView.kt`: theme-aware outlined
  Emoji/ABC and filled mic circles inside the original 48dp touch targets;
  one animator synchronizes mic scale/image intensity, red ring stroke and
  halo. Listening is the strong recording pulse; Processing keeps the
  Transcribing status and a gentler active indication. Idle/error/cancel
  resets. Reduced-motion system setting has static active red fallback.
- Targeted policy tests: `KeyboardMotionSpecTest.kt`,
  `FooterIndicatorPolicyTest.kt` plus existing strip and voice-state tests.

## Constraints / acceptance

No changes to native Glide, brrBrain decoding, emoji content corpus, Big Numpad
(owner says it NOW works), keyboard signed package, original keystore,
permissions, clipboard persistence, or voice recognition provider. No raw
clipboard/suggestion/voice text in logs or committed screenshots. Default
keyboard appearance without animations must remain usable; no panel height
change, blank Emoji Browse top band, suggested-word taps on old content,
or duplicate ABC/mic glass bar.

CI: full Debug + Android tests/B16A/B16B and independent pinned original-signed
release MUST both pass; poll and repair failures before reporting release.
Physical Oppo Reno10 5G dark/light, rapid typing/swipe, Search/Back, Emoji
category and long-press, Dictate startup/listening/processing/error and
accessibility checks remain mandatory. CI cannot certify iOS visual parity.

B3D intermittently absent Paste suggestion fix remains prepared locally in
the earlier brrrBoard_batch2 worktree and was deliberately excluded from these
two scoped feature commits.
