// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.clipboard

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ClipboardSurfacePolicyTest {
    private fun alpha(argb: Int) = argb ushr 24

    @Test fun headerTransitionsToCompletelyClearWithoutBlackStrip() {
        val input = 0xFFE7E9EF.toInt()
        val colors = ClipboardSurfacePolicy.headerStops(input)
        assertEquals(0, alpha(colors.last()))
        assertTrue((1 until colors.size).all { alpha(colors[it - 1]) >= alpha(colors[it]) })
        assertTrue(colors.all { (it and 0x00FFFFFF) == (input and 0x00FFFFFF) })
    }

    @Test fun bottomMaterialProgressivelyCoversScrolledContent() {
        val input = 0xFF202127.toInt()
        val colors = ClipboardSurfacePolicy.bottomStops(input)
        assertEquals(0, alpha(colors.first()))
        assertTrue((1 until colors.size).all { alpha(colors[it - 1]) <= alpha(colors[it]) })
        assertTrue(alpha(colors.last()) < 255)
    }
}
