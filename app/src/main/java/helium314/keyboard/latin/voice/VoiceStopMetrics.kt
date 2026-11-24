// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice
import android.os.SystemClock
/** Only latest eligible session, relative timing and no transcript or editor identity. */
internal object VoiceStopMetrics {
    private var enabled = false
    private var stoppedAt = -1L
    private var released = -1L
    private var partial = -1L
    private var ready = -1L
    private var dispatch = -1L
    data class Snapshot(val captureReleaseMs: Long, val firstDrainPartialMs: Long, val resultReadyMs: Long, val commitDispatchReturnedMs: Long)
    @Synchronized fun begin(eligible: Boolean) {
        enabled = eligible; stoppedAt = -1L; released = -1L; partial = -1L; ready = -1L; dispatch = -1L
    }
    @Synchronized fun stop(now: Long = SystemClock.elapsedRealtime()) { if (enabled && stoppedAt < 0L) stoppedAt = now }
    private fun elapsed(now: Long): Long = if (enabled && stoppedAt >= 0L && now >= stoppedAt) now-stoppedAt else -1L
    @Synchronized fun captureReleased(now: Long = SystemClock.elapsedRealtime()) { released = elapsed(now) }
    @Synchronized fun drainPartial(now: Long = SystemClock.elapsedRealtime()) { if (partial < 0L) partial = elapsed(now) }
    @Synchronized fun resultReady(now: Long = SystemClock.elapsedRealtime()) { ready = elapsed(now) }
    @Synchronized fun dispatchReturned(now: Long = SystemClock.elapsedRealtime()) { dispatch = elapsed(now) }
    @Synchronized fun snapshot() = Snapshot(released,partial,ready,dispatch)
}
