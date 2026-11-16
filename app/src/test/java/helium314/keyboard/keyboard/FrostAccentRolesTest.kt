// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FrostAccentRolesTest {
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

    @Test fun cornerRadiusFitsSmallLandscapeOrResizedKeys() {
        assertEquals(16f, FrostAccentRoles.keyCornerRadius(50f, 60f, 2f), 0.001f)
        assertEquals(6f, FrostAccentRoles.keyCornerRadius(12f, 60f, 2f), 0.001f)
        assertEquals(0f, FrostAccentRoles.keyCornerRadius(0f, 60f, 2f), 0.001f)
    }
}
