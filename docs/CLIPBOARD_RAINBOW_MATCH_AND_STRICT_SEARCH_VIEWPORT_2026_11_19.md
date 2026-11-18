# Clipboard Search — rainbow match and strict alphabet viewport (19 November 2026)

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
