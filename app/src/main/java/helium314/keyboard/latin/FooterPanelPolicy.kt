// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

/** Shared bottom-left action for all non-typing keyboard panels. */
object FooterPanelPolicy {
    @JvmStatic
    fun returnToAbc(emoji: Boolean, clipboard: Boolean, toolbar: Boolean): Boolean =
        emoji || clipboard || toolbar
}
