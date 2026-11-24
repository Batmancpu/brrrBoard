// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings

/** elapsedRealtime only: wall-clock changes cannot extend a private search window. */
internal object VaultSearchWindow {
    const val DURATION_MS = 60_000L
    fun deadline(now: Long): Long = now + DURATION_MS
    fun active(deadline: Long, now: Long): Boolean = deadline > 0L && now < deadline
}
