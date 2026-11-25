// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import kotlin.math.roundToInt

/** Only a short top clearance, never an empty prediction row. */
internal object AccessoryHeaderGeometry {
    fun topClearancePx(radiusPx: Float, density: Float): Int {
        if (!density.isFinite() || density <= 0f) return 0
        val radius = if (radiusPx.isFinite()) radiusPx.coerceAtLeast(0f) else 0f
        return (radius * .45f).coerceIn(8f * density, 16f * density).roundToInt()
    }
}
