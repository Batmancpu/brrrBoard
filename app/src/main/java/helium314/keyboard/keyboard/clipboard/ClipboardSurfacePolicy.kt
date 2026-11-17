// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.clipboard

import helium314.keyboard.keyboard.PanelMaterial

/**
 * Lightweight material overlays for the scrolling Clipboard body.
 * The underlying content stays sharp; only its visibility through the fixed
 * header and bottom-key areas changes. No private clip bitmap is duplicated.
 */
internal object ClipboardSurfacePolicy {
    fun headerStops(rgb: Int): IntArray = PanelMaterial.headerStops(rgb)
    fun bottomStops(rgb: Int): IntArray = PanelMaterial.bottomStops(rgb)
}
