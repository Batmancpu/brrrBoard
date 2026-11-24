// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard

/**
 * Presentation-only safety valve for a short optional morph.
 * Callback gaps are not measured display FPS. Two consecutive large gaps
 * cancel the optional artwork overlay and reveal native controls immediately.
 * No persistent quality changes or work outside the active animator.
 */
internal class MorphFrameBudget(refreshRate: Float) {
    private val intervalNanos = (1_000_000_000.0 /
        (if (refreshRate.isFinite()) refreshRate.coerceIn(30f, 240f) else 60f)).toLong()
    private var lastFrameNanos = Long.MIN_VALUE
    private var consecutiveLate = 0

    fun shouldSnap(nowNanos: Long): Boolean {
        val previous = lastFrameNanos
        lastFrameNanos = nowNanos
        if (previous == Long.MIN_VALUE || nowNanos <= previous) {
            consecutiveLate = 0
            return false
        }
        val gap = nowNanos - previous
        consecutiveLate = if (gap > maxOf(40_000_000L, intervalNanos * 5 / 2)) consecutiveLate + 1 else 0
        return consecutiveLate >= 2
    }
}
