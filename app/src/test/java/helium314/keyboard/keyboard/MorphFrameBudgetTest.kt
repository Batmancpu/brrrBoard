// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard

import org.junit.Assert.*
import org.junit.Test

class MorphFrameBudgetTest {
    @Test fun skipsOptionalMorphAfterRepeatedLargeCallbackGaps() {
        val budget = MorphFrameBudget(120f)
        assertFalse(budget.shouldSnap(0))
        assertFalse(budget.shouldSnap(25_000_000))
        assertTrue(budget.shouldSnap(50_000_000))
    }

    @Test fun isolatedDelayDoesNotDisableMotion() {
        val budget = MorphFrameBudget(60f)
        assertFalse(budget.shouldSnap(0))
        assertFalse(budget.shouldSnap(50_000_000))
        assertFalse(budget.shouldSnap(66_666_667))
        assertFalse(budget.shouldSnap(120_000_000))
        assertFalse(budget.shouldSnap(130_000_000))
    }

    @Test fun ordinarySixtyHertzAndInvalidRefreshRateAreSafe() {
        val budget = MorphFrameBudget(Float.NaN)
        repeat(10) { assertFalse(budget.shouldSnap(it * 16_666_667L)) }
    }
}
