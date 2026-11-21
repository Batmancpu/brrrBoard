// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The single-Binder-transaction ceiling is part of the crash containment. */
class VoiceNoiseChoiceTest {
    @Test fun limitsNativeProcessingToBoundedAlignedShortUtterances() {
        assertFalse(VoiceNoiseChoice.eligible(0))
        assertFalse(VoiceNoiseChoice.eligible(15_999))
        assertTrue(VoiceNoiseChoice.eligible(16_000))
        assertTrue(VoiceNoiseChoice.eligible(VoiceNoiseChoice.MAX_GTCRN_BYTES))
        assertFalse(VoiceNoiseChoice.eligible(VoiceNoiseChoice.MAX_GTCRN_BYTES + 2))
        assertFalse(VoiceNoiseChoice.eligible(16_001))
    }
}
