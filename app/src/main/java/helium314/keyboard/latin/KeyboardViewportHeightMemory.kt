// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

/**
 * Retains the measured typing-key body while another IME panel temporarily
 * hides the alphabet view. No screen/content snapshots are retained.
 */
class KeyboardViewportHeightMemory {
    var lastTypingHeight: Int = 0
        private set
    private var lastTypingWidth: Int = 0

    fun observe(measuredHeight: Int, typingVisible: Boolean, viewportWidth: Int = 0) {
        if (typingVisible && measuredHeight > 0) {
            lastTypingHeight = measuredHeight
            lastTypingWidth = viewportWidth
        }
    }

    /** Reject portrait/landscape or one-handed snapshots with different widths. */
    fun heightForWidth(viewportWidth: Int): Int =
        if (viewportWidth > 0 && viewportWidth == lastTypingWidth) lastTypingHeight else 0
}
