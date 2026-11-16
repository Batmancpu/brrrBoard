// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardSwitcherTransitionTest {
    @Test
    fun newerTransitionInvalidatesAnOlderEndAction() {
        val guard = KeyboardSwitcher.PanelTransitionGuard()
        val older = guard.start()
        val current = guard.start()

        assertFalse(guard.isCurrent(older))
        assertTrue(guard.isCurrent(current))
    }

    @Test
    fun inputViewLifecycleInvalidatesPendingEndAction() {
        val guard = KeyboardSwitcher.PanelTransitionGuard()
        val pending = guard.start()

        guard.invalidate()

        assertFalse(guard.isCurrent(pending))
    }
}
