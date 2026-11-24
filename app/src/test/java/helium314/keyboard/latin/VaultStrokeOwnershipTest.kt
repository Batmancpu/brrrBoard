// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import org.junit.Assert.*
import org.junit.Test

class VaultStrokeOwnershipTest {
    @Test fun normalSwipeNeverBecomesPrivateWhenItCrossesFooter() {
        val stream = VaultStrokeOwnership()
        assertFalse(stream.begin(false))
        assertFalse(stream.owns)
        assertFalse(stream.finish())
    }
    @Test fun footerClaimsWholeStreamEvenAfterRejection() {
        val stream = VaultStrokeOwnership()
        assertTrue(stream.begin(true))
        assertTrue(stream.valid)
        stream.invalidate()
        assertTrue(stream.owns)
        assertFalse(stream.finish())
        assertFalse(stream.owns)
    }
    @Test fun oneContinuousValidStrokeCompletesOnce() {
        val stream = VaultStrokeOwnership()
        assertTrue(stream.begin(true))
        assertTrue(stream.finish())
        assertFalse(stream.finish())
    }
    @Test fun onlyCenterFooterCanStart() {
        assertTrue(VaultStrokeCapture.inStartRegion(180f, 239f, 360, 240, 112, 40))
        assertFalse(VaultStrokeCapture.inStartRegion(180f, 190f, 360, 240, 112, 40))
        assertFalse(VaultStrokeCapture.inStartRegion(40f, 239f, 360, 240, 112, 40))
        assertFalse(VaultStrokeCapture.inStartRegion(320f, 239f, 360, 240, 112, 40))
    }
}
