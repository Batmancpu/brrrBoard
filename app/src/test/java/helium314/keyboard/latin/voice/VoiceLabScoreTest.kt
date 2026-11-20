// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class VoiceLabScoreTest {
    @Test fun noSpeechHasNoLevelOrClipping() {
        assertEquals(VoiceLabScore.Signal(0, 0.0), VoiceLabScore.signal(ByteArray(12)))
    }

    @Test fun detectsClippingWithoutKeepingSpeech() {
        val pcm = byteArrayOf(0xff.toByte(), 0x7f, 0, 0, 0, 0x80.toByte(), 0, 0)
        val result = VoiceLabScore.signal(pcm)
        assertEquals(50.0, result.clippingPercent, 0.001)
        assertEquals(true, result.levelPercent > 0)
    }

    @Test fun punctuationAndCapitalizationDoNotChangeWordError() {
        assertEquals(0, VoiceLabScore.wordErrorPercent("Kal office jaana hai!", "kal, OFFICE jaana hai."))
        assertEquals(25, VoiceLabScore.wordErrorPercent("kal office jaana hai", "kal office hai"))
        assertEquals(25, VoiceLabScore.wordErrorPercent("kal office jaana hai", "kal school jaana hai"))
        assertEquals(100, VoiceLabScore.wordErrorPercent("hello", ""))
        assertNull(VoiceLabScore.wordErrorPercent("!?", "hello"))
    }
}
