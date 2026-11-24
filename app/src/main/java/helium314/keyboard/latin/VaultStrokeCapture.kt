// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.view.MotionEvent
import java.util.function.Consumer
import kotlin.math.hypot

/** Own the complete pointer stream after a footer DOWN; never forward a partial gesture to typing. */
class VaultStrokeCapture(private val density: Float, private val onTrace: Consumer<FloatArray>) {
    private val xs = FloatArray(MAX_POINTS)
    private val ys = FloatArray(MAX_POINTS)
    private var count = 0
    private var startedAt = 0L
    private var startWidth = 0
    private var startHeight = 0
    private val ownership = VaultStrokeOwnership()
    var onRejected: (() -> Unit)? = null

    fun isOwning(): Boolean = ownership.owns

    /** Invalidation wipes trace data but keeps ownership until UP/CANCEL or a fresh DOWN. */
    fun invalidateStroke() {
        ownership.invalidate()
        wipe()
    }

    fun route(event: MotionEvent, width: Int, height: Int, starts: Boolean, eligible: Boolean): Boolean {
        val action = event.actionMasked
        if (action == MotionEvent.ACTION_DOWN) {
            wipe()
            if (!ownership.begin(starts && eligible && event.pointerCount == 1)) return false
            startedAt = event.eventTime
            startWidth = width
            startHeight = height
            add(event.x, event.y)
            return true
        }
        if (!ownership.owns) return false
        if (!eligible || width != startWidth || height != startHeight || event.pointerCount != 1 ||
            action == MotionEvent.ACTION_POINTER_DOWN || action == MotionEvent.ACTION_POINTER_UP) invalidateStroke()
        when (action) {
            MotionEvent.ACTION_MOVE -> if (ownership.valid) {
                for (i in 0 until event.historySize) add(event.getHistoricalX(0, i), event.getHistoricalY(0, i))
                add(event.x, event.y)
            }
            MotionEvent.ACTION_UP -> {
                // ACTION_UP may carry batched MOVE samples on some devices.
                // Capture them before the final endpoint rather than losing a corner.
                if (ownership.valid) {
                    for (i in 0 until event.historySize) add(event.getHistoricalX(0, i), event.getHistoricalY(0, i))
                    add(event.x, event.y, terminal = true)
                }
                val valid = ownership.finish()
                val trace = if (valid && event.eventTime - startedAt in 120L..8_000L)
                    VaultGestureTrace.normalize(xs, ys, count, density) else null
                wipe()
                if (trace != null) try { onTrace.accept(trace) } finally { trace.fill(0f) }
                else onRejected?.invoke()
            }
            MotionEvent.ACTION_CANCEL -> { ownership.invalidate(); ownership.finish(); wipe() }
        }
        return true
    }

    private fun add(x: Float, y: Float, terminal: Boolean = false) {
        if (!ownership.valid) return
        val slack = 6f * density
        if (!x.isFinite() || !y.isFinite() || x < -slack || y < -slack ||
            x >= startWidth + slack || y >= startHeight + slack) { invalidateStroke(); return }
        val px = x.coerceIn(0f, (startWidth - 1).coerceAtLeast(0).toFloat())
        val py = y.coerceIn(0f, (startHeight - 1).coerceAtLeast(0).toFloat())
        if (count > 0 && hypot(px - xs[count - 1], py - ys[count - 1]) < density * .65f) {
            // Preserve the actual lift position, but never overwrite the initial DOWN.
            if (terminal && count > 1) { xs[count - 1] = px; ys[count - 1] = py }
            return
        }
        // Decimate in place instead of rejecting a long, high-refresh-rate stroke.
        if (count == MAX_POINTS) {
            val lastX = xs[MAX_POINTS - 1]; val lastY = ys[MAX_POINTS - 1]
            for (i in 1 until MAX_POINTS / 2) { xs[i] = xs[i * 2]; ys[i] = ys[i * 2] }
            count = MAX_POINTS / 2
            xs[count] = lastX; ys[count] = lastY; count++
            xs.fill(0f, count); ys.fill(0f, count)
        }
        xs[count] = px; ys[count] = py; count++
    }

    private fun wipe() {
        xs.fill(0f, 0, count); ys.fill(0f, 0, count)
        count = 0; startedAt = 0L
    }

    companion object {
        private const val MAX_POINTS = 512
        @JvmStatic fun safeCenterWidth(width: Int, density: Float): Int =
            (width - (136f * density).toInt()).coerceAtLeast(0)
        @JvmStatic fun inStartRegion(x: Float, y: Float, width: Int, height: Int, centerWidth: Int, footerHeight: Int): Boolean {
            if (width <= 0 || height <= 0 || centerWidth <= 0 || footerHeight <= 0) return false
            val half = minOf(centerWidth, width) / 2f
            return x >= width / 2f - half && x < width / 2f + half &&
                y >= (height - footerHeight).coerceAtLeast(0) && y < height
        }
    }
}

/** Pure state machine shared with tests. A rejected stream remains consumed. */
internal class VaultStrokeOwnership {
    var owns = false; private set
    var valid = false; private set
    fun begin(eligible: Boolean): Boolean {
        owns = eligible; valid = eligible
        return owns
    }
    fun invalidate() { valid = false }
    fun finish(): Boolean {
        val accepted = owns && valid
        owns = false; valid = false
        return accepted
    }
}
