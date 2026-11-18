// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestedWordsEmojiContextTest {
    @Test fun previousTokenHintIsEphemeralAndDoesNotChangeCorrectionMetadata() {
        val words = SuggestedWords(
            arrayListOf<SuggestedWords.SuggestedWordInfo>(),
            null, null, false, false, false,
            SuggestedWords.INPUT_STYLE_TYPING,
            SuggestedWords.NOT_A_SEQUENCE_NUMBER,
            "happy birthday"
        )
        assertTrue(words.isEmpty())
        assertEquals("happy birthday", words.mEmojiContextHint)
        assertFalse(words.mWillAutoCorrect)
        // Every non-typing call site retains the old constructor and gets no
        // context hint, so ordinary application completions and gestures cannot
        // accidentally reuse an editor's preceding words.
        val neutral = SuggestedWords(
            arrayListOf<SuggestedWords.SuggestedWordInfo>(), null, null, false, false, false,
            SuggestedWords.INPUT_STYLE_NONE,
            SuggestedWords.NOT_A_SEQUENCE_NUMBER
        )
        assertEquals("", neutral.mEmojiContextHint)
    }
}
