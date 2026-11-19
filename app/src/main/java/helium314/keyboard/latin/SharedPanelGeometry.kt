// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

/**
 * One measured body-height contract for full-keyboard replacement panels.
 * The shared footer and actual OS navigation inset belong to their parents,
 * never to individual Emoji/Clipboard panels.
 */
object SharedPanelGeometry {
    /** No guessed pixel offset: reuse only an observed same-width viewport. */
    @JvmStatic
    fun chooseCompactEmojiViewport(
        savedTypingHeightPx: Int,
        measuredWrapperHeightPx: Int,
        sameWidth: Boolean,
    ): Int = when {
        savedTypingHeightPx > 0 -> savedTypingHeightPx
        sameWidth && measuredWrapperHeightPx > 0 -> measuredWrapperHeightPx
        else -> 0
    }

    /**
     * Native typing geometry wins over the nominal resource calculation for
     * same-width compact Emoji Browse. A fixed 36dp tolerance was insufficient
     * when row scaling/ColorOS measured a larger native alphabet viewport:
     * bottom-gravity then left the difference empty ABOVE the category tabs.
     * The caller has already checked width; never enlarge beyond parent space.
     */
    @JvmStatic
    fun alignCompactEmojiToTypingViewport(
        nominalPx: Int,
        sameWidthTypingViewportPx: Int,
        availableBodyPx: Int,
    ): Int {
        val limit = availableBodyPx.coerceAtLeast(0)
        val normal = nominalPx.coerceIn(0, limit)
        val measured = sameWidthTypingViewportPx.coerceAtLeast(0)
        return if (measured > normal && measured <= limit) measured else normal
    }

    /**
     * Expanded Emoji is an explicit, reversible browse mode. A transition
     * through Search must never silently turn compact Browse into this size.
     * The requested growth includes the 48dp toolbar shown in expanded mode;
     * the remaining room becomes additional fully scrollable emoji rows.
     */
    @JvmStatic
    fun emojiExpandedBodyHeight(
        compactBodyPx: Int,
        requestedGrowthPx: Int,
        availableBodyPx: Int,
    ): Int = (compactBodyPx.coerceAtLeast(0).toLong() + requestedGrowthPx.coerceAtLeast(0))
        .coerceAtMost(availableBodyPx.coerceAtLeast(0).toLong()).toInt()

    @JvmStatic
    fun emojiSearchBodyHeight(
        typingBodyPx: Int,
        searchFieldPx: Int,
        resultRowPx: Int,
        normalPanelPx: Int,
        availableBodyPx: Int,
    ): Int = (typingBodyPx.coerceAtLeast(0).toLong() + searchFieldPx.coerceAtLeast(0)
        + resultRowPx.coerceAtLeast(0))
        .coerceAtLeast(normalPanelPx.coerceAtLeast(0).toLong())
        .coerceAtMost(availableBodyPx.coerceAtLeast(0).toLong()).toInt()

    /**
     * An Emoji panel in the bottom-aligned keyboard wrapper must occupy the
     * same established viewport as the alphabetic keyboard. Otherwise the
     * missing pixels appear as a detached empty band *above* the emoji grid.
     * Only a small, known accessory-sized delta is safe to reconcile: a
     * smaller landscape/window viewport must not be stretched.
     */
    @JvmStatic
    fun retainExistingPanelViewport(
        nominalPx: Int,
        establishedPx: Int,
        availablePx: Int,
        maxGrowthPx: Int,
    ): Int {
        val nominal = nominalPx.coerceAtLeast(0).coerceAtMost(availablePx.coerceAtLeast(0))
        val established = establishedPx.coerceAtLeast(0)
        return if (established > nominal && established <= availablePx &&
            established - nominal <= maxGrowthPx.coerceAtLeast(0)) established else nominal
    }

    @JvmStatic
    fun panelBodyHeight(
        typingKeyBodyPx: Int,
        persistentRowPx: Int,
        expectedAccessoryPx: Int,
        accessoryAlreadyVisible: Boolean,
        availableBodyPx: Int,
    ): Int {
        val missingAccessory = if (accessoryAlreadyVisible) 0 else expectedAccessoryPx.coerceAtLeast(0)
        val requested = typingKeyBodyPx.coerceAtLeast(0).toLong() +
            persistentRowPx.coerceAtLeast(0) + missingAccessory
        return requested.coerceAtMost(availableBodyPx.coerceAtLeast(0).toLong()).toInt()
    }
}
