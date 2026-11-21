// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard

import org.junit.Assert.assertEquals
import org.junit.Test

class ToolbarDragAutoScrollPolicyTest {
    @Test fun edgeScrollIsBoundedByAvailableRange() {
        assertEquals(-1, ToolbarDragAutoScrollPolicy.direction(12f, 400, 48f, 80, 500))
        assertEquals(1, ToolbarDragAutoScrollPolicy.direction(390f, 400, 48f, 80, 500))
        assertEquals(0, ToolbarDragAutoScrollPolicy.direction(12f, 400, 48f, 0, 500))
        assertEquals(0, ToolbarDragAutoScrollPolicy.direction(390f, 400, 48f, 500, 500))
        assertEquals(0, ToolbarDragAutoScrollPolicy.direction(200f, 400, 48f, 80, 500))
    }

    @Test fun edgeSpeedGrowsWithPenetrationAndNeverStalls() {
        val slow = ToolbarDragAutoScrollPolicy.frameStepPx(1f, 0f)
        val fast = ToolbarDragAutoScrollPolicy.frameStepPx(1f, 1f)
        assertEquals(3, slow)
        assertEquals(8, fast)
        assertEquals(1, ToolbarDragAutoScrollPolicy.frameStepPx(0f, 0f))
    }
}
