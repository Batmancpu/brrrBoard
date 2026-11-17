// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

/**
 * Retains the measured typing-key body while another IME panel temporarily
 * hides the alphabet view. No screen/content snapshots are retained.
 */
class KeyboardViewportHeightMemory {
    var lastTypingHeight: Int = 0
        private set

    fun observe(measuredHeight: Int, typingVisible: Boolean) {
        if (typingVisible && measuredHeight > 0) lastTypingHeight = measuredHeight
    }
}
