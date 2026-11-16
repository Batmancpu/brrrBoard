// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.clipboard

/** Pure bounds for history image previews, independent of screen orientation. */
internal object ClipboardCardGeometry {
    private const val MAX_IMAGE_PREVIEW_DP = 112f

    fun previewHeightPx(parentWidth: Int, horizontalPadding: Int, spans: Int, density: Float): Int {
        val maxHeight = (MAX_IMAGE_PREVIEW_DP * density.coerceAtLeast(0f)).toInt().coerceAtLeast(1)
        // During initial RecyclerView layout the width may not be measured yet.
        if (parentWidth <= 0) return maxHeight
        val cellWidth = (parentWidth - horizontalPadding).coerceAtLeast(1) / spans.coerceAtLeast(1)
        return cellWidth.coerceIn(1, maxHeight)
    }
}
