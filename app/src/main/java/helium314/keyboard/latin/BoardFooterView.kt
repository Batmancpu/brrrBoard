// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.annotation.SuppressLint
import android.animation.ObjectAnimator
import android.animation.PropertyValuesHolder
import android.animation.ValueAnimator
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.Gravity
import android.view.MotionEvent
import android.view.ViewConfiguration
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.PopupWindow
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import helium314.keyboard.event.HapticEvent
import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode
import helium314.keyboard.latin.common.ColorType
import helium314.keyboard.latin.settings.Settings
import kotlin.math.abs
import kotlin.math.roundToInt

private const val DICTATION_RED = 0xFFFF3B30.toInt()

class BoardFooterView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {
    private lateinit var ime: LatinIME
    private val emojiButton get() = findViewById<ImageButton>(R.id.board_footer_globe)
    private val microphone get() = findViewById<ImageButton>(R.id.board_footer_mic)
    private var popup: PopupWindow? = null
    private val targets = ArrayList<ImageButton>()
    private var dictationActive = false
    private var dictationProcessing = false
    private var micPulse: ObjectAnimator? = null
    private var cancelDictationActionId: Int? = null
    private var selected = -1
    private var tracking = false
    private var downX = 0f
    private var downY = 0f
    private val screenLocation = IntArray(2)
    private val openShortcuts = Runnable { if (tracking && isShown) showShortcutArc() }
    private val shortcuts = listOf(
        Shortcut(R.string.emoji, R.drawable.ic_board_emoji, KeyCode.EMOJI),
        Shortcut(R.string.clipboard, R.drawable.sym_keyboard_clipboard_lxx, KeyCode.CLIPBOARD),
        Shortcut(R.string.settings, R.drawable.sym_keyboard_settings_lxx, KeyCode.SETTINGS),
    )

    private data class Shortcut(val label: Int, val icon: Int, val code: Int)

    @SuppressLint("ClickableViewAccessibility")
    fun bind(owner: LatinIME) {
        if (this::ime.isInitialized && ime === owner) return
        ime = owner
        emojiButton.setOnClickListener { feedback(it); send(KeyCode.EMOJI) }
        microphone.setOnClickListener { feedback(it); send(KeyCode.VOICE_INPUT) }
        microphone.setOnLongClickListener {
            ime.switchBoardInputMethod()
            true
        }
        ViewCompat.addAccessibilityAction(microphone, resources.getString(R.string.board_system_keyboard_picker)) { _, _ ->
            ime.switchBoardInputMethod()
            true
        }
        ViewCompat.addAccessibilityAction(emojiButton, resources.getString(R.string.board_system_keyboard_picker)) { _, _ ->
            ime.showInputPickerDialog()
            true
        }
        // The ordinary touch gesture deliberately consumes long-press events to allow
        // sliding through the arc. Expose each shortcut directly for touch exploration.
        shortcuts.forEach { shortcut ->
            ViewCompat.addAccessibilityAction(emojiButton, resources.getString(shortcut.label)) { _, _ ->
                send(shortcut.code)
                true
            }
        }
        emojiButton.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    tracking = true
                    downX = event.rawX
                    downY = event.rawY
                    view.isPressed = true
                    postDelayed(openShortcuts, ViewConfiguration.getLongPressTimeout().toLong())
                }
                MotionEvent.ACTION_MOVE -> {
                    if (popup != null) updateSelection(event.rawX, event.rawY)
                    else if (abs(event.rawX - downX) > ViewConfiguration.get(context).scaledTouchSlop ||
                        abs(event.rawY - downY) > ViewConfiguration.get(context).scaledTouchSlop) {
                        tracking = false
                        removeCallbacks(openShortcuts)
                        view.isPressed = false
                    }
                }
                MotionEvent.ACTION_UP -> {
                    val hadPopup = popup != null
                    if (hadPopup) updateSelection(event.rawX, event.rawY)
                    val code = shortcuts.getOrNull(selected)?.code
                    val tap = tracking && !hadPopup
                    reset()
                    if (code != null) send(code) else if (tap) view.performClick()
                }
                MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_DOWN -> reset()
            }
            true
        }
    }

    fun render(show: Boolean, showMicrophone: Boolean) {
        visibility = if (show) VISIBLE else GONE
        if (!show) {
            reset()
            return
        }
        val tint = ColorStateList.valueOf(Settings.getValues().mColors.get(ColorType.KEY_ICON))
        emojiButton.imageTintList = tint
        if (dictationProcessing) showDictationProcessing()
        else showDictationActive(dictationActive)
        microphone.visibility = if (showMicrophone) VISIBLE else INVISIBLE
    }

    fun showDictationActive(active: Boolean) {
        dictationActive = active
        dictationProcessing = false
        val colors = Settings.getValues().mColors
        microphone.imageTintList = ColorStateList.valueOf(
            if (active) DICTATION_RED else colors.get(ColorType.KEY_ICON),
        )
        microphone.contentDescription = resources.getString(
            if (active) R.string.board_dictation_stop else R.string.board_inline_dictation,
        )
        if (active) startMicPulse() else stopMicPulse()
    }

    fun showDictationProcessing(processing: Boolean = true) {
        dictationProcessing = processing
        if (processing) {
            showDictationActive(false)
            dictationProcessing = true
            microphone.contentDescription = resources.getString(R.string.board_dictation_processing)
        }
    }

    fun setDictationCancelAvailable(available: Boolean) {
        val mic = microphone
        if (available && cancelDictationActionId == null) {
            cancelDictationActionId = ViewCompat.addAccessibilityAction(
                mic, resources.getString(R.string.board_cancel_dictation)
            ) { _, _ ->
                ime.cancelBoardDictation()
                true
            }
        } else if (!available) {
            cancelDictationActionId?.let { ViewCompat.removeAccessibilityAction(mic, it) }
            cancelDictationActionId = null
        }
    }

    private fun startMicPulse() {
        if (micPulse != null) return
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O &&
            !ValueAnimator.areAnimatorsEnabled()) return
        val mic = microphone
        micPulse = ObjectAnimator.ofPropertyValuesHolder(
            mic,
            PropertyValuesHolder.ofFloat(View.SCALE_X, 1f, 1.12f),
            PropertyValuesHolder.ofFloat(View.SCALE_Y, 1f, 1.12f),
        ).apply {
            duration = 620
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.REVERSE
            interpolator = AccelerateDecelerateInterpolator()
            start()
        }
    }

    private fun stopMicPulse() {
        micPulse?.cancel()
        micPulse = null
        if (this::ime.isInitialized) {
            microphone.scaleX = 1f
            microphone.scaleY = 1f
        }
    }

    fun reset() {
        showDictationActive(false)
        showDictationProcessing(false)
        setDictationCancelAvailable(false)
        tracking = false
        emojiButton.isPressed = false
        removeCallbacks(openShortcuts)
        popup?.dismiss()
        popup = null
        targets.clear()
        selected = -1
    }

    override fun onDetachedFromWindow() {
        reset()
        super.onDetachedFromWindow()
    }

    private fun feedback(view: android.view.View) {
        AudioAndHapticFeedbackManager.getInstance().performHapticAndAudioFeedback(
            KeyCode.NOT_SPECIFIED, view, HapticEvent.KEY_PRESS,
        )
    }

    private fun send(code: Int) = ime.onCodeInput(code, -1, -1, false)

    private fun showShortcutArc() {
        if (windowToken == null) return
        val colors = Settings.getValues().mColors
        val panel = FrameLayout(context)
        val normal = ColorUtils.setAlphaComponent(colors.get(ColorType.MORE_SUGGESTIONS_BACKGROUND), 242)
        val active = colors.get(ColorType.TOOL_BAR_EXPAND_KEY_BACKGROUND)
        // Three 48dp targets form a compact arc above the lower-left button.
        val offsets = listOf(56 to 0, 0 to 38, 112 to 38)
        shortcuts.forEachIndexed { index, shortcut ->
            val button = ImageButton(context).apply {
                contentDescription = resources.getString(shortcut.label)
                setImageResource(shortcut.icon)
                imageTintList = ColorStateList.valueOf(colors.get(ColorType.KEY_ICON))
                scaleType = android.widget.ImageView.ScaleType.CENTER
                background = android.graphics.drawable.StateListDrawable().apply {
                    addState(intArrayOf(android.R.attr.state_selected), circle(active))
                    addState(intArrayOf(), circle(normal))
                }
            }
            targets.add(button)
            val (x, y) = offsets[index]
            panel.addView(button, LayoutParams(dp(48), dp(48)).apply {
                leftMargin = dp(x)
                topMargin = dp(y)
            })
        }
        val popupWidth = dp(160)
        val popupHeight = dp(86)
        emojiButton.getLocationInWindow(screenLocation)
        val rootWidth = rootView.width
        val left = (screenLocation[0] + emojiButton.width / 2 - popupWidth / 2)
            .coerceIn(dp(4), (rootWidth - popupWidth - dp(4)).coerceAtLeast(dp(4)))
        popup = PopupWindow(panel, popupWidth, popupHeight, false).apply {
            isTouchable = false
            isOutsideTouchable = false
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            elevation = dp(8).toFloat()
            showAsDropDown(
                emojiButton,
                left - screenLocation[0],
                -emojiButton.height - popupHeight,
                Gravity.LEFT,
            )
        }
    }

    private fun updateSelection(x: Float, y: Float) {
        selected = targets.indexOfFirst { target ->
            target.getLocationOnScreen(screenLocation)
            target.isLaidOut && x >= screenLocation[0] && x < screenLocation[0] + target.width &&
                y >= screenLocation[1] && y < screenLocation[1] + target.height
        }
        targets.forEachIndexed { index, target -> target.isSelected = index == selected }
    }

    private fun circle(color: Int) = GradientDrawable().apply {
        shape = GradientDrawable.OVAL
        setColor(color)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).roundToInt()
}
