// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode

/** Public typing remains available on the lock screen, private surfaces do not. */
object LockedKeyboardActionPolicy {
    @JvmStatic fun isRestricted(code: Int): Boolean = when (code) {
        KeyCode.SETTINGS, KeyCode.CLIPBOARD, KeyCode.CLIPBOARD_PASTE,
        KeyCode.CLIPBOARD_COPY, KeyCode.CLIPBOARD_CUT, KeyCode.CLIPBOARD_COPY_ALL,
        KeyCode.CLIPBOARD_CLEAR_HISTORY, KeyCode.ACCESS_POINT -> true
        else -> false
    }
}
