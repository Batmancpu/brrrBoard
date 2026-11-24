// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Color
import android.view.MotionEvent
import android.view.View
import helium314.keyboard.latin.R
import java.util.function.Consumer

/** Non-typing enrollment preview. The live IME uses the same full-stroke capture at its root. */
class VaultFooterGestureView(context: Context) : View(context) {
    private val density = resources.displayMetrics.density
    private val footerHeight = resources.getDimensionPixelSize(R.dimen.config_board_footer_height)
    private val capture = VaultStrokeCapture(density, Consumer { trace -> onTrace?.invoke(trace) })
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private var captureEnabled = false
    var practiceInk: Int = Color.DKGRAY
        set(value) { field = value; invalidate() }
    var practiceAccent: Int = 0xff007aff.toInt()
        set(value) { field = value; invalidate() }
    var onTrace: ((FloatArray) -> Unit)? = null
    var onRejected: (() -> Unit)? = null
    private var tracking = false

    init {
        isClickable = true
        isFocusable = false
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_NO
        capture.onRejected = { onRejected?.invoke() }
    }
    fun setCaptureEnabled(value: Boolean) {
        captureEnabled = value
        if (!value) capture.invalidateStroke()
        invalidate()
    }
    override fun onTouchEvent(event: MotionEvent): Boolean {
        val starts = event.actionMasked == MotionEvent.ACTION_DOWN &&
            VaultStrokeCapture.inStartRegion(event.x, event.y, width, height, VaultStrokeCapture.safeCenterWidth(width, density), footerHeight)
        val owned = capture.route(event, width, height, starts, captureEnabled)
        if (event.actionMasked == MotionEvent.ACTION_DOWN) {
            if (owned) {
                tracking = true
                performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                parent?.requestDisallowInterceptTouchEvent(true)
            } else if (captureEnabled) onRejected?.invoke()
        }
        if (event.actionMasked == MotionEvent.ACTION_UP || event.actionMasked == MotionEvent.ACTION_CANCEL) {
            tracking = false
            parent?.requestDisallowInterceptTouchEvent(false)
        }
        invalidate()
        return owned
    }
    override fun onDetachedFromWindow() {
        capture.invalidateStroke()
        super.onDetachedFromWindow()
    }
    override fun onDraw(canvas: Canvas) {
        val w = width.toFloat()
        val keyArea = (height - footerHeight).coerceAtLeast(0).toFloat()
        val row = keyArea / 4f
        val step = w / 10f
        val gap = 3f * density
        val radius = 5f * density
        val rows = arrayOf("QWERTYUIOP", "ASDFGHJKL", "ZXCVBNM")
        paint.textAlign = Paint.Align.CENTER
        paint.textSize = 14f * density
        for (r in rows.indices) {
            val letters = rows[r]
            val left = (w - letters.length * step) / 2f
            letters.forEachIndexed { i, char ->
                val x = left + i * step
                paint.color = (practiceInk and 0x00ffffff) or 0x22000000
                canvas.drawRoundRect(x + gap, r * row + gap, x + step - gap, (r + 1) * row - gap, radius, radius, paint)
                paint.color = practiceInk
                canvas.drawText(char.toString(), x + step / 2, (r + .5f) * row - (paint.ascent() + paint.descent()) / 2f, paint)
            }
        }
        fun key(left: Float, right: Float, label: String, accent: Boolean) {
            paint.color = if (accent) practiceAccent else (practiceInk and 0x00ffffff) or 0x22000000
            canvas.drawRoundRect(left + gap, 3 * row + gap, right - gap, keyArea - gap, radius, radius, paint)
            paint.color = if (accent) Color.WHITE else practiceInk
            canvas.drawText(label, (left + right) / 2, 3.5f * row - (paint.ascent() + paint.descent()) / 2f, paint)
        }
        key(0f, w * .15f, "123", false)
        key(w * .15f, w * .85f, "space", false)
        key(w * .85f, w, "Go", true)
        val half = VaultStrokeCapture.safeCenterWidth(width, density) / 2f
        paint.color = practiceAccent
        if (tracking) {
            paint.color = (practiceAccent and 0x00ffffff) or 0x33000000
            canvas.drawRoundRect(w / 2f - half, keyArea + density, w / 2f + half, height - density, radius, radius, paint)
            paint.color = practiceAccent
        }
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = density * 1.5f
        canvas.drawRoundRect(w / 2f - half, keyArea + density, w / 2f + half, height - density, radius, radius, paint)
        paint.style = Paint.Style.FILL
        paint.textSize = 10f * density
        canvas.drawText("Start here", w / 2f, keyArea + footerHeight / 2f - (paint.ascent() + paint.descent()) / 2f, paint)
    }
}
