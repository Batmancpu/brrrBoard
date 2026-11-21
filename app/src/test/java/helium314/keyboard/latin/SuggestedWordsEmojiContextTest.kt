// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestedWordsEmojiContextTest {
    @Test fun rankedEmojiCandidatesAreBoundedAndDoNotChangeCorrectionMetadata() {
        val words = SuggestedWords(
            arrayListOf<SuggestedWords.SuggestedWordInfo>(),
            null, null, false, false, false,
            SuggestedWords.INPUT_STYLE_TYPING,
            SuggestedWords.NOT_A_SEQUENCE_NUMBER,
            listOf("🎂", "🎉")
        )
        assertTrue(words.isEmpty())
        assertEquals(listOf("🎂", "🎉"), words.mRankedEmojiCandidates)
        assertFalse(words.mWillAutoCorrect)
        // Ordinary application completions and gestures carry no contextual
        // Emoji list and cannot retain a previous editor's context text.
        val neutral = SuggestedWords(
            arrayListOf<SuggestedWords.SuggestedWordInfo>(), null, null, false, false, false,
            SuggestedWords.INPUT_STYLE_NONE,
            SuggestedWords.NOT_A_SEQUENCE_NUMBER
        )
        assertTrue(neutral.mRankedEmojiCandidates.isEmpty())
    }

    @Test fun rankedEmojiCandidatesAreCopiedCappedAndImmutable() {
        val mutable = (0 until 60).map { "emoji-$it" }.toMutableList()
        val words = SuggestedWords(
            arrayListOf<SuggestedWords.SuggestedWordInfo>(), null, null, false, false, false,
            SuggestedWords.INPUT_STYLE_PREDICTION,
            SuggestedWords.NOT_A_SEQUENCE_NUMBER, mutable
        )
        mutable.clear()
        assertEquals(48, words.mRankedEmojiCandidates.size)
        assertEquals("emoji-0", words.mRankedEmojiCandidates.first())
        try {
            (words.mRankedEmojiCandidates as MutableList<String>).add("stale")
            throw AssertionError("suggestion snapshot should be immutable")
        } catch (_: UnsupportedOperationException) {
            // Expected: UI consumers must not mutate a shared ranking snapshot.
        }
    }
}
