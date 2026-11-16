// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.clipboard

import org.junit.Assert.assertEquals
import org.junit.Test

class ClipboardCardGeometryTest {
    @Test fun widePreviewIsCappedInsteadOfFillingThePanel() {
        assertEquals(224, ClipboardCardGeometry.previewHeightPx(900, 24, 2, 2f))
    }

    @Test fun narrowLandscapeCellsStayWithinTheirColumn() {
        assertEquals(68, ClipboardCardGeometry.previewHeightPx(300, 28, 4, 2f))
    }

    @Test fun unmeasuredAndDegenerateWidthsAreSafe() {
        assertEquals(112, ClipboardCardGeometry.previewHeightPx(0, 0, 2, 1f))
        assertEquals(1, ClipboardCardGeometry.previewHeightPx(10, 99, 0, 1f))
    }
}
