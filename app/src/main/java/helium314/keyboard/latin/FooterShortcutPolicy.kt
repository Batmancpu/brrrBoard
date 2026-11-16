// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

/** Menu actions must be explicit, never a same-panel toggle or implicit paste. */
object FooterShortcutPolicy {
    enum class Action { TOOLBAR, CLIPBOARD, SETTINGS }

    @JvmStatic
    fun isAllowed(action: Action, clipboardHistoryEnabled: Boolean): Boolean =
        action != Action.CLIPBOARD || clipboardHistoryEnabled

    @JvmStatic
    fun targetOffset(index: Int, rightToLeft: Boolean): Pair<Int, Int> {
        require(index in 0..2)
        val x = when (index) { 0 -> 56; 1 -> 0; else -> 112 }
        return Pair(if (rightToLeft) 112 - x else x, if (index == 0) 0 else 38)
    }

    @JvmStatic
    fun clampedPopupLeft(anchorCenter: Int, rootLeft: Int, rootWidth: Int,
        popupWidth: Int, inset: Int): Int {
        val lower = rootLeft + inset
        val upper = (rootLeft + rootWidth - popupWidth - inset).coerceAtLeast(lower)
        return (anchorCenter - popupWidth / 2).coerceIn(lower, upper)
    }
}
