// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class KeyboardMotionSpecTest {
    @Test fun onlyRealSameSurfaceReplacementsMorph() {
        assertTrue(KeyboardMotionSpec.shouldMorphWords("would", "world", true, true))
        assertFalse(KeyboardMotionSpec.shouldMorphWords("would", "would", true, true))
        assertFalse(KeyboardMotionSpec.shouldMorphWords("", "world", true, true))
        assertFalse(KeyboardMotionSpec.shouldMorphWords("would", "", true, true))
        assertFalse(KeyboardMotionSpec.shouldMorphWords("would", "world", false, true))
        assertFalse(KeyboardMotionSpec.shouldMorphWords("would", "world", true, false))
    }

    @Test fun durationsAreShortAndVoicePulseIsSlower() {
        assertTrue(KeyboardMotionSpec.WORD_CROSSFADE_MS in 100L..200L)
        assertTrue(KeyboardMotionSpec.SEARCH_RESULT_MS in 100L..220L)
        assertTrue(KeyboardMotionSpec.VOICE_PULSE_CYCLE_MS > 1_300L)
    }
}
