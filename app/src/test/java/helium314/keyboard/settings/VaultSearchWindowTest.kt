// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings

import org.junit.Assert.*
import org.junit.Test

class VaultSearchWindowTest {
    @Test fun unarmedAndExpiredNeverUnlock() {
        assertFalse(VaultSearchWindow.active(0L, 0L))
        val deadline = VaultSearchWindow.deadline(1000L)
        assertEquals(61000L, deadline)
        assertTrue(VaultSearchWindow.active(deadline, 1000L))
        assertTrue(VaultSearchWindow.active(deadline, 60999L))
        assertFalse(VaultSearchWindow.active(deadline, 61000L))
        assertFalse(VaultSearchWindow.active(deadline, 61001L))
    }
    @Test fun aNewHoldStartsAFreshMinute() {
        assertEquals(120000L, VaultSearchWindow.deadline(60000L))
        assertTrue(VaultSearchWindow.active(VaultSearchWindow.deadline(60000L), 119999L))
    }
}
