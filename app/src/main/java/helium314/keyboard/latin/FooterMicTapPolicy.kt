// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

/** Double-tap means keyboard switch ONLY while idle; a visible active mic stops immediately. */
internal object FooterMicTapPolicy {
    fun isImmediate(recording: Boolean, transcribing: Boolean): Boolean =
        recording || transcribing

    /** Exactly one single-tap callback, chosen before the gesture begins. */
    fun dispatchSingleTap(doubleTapSwitch: Boolean, confirmed: Boolean): Boolean =
        doubleTapSwitch == confirmed
}
