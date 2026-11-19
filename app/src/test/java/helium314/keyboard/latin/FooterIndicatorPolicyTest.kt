// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FooterIndicatorPolicyTest {
    @Test fun darkAndLightHaveDistinctOpaqueSystemInspiredRed() {
        val light = FooterIndicatorPolicy.voiceTint(false)
        val dark = FooterIndicatorPolicy.voiceTint(true)
        assertTrue(light != dark)
        assertEquals(0xFF, light ushr 24)
        assertEquals(0xFF, dark ushr 24)
    }

    @Test fun processingPulseIsQuieterAndAlwaysBounded() {
        assertEquals(0f, FooterIndicatorPolicy.pulseIntensity(-1f, false), 0f)
        assertEquals(1f, FooterIndicatorPolicy.pulseIntensity(2f, false), 0f)
        assertEquals(0.48f, FooterIndicatorPolicy.pulseIntensity(1f, true), 0f)
        assertTrue(FooterIndicatorPolicy.pulseIntensity(0.5f, true) <
            FooterIndicatorPolicy.pulseIntensity(0.5f, false))
    }

    @Test fun blueOfflineNeverSharesTheRedServerColor() {
        for (dark in listOf(false, true)) {
            val red = FooterIndicatorPolicy.voiceTint(dark)
            val blue = FooterIndicatorPolicy.voiceTint(dark, offline = true)
            assertTrue(blue != red)
            assertEquals(0xFF, blue ushr 24)
            assertTrue((blue and 0xff) > ((blue shr 16) and 0xff))
        }
    }
}
