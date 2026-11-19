// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import org.junit.Assert.assertEquals
import org.junit.Test

class FooterMicPulsePolicyTest {
    @Test fun onlyGlyphScalesAndNeverBeyondSmallSafeBound() {
        assertEquals(1f, FooterMicPulsePolicy.scale(-1f), 0.0001f)
        assertEquals(1f, FooterMicPulsePolicy.scale(0f), 0.0001f)
        assertEquals(1.0425f, FooterMicPulsePolicy.scale(.5f), 0.0001f)
        assertEquals(1.085f, FooterMicPulsePolicy.scale(1f), 0.0001f)
        assertEquals(1.085f, FooterMicPulsePolicy.scale(2f), 0.0001f)
    }
}
