// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FooterMicTapPolicyTest {
    @Test fun defaultTapDispatchesImmediatelyAndNeverTwice() {
        assertTrue(FooterMicTapPolicy.dispatchSingleTap(false, confirmed = false))
        assertFalse(FooterMicTapPolicy.dispatchSingleTap(false, confirmed = true))
    }

    @Test fun legacyDoubleTapPreservesConfirmationWindow() {
        assertFalse(FooterMicTapPolicy.dispatchSingleTap(true, confirmed = false))
        assertTrue(FooterMicTapPolicy.dispatchSingleTap(true, confirmed = true))
    }

    @Test fun liveMicAndDecodeAreImmediateRegardlessOfIdleGesture() {
        assertTrue(FooterMicTapPolicy.isImmediate(true, false))
        assertTrue(FooterMicTapPolicy.isImmediate(false, true))
        assertFalse(FooterMicTapPolicy.isImmediate(false, false))
    }
}
