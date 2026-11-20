// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.animation.ValueAnimator
import android.content.Context
import android.view.accessibility.AccessibilityManager
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityNodeInfo
import android.view.animation.DecelerateInterpolator
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

/**
 * Spacebar-sized, deliberate slide control. Nothing is confirmed on DOWN, TAP, MOVE,
 * dismissal, second pointer, or an interrupted animation: only a full UP commits.
 * The entire IME is touch-shielded while shown, to prevent accidental typing.
 */
class SlideConfirmationOverlay(context: Context) : View(context) {
    private val density = resources.displayMetrics.density
    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val slot = RectF()
    private var label: String = ""
    private var accent: Int = 0xFFE1AF4D.toInt()
    private var commit: (() -> Unit)? = null
    private var dismiss: (() -> Unit)? = null
    private var enteredOnThumb = false
    private var dragging = false
    private var downX = 0f
    private var downY = 0f
    private var progress = 0f
    private var reveal = 0f
    private var finishing = false
    private var thresholdFeedbackGiven = false
    private var motion: ValueAnimator? = null
    private val touchSlop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()

    // FrameLayout's wrap-content pass must not count a MATCH_PARENT touch shield
    // toward IME height. Its second exact measure fills only the computed root.
    override fun onMeasure(widthMeasureSpec: Int, heightMeasureSpec: Int) {
        if (MeasureSpec.getMode(heightMeasureSpec) != MeasureSpec.EXACTLY)
            setMeasuredDimension(0, 0)
        else super.onMeasure(widthMeasureSpec, heightMeasureSpec)
    }

    fun present(bounds: android.graphics.Rect, prompt: String, tint: Int,
                onConfirmed: () -> Unit, onDismissed: () -> Unit) {
        motion?.cancel()
        slot.set(bounds)
        label = prompt
        accent = tint
        commit = onConfirmed
        dismiss = onDismissed
        progress = 0f
        reveal = 0f
        finishing = false
        thresholdFeedbackGiven = false
        enteredOnThumb = false
        dragging = false
        visibility = VISIBLE
        importantForAccessibility = IMPORTANT_FOR_ACCESSIBILITY_YES
        val exploring = (context.getSystemService(Context.ACCESSIBILITY_SERVICE)
            as? AccessibilityManager)?.isTouchExplorationEnabled == true
        contentDescription = if (exploring) {
            "$prompt. Use the accessibility actions to confirm or dismiss."
        } else {
            "$prompt. Drag the arrow all the way to the right. Tap outside to cancel."
        }
        animate(0f, 1f, 255L, update = { reveal = it })
    }

    private fun animate(from: Float, to: Float, time: Long, update: (Float) -> Unit,
                        after: (() -> Unit)? = null) {
        motion?.cancel()
        motion = ValueAnimator.ofFloat(from, to).apply {
            duration = time
            interpolator = DecelerateInterpolator(1.65f)
            addUpdateListener { update(it.animatedValue as Float); postInvalidateOnAnimation() }
            if (after != null) addListener(object : android.animation.AnimatorListenerAdapter() {
                private var cancelled = false

                override fun onAnimationCancel(animation: android.animation.Animator) {
                    cancelled = true
                }

                override fun onAnimationEnd(animation: android.animation.Animator) {
                    // Animator.cancel() also invokes onAnimationEnd(). A replaced
                    // overlay or editor switch must never run a stale confirm.
                    if (!cancelled && animation === motion) after()
                }
            })
            start()
        }
    }

    fun closeNow() {
        val pendingMotion = motion
        motion = null
        pendingMotion?.cancel()
        commit = null
        dismiss = null
        enteredOnThumb = false
        dragging = false
        visibility = GONE
        reveal = 0f
        progress = 0f
        finishing = false
        thresholdFeedbackGiven = false
    }

    private fun finish(approved: Boolean) {
        if (finishing) return
        finishing = true
        val action = if (approved) commit else dismiss
        animate(reveal, 0f, if (approved) 210L else 180L,
            { reveal = it }, { closeNow(); action?.invoke() })
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        if (visibility != VISIBLE || finishing) return true
        val thumbDiameter = max(0f, slot.height() - 5f * density)
        val startX = slot.left + (slot.height() - thumbDiameter) / 2f
        val travel = max(0f, slot.width() - slot.height())
        when (event.actionMasked) {
            MotionEvent.ACTION_DOWN -> {
                downX = event.x; downY = event.y
                enteredOnThumb = event.x in (startX - 8f * density)..(startX + thumbDiameter + 8f * density)
                    && event.y in (slot.top - 8f * density)..(slot.bottom + 8f * density)
                dragging = enteredOnThumb
                return true
            }
            MotionEvent.ACTION_POINTER_DOWN, MotionEvent.ACTION_CANCEL -> {
                enteredOnThumb = false; dragging = false
                if (event.actionMasked == MotionEvent.ACTION_CANCEL) finish(false)
                else animate(progress, 0f, 180L, update = { progress = it })
                return true
            }
            MotionEvent.ACTION_MOVE -> {
                if (enteredOnThumb && dragging) {
                    if (abs(event.y - downY) > slot.height() * 1.7f) {
                        enteredOnThumb = false; dragging = false
                        animate(progress, 0f, 190L, update = { progress = it })
                    } else {
                        progress = SlideConfirmationPolicy.progress(event.x, downX, travel)
                        if (!thresholdFeedbackGiven && progress >= SlideConfirmationPolicy.REQUIRED_PROGRESS) {
                            thresholdFeedbackGiven = true
                            AudioAndHapticFeedbackManager.getInstance().performHapticFeedback(
                                this, helium314.keyboard.event.HapticEvent.KEY_PRESS)
                        }
                        postInvalidateOnAnimation()
                    }
                }
                return true
            }
            MotionEvent.ACTION_UP -> {
                val approved = SlideConfirmationPolicy.approved(enteredOnThumb, dragging,
                    false, progress)
                dragging = false; enteredOnThumb = false
                if (approved) {
                    progress = 1f
                    finish(true)
                } else if (!slot.contains(event.x, event.y) &&
                    abs(event.x - downX) < touchSlop && abs(event.y - downY) < touchSlop) {
                    finish(false)
                } else animate(progress, 0f, 230L, update = { progress = it })
                return true
            }
        }
        return true
    }

    override fun onInitializeAccessibilityNodeInfo(info: AccessibilityNodeInfo) {
        super.onInitializeAccessibilityNodeInfo(info)
        info.className = "android.widget.SeekBar"
        info.isClickable = false
        info.isDismissable = true
        info.addAction(AccessibilityNodeInfo.AccessibilityAction(
            AccessibilityNodeInfo.ACTION_SCROLL_FORWARD, "Swipe forward to confirm $label"))
        info.addAction(AccessibilityNodeInfo.AccessibilityAction.ACTION_DISMISS)
    }

    override fun performAccessibilityAction(action: Int, arguments: android.os.Bundle?): Boolean {
        if (action == AccessibilityNodeInfo.ACTION_SCROLL_FORWARD && visibility == VISIBLE
            && !finishing) {
            progress = 1f
            finish(true)
            return true
        }
        if (action == AccessibilityNodeInfo.ACTION_DISMISS && visibility == VISIBLE
            && !finishing) {
            finish(false)
            return true
        }
        return super.performAccessibilityAction(action, arguments)
    }

    override fun onDraw(canvas: Canvas) {
        if (reveal <= 0f || slot.width() <= 0) return
        val scale = .93f + .07f * reveal
        canvas.save()
        canvas.scale(scale, scale, slot.centerX(), slot.centerY())
        val radius = slot.height() * .5f
        paint.shader = null
        paint.color = Color.argb((27 * reveal).toInt(), 0, 0, 0)
        canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)
        paint.color = Color.argb((238 * reveal).toInt(), 27, 29, 36)
        canvas.drawRoundRect(slot, radius, radius, paint)
        paint.color = Color.argb((90 * reveal).toInt(), 255, 255, 255)
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = density
        canvas.drawRoundRect(slot.left + density * .5f, slot.top + density * .5f,
            slot.right - density * .5f, slot.bottom - density * .5f, radius, radius, paint)
        paint.style = Paint.Style.FILL

        val thumbMargin = 2.5f * density
        val thumbSize = max(0f, slot.height() - thumbMargin * 2f)
        val travel = max(0f, slot.width() - slot.height())
        val x = slot.left + thumbMargin + progress * travel
        val thumb = RectF(x, slot.top + thumbMargin, x + thumbSize, slot.bottom - thumbMargin)
        paint.shader = LinearGradient(thumb.left, thumb.top, thumb.right, thumb.bottom,
            accent, Color.rgb(255, 228, 175), Shader.TileMode.CLAMP)
        canvas.drawRoundRect(thumb, thumbSize / 2f, thumbSize / 2f, paint)
        paint.shader = null

        val cx = thumb.centerX(); val cy = thumb.centerY()
        paint.color = 0xFF28221A.toInt()
        paint.style = Paint.Style.STROKE
        paint.strokeWidth = 2.7f * density
        paint.strokeCap = Paint.Cap.ROUND
        paint.strokeJoin = Paint.Join.ROUND
        for (offset in listOf(-3f * density, 3f * density)) {
            val arrow = android.graphics.Path().apply {
                moveTo(cx + offset - 2.2f * density, cy - 5f * density)
                lineTo(cx + offset + 2.7f * density, cy)
                lineTo(cx + offset - 2.2f * density, cy + 5f * density)
            }
            canvas.drawPath(arrow, paint)
        }
        paint.style = Paint.Style.FILL
        paint.color = Color.argb((232 * (1f - progress) * reveal).toInt(), 255, 255, 255)
        paint.textSize = min(13.2f * density, slot.height() * .3f)
        paint.typeface = android.graphics.Typeface.create("sans-serif-medium", 0)
        paint.textAlign = Paint.Align.CENTER
        // Keep the instruction inside the track beyond the thumb, even on small keys.
        val textWidth = slot.width() - slot.height() - 12f * density
        val display = android.text.TextUtils.ellipsize(label, android.text.TextPaint(paint),
            max(0f, textWidth), android.text.TextUtils.TruncateAt.END).toString()
        canvas.drawText(display, slot.left + slot.height() + textWidth * .5f,
            slot.centerY() - (paint.ascent() + paint.descent()) * .5f, paint)
        canvas.restore()
    }
}
