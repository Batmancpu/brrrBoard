// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.clipboard

/** Prevent delayed native decoding from entering a different search/editor session. */
internal object ClipboardSearchGestureGate {
    fun accepts(searchActive: Boolean, gestureSession: Long, currentSession: Long,
            attached: Boolean, searchFocused: Boolean): Boolean =
        searchActive && gestureSession == currentSession && attached && searchFocused
}
