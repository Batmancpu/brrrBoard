// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

/**
 * One measured body-height contract for full-keyboard replacement panels.
 * The shared footer and actual OS navigation inset belong to their parents,
 * never to individual Emoji/Clipboard panels.
 */
object SharedPanelGeometry {
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
