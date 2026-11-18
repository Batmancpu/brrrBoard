# Clipboard Search: exact footer blur and washed-out rainbow roots (2026-11-19)

## Source-proven causes after v1.0.133 owner rejection
1. In InputView.onMeasure, mFooter.setPanelGlassSource selects clipboard_list whenever ClipboardHistoryView is visible, without checking ClipboardHistoryView.isSearchMode. The distinct BoardFooterView/PanelGlassOverlayView renderer records that source again outside ClipboardGlassFrameLayout's canvas clipping. Thus a visible alphabet/footer can still show blurred clipboard card text beneath ABC/mic even though ClipboardHistoryView was clipped correctly. Fix: the footer samples Clipboard *Browse* only. While Search mode is active, pass a null source; preserve normal Browse moving glass untouched.
2. In ClipboardAdapter.RainbowMatchSpan, an initial translucent border sets Paint.color to 0x66... (40% alpha), then a rainbow LinearGradient is installed without resetting paint alpha. Shader output is multiplied by paint alpha, so the rainbow fill also became translucent and looked gray/washed out. Fix explicitly set Paint.color = Color.WHITE (opaque) immediately before applying the gradient, then draw pure white matched glyphs.
3. The previous static edge halo was intentionally faint (17%, 45%, 88% concentric strokes). Increase visible stroke strengths with saturated cyan/violet/pink gradients while still using cached immutable vector gradients and no looping animation or CPU bitmap. Preserve legibility and reduce overdraw by drawing only search matches.

## Scoped patch and checks
- InputView.java uses FooterGlassSourcePolicy.shouldSampleClipboard(panel,search) and only samples clipboard_list in Browse; FooterGlassSourcePolicyTest covers active Browse/Search and no panel.
- ClipboardAdapter.kt restores full opaque gradient for matched substring and keeps white glyph; ClipboardRainbowGlowDrawable.kt improves edge contrast.
- Regression: search long local passage matching middle word, show result card and search query, scroll and press ABC/mic, confirm **zero** card pixel sampling behind shared footer; Browse retains original live scroll glass. Check dark/light/landscape/first Search frame and delayed EditorInfo changes.
- No new camera/mic/network/clipboard permission; no copying text into render buffers; no raw private screenshots committed.
- Unit/debug/full original-signed workflows and Oppo screen recording acceptance are separate gates. Do not claim fixed until physical inspection.

## Context
Dashboard/strip v1.0.135 source is green and Keystore encrypted-pair vault candidate fa27e4e2 is independent and must complete its CI gates before this patch is published. The user wants high-saturation original rainbow edge derived visually from the MeTube recording without copying proprietary assets. All other remaining batches are sequenced separately.
