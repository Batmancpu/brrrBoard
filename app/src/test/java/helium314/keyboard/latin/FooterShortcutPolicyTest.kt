// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FooterShortcutPolicyTest {
    @Test fun clipboardIsNeverImplicitPasteWhenHistoryDisabled() {
        assertFalse(FooterShortcutPolicy.isAllowed(FooterShortcutPolicy.Action.CLIPBOARD, false))
        assertTrue(FooterShortcutPolicy.isAllowed(FooterShortcutPolicy.Action.CLIPBOARD, true))
        assertTrue(FooterShortcutPolicy.isAllowed(FooterShortcutPolicy.Action.TOOLBAR, false))
        assertTrue(FooterShortcutPolicy.isAllowed(FooterShortcutPolicy.Action.SETTINGS, false))
    }

    @Test fun threeArcTargetsMirrorWithoutChangingHeight() {
        for (index in 0..2) {
            val normal = FooterShortcutPolicy.targetOffset(index, false)
            val mirrored = FooterShortcutPolicy.targetOffset(index, true)
            assertEquals(112, normal.first + mirrored.first)
            assertEquals(normal.second, mirrored.second)
        }
    }

    @Test fun popupClampsToRealRootWindowOrigin() {
        assertEquals(12, FooterShortcutPolicy.clampedPopupLeft(0, 8, 300, 160, 4))
        assertEquals(144, FooterShortcutPolicy.clampedPopupLeft(900, 8, 300, 160, 4))
        assertEquals(12, FooterShortcutPolicy.clampedPopupLeft(10, 8, 100, 160, 4))
    }
}
