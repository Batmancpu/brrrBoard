// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.suggestions

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SuggestionGlyphMotionTest {
    private fun glyphs(word: String): List<SuggestionGlyphSnapshot> = word.mapIndexed { i, c ->
        SuggestionGlyphSnapshot(c, i.toFloat() - word.length / 2f)
    }

    private fun plan(old: String, next: String): List<SuggestionGlyphTrack> =
        SuggestionGlyphMotion.plan(glyphs(old), next,
            next.indices.map { it.toFloat() - next.length / 2f })

    @Test fun contractionMovesOriginalAndRemovesOnlySuffix() {
        val result = plan("they're", "they")
        assertEquals(4, result.count { it.kind == SuggestionGlyphTrack.Kind.RETAINED })
        assertEquals(3, result.count { it.kind == SuggestionGlyphTrack.Kind.REMOVED })
        assertEquals(0, result.count { it.kind == SuggestionGlyphTrack.Kind.INSERTED })
        assertTrue(result.first().fromX != result.first().toX)
        assertEquals("'re", result.filter { it.kind == SuggestionGlyphTrack.Kind.REMOVED }
            .joinToString("") { it.character.toString() })
    }

    @Test fun expansionMovesPrefixAndRevealsOnlyNewCharacters() {
        val result = plan("they", "they're")
        assertEquals(4, result.count { it.kind == SuggestionGlyphTrack.Kind.RETAINED })
        assertEquals(0, result.count { it.kind == SuggestionGlyphTrack.Kind.REMOVED })
        assertEquals(3, result.count { it.kind == SuggestionGlyphTrack.Kind.INSERTED })
        assertTrue(result.first().fromX != result.first().toX)
        assertTrue(result.filter { it.kind == SuggestionGlyphTrack.Kind.INSERTED }
            .all { it.fromOpacity == 0f && it.toOpacity == 1f })
    }

    @Test fun interiorReplacementRetainsMatchingSuffixWithoutGhostCopies() {
        val result = plan("bookkeeper", "bookseller")
        val retained = result.filter { it.kind == SuggestionGlyphTrack.Kind.RETAINED }
        assertEquals(retained.size, retained.map { it.order }.distinct().size)
        assertTrue(retained.size >= 4)
        assertTrue(result.any { it.kind == SuggestionGlyphTrack.Kind.REMOVED })
        assertTrue(result.any { it.kind == SuggestionGlyphTrack.Kind.INSERTED })
    }

    @Test fun interruptionStartsWithCurrentlyVisiblePositionsAndAlpha() {
        val current = listOf(SuggestionGlyphSnapshot('t', -3.25f, .38f, .94f))
        val result = SuggestionGlyphMotion.plan(current, "told", listOf(-2f, -1f, 0f, 1f))
        val retained = result.first { it.kind == SuggestionGlyphTrack.Kind.RETAINED }
        assertEquals(-3.25f, retained.fromX, 0f)
        assertEquals(.38f, retained.fromOpacity, 0f)
        assertEquals(.94f, retained.fromScale, 0f)
    }

    @Test fun fixedDurationAndUnsupportedUnicode() {
        val inserted = plan("they", "they're").first {
            it.kind == SuggestionGlyphTrack.Kind.INSERTED
        }
        assertEquals(0f, SuggestionGlyphMotion.progress(inserted, 0f), 0f)
        assertEquals(1f, SuggestionGlyphMotion.progress(inserted, 1f), 0f)
        assertFalse(SuggestionGlyphMotion.supported("hey 👨‍👩‍👧"))
        assertFalse(SuggestionGlyphMotion.supported("مرحبا"))
        assertTrue(SuggestionGlyphMotion.supported("they're"))
    }
}
