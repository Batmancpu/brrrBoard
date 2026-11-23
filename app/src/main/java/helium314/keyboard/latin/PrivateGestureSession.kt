// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.content.Context
import android.os.Handler
import android.os.Looper
import helium314.keyboard.latin.utils.prefs
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import java.lang.ref.WeakReference
import java.util.concurrent.CopyOnWriteArraySet

/** Short-lived command state. Enrolled gesture/secret bytes live in [PrivateGestureVault]. */
object PrivateGestureSession {
    const val PREF_ENABLED = "pref_private_custom_gesture_enabled_v1"

    enum class CaptureMode { IDLE, ENROLLING, UNLOCKING }
    sealed interface Event {
        data class SampleCaptured(val normalizedTrace: FloatArray) : Event
        data object Unlocked : Event
        data object Mismatch : Event
        data object SecretInserted : Event
    }

    private val mainHandler = Handler(Looper.getMainLooper())
    private val events = MutableSharedFlow<Event>(extraBufferCapacity = 8)
    val eventFlow = events.asSharedFlow()
    private val listeners = CopyOnWriteArraySet<WeakReference<() -> Unit>>()
    @Volatile private var mode = CaptureMode.IDLE
    private var armedSecret: CharArray? = null
    private var unlockExpiresAt = 0L

    fun captureMode(): CaptureMode = mode
    fun isUnlocked(): Boolean = android.os.SystemClock.elapsedRealtime() < unlockExpiresAt

    fun beginEnrollment() {
        mode = CaptureMode.ENROLLING
        notifyChanged()
    }

    fun beginUnlock() {
        mode = CaptureMode.UNLOCKING
        notifyChanged()
    }

    fun cancelCapture() {
        mode = CaptureMode.IDLE
        synchronized(this) { clearArmedSecretLocked() }
        notifyChanged()
    }

    fun lock() {
        mode = CaptureMode.IDLE
        unlockExpiresAt = 0L
        synchronized(this) { clearArmedSecretLocked() }
        notifyChanged()
    }

    fun addAvailabilityListener(listener: () -> Unit) {
        listeners.add(WeakReference(listener))
    }

    fun removeAvailabilityListener(listener: () -> Unit) {
        listeners.removeAll { it.get() == null || it.get() === listener }
    }

    /** Called only by the dedicated footer hit zone after a complete single-finger trace. */
    fun onFooterGesture(context: Context, normalizedTrace: FloatArray): Boolean {
        when (mode) {
            CaptureMode.ENROLLING -> {
                mode = CaptureMode.IDLE
                events.tryEmit(Event.SampleCaptured(normalizedTrace.copyOf()))
                notifyChanged()
                return true
            }
            CaptureMode.UNLOCKING -> {
                val match = PrivateGestureVault.matches(context, normalizedTrace)
                mode = CaptureMode.IDLE
                if (match) {
                    unlockExpiresAt = android.os.SystemClock.elapsedRealtime() + UNLOCK_SESSION_MS
                    events.tryEmit(Event.Unlocked)
                } else {
                    events.tryEmit(Event.Mismatch)
                }
                notifyChanged()
                return match
            }
            CaptureMode.IDLE -> Unit
        }

        if (!context.prefs().getBoolean(PREF_ENABLED, false) ||
            !PrivateGestureVault.matches(context, normalizedTrace)) return false
        val secret = PrivateGestureVault.readSecret(context) ?: return false
        synchronized(this) {
            clearArmedSecretLocked()
            armedSecret = secret.copyOf()
            armedUntil = android.os.SystemClock.elapsedRealtime() + SECRET_LIFETIME_MS
        }
        secret.fill('\u0000')
        notifyChanged()
        mainHandler.postDelayed({ synchronized(this) { clearArmedSecretLocked() }; notifyChanged() }, SECRET_LIFETIME_MS)
        return true
    }

    @JvmStatic
    @Synchronized
    fun consume(): String? {
        if (!contextIsCurrent()) {
            clearArmedSecretLocked()
            return null
        }
        val current = armedSecret ?: return null
        val result = String(current)
        clearArmedSecretLocked()
        events.tryEmit(Event.SecretInserted)
        return result
    }

    private var armedUntil = 0L
    private fun contextIsCurrent() = android.os.SystemClock.elapsedRealtime() < armedUntil

    @Synchronized
    fun remainingMs(): Long {
        val remaining = armedUntil - android.os.SystemClock.elapsedRealtime()
        if (remaining <= 0L || armedSecret == null) {
            clearArmedSecretLocked()
            return 0L
        }
        return remaining
    }

    private fun clearArmedSecretLocked() {
        armedSecret?.fill('\u0000')
        armedSecret = null
        armedUntil = 0L
    }

    private fun notifyChanged() {
        listeners.removeAll { ref ->
            val callback = ref.get()
            if (callback == null) true else {
                if (Looper.myLooper() == Looper.getMainLooper()) callback()
                else mainHandler.post(callback)
                false
            }
        }
    }

    private const val SECRET_LIFETIME_MS = 5_000L
    private const val UNLOCK_SESSION_MS = 5 * 60_000L
}
