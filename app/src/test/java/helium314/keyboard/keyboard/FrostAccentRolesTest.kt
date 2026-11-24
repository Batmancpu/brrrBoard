// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FrostAccentRolesTest {
    @Test fun accentDefaultMigrationOnlyEnablesOnce() {
        assertTrue(FrostAccentRoles.actionTintAfterMigration(false,false))
        assertTrue(FrostAccentRoles.actionTintAfterMigration(false,true))
        assertFalse(FrostAccentRoles.actionTintAfterMigration(true,false))
        assertTrue(FrostAccentRoles.actionTintAfterMigration(true,true))
    }

    @Test fun ordinaryAndModifierKeysStayNeutralInTintedGlass() {
        val colors = FrostAccentRoles.palette(true, 0xFF111111.toInt(),
            0xFF222222.toInt(), 0xFF007AFF.toInt())
        assertEquals(0xFF111111.toInt(), colors.functional)
        assertEquals(0xFF222222.toInt(), colors.special)
        assertEquals(0xFF007AFF.toInt(), colors.action)
        assertFalse(FrostAccentRoles.tintControl(true, false, false, false))
        assertFalse(FrostAccentRoles.tintControl(true, true, false, true))
        assertFalse(FrostAccentRoles.tintControl(true, false, true, true))
        assertTrue(FrostAccentRoles.tintControl(true, false, false, true))
    }

    @Test fun untintedGlassKeepsAllRoleBackgroundsNeutral() {
        val colors = FrostAccentRoles.palette(false, 3, 7, 42)
        assertEquals(3, colors.functional)
        assertEquals(7, colors.special)
        assertEquals(7, colors.action)
        assertFalse(FrostAccentRoles.tintControl(false, false, false, true))
    }

    @Test fun actualReturnReceivesAccentEvenWithoutContextualImeAction() {
        for (action in listOf(0,1,2,3,4,5,6,7,8,255))
            assertTrue(FrostAccentRoles.tintIosAction(false, false, true, false, action))
        assertFalse(FrostAccentRoles.tintIosAction(false, false, false, false, 3))
        assertFalse(FrostAccentRoles.tintIosAction(true, false, true, false, 3))
        assertFalse(FrostAccentRoles.tintIosAction(false, true, true, false, 3))
        assertFalse(FrostAccentRoles.tintIosAction(false, false, true, true, 3))
    }
    @Test fun panelSearchIsExplicitlyEligibleWithoutHostImeAction() {
        assertTrue(FrostAccentRoles.tintPanelSearch(false, false, true, true))
        assertFalse(FrostAccentRoles.tintPanelSearch(true, false, true, true))
        assertFalse(FrostAccentRoles.tintPanelSearch(false, true, true, true))
        assertFalse(FrostAccentRoles.tintPanelSearch(false, false, false, true))
        assertFalse(FrostAccentRoles.tintPanelSearch(false, false, true, false))
    }
    @Test fun accentPreservesEveryConfiguredAlpha() {
        for (alpha in 0..255) {
            val base = (alpha shl 24) or 0x112233
            assertEquals((alpha shl 24) or 0x0088ff, FrostAccentRoles.withBaseAlpha(0xff0088ff.toInt(), base))
        }
    }

    @Test fun cornerRadiusFitsSmallLandscapeOrResizedKeys() {
        assertEquals(16f, FrostAccentRoles.keyCornerRadius(50f, 60f, 2f), 0.001f)
        assertEquals(6f, FrostAccentRoles.keyCornerRadius(12f, 60f, 2f), 0.001f)
        assertEquals(0f, FrostAccentRoles.keyCornerRadius(0f, 60f, 2f), 0.001f)
    }
}
