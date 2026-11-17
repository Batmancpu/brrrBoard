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
}
