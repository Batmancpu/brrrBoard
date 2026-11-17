// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenshotSuggestionPolicyTest {
    @Test fun oneMinuteWindowUsesActualScreenshotTime() {
        val now = 1_900_000_000_000L
        assertTrue(ScreenshotSuggestionPolicy.isRecent(now, now))
        assertTrue(ScreenshotSuggestionPolicy.isRecent(now - 59_999, now))
        assertTrue(ScreenshotSuggestionPolicy.isRecent(now - 60_000, now))
        assertFalse(ScreenshotSuggestionPolicy.isRecent(now - 60_001, now))
        assertFalse(ScreenshotSuggestionPolicy.isRecent(now + 1, now))
        assertFalse(ScreenshotSuggestionPolicy.isRecent(0, now))
    }
}
