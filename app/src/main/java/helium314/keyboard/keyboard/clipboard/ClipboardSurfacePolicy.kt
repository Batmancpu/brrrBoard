// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.clipboard

/**
 * Lightweight material overlays for the scrolling Clipboard body.
 * The underlying content stays sharp; only its visibility through the fixed
 * header and bottom-key areas changes. No private clip bitmap is duplicated.
 */
internal object ClipboardSurfacePolicy {
    private fun withAlpha(rgb: Int, alpha: Int): Int =
        (rgb and 0x00FFFFFF) or (alpha.coerceIn(0, 255) shl 24)

    fun headerStops(rgb: Int): IntArray = intArrayOf(
        withAlpha(rgb, 236),
        withAlpha(rgb, 225),
        withAlpha(rgb, 188),
        withAlpha(rgb, 108),
        withAlpha(rgb, 0),
    )

    fun bottomStops(rgb: Int): IntArray = intArrayOf(
        withAlpha(rgb, 0),
        withAlpha(rgb, 76),
        withAlpha(rgb, 154),
        withAlpha(rgb, 204),
        withAlpha(rgb, 226),
    )
}
