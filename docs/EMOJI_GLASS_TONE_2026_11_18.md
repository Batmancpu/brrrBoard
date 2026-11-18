# Emoji-only glass tone — 2026-11-18

## Owner's reference and absolute boundary

Two ChatGPT iPad screenshots show bright yellow/orange content under a fixed
blurred header: its blurred content appears noticeably darker in dark mode
and subtly lighter in light mode. Owner requests analogous contrast for brrBoard
Emoji glass, with an on/off Settings switch. **Do not add a tint over the
entire keyboard or the host app**. Android IME backdrop already has its own
frosted treatment; another full-surface black/white wash would compound it.

## Implementation candidate (independent of A2 Expanded Emoji)

- `EmojiGlassTonePolicy` supplies dark RGB gain 0.72 and light gain 0.88 with
  +30.6/255 offset, with identity alpha. These are calibrated **initial
  approximations** of the reference, not claimed pixel-perfect measurements
  across unrelated screenshots, color spaces or phones.
- `PanelGlassOverlayView` applies the matrix as a `saveLayer` paint only to the
  live Emoji RenderNode's output pixels. All alpha-zero pixels remain alpha
  zero. There is no new `drawColor`, rectangle wash, drawable background, IME
  wallpaper capture, global overlay or change to the blur radius/bleed.
- Only Emoji's existing bottom/header glass views opt in. The companion
  BoardFooterView glass opts in **only when its source is `emoji_pager`**, so
  the shared lower glass remains continuous across the Search/Space/Delete
  and ABC/microphone seam. Clipboard/Toolbar/typing explicitly receive no
  tone and retain their previous rendering.
- Appearance → **Emoji glass light and dark tint** defaults On; Off passes
  `null` filter and restores the old unfiltered `drawRenderNode` path exactly.

## Gates and device acceptance

Keep this a separately gated release after A2, with unchanged original signing,
package and parent key-row geometry. Android build and unit tests must pass.
On Oppo, inspect the same scrolled emoji categories under lower glass in both
themes with switch On/Off, compare darkening/lightening, check there is no
double-dark host background, no new dark slab in empty panel areas, no bottom
seam, no lost touch targets, and no speed/performance regression. Tune only
the gains after the user's actual screenshots. Source logic alone cannot
verify visual parity with the iPad reference.
