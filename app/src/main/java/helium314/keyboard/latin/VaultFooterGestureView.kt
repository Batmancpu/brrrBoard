// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.content.Context
import android.view.MotionEvent
import android.view.View

/** Dedicated touch zone between footer controls for the optional custom private gesture. */
class VaultFooterGestureView(context: Context) : View(context) {
    private val density = resources.displayMetrics.density
    private val xs = FloatArray(MAX_POINTS)
    private val ys = FloatArray(MAX_POINTS)
    private var count = 0
    private var startedAt = 0L
    private var enabled = false
    private var strokeRejected = false
    // The live keyboard intentionally never draws the trace or a vault label.

    var onTrace: ((FloatArray) -> Unit)? = null

    init {
        isClickable = true
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        visibility = GONE
        setWillNotDraw(true)
    }

    fun setCaptureEnabled(value: Boolean) {
        enabled = value
        if (!value) { strokeRejected = true; resetStroke() }
        visibility = if (value) VISIBLE else GONE
        invalidate()
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!enabled) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                resetStroke()
                strokeRejected = false
                startedAt = event.eventTime
                parent?.requestDisallowInterceptTouchEvent(true)
                addPoint(event.x, event.y)
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (event.pointerCount != 1) {
                    strokeRejected = true
                    resetStroke()
                    return true
                }
                for (history in 0 until event.historySize) {
                    addPoint(event.getHistoricalX(0, history), event.getHistoricalY(0, history))
                }
                addPoint(event.x, event.y)
                return true
            }
            MotionEvent.ACTION_UP -> {
                addPoint(event.x, event.y)
                val trace = if (!strokeRejected && event.eventTime - startedAt in MIN_DURATION_MS..MAX_DURATION_MS)
                    VaultGestureTrace.normalize(xs, ys, count, density) else null
                resetStroke()
                parent?.requestDisallowInterceptTouchEvent(false)
                if (trace != null) {
                    try { onTrace?.invoke(trace) } finally { trace.fill(0f) }
                }
                return true
            }
            MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                strokeRejected = true
                resetStroke()
                parent?.requestDisallowInterceptTouchEvent(false)
                return true
            }
        }
        return true
    }

    private fun addPoint(x: Float, y: Float) {
        if (x < 0f || y < 0f || x > width.toFloat() || y > height.toFloat() || count >= MAX_POINTS) {
            strokeRejected = true
            resetStroke()
            return
        }
        if (strokeRejected) return
        if (count > 0 && kotlin.math.hypot(x - xs[count - 1], y - ys[count - 1]) < density * 0.65f) return
        xs[count] = x
        ys[count] = y
        count++
    }

    private fun resetStroke() {
        xs.fill(0f, 0, count.coerceAtMost(xs.size))
        ys.fill(0f, 0, count.coerceAtMost(ys.size))
        count = 0
        startedAt = 0L
    }

    companion object {
        private const val MAX_POINTS = 256
        private const val MIN_DURATION_MS = 250L
        private const val MAX_DURATION_MS = 5_000L
    }
}
