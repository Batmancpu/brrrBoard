// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.clipboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ChromaticSearchInkPolicyTest {
    @Test fun onlyNonblankMatchingQueriesHaveChromaticInk() {
        assertFalse(ChromaticSearchInkPolicy.active(null, true))
        assertFalse(ChromaticSearchInkPolicy.active("", true))
        assertFalse(ChromaticSearchInkPolicy.active("   ", true))
        assertFalse(ChromaticSearchInkPolicy.active("hello", false))
        assertTrue(ChromaticSearchInkPolicy.active("hello", true))
    }

    @Test fun inkIsFullyOpaqueAndThemeAware() {
        val dark = ChromaticSearchInkPolicy.colors(true)
        val light = ChromaticSearchInkPolicy.colors(false)
        assertEquals(dark.size, light.size)
        assertTrue(dark.size >= 5)
        assertTrue(dark.any { it != dark[0] })
        assertTrue(light.any { it != light[0] })
        assertTrue(dark.toList() != light.toList())
        assertTrue((dark + light).all { it ushr 24 == 0xFF })
    }
}
