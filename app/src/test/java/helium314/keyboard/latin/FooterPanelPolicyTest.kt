// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FooterPanelPolicyTest {
    @Test fun abcAppearsAndReturnsToTypingFromEveryPanel() {
        assertFalse(FooterPanelPolicy.returnToAbc(false, false, false))
        assertTrue(FooterPanelPolicy.returnToAbc(true, false, false))
        assertTrue(FooterPanelPolicy.returnToAbc(false, true, false))
        assertTrue(FooterPanelPolicy.returnToAbc(false, false, true))
    }
}
