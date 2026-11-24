// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings

/** In-memory, foreground-only authorization; no token is stored in saved state or prefs. */
internal object PrivateSettingsSession {
    const val DURATION_MS = 300_000L
    fun deadline(now: Long): Long = now + DURATION_MS
    fun active(deadline: Long, now: Long): Boolean =
        deadline > 0 && now >= deadline - DURATION_MS && now < deadline
}
