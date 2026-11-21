// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.utils

/** Row-major cells for the horizontally scrolling, fixed two-row Toolbar. */
object ToolbarGridPlacement {
    data class Cell(val row: Int, val column: Int)

    @JvmStatic
    fun cell(index: Int, count: Int): Cell {
        require(count >= 0 && index in 0 until count)
        val firstRowCount = (count + 1) / 2
        return if (index < firstRowCount) Cell(0, index)
        else Cell(1, index - firstRowCount)
    }

    @JvmStatic
    fun columnCount(count: Int): Int = ((count.coerceAtLeast(0) + 1) / 2).coerceAtLeast(1)
}
