// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.settings

/** Bounded content-free evidence; never stores an app identifier or text. */
internal class AppProfileRejectionWindow {
    private var epoch = Long.MIN_VALUE
    private var started = 0L
    private var count = 0
    fun clear() { epoch = Long.MIN_VALUE; started = 0; count = 0 }
    fun record(editorEpoch: Long, now: Long): Boolean {
        if (epoch != editorEpoch || now < started || now - started >= 30_000L) {
            epoch = editorEpoch; started = now; count = 0
        }
        if (++count < 6) return false
        clear()
        return true
    }
}
