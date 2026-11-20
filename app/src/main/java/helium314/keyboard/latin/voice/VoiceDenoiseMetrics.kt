// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import java.util.concurrent.atomic.AtomicLong
import java.util.concurrent.atomic.AtomicReference
import java.util.concurrent.atomic.AtomicBoolean

/** RAM-only operational counters. No words, raw audio, app identities or histories. */
internal object VoiceDenoiseMetrics {
    data class Snapshot(
        val enhancedWindows: Long,
        val cleanBypasses: Long,
        val fallbacks: Long,
        val lastEnhancementMs: Long,
        val lastInputRoute: String,
        val platformNoiseSuppression: String,
        val blueMicCapturing: Boolean,
        val completedBlueCaptures: Long,
    )
    private val enhanced = AtomicLong()
    private val bypassed = AtomicLong()
    private val fallback = AtomicLong()
    private val lastMs = AtomicLong(-1L)
    private val route = AtomicReference("Not measured")
    private val platform = AtomicReference("Not measured")
    private val capturing = AtomicBoolean(false)
    private val captures = AtomicLong()

    fun enhanced(elapsedMs: Long) {
        lastMs.set(elapsedMs.coerceAtLeast(0))
        enhanced.incrementAndGet()
    }
    fun bypassed() { bypassed.incrementAndGet() }
    fun fallback() { fallback.incrementAndGet() }
    fun inputRoute(description: String) { route.set(description) }
    fun platformState(code: String) { platform.set(code) }
    fun blueCaptureStarted() { capturing.set(true) }
    fun blueCaptureStopped() {
        if (capturing.getAndSet(false)) captures.incrementAndGet()
    }
    fun snapshot() = Snapshot(enhanced.get(), bypassed.get(), fallback.get(), lastMs.get(),
        route.get(), platform.get(), capturing.get(), captures.get())
}
