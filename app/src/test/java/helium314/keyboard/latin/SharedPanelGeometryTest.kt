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

    @Test fun searchAddsFieldAndExactlyOneResultRowWithoutTruncatingTypingBody() {
        assertEquals(356, SharedPanelGeometry.emojiSearchBodyHeight(252, 48, 56, 288, 1000))
        assertEquals(300, SharedPanelGeometry.emojiSearchBodyHeight(252, 48, 56, 288, 300))
        assertEquals(288, SharedPanelGeometry.emojiSearchBodyHeight(252, 0, 0, 288, 1000))
    }
}
