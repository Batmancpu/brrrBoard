// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.clipboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipboardSearchPreviewTest {
    @Test fun matchingPhraseIsVisibleEvenAtEndOfLongNote() {
        val source = "intro ".repeat(400) + "The extraordinary result was confirmed yesterday."
        val result = ClipboardSearchPreview.window(source, "extraordinary")
        assertTrue(result.text.startsWith("…"))
        assertTrue(result.hasMatch)
        assertEquals("extraordinary", result.text.substring(result.matchStart, result.matchEnd))
        assertTrue(result.text.contains("confirmed"))
        assertTrue(result.text.length <= 138)
    }

    @Test fun caseInsensitiveTextPreservesOriginalCapitalizationAndPasteSource() {
        val text = "Rani Singh's Extraordinary Life"
        val result = ClipboardSearchPreview.window(text, "extraordinary")
        assertEquals("Extraordinary", result.text.substring(result.matchStart, result.matchEnd))
        assertEquals(text, result.text)
    }

    @Test fun noQueryOrNoMatchDoesNotHighlightAnything() {
        assertFalse(ClipboardSearchPreview.window("just some text", "").hasMatch)
        assertFalse(ClipboardSearchPreview.window("just some text", "xyz").hasMatch)
        assertEquals("", ClipboardSearchPreview.window("", "a").text)
    }

    @Test fun emojiSurrogatesStayIntactAndMarkupIndicesStillPointAtMatch() {
        val source = "🌅".repeat(100) + " happiness " + "🎉".repeat(100)
        val result = ClipboardSearchPreview.window(source, "happiness")
        assertEquals("happiness", result.text.substring(result.matchStart, result.matchEnd))
        assertFalse(result.text.contains('\uFFFD'))
        assertTrue(result.hasMatch)
    }
}
