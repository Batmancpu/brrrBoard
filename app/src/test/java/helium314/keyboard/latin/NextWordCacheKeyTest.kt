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
        // Use a mutable CharSequence to prove the key takes a value snapshot.
        // NgramContext.changeWordIfAfterBeginningOfSentence calls Android
        // TextUtils.equals, unavailable in the plain-JVM unit test runner.
        val mutableWord = StringBuilder("hello")
        val context = NgramContext(NgramContext.WordInfo(mutableWord),
            NgramContext.WordInfo.BEGINNING_OF_SENTENCE_WORD_INFO)
        val key = NextWordCacheKey.from(context, "en-US", 12, 1, false, false)
        mutableWord.replace(0, mutableWord.length, "changed")
        assertEquals("hello", key.context.first().text)
        assertEquals("changed", context.getNthPrevWord(1).toString())
    }
}
