// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.suggestions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SuggestionGlyphDiffTest {
    @Test fun sharedPrefixStaysVisibleWhenWordExpands() {
        assertEquals(SuggestionGlyphDiff.Range(7, 10),
            SuggestionGlyphDiff.changedRange("suggested", "suggestion"))
        assertEquals(SuggestionGlyphDiff.Range(4, 5),
            SuggestionGlyphDiff.changedRange("sugg", "sugge"))
    }

    @Test fun keepsCommonSuffixAndNeverAnimatesAnUnchangedWord() {
        assertEquals(SuggestionGlyphDiff.Range(0, 1),
            SuggestionGlyphDiff.changedRange("bat", "cat"))
        assertNull(SuggestionGlyphDiff.changedRange("going", "going"))
        assertNull(SuggestionGlyphDiff.changedRange("hello!", "hello"))
    }

    @Test fun avoidsSplittingSimpleSurrogatePairs() {
        assertEquals(SuggestionGlyphDiff.Range(2, 4),
            SuggestionGlyphDiff.changedRange("go", "go😀"))
        assertEquals(SuggestionGlyphDiff.Range(0, 2),
            SuggestionGlyphDiff.changedRange("go", "😀go"))
    }

    @Test fun emptyInputsAreSafe() {
        assertNull(SuggestionGlyphDiff.changedRange(null, ""))
        assertEquals(SuggestionGlyphDiff.Range(0, 1),
            SuggestionGlyphDiff.changedRange("", "a"))
    }
}
