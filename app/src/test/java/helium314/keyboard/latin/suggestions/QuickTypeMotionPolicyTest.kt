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
    @Test fun stylesAreIndependentAndHaveBoundedProgress() {
        assertEquals(SuggestionMotionPreset.SCALE, SuggestionMotionPreset.fromId(null))
        assertEquals(SuggestionMotionPreset.SCALE, SuggestionMotionPreset.fromId("invalid"))
        assertEquals(4, SuggestionMotionPreset.values().map { it.id }.distinct().size)
        for (style in SuggestionMotionPreset.values()) {
            assertEquals(style, SuggestionMotionPreset.fromId(style.id))
            assertEquals(0f, QuickTypeMotionPolicy.glyphProgress(style, 0f, style.glyphMs), .0001f)
            assertEquals(1f, QuickTypeMotionPolicy.glyphProgress(style, style.glyphMs.toFloat(), style.glyphMs), .0001f)
            val values = (0..100).map {
                QuickTypeMotionPolicy.glyphProgress(style, style.glyphMs * it / 100f, style.glyphMs)
            }
            assertTrue(values.all { it in 0f..1.0001f })
            assertTrue(values.zipWithNext().all { (a, b) -> b >= a })
        }
        assertEquals(1f, SuggestionMotionPreset.CLASSIC.entryScale, 0f)
        assertTrue(SuggestionMotionPreset.SCALE.entryScale < .2f)
        assertEquals(30L, QuickTypeMotionPolicy.delay(SuggestionMotionPreset.RIPPLE, 100))
        assertEquals(0L, QuickTypeMotionPolicy.delay(SuggestionMotionPreset.SCALE, 100))
    }

    @Test fun springRetargetStartsAtTheVisiblePositionAndVelocity() {
        val style = SuggestionMotionPreset.SPRING
        val duration = style.glyphMs
        val position = QuickTypeMotionPolicy.position(style, 0f, 60f, 0f, 40f, duration)
        val velocity = QuickTypeMotionPolicy.velocity(style, 0f, 60f, 0f, 40f, duration)
        assertTrue(position in 0f..60f)
        assertTrue(velocity > 0f)
        assertEquals(position, QuickTypeMotionPolicy.position(style, position, 100f, velocity, 0f, duration), 0f)
        assertEquals(velocity, QuickTypeMotionPolicy.velocity(style, position, 100f, velocity, 0f, duration), .001f)
        assertEquals(100f, QuickTypeMotionPolicy.position(style, position, 100f, velocity, duration.toFloat(), duration), 0f)
        assertEquals(0f, QuickTypeMotionPolicy.velocity(style, position, 100f, velocity, duration.toFloat(), duration), 0f)
    }

    @Test fun confidenceCapsuleIsSlotSizedAndBounded() {
        assertEquals(178f, SuggestionConfidencePillGeometry.fixedWidth(180f, 1f), 0f)
        assertEquals(1f, SuggestionConfidencePillGeometry.fixedWidth(0f, 1f), 0f)
    }
}
