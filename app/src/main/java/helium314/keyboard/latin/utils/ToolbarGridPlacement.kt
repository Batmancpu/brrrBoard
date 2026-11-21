// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.utils

/** Row-major cells for the four-column, vertically scrolling Toolbar grid. */
object ToolbarGridPlacement {
    const val COLUMN_COUNT = 4

    data class Cell(val row: Int, val column: Int)

    @JvmStatic
    fun cell(index: Int, count: Int): Cell {
        require(count >= 0 && index in 0 until count)
        return Cell(index / COLUMN_COUNT, index % COLUMN_COUNT)
    }

    @JvmStatic
    fun rowCount(count: Int): Int = ((count.coerceAtLeast(0) + COLUMN_COUNT - 1) / COLUMN_COUNT)
        .coerceAtLeast(3)

    /** Keep Settings prominent and predictable, never in the second slot. */
    @JvmStatic
    fun settingsFirst(keys: List<ToolbarKey>): List<ToolbarKey> {
        val settingsIndex = keys.indexOf(ToolbarKey.SETTINGS)
        if (settingsIndex <= 0) return keys
        return buildList(keys.size) {
            add(ToolbarKey.SETTINGS)
            keys.forEachIndexed { index, key -> if (index != settingsIndex) add(key) }
        }
    }
}
