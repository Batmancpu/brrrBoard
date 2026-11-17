// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

/** Bounds the ephemeral chip by capture time, never observer-delivery time. */
internal object ScreenshotSuggestionPolicy {
    const val MAX_AGE_MILLIS = 60_000L
    fun isRecent(capturedAtMillis: Long, nowMillis: Long): Boolean {
        if (capturedAtMillis <= 0 || nowMillis < capturedAtMillis) return false
        return nowMillis - capturedAtMillis <= MAX_AGE_MILLIS
    }
}
