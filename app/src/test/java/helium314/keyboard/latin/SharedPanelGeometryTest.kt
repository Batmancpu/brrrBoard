// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import org.junit.Assert.assertEquals
import org.junit.Test

class SharedPanelGeometryTest {
    @Test fun compactEmojiFallsBackToActualSameWidthWrapperOnlyWithoutSavedTypingFrame() {
        assertEquals(490, SharedPanelGeometry.chooseCompactEmojiViewport(490, 520, true))
        assertEquals(520, SharedPanelGeometry.chooseCompactEmojiViewport(0, 520, true))
        assertEquals(0, SharedPanelGeometry.chooseCompactEmojiViewport(0, 520, false))
        assertEquals(0, SharedPanelGeometry.chooseCompactEmojiViewport(0, 0, true))
    }

    @Test fun compactEmojiAdoptsSameWidthTypingViewportEvenBeyondFormerStripTolerance() {
        assertEquals(492, SharedPanelGeometry.alignCompactEmojiToTypingViewport(380, 492, 550))
        assertEquals(380, SharedPanelGeometry.alignCompactEmojiToTypingViewport(380, 0, 550))
        assertEquals(380, SharedPanelGeometry.alignCompactEmojiToTypingViewport(380, 700, 550))
        assertEquals(380, SharedPanelGeometry.alignCompactEmojiToTypingViewport(380, 350, 550))
        // A saved viewport taller than the current window must be rejected,
        // not clipped into a new artificial height.
        assertEquals(380, SharedPanelGeometry.alignCompactEmojiToTypingViewport(380, 492, 400))
        assertEquals(400, SharedPanelGeometry.alignCompactEmojiToTypingViewport(480, 492, 400))
    }

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

    @Test fun compactTopBandCanOnlyBorrowOneRealTypingAccessory() {
        val nativeTypingViewport = KeyboardViewportHeightMemory()
        nativeTypingViewport.observe(694, true)
        // Search moves the alphabet into Emoji; this must not redefine the
        // native typing viewport used to size the returning compact browser.
        nativeTypingViewport.observe(962, false)
        val compact = SharedPanelGeometry.retainExistingPanelViewport(
            640, nativeTypingViewport.lastTypingHeight, 1100, 64)
        assertEquals(694, compact)
        assertEquals(640, SharedPanelGeometry.retainExistingPanelViewport(
            640, 962, 1100, 64))
        assertEquals(640, SharedPanelGeometry.retainExistingPanelViewport(
            640, nativeTypingViewport.lastTypingHeight, 650, 64))
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
