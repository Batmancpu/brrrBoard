// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.utils

import org.junit.Assert.assertEquals
import org.junit.Test

class ToolbarGridPlacementTest {
    @Test fun fillsFourColumnsThenScrollsIntoAdditionalRows() {
        val cells = (0 until 13).map { ToolbarGridPlacement.cell(it, 13) }
        assertEquals(listOf(
            ToolbarGridPlacement.Cell(0, 0),
            ToolbarGridPlacement.Cell(0, 3),
            ToolbarGridPlacement.Cell(1, 0),
            ToolbarGridPlacement.Cell(2, 3),
            ToolbarGridPlacement.Cell(3, 0),
        ), listOf(cells[0], cells[3], cells[4], cells[11], cells[12]))
        assertEquals(4, ToolbarGridPlacement.COLUMN_COUNT)
        assertEquals(3, ToolbarGridPlacement.rowCount(12))
        assertEquals(4, ToolbarGridPlacement.rowCount(13))
    }

    @Test fun zeroAndSingleItemHaveValidGridDimensions() {
        assertEquals(ToolbarGridPlacement.Cell(0, 0), ToolbarGridPlacement.cell(0, 1))
        assertEquals(3, ToolbarGridPlacement.rowCount(0))
        assertEquals(3, ToolbarGridPlacement.rowCount(1))
    }

    @Test fun settingsIsPromotedWithoutReorderingOtherTools() {
        val ordered = ToolbarGridPlacement.settingsFirst(listOf(
            ToolbarKey.CLIPBOARD, ToolbarKey.SETTINGS, ToolbarKey.UNDO, ToolbarKey.REDO,
        ))
        assertEquals(listOf(ToolbarKey.SETTINGS, ToolbarKey.CLIPBOARD,
            ToolbarKey.UNDO, ToolbarKey.REDO), ordered)
    }
}
