// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SlideConfirmationPolicyTest {
    @Test fun accidentalTapCancelledPointerAndShortDragCannotCommit() {
        val policy = SlideConfirmationPolicy
        assertFalse(policy.approved(true, true, false, 0f))
        assertFalse(policy.approved(true, true, false, .91f))
        assertFalse(policy.approved(false, true, false, 1f))
        assertFalse(policy.approved(true, false, false, 1f))
        assertFalse(policy.approved(true, true, true, 1f))
        assertTrue(policy.approved(true, true, false, 1f))
        assertTrue(policy.approved(true, true, false, .92f))
    }

    @Test fun displacementClampsAndNeverDividesByZero() {
        assertEquals(0f, SlideConfirmationPolicy.progress(100f, 80f, 0f), 0f)
        assertEquals(0f, SlideConfirmationPolicy.progress(20f, 80f, 100f), 0f)
        assertEquals(.5f, SlideConfirmationPolicy.progress(130f, 80f, 100f), .0001f)
        assertEquals(1f, SlideConfirmationPolicy.progress(300f, 80f, 100f), 0f)
    }
}
