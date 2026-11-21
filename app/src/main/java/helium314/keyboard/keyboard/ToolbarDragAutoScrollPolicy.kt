// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard

import kotlin.math.roundToInt

/** Pure edge and velocity policy for the horizontally scrollable Toolbar rail. */
internal object ToolbarDragAutoScrollPolicy {
    fun direction(pointerX: Float, viewportWidth: Int, edgePx: Float,
            scrollX: Int, maxScrollX: Int): Int {
        val edge = edgePx.coerceAtLeast(1f)
        return when {
            pointerX < edge && scrollX > 0 -> -1
            pointerX > viewportWidth - edge && scrollX < maxScrollX -> 1
            else -> 0
        }
    }

    fun frameStepPx(density: Float, penetration: Float): Int {
        val fraction = penetration.coerceIn(0f, 1f)
        return ((3f + 5f * fraction) * density.coerceAtLeast(0.1f))
            .roundToInt().coerceAtLeast(1)
    }
}
