// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.suggestions

import helium314.keyboard.latin.KeyboardMotionSpec
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestionInkTimingTest {
    @Test fun inkStartsAfterQuietBeatAndArrivesBeforeSettleEnd() {
        assertEquals(0f, SuggestionInkTiming.reveal(0f), 0f)
        assertEquals(0f, SuggestionInkTiming.reveal(
            KeyboardMotionSpec.IOS_SUGGESTION_REVEAL_DELAY_MS.toFloat()), 0f)
        val midpoint = KeyboardMotionSpec.IOS_SUGGESTION_REVEAL_DELAY_MS +
            KeyboardMotionSpec.IOS_SUGGESTION_REVEAL_MS / 2
        assertTrue(SuggestionInkTiming.reveal(midpoint.toFloat()) > .8f)
        assertEquals(1f, SuggestionInkTiming.reveal(
            KeyboardMotionSpec.IOS_SUGGESTION_SETTLE_MS.toFloat()), 0f)
    }
}
