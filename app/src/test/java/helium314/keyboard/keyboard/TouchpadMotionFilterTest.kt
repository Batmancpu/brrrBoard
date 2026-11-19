// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class TouchpadMotionFilterTest {
    @Test fun tinyFingerMovesAccumulateWithoutRoundingAwayDisplacement() {
        val filter = TouchpadMotionFilter()
        filter.advance(5, 0, 16L, 50)
        assertEquals(0, filter.horizontalSteps())
        var total = 0
        for (i in 2..10) {
            filter.advance(5, 0, (i * 16).toLong(), 50)
            total += filter.horizontalSteps()
        }
        assertTrue(total >= 1)
        assertEquals(0, filter.verticalSteps())
    }

    @Test fun diagonalDragKeepsBothAxesInsteadOfDiscardingTheSmallerAxis() {
        val filter = TouchpadMotionFilter()
        filter.advance(30, 44, 16L, 50)
        assertTrue(filter.horizontalSteps() > 0)
        assertTrue(filter.verticalSteps() > 0)
    }

    @Test fun reversingDirectionRetainsContinuousFractionalState() {
        val filter = TouchpadMotionFilter()
        filter.advance(11, 0, 16L, 50)
        assertEquals(0, filter.horizontalSteps())
        filter.advance(-5, 0, 32L, 50)
        assertEquals(0, filter.horizontalSteps())
        filter.advance(-35, 0, 48L, 50)
        assertTrue(filter.horizontalSteps() < 0)
    }

    @Test fun hugePointerDeltaIsCappedAndReleaseResetsVelocityAndFractions() {
        val filter = TouchpadMotionFilter()
        filter.advance(800, -700, 16L, 100)
        assertEquals(3, filter.horizontalSteps())
        assertEquals(-3, filter.verticalSteps())
        filter.reset()
        filter.advance(1, 1, 32L, 50)
        assertEquals(0, filter.horizontalSteps())
        assertEquals(0, filter.verticalSteps())
    }

    @Test fun lowSensitivityRequiresMoreTravelThanHighSensitivity() {
        val slow = TouchpadMotionFilter()
        val fast = TouchpadMotionFilter()
        slow.advance(20, 0, 16L, 0)
        fast.advance(20, 0, 16L, 100)
        assertEquals(0, slow.horizontalSteps())
        assertTrue(fast.horizontalSteps() > 0)
    }
}
