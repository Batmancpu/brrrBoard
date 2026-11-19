// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class FooterIncognitoMotionTest {
    @Test fun entranceHasHatThenNodThenGlassesAndFinalGlint() {
        val normal = FooterIncognitoMotion.settled(false)
        val hat = FooterIncognitoMotion.at(normal, true, .30f)
        val lookDown = FooterIncognitoMotion.at(normal, true, .52f)
        val shades = FooterIncognitoMotion.at(normal, true, .88f)
        assertTrue(hat.hat > .99f)
        assertEquals(0f, hat.glasses, .001f)
        assertTrue(lookDown.nod > .75f)
        assertTrue(shades.glasses > .99f)
        assertEquals(1f, FooterIncognitoMotion.at(normal, true, 1f).glint, .001f)
    }

    @Test fun exitRevealsEyesBeforeHatFallsBehind() {
        val incognito = FooterIncognitoMotion.settled(true)
        val lookDown = FooterIncognitoMotion.at(incognito, false, .30f)
        val bareEyes = FooterIncognitoMotion.at(incognito, false, .62f)
        val fall = FooterIncognitoMotion.at(incognito, false, .90f)
        assertTrue(lookDown.nod > .7f)
        assertEquals(0f, bareEyes.glasses, .001f)
        assertEquals(1f, bareEyes.hat, .001f)
        assertTrue(fall.hatBehind > .95f)
        assertTrue(fall.hatDrop > 0f)
    }

    @Test fun reversalStartsAtLastDrawnPoseWithoutSnapping() {
        val midEntrance = FooterIncognitoMotion.at(
            FooterIncognitoMotion.settled(false), true, .67f)
        val firstReversedFrame = FooterIncognitoMotion.at(midEntrance, false, 0f)
        assertEquals(midEntrance, firstReversedFrame)
        val midExit = FooterIncognitoMotion.at(midEntrance, false, .63f)
        assertEquals(midExit, FooterIncognitoMotion.at(midExit, true, 0f))
    }

    @Test fun allChannelsRemainBoundedAcrossFastRepeatedToggles() {
        var current = FooterIncognitoMotion.settled(false)
        repeat(24) { i ->
            current = FooterIncognitoMotion.at(current, i % 2 == 0, .37f)
            assertTrue(current.hat in 0f..1f)
            assertTrue(current.glasses in 0f..1f)
            assertTrue(current.nod in 0f..1f)
            assertTrue(current.hatBehind in 0f..1f)
            assertTrue(current.glint in 0f..1f)
        }
    }
}
