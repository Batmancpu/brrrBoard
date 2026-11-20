package helium314.keyboard.latin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class NextWordCacheKeyTest {
    @Test fun syntheticSamePhraseCannotCrossLocaleStyleOrKeyboard() {
        val context = NgramContext(NgramContext.WordInfo("tomorrow"))
        fun key(locale: String, style: Int, keyboard: Int, offensive: Boolean = false) =
            NextWordCacheKey.from(context, locale, keyboard, style, offensive, false)
        val baseline = key("en-US", 1, 42)
        assertEquals(baseline, key("en-US", 1, 42))
        assertNotEquals(baseline, key("en-GB", 1, 42))
        assertNotEquals(baseline, key("en-US", 2, 42))
        assertNotEquals(baseline, key("en-US", 1, 43))
        assertNotEquals(baseline, key("en-US", 1, 42, true))
    }

    @Test fun snapshotIsNotMutatedAfterCacheInsertion() {
        val context = NgramContext(NgramContext.WordInfo("hello"),
            NgramContext.WordInfo.BEGINNING_OF_SENTENCE_WORD_INFO)
        val key = NextWordCacheKey.from(context, "en-US", 12, 1, false, false)
        context.changeWordIfAfterBeginningOfSentence("hello", "changed")
        assertEquals("hello", key.context.first().text)
    }
}
