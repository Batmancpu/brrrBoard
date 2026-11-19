// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

/** Menu actions must be explicit, never a same-panel toggle or implicit paste. */
object FooterShortcutPolicy {
    enum class Action { CLIPBOARD, TOOLBAR, SETTINGS }

    @JvmStatic
    fun isAllowed(action: Action, clipboardHistoryEnabled: Boolean): Boolean =
        action != Action.CLIPBOARD || clipboardHistoryEnabled

    @JvmStatic
    fun targetOffset(index: Int, rightToLeft: Boolean): Pair<Int, Int> {
        require(index in 0..2)
        // An Apple-style compact shelf: three comfortable 88x94dp targets,
        // each with a circular symbol and small label. Whole cell is active.
        val x = 10 + index * 94
        return Pair(if (rightToLeft) 208 - x else x, 9)
    }

    @JvmStatic
    fun clampedPopupLeft(anchorCenter: Int, rootLeft: Int, rootWidth: Int,
        popupWidth: Int, inset: Int): Int {
        val lower = rootLeft + inset
        val upper = (rootLeft + rootWidth - popupWidth - inset).coerceAtLeast(lower)
        return (anchorCenter - popupWidth / 2).coerceIn(lower, upper)
    }
}
