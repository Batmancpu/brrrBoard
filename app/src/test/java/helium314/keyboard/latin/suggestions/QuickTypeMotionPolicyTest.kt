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
    @Test fun twoStylesKeepAcceptedScaleAndMigrateOldSelections() {
        val scale = SuggestionMotionPreset.SCALE
        val improved = SuggestionMotionPreset.SCALE_IMPROVED
        assertEquals(listOf("scale", "scale_improved"), SuggestionMotionPreset.values().map { it.id })
        for (legacy in listOf(null, "invalid", "classic", "ripple", "spring", "scale")) {
            assertEquals(scale, SuggestionMotionPreset.fromId(legacy))
        }
        assertEquals(improved, SuggestionMotionPreset.fromId("scale_improved"))
        assertEquals(145L, scale.glyphMs)
        assertEquals(130L, scale.replacementMs)
        assertEquals(.12f, scale.entryScale, 0f)
        assertEquals(scale.glyphMs, improved.glyphMs)
        assertEquals(scale.replacementMs, improved.replacementMs)
        assertEquals(.72f, improved.entryScale, 0f)
        assertEquals(1f, scale.compression, 0f)
        assertEquals(.35f, improved.compression, 0f)
        assertEquals(0L, QuickTypeMotionPolicy.delay(scale, 100))
        assertEquals(10L, QuickTypeMotionPolicy.delay(improved, 100))
    }

    @Test fun improvedInkIsReadableWithoutStackingUnrelatedWords() {
        val scale = SuggestionMotionPreset.SCALE
        val improved = SuggestionMotionPreset.SCALE_IMPROVED
        assertEquals(33L, QuickTypeMotionPolicy.chromeDuration(scale))
        assertEquals(85L, QuickTypeMotionPolicy.chromeDuration(improved))
        assertEquals(.12f, QuickTypeMotionPolicy.exitScale(scale), 0f)
        assertEquals(.82f, QuickTypeMotionPolicy.exitScale(improved), 0f)
        assertEquals(0f, QuickTypeMotionPolicy.entryOpacity(scale, false, false), 0f)
        assertEquals(.65f, QuickTypeMotionPolicy.entryOpacity(improved, false, false), 0f)
        assertEquals(.30f, QuickTypeMotionPolicy.entryOpacity(improved, true, true), 0f)
        assertEquals(0f, QuickTypeMotionPolicy.entryOpacity(improved, true, false), 0f)
    }

    @Test fun independentOpacityPreservesSlowGrowthAndExactEndpoints() {
        val scale = SuggestionMotionPreset.SCALE
        val improved = SuggestionMotionPreset.SCALE_IMPROVED
        for (duration in listOf(130L, 145L)) {
            var previous = -1f
            for (i in 0..100) {
                val elapsed = duration * i / 100f
                val old = QuickTypeMotionPolicy.glyphProgress(scale, elapsed, duration)
                val new = QuickTypeMotionPolicy.glyphProgress(improved, elapsed, duration)
                val alpha = QuickTypeMotionPolicy.opacityProgress(improved, elapsed, duration, false)
                assertEquals(old, new, 0f)
                assertTrue(alpha in 0f..1f && alpha >= previous)
                assertTrue(alpha - new <= .05f)
                previous = alpha
            }
            assertEquals(0f, QuickTypeMotionPolicy.opacityProgress(improved, 0f, duration, true), 0f)
            assertEquals(1f, QuickTypeMotionPolicy.opacityProgress(improved, duration.toFloat(), duration, true), 0f)
        }
    }

    @Test fun improvedRetargetPreservesBoundedMomentumAndExactEndpoint() {
        val style = SuggestionMotionPreset.SCALE_IMPROVED
        val duration = style.glyphMs
        val position = QuickTypeMotionPolicy.position(style, 0f, 60f, 0f, 40f, duration)
        val velocity = QuickTypeMotionPolicy.velocity(style, 0f, 60f, 0f, 40f, duration)
        assertTrue(position in 0f..60f)
        assertTrue(velocity > 0f)
        assertEquals(position, QuickTypeMotionPolicy.position(style, position, 100f, velocity, 0f, duration), 0f)
        assertEquals(velocity, QuickTypeMotionPolicy.velocity(style, position, 100f, velocity, 0f, duration), .01f)
        assertEquals(100f, QuickTypeMotionPolicy.position(style, position, 100f, velocity, duration.toFloat(), duration), 0f)
        assertEquals(0f, QuickTypeMotionPolicy.velocity(style, position, 100f, velocity, duration.toFloat(), duration), 0f)
        // A reversal can carry a small impulse, but cannot fling a glyph across its slot.
        for (i in 0..145) {
            val x = QuickTypeMotionPolicy.position(style, 60f, 40f, 2400f, i.toFloat(), duration)
            assertTrue(x in 39.99f..62f)
        }
    }

    @Test fun improvedAnchorsRepeatedCharactersAndSingleLetterPrefix() {
        assertEquals(listOf(0 to 0), QuickTypeMotionPolicy.improvedMatches("i", "in"))
        assertEquals(listOf(0 to 0, 1 to 1, 2 to 2), QuickTypeMotionPolicy.improvedMatches("cat", "cats"))
        assertEquals(listOf(0 to 0, 1 to 1, 2 to 2, 3 to 4, 4 to 5),
            QuickTypeMotionPolicy.improvedMatches("leter", "letter"))
        assertTrue(QuickTypeMotionPolicy.improvedMatches("the", "and").isEmpty())
        val samples = listOf("letter", "letters", "installation", "install", "don't", "dont", "a", "and")
        for (a in samples) for (b in samples) {
            val pairs = QuickTypeMotionPolicy.improvedMatches(a, b)
            assertTrue(pairs.all { (i, j) -> a[i] == b[j] })
            assertTrue(pairs.zipWithNext().all { (x, y) -> x.first < y.first && x.second < y.second })
        }
    }

    @Test fun confidenceCapsuleIsSlotSizedAndBounded() {
        assertEquals(178f, SuggestionConfidencePillGeometry.fixedWidth(180f, 1f), 0f)
        assertEquals(1f, SuggestionConfidencePillGeometry.fixedWidth(0f, 1f), 0f)
    }
    @org.junit.Test fun improvedSamplesLastDrawInsteadOfScheduledFrame() {
        org.junit.Assert.assertEquals(16f,QuickTypeMotionPolicy.captureElapsed(SuggestionMotionPreset.SCALE_IMPROVED,32f,16f),0f)
        org.junit.Assert.assertEquals(0f,QuickTypeMotionPolicy.captureElapsed(SuggestionMotionPreset.SCALE_IMPROVED,32f,0f),0f)
        org.junit.Assert.assertEquals(32f,QuickTypeMotionPolicy.captureElapsed(SuggestionMotionPreset.SCALE,32f,16f),0f)
    }

}
