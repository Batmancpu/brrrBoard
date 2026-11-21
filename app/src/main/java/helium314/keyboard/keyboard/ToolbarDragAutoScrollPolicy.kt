// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard

import kotlin.math.roundToInt

/** Pure edge and velocity policy for vertical Toolbar-grid drag scrolling. */
internal object ToolbarDragAutoScrollPolicy {
    fun direction(pointer: Float, viewportExtent: Int, edgePx: Float,
            scrollOffset: Int, maxScrollOffset: Int): Int {
        val edge = edgePx.coerceAtLeast(1f)
        return when {
            pointer < edge && scrollOffset > 0 -> -1
            pointer > viewportExtent - edge && scrollOffset < maxScrollOffset -> 1
            else -> 0
        }
    }

    fun frameStepPx(density: Float, penetration: Float): Int {
        val fraction = penetration.coerceIn(0f, 1f)
        return ((3f + 5f * fraction) * density.coerceAtLeast(0.1f))
            .roundToInt().coerceAtLeast(1)
    }
}
