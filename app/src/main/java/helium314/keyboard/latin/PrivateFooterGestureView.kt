// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View

/** Dedicated touch zone between footer controls for the optional custom private gesture. */
class PrivateFooterGestureView(context: Context) : View(context) {
    private val density = resources.displayMetrics.density
    private val xs = FloatArray(MAX_POINTS)
    private val ys = FloatArray(MAX_POINTS)
    private var count = 0
    private var startedAt = 0L
    private var enabled = false
    private var strokeRejected = false
    private val cuePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cueRect = RectF()

    var onTrace: ((FloatArray) -> Unit)? = null

    init {
        isClickable = true
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        visibility = GONE
        setWillNotDraw(false)
    }

    fun setCaptureEnabled(value: Boolean) {
        enabled = value
        if (!value) resetStroke()
        visibility = if (value) VISIBLE else GONE
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        if (!enabled) return
        val pillWidth = 92f * density
        val pillHeight = 24f * density
        cueRect.set((width - pillWidth) / 2f, (height - pillHeight) / 2f,
            (width + pillWidth) / 2f, (height + pillHeight) / 2f)
        cuePaint.style = Paint.Style.FILL
        cuePaint.color = Color.argb(78, 221, 183, 112)
        canvas.drawRoundRect(cueRect, pillHeight / 2f, pillHeight / 2f, cuePaint)
        cuePaint.style = Paint.Style.STROKE
        cuePaint.strokeWidth = density
        cuePaint.color = Color.argb(180, 239, 207, 145)
        canvas.drawRoundRect(cueRect, pillHeight / 2f, pillHeight / 2f, cuePaint)
        cuePaint.style = Paint.Style.FILL
        cuePaint.textAlign = Paint.Align.CENTER
        cuePaint.textSize = 9f * density
        cuePaint.typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        cuePaint.color = Color.WHITE
        val label = when (PrivateGestureSession.captureMode()) {
            PrivateGestureSession.CaptureMode.ENROLLING -> "DRAW PRIVATE GESTURE"
            PrivateGestureSession.CaptureMode.UNLOCKING -> "DRAW TO UNLOCK"
            PrivateGestureSession.CaptureMode.IDLE -> "PRIVATE GESTURE"
        }
        canvas.drawText(label, width / 2f, height / 2f + cuePaint.textSize * .35f, cuePaint)
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (!enabled) return false
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                resetStroke()
                strokeRejected = false
                startedAt = event.eventTime
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
                    PrivateGestureTrace.normalize(xs, ys, count, density) else null
                resetStroke()
                if (trace != null) {
                    onTrace?.invoke(trace)
                    trace.fill(0f)
                }
                return true
            }
            MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_POINTER_UP, MotionEvent.ACTION_CANCEL -> {
                strokeRejected = true
                resetStroke()
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
        private const val MAX_POINTS = 80
        private const val MIN_DURATION_MS = 250L
        private const val MAX_DURATION_MS = 5_000L
    }
}
