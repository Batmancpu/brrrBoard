// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.suggestions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestionConfidencePillGeometryTest {
    @Test fun pillWidthTracksWordButStaysInsideCenterSlot() {
        val short = SuggestionConfidencePillGeometry.width(35f, 180f, 1f)
        val long = SuggestionConfidencePillGeometry.width(108f, 180f, 1f)
        assertTrue(short < long)
        assertTrue(long <= 172f)
    }

    @Test fun remainsBoundedOnCompactScreens() {
        assertEquals(72f, SuggestionConfidencePillGeometry.width(500f, 80f, 1f), 0f)
        assertEquals(1f, SuggestionConfidencePillGeometry.width(500f, 0f, 1f), 0f)
    }
}
