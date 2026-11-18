# Clipboard Search — rainbow match and strict alphabet viewport (19 November 2026)

## 2026-11-19 — owner-verified Clipboard visual defect second pass (awaiting CI)
Owner explicitly states the prior Clipboard rainbow/clipping request still does not match phone behavior. Source-level audit found TWO exact omissions after v1.0.133: (1) InputView.onMeasure unconditionally fed clipboard_list into BoardFooterView's independent PanelGlassOverlayView source even in Clipboard SEARCH; the previous ClipboardGlassFrameLayout clip cannot clip that separate footer glass sample. Proposed FooterGlassSourcePolicy lets Clipboard BROWSE sample list but forbids it in Search, while keeping footer ABC/mic visible; pure test. (2) ClipboardAdapter.RainbowMatchSpan first drew a translucent 0x66-alpha outline, then installed LinearGradient without resetting Paint.color alpha; the whole rainbow shader was therefore only ~40% opaque, creating the gray wash owner reports. New source sets paint.color=Color.WHITE before shader to retain 100% multi-hue fill and pure white glyph, and stronger static luminous edge strokes (no perpetual animation). See docs/CLIPBOARD_SEARCH_FOOTER_GLASS_AND_OPAQUE_RAINBOW_2026_11_19.md. The prior v1.0.133 was CI GREEN but device NOT accepted. This patch should be published separately AFTER secure learning vault fa27e4e2 clears both full CI gates. Verify on device; do not claim accepted from CI. Preserve browse glass, package, signing and independent Dictate.

## 2026-11-19 — verified brrBrain crash-recovery and Clipboard rainbow releases

**AUTOMATED RELEASES VERIFIED:** v1.0.132 is original-signed from functional commit `7207fdb5b5e2ace0a6798330bf19eddc7b8d90eb`, debug run `44378856429` and original-signer run `44378856356` both terminal SUCCESS. Its direct APK is https://github.com/Batmancpu/brrrBoard/releases/download/v1.0.132/brrBoard.apk, SHA256 `5cf3200bec092c1ed01d1bbd82a2f9f7bc5a68f3b1fdf4a5d1b8318b03267b02`.

Follow-up Clipboard Search rainbow/strict-viewport source commit `de195a385a01384fb411d9f3773b0d2d7a1c8dd8` passed debug `44380515859` and production original-signing/release `44380515770`, BOTH terminal SUCCESS. **LATEST signed v1.0.133**: https://github.com/Batmancpu/brrrBoard/releases/download/v1.0.133/brrBoard.apk; 38,143,159 bytes; SHA256 `eae685e78af720481aefc593fb369d0bbae388044bc9d6cb7d9579b8e579c752`; release targets exact source `de195a38`. Matches remain offline and locally pasted. No Dictate/voice/Android package/signing/network changes. The earlier red jobs on old `dc248592` were repaired by `2551c261`; do not rerun old known-bad commits to turn history green. Owner-reported *real Oppo* swipe crash, swipe recall, search gradient contrast, alphabet clipping and footer appearance remain **PHYSICAL DEVICE ACCEPTANCE OPEN**; source+CI does not prove hardware behavior. Next: owner installs only latest v1.0.133 (contains v132 recovery) and reports first swipe test, static rainbow match, and no overlap; if swipe still crashes obtain minimal Android crash log/tombstone where safe, disable geometric fallback if necessary. Future per-app learned profiles/CTC are not shipped and need separate privacy/benchmark review.

## Recording evidence
Owner's 23.07s 512x1142 recording shows brrBoard Clipboard Search in Notes at 0–13 seconds (low-contrast gray search hit and long result card near alphabet/ABC boundary) and MeTube edge rainbow at 14–22 seconds. It contains no swipe crash stack trace and does not demonstrate the underlying native crash. No owner raw media or clipboard text is published.

## Engineering findings
1. ClipboardAdapter.MatchedWordHazeSpan and its BackgroundColorSpan use wallpaper-key accent, which can be neutral gray and indistinct.
2. ClipboardHistoryView.showSearchMatch makes user-entered search letters the accent color; its 38%-alpha capsule is faint.
3. PanelMaterial.extendIntoFooter always extends the Clipboard RecyclerView by a footer height to feed moving Browse glass. This is wrong when Search reparents alphabet inside panel, hides bottom glass and needs a strict bottom boundary. The old child drawing clip also missed a frame if the alphabet top was not laid out yet.

## Code changes
- ClipboardAdapter: replace gray/accent character spans with static deep rainbow background and WHITE text; apply a restrained multicolor edge to matched Search cards only. Original full clip text, pins and paste mappings unchanged.
- ClipboardRainbowGlowDrawable: one original GPU-friendly static gradient with three contours, cached shader by bounds, no looping animation, screenshot, new assets, or network.
- ClipboardHistoryView: use same rainbow inside the Search capsule; WHITE user query when a match exists; preserve theme-default color otherwise.
- PanelMaterial and ClipboardGlassFrameLayout: normal Browse still extends cards into approved live glass, Search no longer does; always crop search card drawing above actual keyboard top, or fallback parent minus keyboard height while first layout is pending.
- ClipboardSearchViewportTest: top boundary/fallback/small window. CI compile/unit, original signature and Oppo appearance are separate checks.

## Oppo acceptance
Search synthetic term keyboard against a long local clipboard text. See bright multicolor match-pill under white letters, visible rainbow matched-card edges, readable search query, no result pixels behind alphabet/ABC/mic; Search→ABC instant and Browse original moving glass unchanged. Check light/dark and first-open, keyboard resize, horizontal scrolling. Real device contrast and FPS are unverified until owner reports.

Dictate, app ID, signer, and unrelated clipboard privacy features must not change. Per-app dictionaries and CTC decoder remain separate quality/privacy-gated follow-ups.
