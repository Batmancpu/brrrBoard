// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import org.junit.Assert.assertEquals
import org.junit.Test

class CorrectionPresetTest {
    @Test fun strengthUsesExistingThresholds() {
        assertEquals(CorrectionPreset.OFF, CorrectionPreset.read(false, .185f, true))
        assertEquals(CorrectionPreset.GENTLE, CorrectionPreset.read(true, .185f, false))
        assertEquals(CorrectionPreset.BALANCED, CorrectionPreset.read(true, .067f, false))
        assertEquals(CorrectionPreset.STRONG, CorrectionPreset.read(true, -1f, false))
        assertEquals(CorrectionPreset.CUSTOM, CorrectionPreset.read(true, .185f, true))
        assertEquals(CorrectionPreset.CUSTOM, CorrectionPreset.read(true, .1f, false))
    }
    @Test fun suggestionModes() {
        assertEquals(SuggestionVisibility.OFF, SuggestionVisibility.read(false, true))
        assertEquals(SuggestionVisibility.CONTEXTUAL, SuggestionVisibility.read(true, false))
        assertEquals(SuggestionVisibility.ALWAYS, SuggestionVisibility.read(true, true))
    }
}
