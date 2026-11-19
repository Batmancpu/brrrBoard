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
import android.view.View
import android.view.ViewConfiguration
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.FrameLayout
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.PopupWindow
import android.widget.TextView
import androidx.core.graphics.ColorUtils
import androidx.core.view.ViewCompat
import helium314.keyboard.event.HapticEvent
import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode
import helium314.keyboard.latin.common.ColorType
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.keyboard.KeyboardTheme
import helium314.keyboard.keyboard.KeyboardSwitcher
import helium314.keyboard.keyboard.PanelGlassOverlayView
import helium314.keyboard.keyboard.PanelMaterial
import kotlin.math.abs
import kotlin.math.roundToInt

private const val DICTATION_RED = 0xFFFF3B30.toInt()
private const val MIC_SWITCH_HOLD_TIMEOUT_MS = 300L

class BoardFooterView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {
    private val glass = PanelGlassOverlayView(context)
    private lateinit var ime: LatinIME
    private val emojiButton get() = findViewById<ImageButton>(R.id.board_footer_globe)
    private val microphone get() = findViewById<ImageButton>(R.id.board_footer_mic)
    private val voiceError get() = findViewById<TextView>(R.id.board_footer_voice_error)
    private var popup: PopupWindow? = null
    private val targets = ArrayList<View>()
    private var fadedKeys: View? = null
    private var dictationActive = false
    private var dictationProcessing = false
    private var returnToLetters = false
    private var micPulse: ObjectAnimator? = null
    private var cancelDictationActionId: Int? = null
    private val clearVoiceError = Runnable { voiceError.visibility = GONE }
    private var selected = -1
    private var tracking = false
    private var downX = 0f
    private var downY = 0f
    private var micTracking = false
    private var micHoldTriggered = false
    private var micDownX = 0f
    private var micDownY = 0f
    private val screenLocation = IntArray(2)
    private val openShortcuts = Runnable { if (tracking && isShown) showShortcutArc() }
    private val switchInputMethodOnMicHold = Runnable {
        if (micTracking && isShown && this::ime.isInitialized) {
            micHoldTriggered = true
            microphone.isPressed = false
            feedback(microphone, HapticEvent.KEY_LONG_PRESS)
            ime.switchBoardInputMethod()
        }
    }
    private val shortcuts = listOf(
        Shortcut(R.string.emoji, R.drawable.ic_board_emoji, FooterShortcutPolicy.Action.EMOJI),
        Shortcut(R.string.clipboard, R.drawable.ic_brr_clipboard, FooterShortcutPolicy.Action.CLIPBOARD),
        Shortcut(R.string.settings, R.drawable.sym_keyboard_settings_rounded, FooterShortcutPolicy.Action.SETTINGS),
    )

    private data class Shortcut(val label: Int, val icon: Int, val action: FooterShortcutPolicy.Action)

    override fun onFinishInflate() {
        super.onFinishInflate()
        // Backdrop underlays the *existing* footer buttons; they never blur.
        addView(glass, 0, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        glass.visibility = GONE
    }

    fun setPanelGlassSource(source: View?, emojiSource: Boolean = false) {
        glass.visibility = if (source == null) GONE else VISIBLE
        if (source != null) {
            PanelMaterial.apply(context, Settings.getValues().mColors, null, glass, source)
        } else {
            glass.configure(null, PanelGlassOverlayView.Edge.BOTTOM, Color.TRANSPARENT)
        }
        // The footer continues the *same* Emoji scroll source beneath ABC/mic.
        // Apply identical source-only tone to avoid a colored seam, and always
        // turn it off for Clipboard/Toolbar/typing content.
        glass.setEmojiContentTone(source != null && emojiSource && context.prefs().getBoolean(
            Settings.PREF_EMOJI_GLASS_TONE, Defaults.PREF_EMOJI_GLASS_TONE),
            KeyboardTheme.isDarkThemeActive(context))
    }

    /** The source may move child views without changing its own scrollX/Y. */
    fun invalidatePanelGlass() {
        if (glass.visibility == VISIBLE) glass.postInvalidateOnAnimation()
    }

    @SuppressLint("ClickableViewAccessibility")
    fun bind(owner: LatinIME) {
        if (this::ime.isInitialized && ime === owner) return
        ime = owner
        emojiButton.setOnClickListener {
            feedback(it)
            if (returnToLetters) {
                // Search uses the same alphabet View for PRIVATE query editing;
                // the footer ABC means leave the entire panel, not change its
                // internal keyboard layout. Collapse first to avoid a hard cut.
                if (!KeyboardSwitcher.getInstance().collapsePanelSearchForFooterAlphabet(
                        Runnable { send(KeyCode.ALPHA) })) {
                    send(KeyCode.ALPHA)
                }
            } else {
                send(KeyCode.EMOJI)
            }
        }
        // This footer control is not a virtual keyboard key; dispatch it directly to the
        // IME instead of sending -233 through key-coordinate/meta-state decoding.
        microphone.setOnClickListener { feedback(it); ime.toggleBoardDictation() }
        // Input-method switching is a distinct mic hold action. Start it sooner
        // than Android's default 500 ms long-press while preserving tap-to-dictate.
        microphone.setOnTouchListener { view, event ->
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    microphone.removeCallbacks(switchInputMethodOnMicHold)
                    micTracking = true
                    micHoldTriggered = false
                    micDownX = event.rawX
                    micDownY = event.rawY
                    view.isPressed = true
                    microphone.postDelayed(switchInputMethodOnMicHold, MIC_SWITCH_HOLD_TIMEOUT_MS)
                }
                MotionEvent.ACTION_MOVE -> {
                    val slop = ViewConfiguration.get(context).scaledTouchSlop
                    if (micTracking && (abs(event.rawX - micDownX) > slop || abs(event.rawY - micDownY) > slop)) {
                        micTracking = false
                        microphone.removeCallbacks(switchInputMethodOnMicHold)
                        view.isPressed = false
                    }
                }
                MotionEvent.ACTION_UP -> {
                    val tap = micTracking && !micHoldTriggered
                    cancelMicHold()
                    if (tap) view.performClick()
                }
                MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_DOWN -> cancelMicHold()
            }
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
                performShortcut(shortcut.action)
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
                    val actionToPerform = shortcuts.getOrNull(selected)?.action
                    val tap = tracking && !hadPopup
                    dismissShortcutArc()
                    if (actionToPerform != null) performShortcut(actionToPerform) else if (tap) view.performClick()
                }
                MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_DOWN -> dismissShortcutArc()
            }
            true
        }
    }

    fun render(show: Boolean, showMicrophone: Boolean, returnToAbc: Boolean) {
        visibility = if (show) VISIBLE else GONE
        returnToLetters = returnToAbc
        emojiButton.setImageResource(if (returnToAbc) R.drawable.ic_board_abc else R.drawable.ic_board_emoji)
        emojiButton.contentDescription = resources.getString(
            if (returnToAbc) R.string.board_return_to_letters else R.string.board_language_and_shortcuts,
        )
        if (!show) {
            reset()
            return
        }
        val tint = ColorStateList.valueOf(Settings.getValues().mColors.get(ColorType.KEY_ICON))
        emojiButton.imageTintList = tint
        voiceError.setTextColor(Settings.getValues().mColors.get(ColorType.KEY_ICON))
        if (dictationProcessing) showDictationProcessing()
        else showDictationActive(dictationActive)
        microphone.visibility = if (showMicrophone) VISIBLE else INVISIBLE
    }

    fun showDictationActive(active: Boolean) {
        // A layout/theme re-render must not erase the live voice status.
        if (active && !dictationActive) clearDictationError()
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

    fun showDictationError(message: Int) {
        removeCallbacks(clearVoiceError)
        voiceError.setText(message)
        voiceError.visibility = VISIBLE
        voiceError.announceForAccessibility(resources.getString(message))
        postDelayed(clearVoiceError, 4_000L)
    }

    /** Stable footer status; changing state never changes the keyboard's measured height. */
    fun showDictationStatus(message: Int) {
        removeCallbacks(clearVoiceError)
        val label = resources.getString(message)
        val changed = voiceError.text.toString() != label || voiceError.visibility != VISIBLE
        voiceError.setText(message)
        voiceError.visibility = VISIBLE
        if (changed) voiceError.announceForAccessibility(label)
    }

    fun clearDictationError() {
        removeCallbacks(clearVoiceError)
        voiceError.visibility = GONE
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
        clearDictationError()
        showDictationActive(false)
        showDictationProcessing(false)
        setDictationCancelAvailable(false)
        cancelMicHold()
        dismissShortcutArc()
    }

    private fun dismissShortcutArc() {
        tracking = false
        emojiButton.isPressed = false
        removeCallbacks(openShortcuts)
        popup?.dismiss()
        popup = null
        fadedKeys?.animate()?.cancel()
        fadedKeys?.animate()?.alpha(1f)?.setDuration(130)?.start()
        fadedKeys = null
        targets.clear()
        selected = -1
    }

    override fun onDetachedFromWindow() {
        reset()
        super.onDetachedFromWindow()
    }

    private fun cancelMicHold() {
        microphone.removeCallbacks(switchInputMethodOnMicHold)
        micTracking = false
        micHoldTriggered = false
        microphone.isPressed = false
    }

    private fun feedback(view: android.view.View, event: HapticEvent = HapticEvent.KEY_PRESS) {
        AudioAndHapticFeedbackManager.getInstance().performHapticAndAudioFeedback(
            KeyCode.NOT_SPECIFIED, view, event,
        )
    }

    private fun send(code: Int) = ime.onCodeInput(code, -1, -1, false)

    private fun performShortcut(action: FooterShortcutPolicy.Action) {
        val switcher = helium314.keyboard.keyboard.KeyboardSwitcher.getInstance()
        if (!FooterShortcutPolicy.isAllowed(action, Settings.getValues().mClipboardHistoryEnabled)) {
            // Never let a menu item named Clipboard unexpectedly paste text when
            // history is disabled; explicit clipboard paste keys still work.
            return
        }
        when (action) {
            FooterShortcutPolicy.Action.EMOJI -> send(KeyCode.EMOJI)
            FooterShortcutPolicy.Action.CLIPBOARD -> switcher.setClipboardKeyboard()
            FooterShortcutPolicy.Action.SETTINGS -> send(KeyCode.SETTINGS)
        }
    }

    private fun showShortcutArc() {
        if (windowToken == null || popup != null) return
        val colors = Settings.getValues().mColors
        val panel = FrameLayout(context).apply {
            background = rounded(
                ColorUtils.setAlphaComponent(colors.get(ColorType.MAIN_BACKGROUND), 236),
                dp(24).toFloat(),
                ColorUtils.setAlphaComponent(colors.get(ColorType.KEY_ICON), 65))
            clipToOutline = true
        }
        val normal = ColorUtils.setAlphaComponent(colors.get(ColorType.KEY_BACKGROUND), 178)
        val active = ColorUtils.setAlphaComponent(colors.get(ColorType.ACTION_KEY_BACKGROUND), 215)
        // iOS-inspired single glass shelf: 80x82dp labeled hit targets, rather
        // than a small unlabelled 48dp arc with diagonal motor demands.
        val rtl = ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL
        shortcuts.forEachIndexed { index, shortcut ->
            val button = LinearLayout(context).apply {
                contentDescription = resources.getString(shortcut.label)
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                background = android.graphics.drawable.StateListDrawable().apply {
                    addState(intArrayOf(android.R.attr.state_selected), rounded(active, dp(18).toFloat()))
                    addState(intArrayOf(), rounded(normal, dp(18).toFloat()))
                }
                addView(ImageView(context).apply {
                    setImageResource(shortcut.icon)
                    imageTintList = ColorStateList.valueOf(colors.get(ColorType.KEY_ICON))
                    scaleType = ImageView.ScaleType.FIT_CENTER
                    setPadding(dp(3), dp(3), dp(3), dp(3))
                }, LinearLayout.LayoutParams(dp(32), dp(32)))
                addView(TextView(context).apply {
                    text = resources.getString(shortcut.label)
                    setTextColor(colors.get(ColorType.KEY_ICON))
                    textSize = 11f
                    gravity = Gravity.CENTER
                    maxLines = 1
                }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(25)))
            }
            targets.add(button)
            val (x, y) = FooterShortcutPolicy.targetOffset(index, rtl)
            panel.addView(button, LayoutParams(dp(80), dp(82)).apply {
                leftMargin = dp(x)
                topMargin = dp(y)
            })
        }
        val popupWidth = dp(280)
        val popupHeight = dp(100)
        emojiButton.getLocationInWindow(screenLocation)
        val anchorX = screenLocation[0]
        rootView.getLocationInWindow(screenLocation)
        val rootX = screenLocation[0]
        val rootWidth = rootView.width
        val left = FooterShortcutPolicy.clampedPopupLeft(anchorX + emojiButton.width / 2,
            rootX, rootWidth, popupWidth, dp(4))
        popup = PopupWindow(panel, popupWidth, popupHeight, false).apply {
            isTouchable = false
            isOutsideTouchable = false
            setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
            elevation = dp(14).toFloat()
            showAsDropDown(
                emojiButton,
                left - anchorX,
                -emojiButton.height - popupHeight - dp(8),
                Gravity.LEFT,
            )
        }
        // A reversible trackpad-style focus cue. Hide only the keycaps, not
        // the keyboard's wallpaper glass or the active input field.
        fadedKeys = KeyboardSwitcher.getInstance().mainKeyboardView?.takeIf { it.isShown }
        fadedKeys?.animate()?.cancel()
        fadedKeys?.animate()?.alpha(0f)?.setDuration(110)?.start()
    }

    private fun updateSelection(x: Float, y: Float) {
        val previous = selected
        selected = targets.indexOfFirst { target ->
            target.getLocationOnScreen(screenLocation)
            target.isLaidOut && x >= screenLocation[0] && x < screenLocation[0] + target.width &&
                y >= screenLocation[1] && y < screenLocation[1] + target.height
        }
        targets.forEachIndexed { index, target -> target.isSelected = index == selected }
        if (selected >= 0 && previous != selected) feedback(emojiButton)
    }

    private fun rounded(color: Int, radius: Float, stroke: Int = Color.TRANSPARENT) = GradientDrawable().apply {
        shape = GradientDrawable.RECTANGLE
        cornerRadius = radius
        setColor(color)
        if (Color.alpha(stroke) > 0) setStroke(dp(1), stroke)
    }

    private fun dp(value: Int) = (value * resources.displayMetrics.density).roundToInt()
}
