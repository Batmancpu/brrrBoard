// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.clipboard

import helium314.keyboard.latin.ClipboardHistoryEntry
import org.junit.Assert.assertEquals
import org.junit.Test

class ClipboardSearchPolicyTest {
    @Test fun newestTextMatchesFirstEvenWhenOldEntryPinned() {
        val entries = listOf(
            ClipboardHistoryEntry(1, 100, true, "Mango recipe"),
            ClipboardHistoryEntry(2, 400, false, "Learn about mangoes"),
            ClipboardHistoryEntry(3, 300, false, "Apple"),
            ClipboardHistoryEntry(4, 200, false, "MANGO syrup")
        )
        assertEquals(listOf(2L, 4L, 1L), ClipboardSearchPolicy.matchingIds("mango", entries))
        assertEquals(emptyList<Long>(), ClipboardSearchPolicy.matchingIds("unicorn", entries))
        assertEquals(emptyList<Long>(), ClipboardSearchPolicy.matchingIds(" ", entries))
    }
}
