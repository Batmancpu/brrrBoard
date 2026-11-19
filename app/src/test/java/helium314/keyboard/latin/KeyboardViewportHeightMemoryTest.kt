// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import org.junit.Assert.assertEquals
import org.junit.Test

class KeyboardViewportHeightMemoryTest {
    @Test fun hiddenAlphabetMeasurementDoesNotEraseLastRealHeight() {
        val cache = KeyboardViewportHeightMemory()
        cache.observe(0, true)
        assertEquals(0, cache.lastTypingHeight)
        cache.observe(694, true)
        cache.observe(0, false)
        cache.observe(562, false)
        assertEquals(694, cache.lastTypingHeight)
        cache.observe(648, true)
        assertEquals(648, cache.lastTypingHeight)
    }

    @Test fun staleScreenWidthCannotExpandEmojiBrowseAfterRotationOrResize() {
        val cache = KeyboardViewportHeightMemory()
        cache.observe(810, true, 1080)
        assertEquals(810, cache.heightForWidth(1080))
        assertEquals(0, cache.heightForWidth(900))
        assertEquals(0, cache.heightForWidth(0))
        cache.observe(600, true, 900)
        cache.observe(1010, false, 900)
        assertEquals(0, cache.heightForWidth(1080))
        assertEquals(600, cache.heightForWidth(900))
    }
}
