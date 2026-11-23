// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.os.SystemClock

/** Short-lived, one-use in-memory secret authorization created only after vault decryption. */
object PrivateGestureSession {
    private const val LIFETIME_MS = 30_000L
    const val PREF_ENABLED = "pref_private_note_circle_enabled"
    private var secret: CharArray? = null
    private var expiresAtMs = 0L

    @Synchronized
    fun arm(value: String): Boolean {
        clearLocked()
        if (value.isBlank() || value.length > 2048) return false
        secret = value.toCharArray()
        expiresAtMs = SystemClock.elapsedRealtime() + LIFETIME_MS
        return true
    }

    @Synchronized
    fun consume(): String? {
        if (SystemClock.elapsedRealtime() >= expiresAtMs) {
            clearLocked()
            return null
        }
        val value = secret ?: return null
        val result = String(value)
        clearLocked()
        return result
    }

    @Synchronized
    fun remainingMs(): Long {
        if (secret == null || SystemClock.elapsedRealtime() >= expiresAtMs) {
            clearLocked()
            return 0L
        }
        return expiresAtMs - SystemClock.elapsedRealtime()
    }

    @Synchronized
    fun clear() = clearLocked()

    private fun clearLocked() {
        secret?.fill('\u0000')
        secret = null
        expiresAtMs = 0L
    }
}
