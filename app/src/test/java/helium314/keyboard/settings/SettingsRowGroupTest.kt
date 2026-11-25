// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings

import org.junit.Assert.assertEquals
import org.junit.Test

class SettingsRowGroupTest {
    @Test fun groupingPreservesOrderAndSkipsInvisibleRows() {
        assertEquals(listOf(SettingsRowGroup(null, listOf("root")),
            SettingsRowGroup(1, listOf("a", "b")), SettingsRowGroup(3, listOf("c"))),
            groupSettingsRows(listOf("root", 1, "a", null, "b", 2, null, 3, "c")))
    }
    @Test fun emptyGroupsAreNotRendered() {
        assertEquals(emptyList<SettingsRowGroup>(), groupSettingsRows(listOf(null, 1, null, 2)))
    }
}
