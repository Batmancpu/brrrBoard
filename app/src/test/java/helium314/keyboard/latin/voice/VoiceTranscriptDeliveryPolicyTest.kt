// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import org.junit.Assert.assertEquals
import org.junit.Test

class VoiceTranscriptDeliveryPolicyTest {
    private val p = VoiceTranscriptDeliveryPolicy

    @Test fun unchangedEditorInsertsAndEveryOtherDestinationDrops() {
        assertEquals(VoiceTranscriptDeliveryPolicy.Outcome.INSERT,
            p.choose(false, true, true, true))
        assertEquals(VoiceTranscriptDeliveryPolicy.Outcome.DROP,
            p.choose(false, true, false, true))
        assertEquals(VoiceTranscriptDeliveryPolicy.Outcome.DROP,
            p.choose(false, false, true, true))
        assertEquals(VoiceTranscriptDeliveryPolicy.Outcome.DROP,
            p.choose(true, true, true, true))
    }

    @Test fun failedRecognitionNeverCreatesRecoveryCard() {
        assertEquals(VoiceTranscriptDeliveryPolicy.Outcome.DROP,
            p.choose(true, false, false, false))
    }
}
