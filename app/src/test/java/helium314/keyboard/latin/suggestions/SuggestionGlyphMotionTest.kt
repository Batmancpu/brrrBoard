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

    @Test fun newSuggestionHasEveryCharacterVisibleOnTheFirstFrame() {
        val result = requireNotNull(plan("they", "they're"))
        assertEquals("they're", result.word)
        assertEquals(7, result.startOffsets.size)
        assertEquals(7, result.startScales.size)
        // Unlike v1.0.202, there is no alpha track and no postponed glyph.
        assertTrue(result.startScales.all { it >= .97f && it <= 1f })
        assertTrue(result.startScales.slice(4..6).all { it == .97f })
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
        assertEquals(.97f, result.startScales[4], 0f)
    }

    @Test fun interruptedMotionStartsFromActuallyVisiblePosition() {
        val previous = SuggestionGlyphFrame("they", floatArrayOf(-3.25f, -2f, -1f, 0f))
        val result = requireNotNull(SuggestionGlyphMotion.plan(previous, "they're",
            floatArrayOf(-3f, -2f, -1f, 0f, 1f, 2f, 3f), 8f))
        assertEquals(-3.25f, SuggestionGlyphMotion.x(result, 0, 0f), 0f)
        assertEquals(-3f, SuggestionGlyphMotion.x(result, 0, 1f), 0f)
    }

    @Test fun unrelatedWordsAndUnchangedWordsUseNativeTextView() {
        assertNull(plan("hello", "world"))
        assertNull(plan("hello", "hello"))
        assertNull(plan("a", "an"))
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
        assertTrue(SuggestionGlyphMotion.SETTLE_MS <= 120L)
        for (i in result.word.indices) {
            assertEquals(result.positions[i], SuggestionGlyphMotion.x(result, i, 1f), 0f)
            assertEquals(1f, SuggestionGlyphMotion.scale(result, i, 1f), 0f)
        }
        assertNotNull(result)
    }
}
