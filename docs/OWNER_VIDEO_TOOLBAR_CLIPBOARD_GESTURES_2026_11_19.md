# brrBoard owner recording: Clipboard Search, live glow, footer, toolbar, long-press, brrBrain

Date: 2026-11-19. Base: `main` `89f9b56` (signed v1.0.137 already CI-green).
This is a **source candidate until BOTH authoritative CI workflows pass**. A green
Android build is not physical Oppo Reno 10 5G visual or gesture acceptance.

## Owner request and video evidence

- Owner reports swipe typing is inert in Clipboard Search, rainbow highlight
  and text are stationary/not sufficiently luminous, a detached horizontal blur
  bar masks the lower Toolbar row above ABC/microphone, and the emoji footer's
  long-press choices are small, awkward and visually inconsistent with brrBoard.
- Wants a live, diffuse multi-hue haze/glow like their earlier video example,
  with readable text, an iOS-style temporary key fade while holding the emoji
  button, more ergonomic large targets, smarter predictions and remaining-backlog
  audit. Dashboard is newly available but still awaiting the owner's usage test.
- Examined supplied 23.70-second 512x1140 30fps recording. FFprobe showed
  711 video frames; a 2fps full-duration contact sheet and scene-difference
  selection evaluating every decoded input frame confirmed panel transitions,
  obscured Toolbar tiles and the static colored Clipboard Search edge. No
  owner screen text, raw clipboard, images or video entered the repository.

## Source changes and reasons

1. `ClipboardHistoryView`'s Search-specific `KeyboardActionListener` had three
   explicit no-op batch gesture callbacks. Reuse the same isolated native
   external-input gesture suggestion path already in Emoji Search and Klipy
   Search; accept only a current, focused, attached Clipboard query session.
   Never call host editor batch composition, write to the app or retain traces.
   New pure `ClipboardSearchGestureGate` and test cover stale callbacks.
2. `ClipboardRainbowGlowDrawable` now animates a shader matrix across cached
   multi-hue borders using graphics-layer redraw only, with soft halo strokes;
   its animators start only for active matched Search cards/query and stop when
   Search exits or a RecyclerView holder detaches/recycles. The inline matched
   substring's already-legible white text sits over similarly moving colorful
   ink (its foreground redraw drives the effect). Respect reduced/system
   animations; no bitmap, host screenshots, network or content storage.
3. Toolbar: hide the separate fixed 56dp bottom glass masking its second row,
   clip the sharp scrolling tile list only to the true panel area and retain
   one existing shared live footer sampler. Shorten trailing scroll padding
   from 114dp to 56dp so the last tile can scroll fully above the ABC footer.
   Emoji's distinct bottom action-row glass remains intact.
4. Footer emoji hold: a 280x100dp frosted, rounded shelf with three 80x82dp
   labeled choices: Emoji, Clipboard, Settings (mirrored for RTL), selection
   feedback, and a reversible 110ms fade of normal keycaps while holding;
   keyboard host glass/background and current text field are not hidden.
   A normal tap remains Emoji, mic hold remains the system keyboard picker;
   settings and Clipboard protections remain unchanged.
5. brrBrain suggestion safety: for two conflicting explicitly confirmed
   correction pairs with equal support, show neither as a learned alternative;
   require a clear most-confirmed correction. Existing native Glide top-1,
   encryption, eligibility restrictions and learner storage are unchanged.

## Validation and release gates

Pure tests: `ClipboardSearchGestureGateTest`, expanded
`FooterShortcutPolicyTest`, expanded `SwipeCorrectionLearnerTest`. Also run
full `:app:testDebugUnitTest`, keyboard generator and safety guards through the
full debug workflow, then original-keystore pinned release workflow. Keep
`com.mangoloads.board`, original signer, offline/three-theme/privacy policy,
main-only and independent Dictate code unchanged. Do NOT claim on-device QA
without actual feedback.

Oppo acceptance steps:

1. Open Clipboard > Search and glide `grand` across the internal alphabet.
   It must populate *only* the search field; the host app's text must stay put.
   Immediately close/reopen Search during a glide; stale suggestions must not
   land in the new query. Tap type/delete/clear/back/ABC still work.
2. On a matching Search, observe the neon edge, glyph-backed rainbow and
   readable white letters for 5 seconds: their hues should visibly travel,
   then stop when leaving. Check no list pixels or haze under QWERTY/ABC/mic.
3. Open Toolbar, scroll several rows. First two tile rows should be usable,
   with natural clipping at bottom and no distinct gray 56dp bar above footer.
   Last row must be fully reachable by scrolling. Check Emoji compact/expanded
   category/action blur is not regressed.
4. Hold the left emoji footer key: labels and three touch targets appear,
   keyboard keycaps temporarily fade; slide into each target, release to act;
   cancel restores keycaps, normal tap and accessible actions still work.
5. Test three-theme contrast, light/dark, RTL, ColorOS animations, voice mic,
   and normal gesture typing plus brrBrain dashboard. Distinguish measured
   learner counts from any asserted accuracy (not measured by this batch).

## Not closed by this patch

Dictate's on-phone provider/mic lifecycle and permission error; prior Emoji
category/grid dead band and glass seam device QA; big numpad availability;
all remaining iOS visual parity, advanced per-app predictors and offline
accuracy/latency baselines. Do not silently mark these delivered. No new
predictor model, broad permission, cloud call, microphone data or raw trace
collection was introduced.
