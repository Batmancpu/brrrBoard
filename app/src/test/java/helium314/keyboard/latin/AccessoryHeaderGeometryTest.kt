// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import helium314.keyboard.latin.suggestions.AccessoryPriorityPolicy
import org.junit.Assert.*
import org.junit.Test

class AccessoryHeaderGeometryTest {
    @Test fun collapseOnlyWhenNoUsefulContent() {
        assertTrue(AccessoryPriorityPolicy.collapseUnusedHeader(false, false, false, false))
        assertFalse(AccessoryPriorityPolicy.collapseUnusedHeader(true, false, false, false))
        assertFalse(AccessoryPriorityPolicy.collapseUnusedHeader(false, true, false, false))
        assertFalse(AccessoryPriorityPolicy.collapseUnusedHeader(false, false, true, false))
        assertFalse(AccessoryPriorityPolicy.collapseUnusedHeader(false, false, false, true))
    }
    @Test fun clearanceTracksRadiusAndDensityWithinCompactBounds() {
        assertEquals(16, AccessoryHeaderGeometry.topClearancePx(0f, 2f))
        assertEquals(22, AccessoryHeaderGeometry.topClearancePx(48f, 2f))
        assertEquals(32, AccessoryHeaderGeometry.topClearancePx(100f, 2f))
        assertEquals(0, AccessoryHeaderGeometry.topClearancePx(24f, 0f))
    }
}
