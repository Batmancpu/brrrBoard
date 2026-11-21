// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

/** Menu actions must be explicit, never a same-panel toggle or implicit paste. */
object FooterShortcutPolicy {
    enum class Action { CLIPBOARD, TOOLBAR }

    @JvmStatic
    fun isAllowed(action: Action, clipboardHistoryEnabled: Boolean): Boolean =
        action != Action.CLIPBOARD || clipboardHistoryEnabled

    @JvmStatic
    fun targetOffset(index: Int, rightToLeft: Boolean): Pair<Int, Int> {
        require(index in 0..1)
        // Two comfortable 88x94dp targets centered in the 296dp shelf.
        val x = if (index == 0) 40 else 168
        return Pair(if (rightToLeft) 296 - 88 - x else x, 9)
    }

    @JvmStatic
    fun clampedPopupLeft(anchorCenter: Int, rootLeft: Int, rootWidth: Int,
        popupWidth: Int, inset: Int): Int {
        val lower = rootLeft + inset
        val upper = (rootLeft + rootWidth - popupWidth - inset).coerceAtLeast(lower)
        return (anchorCenter - popupWidth / 2).coerceIn(lower, upper)
    }
}
