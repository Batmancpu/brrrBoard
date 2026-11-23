// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import android.view.MotionEvent
import android.view.View
import android.graphics.Color
import kotlin.math.abs
import kotlin.math.hypot
import kotlin.math.max

/** Invisible capture zone for an explicitly armed, one-use circle in the empty footer center. */
class PrivateFooterGestureView(context: Context) : View(context) {
    private val density = resources.displayMetrics.density
    private val xs = FloatArray(MAX_POINTS)
    private val ys = FloatArray(MAX_POINTS)
    private var count = 0
    private var pathLength = 0f
    private var startedAt = 0L
    private var enabled = false
    private var strokeRejected = false
    private val cuePaint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val cueRect = RectF()

    var onCircle: (() -> Unit)? = null

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
        val pillWidth = 68f * density
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
        cuePaint.textSize = 10f * density
        cuePaint.typeface = android.graphics.Typeface.create("sans-serif-medium", android.graphics.Typeface.NORMAL)
        cuePaint.color = Color.WHITE
        canvas.drawText("PRIVATE READY", width / 2f, height / 2f + cuePaint.textSize * .35f, cuePaint)
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
                val recognized = !strokeRejected && isCircle(event.eventTime)
                resetStroke()
                if (recognized) onCircle?.invoke()
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
        if (count > 0) pathLength += hypot(x - xs[count - 1], y - ys[count - 1])
        xs[count] = x
        ys[count] = y
        count++
    }

    private fun isCircle(endTime: Long): Boolean {
        if (count < 8 || endTime - startedAt !in MIN_DURATION_MS..MAX_DURATION_MS) return false
        var minX = xs[0]; var maxX = xs[0]; var minY = ys[0]; var maxY = ys[0]
        for (i in 1 until count) {
            minX = minOf(minX, xs[i]); maxX = maxOf(maxX, xs[i])
            minY = minOf(minY, ys[i]); maxY = maxOf(maxY, ys[i])
        }
        val boxWidth = maxX - minX
        val boxHeight = maxY - minY
        val shortSide = minOf(boxWidth, boxHeight)
        val longSide = max(boxWidth, boxHeight)
        if (shortSide < 30f * density || longSide / shortSide > 1.7f) return false
        val closure = hypot(xs[count - 1] - xs[0], ys[count - 1] - ys[0])
        if (closure > 0.35f * shortSide || pathLength < 1.35f * (boxWidth + boxHeight)) return false
        // Reject a near-line ellipse and repeated tiny scribbles by requiring visible area.
        val areaRatio = (boxWidth * boxHeight) / (width.toFloat() * height.toFloat()).coerceAtLeast(1f)
        return areaRatio >= 0.12f && abs(boxWidth - boxHeight) <= 0.7f * longSide
    }

    private fun resetStroke() {
        count = 0
        pathLength = 0f
        startedAt = 0L
    }

    companion object {
        private const val MAX_POINTS = 80
        private const val MIN_DURATION_MS = 180L
        private const val MAX_DURATION_MS = 3_000L
    }
}
