// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.suggestions

import org.junit.Assert.*
import org.junit.Test

class QuickTypeMotionPolicyTest {
    @Test fun expansionAndDeletionKeepOrderedLetters() {
        assertEquals(listOf(0 to 0, 1 to 1, 2 to 2), QuickTypeMotionPolicy.matches("cat", "cats"))
        assertEquals(listOf(0 to 0, 1 to 1, 2 to 2), QuickTypeMotionPolicy.matches("cats", "cat"))
        assertEquals(listOf(0 to 0, 1 to 2, 2 to 3), QuickTypeMotionPolicy.matches("dont".take(3), "d'ont".take(4)))
    }
    @Test fun unrelatedLabelsDissolveAndRepeatedLettersAreUnique() {
        assertTrue(QuickTypeMotionPolicy.matches("the", "and").isEmpty())
        val pairs = QuickTypeMotionPolicy.matches("letter", "letters")
        assertEquals(6, pairs.size)
        assertEquals(6, pairs.map { it.first }.distinct().size)
        assertEquals(6, pairs.map { it.second }.distinct().size)
    }
    @Test fun boundariesAndUnsupportedShapingFallBack() {
        assertTrue(QuickTypeMotionPolicy.supports("don’t"))
        assertTrue(QuickTypeMotionPolicy.supports(""))
        assertFalse(QuickTypeMotionPolicy.supports("😀"))
        assertFalse(QuickTypeMotionPolicy.supports("a".repeat(41)))
        assertTrue(QuickTypeMotionPolicy.matches("a".repeat(81), "aa").isEmpty())
    }
    @Test fun progressIsBoundedAndMonotonic() {
        for (duration in listOf(QuickTypeMotionPolicy.GLYPH_MS,
            QuickTypeMotionPolicy.REPLACEMENT_MS, QuickTypeMotionPolicy.CHROME_MS)) {
            assertEquals(0f, QuickTypeMotionPolicy.progress(-1f, duration), 0f)
            assertEquals(1f, QuickTypeMotionPolicy.progress(duration.toFloat(), duration), 0f)
            assertTrue(QuickTypeMotionPolicy.progress(30f, duration) <
                QuickTypeMotionPolicy.progress(60f, duration))
        }
    }
    @Test fun confidenceCapsuleIsSlotSizedAndBounded() {
        assertEquals(178f, SuggestionConfidencePillGeometry.fixedWidth(180f, 1f), 0f)
        assertEquals(1f, SuggestionConfidencePillGeometry.fixedWidth(0f, 1f), 0f)
    }
}
