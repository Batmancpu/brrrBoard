// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

/** Presentation-only gates. Never controls recording or authorization. */
internal object FooterRenderWorkPolicy {
    fun canPulse(visible: Boolean, micVisible: Boolean, toolbar: Boolean,
        recording: Boolean, processing: Boolean, motionEnabled: Boolean): Boolean =
        visible && micVisible && !toolbar && (recording || processing) && motionEnabled

    fun needsDeadlinePost(currentDeadline: Long, activeDeadline: Long): Boolean =
        activeDeadline > 0L && currentDeadline != activeDeadline
}
