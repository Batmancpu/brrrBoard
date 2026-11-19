// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FooterContextActionPolicyTest {
    @Test fun onlyCurrentActionOwnerCanRevokeItsChip() {
        val clipboard = Any()
        val dictate = Any()
        assertTrue(FooterContextActionPolicy.mayClear(clipboard, clipboard))
        assertFalse(FooterContextActionPolicy.mayClear(clipboard, dictate))
        assertTrue(FooterContextActionPolicy.mayClear(null, dictate))
        assertFalse(FooterContextActionPolicy.mayClear(dictate, null))
    }

    @Test fun ephemeralActionsHaveBoundedLifetime() {
        assertEquals(2_000L, FooterContextActionPolicy.durationMs(0))
        assertEquals(6_000L, FooterContextActionPolicy.durationMs(6_000))
        assertEquals(10_000L, FooterContextActionPolicy.durationMs(Long.MAX_VALUE))
    }
}
