// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings

import org.junit.Assert.*
import org.junit.Test

class SettingsSearchRankTest {
    private fun rank(term: String, title: String = "keyboard animation",
                     description: String = "smooth transitions",
                     category: String = "interactions motion",
                     alias: Boolean = false, coverage: Float = 0f, count: Int = 1) =
        settingsSearchRank(term, title, title.split(' ', '-', '&'), description, category,
            alias, coverage, count)

    @Test fun indexedSearchRetainsOriginalPrecedence() {
        assertEquals(0, rank("keyboard animation"))
        assertEquals(1, rank("key"))
        assertEquals(2, rank("anim"))
        assertEquals(3, rank("smooth"))
        assertEquals(4, rank("motion"))
        assertEquals(5, rank("ios", alias = true, coverage = 1f))
        assertEquals(6, rank("something", coverage = .67f))
        assertEquals(7, rank("something else", coverage = .5f, count = 2))
        assertNull(rank("unknown", coverage = .5f))
        assertNull(rank("unknown"))
    }
}
