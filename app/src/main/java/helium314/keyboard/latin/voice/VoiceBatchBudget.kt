// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

/**
 * Conservative session-local ceiling; no upward oscillation within a recording.
 * Twelve seconds fits the existing 15s decoder and 12s isolated batch bounds.
 * Pressure changes only affect the next batch; queued audio is never dropped.
 */
internal class VoiceBatchBudget {
    private var ceiling = PrivateVoiceChunker.MAX_SECONDS

    fun choose(
        adaptive: Boolean,
        thoughtReady: Boolean,
        memoryPressure: Boolean,
        queuedBatches: Int,
        lastDecodeRealtimePermille: Long,
    ): Int {
        if (!adaptive || !thoughtReady || memoryPressure) {
            ceiling = PrivateVoiceChunker.LEGACY_MAX_SECONDS
        } else if (queuedBatches >= 2 || lastDecodeRealtimePermille >= 900L) {
            ceiling = minOf(ceiling, 8)
        }
        return ceiling
    }
}
