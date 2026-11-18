// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import org.junit.Assert.assertEquals
import org.junit.Test

class SharedPanelGeometryTest {
    @Test fun emojiAndClipboardHaveIdenticalHeightForIdenticalShell() {
        val emoji = SharedPanelGeometry.panelBodyHeight(642, 64, 108, false, 1100)
        val clipboard = SharedPanelGeometry.panelBodyHeight(642, 64, 108, false, 1100)
        assertEquals(814, emoji)
        assertEquals(emoji, clipboard)
    }

    @Test fun visibleAccessoryIsNotCountedAgainAndLandscapeCapsBody() {
        assertEquals(706, SharedPanelGeometry.panelBodyHeight(642, 64, 108, true, 1100))
        assertEquals(440, SharedPanelGeometry.panelBodyHeight(642, 64, 108, false, 440))
        assertEquals(0, SharedPanelGeometry.panelBodyHeight(-3, 0, 0, false, 0))
    }

    @Test fun emojiBrowseKeepsExistingViewportOnlyWithinSafeAccessoryDelta() {
        assertEquals(400, SharedPanelGeometry.retainExistingPanelViewport(336, 400, 500, 100))
        assertEquals(336, SharedPanelGeometry.retainExistingPanelViewport(336, 500, 500, 100))
        assertEquals(336, SharedPanelGeometry.retainExistingPanelViewport(336, 400, 350, 100))
        assertEquals(336, SharedPanelGeometry.retainExistingPanelViewport(336, 320, 500, 100))
        assertEquals(0, SharedPanelGeometry.retainExistingPanelViewport(-1, 100, 0, 100))
    }

    @Test fun searchAddsFieldAndExactlyOneResultRowWithoutTruncatingTypingBody() {
        assertEquals(356, SharedPanelGeometry.emojiSearchBodyHeight(252, 48, 56, 288, 1000))
        assertEquals(300, SharedPanelGeometry.emojiSearchBodyHeight(252, 48, 56, 288, 300))
        assertEquals(288, SharedPanelGeometry.emojiSearchBodyHeight(252, 0, 0, 288, 1000))
    }
    @Test fun expandedEmojiIsDeliberateAndClampedWithoutMutatingCompactBrowse() {
        val compact = SharedPanelGeometry.panelBodyHeight(252, 0, 36, true, 1000)
        assertEquals(252, compact)
        assertEquals(412, SharedPanelGeometry.emojiExpandedBodyHeight(compact, 160, 1000))
        assertEquals(340, SharedPanelGeometry.emojiExpandedBodyHeight(compact, 160, 340))
        assertEquals(compact, SharedPanelGeometry.panelBodyHeight(252, 0, 36, true, 1000))
        assertEquals(0, SharedPanelGeometry.emojiExpandedBodyHeight(-5, -1, 0))
    }
    @Test fun compactBrowseIsIndependentOfSearchHeightAndClampedToCurrentWindow() {
        val compactBefore = SharedPanelGeometry.panelBodyHeight(642, 0, 108, false, 1100)
        val search = SharedPanelGeometry.emojiSearchBodyHeight(642, 48, 56, compactBefore, 1100)
        val compactAfter = SharedPanelGeometry.panelBodyHeight(642, 0, 108, false, 1100)
        assertEquals(750, compactBefore)
        assertEquals(750, search) // Search requests 746px, but retains the 750px normal-panel minimum.
        assertEquals(compactBefore, compactAfter)
        assertEquals(400, SharedPanelGeometry.panelBodyHeight(642, 0, 108, false, 400))
    }

}
