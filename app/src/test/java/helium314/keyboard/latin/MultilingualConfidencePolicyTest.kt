// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import org.junit.Assert.assertEquals
import org.junit.Test

class MultilingualConfidencePolicyTest {
    @Test fun aSingleMainDictionaryMatchSelectsOnlyThatLanguage() {
        assertEquals(0, MultilingualConfidencePolicy.uniqueLanguageIndex(booleanArrayOf(true, false)))
        assertEquals(1, MultilingualConfidencePolicy.uniqueLanguageIndex(booleanArrayOf(false, true)))
    }

    @Test fun sharedAndUnknownWordsProvideNoLanguageEvidence() {
        assertEquals(-1, MultilingualConfidencePolicy.uniqueLanguageIndex(booleanArrayOf(true, true)))
        assertEquals(-1, MultilingualConfidencePolicy.uniqueLanguageIndex(booleanArrayOf(false, false)))
        assertEquals(-1, MultilingualConfidencePolicy.uniqueLanguageIndex(booleanArrayOf()))
    }
}
