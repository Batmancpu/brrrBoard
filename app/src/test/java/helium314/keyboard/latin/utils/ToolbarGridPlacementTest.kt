// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class ToolbarGridPlacementTest {
    @Test fun fillsFirstRowThenSecondWithoutGrowingVertically() {
        val cells = (0 until 7).map { ToolbarGridPlacement.cell(it, 7) }
        assertEquals(listOf(
            ToolbarGridPlacement.Cell(0, 0),
            ToolbarGridPlacement.Cell(0, 1),
            ToolbarGridPlacement.Cell(0, 2),
            ToolbarGridPlacement.Cell(0, 3),
            ToolbarGridPlacement.Cell(1, 0),
            ToolbarGridPlacement.Cell(1, 1),
            ToolbarGridPlacement.Cell(1, 2),
        ), cells)
        assertEquals(4, ToolbarGridPlacement.columnCount(7))
    }

    @Test fun zeroAndSingleItemHaveValidGridDimensions() {
        assertEquals(1, ToolbarGridPlacement.columnCount(0))
        assertEquals(ToolbarGridPlacement.Cell(0, 0), ToolbarGridPlacement.cell(0, 1))
        assertEquals(1, ToolbarGridPlacement.columnCount(1))
    }
}
