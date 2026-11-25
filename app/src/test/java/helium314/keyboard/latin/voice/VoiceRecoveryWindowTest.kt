// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import org.junit.Assert.*
import org.junit.Test

class VoiceRecoveryWindowTest {
    @Test fun fullSessionSurvivesRepeatedReadsButExpiresAtExactlySixtySeconds() {
        val store = VoiceRecoveryWindow()
        val text = "First segment. Second segment. Final segment."
        val item = store.add(text, 10_000L)!!
        assertEquals(text, store.all(10_001L).single().text)
        assertEquals(item, store.all(69_999L).single())
        assertTrue(store.all(70_000L).isEmpty())
        assertTrue(store.all(70_001L).isEmpty())
    }

    @Test fun newestSessionReplacesOldAndOldDismissCannotDeleteNew() {
        val store = VoiceRecoveryWindow()
        val old = store.add("old", 0L)!!
        val fresh = store.add("new", 1L)!!
        assertFalse(store.remove(old.id))
        assertEquals(fresh, store.all(2L).single())
        store.clear()
        assertTrue(store.all(3L).isEmpty())
    }

    @Test fun blankOversizeAndClockRollbackAreSafe() {
        val store = VoiceRecoveryWindow()
        assertNull(store.add(" ", 0L))
        assertNull(store.add("x".repeat(VoiceRecoveryWindow.MAX_TEXT_CHARS + 1), 0L))
        store.add("valid", 20L)
        assertTrue(store.all(19L).isEmpty())
    }

    @Test fun explicitDismissAndLongWholeSession() {
        val store = VoiceRecoveryWindow()
        val text = "word ".repeat(5_000)
        val item = store.add(text, 0L)!!
        assertEquals(text, store.all(1L).single().text)
        assertTrue(store.remove(item.id))
        assertTrue(store.all(2L).isEmpty())
    }
}
