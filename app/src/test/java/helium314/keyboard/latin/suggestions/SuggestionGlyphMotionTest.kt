// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.suggestions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class SuggestionGlyphMotionTest {
    private fun positions(word: String): FloatArray =
        FloatArray(word.length) { it.toFloat() - word.length / 2f }

    private fun plan(old: String, next: String, limit: Float = 8f): SuggestionGlyphPlan? =
        SuggestionGlyphMotion.plan(SuggestionGlyphFrame(old, positions(old)),
            next, positions(next), limit)

    @Test fun newSuggestionIsLegibleAndNewSuffixFadesIn() {
        val result = requireNotNull(plan("they", "they're"))
        assertEquals("they're", result.word)
        assertEquals(7, result.startOffsets.size)
        assertEquals(7, result.startScales.size)
        assertTrue(result.startAlphas.all { it >= .58f && it <= 1f })
        assertTrue(result.startScales.all { it >= .80f && it <= 1f })
        assertTrue(result.startScales.slice(4..6).all { it == .80f })
        assertEquals(.58f, SuggestionGlyphMotion.incomingAlpha(result, 4, 0f), 0f)
        assertEquals(1f, SuggestionGlyphMotion.incomingAlpha(result, 4, 1f), 0f)
    }

    @Test fun contractionAndExpansionMoveRetainedLettersWithinSmallBound() {
        for ((before, after) in listOf("they" to "they're", "they're" to "they")) {
            val result = requireNotNull(plan(before, after, 4f))
            assertTrue(result.startOffsets.any { abs(it) > .35f })
            assertTrue(result.startOffsets.all { abs(it) <= 4f })
            assertTrue(result.startScales.all { it > 0f })
        }
    }

    @Test fun interiorReplacementKeepsCommonPrefixAndSuffix() {
        val result = requireNotNull(plan("bookkeeper", "bookseller"))
        assertEquals(1f, result.startScales[0], 0f)
        assertEquals(1f, result.startScales[1], 0f)
        assertEquals(1f, result.startScales[2], 0f)
        assertEquals(1f, result.startScales[3], 0f)
        assertEquals(1f, result.startScales.last(), 0f)
        assertEquals(.80f, result.startScales[4], 0f)
    }

    @Test fun interruptedMotionStartsFromActuallyVisiblePosition() {
        val previous = SuggestionGlyphFrame("they", floatArrayOf(-3.25f, -2f, -1f, 0f))
        val result = requireNotNull(SuggestionGlyphMotion.plan(previous, "they're",
            floatArrayOf(-3f, -2f, -1f, 0f, 1f, 2f, 3f), 8f))
        assertEquals(-3.25f, SuggestionGlyphMotion.x(result, 0, 0f), 0f)
        assertEquals(-3f, SuggestionGlyphMotion.x(result, 0, 1f), 0f)
    }

    @Test fun unrelatedAndOneLetterTransitionsNeverSnap() {
        val unrelated = requireNotNull(plan("cat", "dog"))
        assertTrue(unrelated.startOffsets.any { abs(it) > 1f })
        assertTrue(unrelated.outgoingVisible.all { it })
        assertFalse(unrelated.isRelated)
        assertTrue(unrelated.startScales.all { it < 1f })
        assertTrue(SuggestionGlyphMotion.outgoingAlpha(unrelated, 0, 0f) > 0f)
        assertEquals(0f, SuggestionGlyphMotion.outgoingAlpha(unrelated, 0, 1f), 0f)
        val oneLetter = requireNotNull(plan("t", "there"))
        assertEquals(1f, oneLetter.startScales.first(), 0f)
        assertTrue(oneLetter.startScales.drop(1).all { it < 1f })
        assertTrue(oneLetter.outgoingVisible.none { it })
    }

    @Test fun competingCandidatesAfterTheFirstLetterBothAnimate() {
        val first = requireNotNull(plan("tree", "there"))
        val second = requireNotNull(plan("there", "they"))
        assertTrue(first.startOffsets.any { abs(it) > 0f })
        assertTrue(second.startOffsets.any { abs(it) > 0f })
        assertTrue(first.startScales.any { it == .80f })
        assertTrue(second.startScales.any { it == .80f })
        assertTrue(first.isRelated)
        assertTrue(second.isRelated)
    }

    @Test fun coincidentalSharedLetterDoesNotGlueUnrelatedWordsTogether() {
        val result = requireNotNull(plan("ask", "music"))
        assertFalse(result.isRelated)
        assertTrue(result.outgoingVisible.all { it })
        assertTrue(result.startScales.all { it < 1f })
    }

    @Test fun unchangedWordsNeedNoAnimation() {
        assertNull(plan("hello", "hello"))
        assertNotNull(plan("a", "an"))
    }

    @Test fun longUnicodeRtlAndInvalidCoordinatesUseNativeTextView() {
        assertFalse(SuggestionGlyphMotion.supported("hello 👨‍👩‍👧"))
        assertFalse(SuggestionGlyphMotion.supported("مرحبا"))
        assertFalse(SuggestionGlyphMotion.supported("suggestion_that_is_far_too_long"))
        assertFalse(SuggestionGlyphMotion.supported("hello!"))
        assertFalse(SuggestionGlyphMotion.supported("cat123"))
        assertTrue(SuggestionGlyphMotion.supported("they're"))
        assertNull(SuggestionGlyphMotion.plan(
            SuggestionGlyphFrame("they", floatArrayOf(Float.NaN, 1f, 2f, 3f)),
            "their", positions("their"), 8f))
        assertNull(SuggestionGlyphMotion.plan(
            SuggestionGlyphFrame("they", positions("they")),
            "their", positions("their"), 0f))
    }

    @Test fun movementAlwaysFinishesWithinFiniteAnimation() {
        val result = requireNotNull(plan("suggested", "suggestion"))
        assertEquals(0f, SuggestionGlyphMotion.eased(0f), 0f)
        assertEquals(1f, SuggestionGlyphMotion.eased(1f), 0f)
        assertEquals(1f, SuggestionGlyphMotion.eased(2f), 0f)
        assertEquals(164L, SuggestionGlyphMotion.SETTLE_MS)
        for (i in result.word.indices) {
            assertEquals(result.positions[i], SuggestionGlyphMotion.x(result, i, 1f), 0f)
            assertEquals(1f, SuggestionGlyphMotion.scale(result, i, 1f), 0f)
        }
        assertNotNull(result)
    }
}
