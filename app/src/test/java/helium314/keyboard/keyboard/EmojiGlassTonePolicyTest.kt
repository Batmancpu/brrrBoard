// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class EmojiGlassTonePolicyTest {
    @Test fun disabledMeansNoFilterAndRestoresOriginalSourcePixels() {
        assertNull(EmojiGlassTonePolicy.matrix(false, true))
        assertNull(EmojiGlassTonePolicy.matrix(false, false))
    }

    @Test fun darkOnlyLowersSourceRgbAndLeavesAlphaUntouched() {
        val m = EmojiGlassTonePolicy.matrix(true, true)!!
        assertEquals(20, m.size)
        assertTrue(m[0] in 0f..0.99f)
        assertEquals(m[0], m[6], 0f)
        assertEquals(m[0], m[12], 0f)
        assertEquals(0f, m[4], 0f)
        assertEquals(0f, m[9], 0f)
        assertEquals(0f, m[14], 0f)
        assertEquals(1f, m[18], 0f)
        assertEquals(0f, m[19], 0f)
    }

    @Test fun lightOnlyMovesSourceRgbTowardWhiteAndKeepsAlpha() {
        val m = EmojiGlassTonePolicy.matrix(true, false)!!
        assertEquals(20, m.size)
        assertTrue(m[0] in 0f..1f)
        assertTrue(m[4] > 0f)
        assertEquals(m[4], m[9], 0f)
        assertEquals(m[4], m[14], 0f)
        assertEquals(1f, m[18], 0f)
        assertEquals(0f, m[19], 0f)
        assertEquals(255f, 255f * m[0] + m[4], 0.001f)
    }
}
