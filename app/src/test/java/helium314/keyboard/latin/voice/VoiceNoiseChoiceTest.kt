// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The single-Binder-transaction ceiling is part of the crash containment. */
class VoiceNoiseChoiceTest {
    @Test fun newInstallDefaultsToBrrCleanButSavedLegacyChoicesSurviveUpgrade() {
        assertEquals(VoiceNoiseMode.GTCRN, VoiceNoiseChoice.modeForLegacyChoice(false, false))
        assertEquals(VoiceNoiseMode.ANDROID, VoiceNoiseChoice.modeForLegacyChoice(true, true))
        assertEquals(VoiceNoiseMode.OFF, VoiceNoiseChoice.modeForLegacyChoice(true, false))
    }

    @Test fun explicitCurrentChoiceTakesPrecedenceAndUnknownValuesFailSafely() {
        assertEquals(VoiceNoiseMode.OFF, VoiceNoiseChoice.modeForSelection(VoiceNoiseMode.OFF.name))
        assertEquals(VoiceNoiseMode.ANDROID, VoiceNoiseChoice.modeForSelection(VoiceNoiseMode.ANDROID.name))
        assertEquals(VoiceNoiseMode.GTCRN, VoiceNoiseChoice.modeForSelection(VoiceNoiseMode.GTCRN.name))
        assertEquals(VoiceNoiseMode.GTCRN, VoiceNoiseChoice.modeForSelection("future"))
    }

    @Test fun limitsNativeProcessingToBoundedAlignedShortUtterances() {
        assertFalse(VoiceNoiseChoice.eligible(0))
        assertFalse(VoiceNoiseChoice.eligible(15_999))
        assertTrue(VoiceNoiseChoice.eligible(16_000))
        assertTrue(VoiceNoiseChoice.eligible(VoiceNoiseChoice.MAX_GTCRN_BYTES))
        assertFalse(VoiceNoiseChoice.eligible(VoiceNoiseChoice.MAX_GTCRN_BYTES + 2))
        assertFalse(VoiceNoiseChoice.eligible(16_001))
    }
}
