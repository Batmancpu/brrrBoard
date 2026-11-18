# Compact Emoji top gap: Smart row layout correction — 2026-11-18

## Device evidence and code-level cause

Owner's 77.5-second, 382×854 screen recording was captured on v1.0.113; owner separately confirms the problem is still present after installing v1.0.114. Compact Emoji Browse shows a conspicuous empty-looking frosted band **above the category icons**; expanded Browse places the categories near the upper edge. The moving category glass and ABC/mic footer are approved. Search in the recording returns no results for descriptions such as `smiley with sunglasses` and `man with glasses`.

`emoji_palettes_view.xml` declared `emoji_context_smart_section` as a `wrap_content` vertical sibling **preceding** the weighted `emoji_grid_shell`. The optional Smart picks section contains a 20dp label row and a 50dp candidate row: it reserves approximately **70dp before the fixed category header** whenever Smart picks are available in compact Recents. `EmojiPalettesView.updateContextSmartVisibility()` explicitly hides this section in expanded mode. Previous viewport/RecyclerView position changes cannot remove this independent sibling's reserved height. This is a verified layout failure mode consistent with the video; a device layout trace is still needed to establish whether any additional offset exists.

## Focused correction

Move the **same** Smart picks section into `emoji_grid_shell` as a `FrameLayout` overlay beginning immediately **below** the fixed category strip. It no longer participates in the outer vertical measurement, so cannot shift the categories downward. When Smart picks exist, add 70dp only to **Recents' RecyclerView top inset**; otherwise retain the original category-height + 8dp top inset. Other categories never inherit the inset. Leave scrolling behind the live top glass and retain the native Search/Space/Delete action row and ABC/mic footer unchanged.

Regression tests verify reparenting, no duplicates, overlay gravity/margin and per-category inset. This change does not modify Search, fonts, application ID, ordinary typing or the blur compositor. Automated CI and pinned original signer, then real Oppo device acceptance, are separate gates.

## Subsequent owner requests

1. Exercise actual CLDR Describe queries using indirect everyday language; improve bounded conservative offline alias/intent ranking without models, network, logging or unsupported Unicode sequences.
2. Evaluate a smooth, interruptible, reduced-motion-aware keyboard-height transition between compact Browse, expanded Browse and Search, without input lag or double-tint.
3. Oppo checks: first open, Recents with text context, Smileys, expanded/Back/Search/ABC, no extra gap above category icons, Smart picks below them, no first-row clipping, unchanged moving bottom glass.
