// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FooterIncognitoMotionTest {
    @Test fun matchingPrivacyBadgeUsesPremiumGoldWithoutChangingVoicePulsePalette() {
        assertEquals(0xFFD8B766.toInt(), FooterPrivacyPalette.GOLD)
        assertEquals(0xFFF4DC98.toInt(), FooterPrivacyPalette.HIGHLIGHT)
        assertEquals(0xFF916B2B.toInt(), FooterPrivacyPalette.DEPTH)
        assertTrue(FooterPrivacyPalette.GOLD != FooterIndicatorPolicy.processingTint(true))
        assertTrue(FooterPrivacyPalette.GOLD != FooterIndicatorPolicy.voiceTint(false))
    }

    @Test fun privacyBadgeMotionIsSmallReversibleAndSynchronized() {
        assertTrue(FooterIncognitoMotion.shouldAnimatePrivacyChange(true, true))
        assertEquals(false, FooterIncognitoMotion.shouldAnimatePrivacyChange(false, true))
        assertEquals(false, FooterIncognitoMotion.shouldAnimatePrivacyChange(true, false))

        val normal = FooterIncognitoMotion.settled(false)
        val entering = FooterIncognitoMotion.at(normal, true, .5f)
        assertTrue(entering.shield in 0f..1f)
        assertEquals(entering, FooterIncognitoMotion.at(entering, false, 0f))
        assertEquals(1f, FooterIncognitoMotion.at(normal, true, 1f).shield, .001f)

        val privateFrame = FooterIncognitoMotion.settled(true)
        val leaving = FooterIncognitoMotion.at(privateFrame, false, .5f)
        assertEquals(leaving, FooterIncognitoMotion.at(leaving, true, 0f))
        assertEquals(0f, FooterIncognitoMotion.at(privateFrame, false, 1f).shield, .001f)
        assertEquals(220L, FooterIncognitoMotion.ENTER_MS)
        assertEquals(180L, FooterIncognitoMotion.EXIT_MS)
    }
}
