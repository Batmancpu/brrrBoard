// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FooterEmojiTapPolicyTest {
    @Test fun secondTapNeedsAnUnconsumedFirstTapAndShortDistance() {
        assertTrue(FooterEmojiTapPolicy.isSecondTap(true, 160, 5f, 5f, 300, 30f))
        assertFalse(FooterEmojiTapPolicy.isSecondTap(false, 160, 5f, 5f, 300, 30f))
        assertFalse(FooterEmojiTapPolicy.isSecondTap(true, 160, 40f, 5f, 300, 30f))
    }

    @Test fun delayedOrStaleTapsNeverToggleIncognito() {
        assertFalse(FooterEmojiTapPolicy.isSecondTap(true, 301, 0f, 0f, 300, 30f))
        assertFalse(FooterEmojiTapPolicy.isSecondTap(true, -1, 0f, 0f, 300, 30f))
        assertTrue(FooterEmojiTapPolicy.isSecondTap(true, 300, 0f, 0f, 300, 30f))
    }
}
