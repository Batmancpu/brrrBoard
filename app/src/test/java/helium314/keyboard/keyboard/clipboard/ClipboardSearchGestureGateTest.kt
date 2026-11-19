// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.clipboard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipboardSearchGestureGateTest {
    @Test fun onlyCurrentFocusedClipboardSearchMayAcceptGestureWord() {
        assertTrue(ClipboardSearchGestureGate.accepts(true, 3, 3, true, true))
        assertFalse(ClipboardSearchGestureGate.accepts(false, 3, 3, true, true))
        assertFalse(ClipboardSearchGestureGate.accepts(true, 2, 3, true, true))
        assertFalse(ClipboardSearchGestureGate.accepts(true, 3, 3, false, true))
        assertFalse(ClipboardSearchGestureGate.accepts(true, 3, 3, true, false))
    }
}
