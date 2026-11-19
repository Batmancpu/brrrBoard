// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.annotation.SuppressLint
import android.animation.ValueAnimator
import android.graphics.drawable.RippleDrawable
import android.content.Context
import android.content.res.ColorStateList
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.graphics.drawable.GradientDrawable
import android.util.AttributeSet
import android.view.GestureDetector
import android.view.Gravity
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import android.view.accessibility.AccessibilityManager
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
import helium314.keyboard.keyboard.KeyboardTypeface
import helium314.keyboard.keyboard.PanelGlassOverlayView
import helium314.keyboard.keyboard.PanelMaterial
import kotlin.math.abs
import kotlin.math.roundToInt


class BoardFooterView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {
    private val glass = PanelGlassOverlayView(context)
    private lateinit var ime: LatinIME
    private val emojiButton get() = findViewById<ImageButton>(R.id.board_footer_globe)
    private val microphone get() = findViewById<ImageButton>(R.id.board_footer_mic)
    private val voiceError get() = findViewById<TextView>(R.id.board_footer_voice_error)
    private val contextAction get() = findViewById<TextView>(R.id.board_footer_context_action)
    private var popup: PopupWindow? = null
    private val targets = ArrayList<View>()
    private var fadedKeys: View? = null
    private var dictationActive = false
    private var dictationProcessing = false
    private var returnToLetters = false
    private var micPulse: ValueAnimator? = null
    private val emojiRing = FooterStatusRingDrawable(resources.displayMetrics.density, false)
    private val micRing = FooterStatusRingDrawable(resources.displayMetrics.density, true)
    private var cancelDictationActionId: Int? = null
    private val clearVoiceError = Runnable { voiceError.visibility = GONE }
    private val clearContextActionRunnable = Runnable { clearContextAction() }
    private var contextActionOwner: Any? = null
    private var contextActionCallback: (() -> Unit)? = null
    private var selected = -1
    private var tracking = false
    private var downX = 0f
    private var downY = 0f
    private val screenLocation = IntArray(2)
    private val openShortcuts = Runnable { if (tracking && isShown) showShortcutArc() }
    /** Single taps are confirmed only after the second-tap window has elapsed. */
    private val micGestureDetector = GestureDetector(context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (!micActionAvailable()) return false
                microphone.performClick()
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean = true

            override fun onDoubleTapEvent(e: MotionEvent): Boolean {
                // Wait for the second UP; a dragged/cancelled second touch must not switch IMEs.
                if (e.actionMasked == MotionEvent.ACTION_UP && micActionAvailable()) {
                    microphone.isPressed = false
                    feedback(microphone)
                    ime.switchBoardInputMethod()
                }
                return true
            }
        })

    private fun micActionAvailable(): Boolean = this::ime.isInitialized &&
        microphone.visibility == VISIBLE && microphone.isShown &&
        ViewCompat.isAttachedToWindow(microphone)

    private fun isMicTouchExplorationEnabled(): Boolean =
        (context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager)
            ?.isTouchExplorationEnabled == true

    private fun cancelMicGesture() {
        // GestureDetector owns a delayed single-tap confirmation. Cancel it on
        // keyboard exit so an old tap cannot start Dictate in the next editor.
        val event = MotionEvent.obtain(0L, android.os.SystemClock.uptimeMillis(),
            MotionEvent.ACTION_CANCEL, 0f, 0f, 0)
        micGestureDetector.onTouchEvent(event)
        event.recycle()
        microphone.isPressed = false
    }
    private val shortcuts = listOf(
        Shortcut(R.string.clipboard, R.drawable.ic_brr_clipboard, FooterShortcutPolicy.Action.CLIPBOARD),
        Shortcut(R.string.board_toolbar, R.drawable.ic_board_toolbar, FooterShortcutPolicy.Action.TOOLBAR),
        Shortcut(R.string.settings, R.drawable.sym_keyboard_settings_rounded, FooterShortcutPolicy.Action.SETTINGS),
    )

    private data class Shortcut(val label: Int, val icon: Int, val action: FooterShortcutPolicy.Action)

    override fun onFinishInflate() {
        super.onFinishInflate()
        // Backdrop underlays the *existing* footer buttons; they never blur.
        addView(glass, 0, LayoutParams(LayoutParams.MATCH_PARENT, LayoutParams.MATCH_PARENT))
        glass.visibility = GONE
        val ripple = ColorStateList.valueOf(0x338A8D9B)
        emojiButton.background = RippleDrawable(ripple, emojiRing, null)
        microphone.background = RippleDrawable(ripple, micRing, null)
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
        // Touch-exploration double-taps activate an accessibility click, not the
        // IME-switch gesture. The dedicated accessibility switch action stays below.
        microphone.setOnTouchListener { view, event ->
            if (isMicTouchExplorationEnabled()) return@setOnTouchListener false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> view.isPressed = true
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL,
                MotionEvent.ACTION_POINTER_DOWN -> view.isPressed = false
            }
            micGestureDetector.onTouchEvent(event)
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
        val dark = KeyboardTheme.isDarkThemeActive(context)
        emojiRing.setAppearance(dark)
        micRing.setAppearance(dark)
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
            if (active) FooterIndicatorPolicy.voiceTint(KeyboardTheme.isDarkThemeActive(context))
            else Color.WHITE,
        )
        microphone.contentDescription = resources.getString(
            if (active) R.string.board_dictation_stop else R.string.board_inline_dictation,
        )
        micRing.setVoiceState(if (active) FooterStatusRingDrawable.VoiceState.LISTENING
            else FooterStatusRingDrawable.VoiceState.IDLE)
        if (active) startMicPulse() else stopMicPulse()
    }

    fun showDictationProcessing(processing: Boolean = true) {
        dictationProcessing = processing
        if (processing) {
            showDictationActive(false)
            dictationProcessing = true
            micRing.setVoiceState(FooterStatusRingDrawable.VoiceState.PROCESSING)
            microphone.imageTintList = ColorStateList.valueOf(
                FooterIndicatorPolicy.voiceTint(KeyboardTheme.isDarkThemeActive(context)))
            microphone.contentDescription = resources.getString(R.string.board_dictation_processing)
            startMicPulse()
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

    /** The footer's center is normally empty: one scoped, short-lived action only. */
    fun showContextAction(owner: Any, message: Int, action: Int,
        durationMs: Long = 6_000L, callback: () -> Unit) {
        clearContextAction()
        if (!isShown) return
        contextActionOwner = owner
        contextActionCallback = callback
        val colors = Settings.getValues().mColors
        val dark = KeyboardTheme.isDarkThemeActive(context)
        val chip = contextAction
        val label = resources.getString(message) + "  ·  " + resources.getString(action)
        chip.text = label
        chip.contentDescription = label
        chip.setTextColor(colors.get(ColorType.KEY_ICON))
        KeyboardTypeface.applyToTextView(chip)
        chip.background = rounded(
            ColorUtils.setAlphaComponent(if (dark) 0xFF41434A.toInt() else 0xFFE4E5E9.toInt(), 228),
            dp(18).toFloat(),
            ColorUtils.setAlphaComponent(colors.get(ColorType.KEY_ICON), 32))
        chip.setOnClickListener { view ->
            feedback(view)
            val onSelected = contextActionCallback
            clearContextAction(owner)
            onSelected?.invoke()
        }
        voiceError.visibility = GONE
        chip.visibility = VISIBLE
        chip.animate().cancel()
        if (KeyboardMotionSpec.enabled()) {
            chip.alpha = 0f
            chip.scaleX = 0.95f
            chip.scaleY = 0.95f
            chip.animate().alpha(1f).scaleX(1f).scaleY(1f)
                .setDuration(KeyboardMotionSpec.FOOTER_ACTION_MS).start()
        } else {
            chip.alpha = 1f
            chip.scaleX = 1f
            chip.scaleY = 1f
        }
        postDelayed(clearContextActionRunnable, FooterContextActionPolicy.durationMs(durationMs))
    }

    /** Only the original operation can revoke its action after mode changes. */
    fun clearContextAction(owner: Any? = null) {
        if (!FooterContextActionPolicy.mayClear(owner, contextActionOwner)) return
        removeCallbacks(clearContextActionRunnable)
        contextActionOwner = null
        contextActionCallback = null
        val chip = contextAction
        chip.setOnClickListener(null)
        chip.visibility = GONE
    }

    private fun startMicPulse() {
        if (micPulse != null) return
        if (!KeyboardMotionSpec.enabled()) return
        val mic = microphone
        micPulse = ValueAnimator.ofFloat(0f, 1f, 0f).apply {
            duration = KeyboardMotionSpec.VOICE_PULSE_CYCLE_MS
            repeatCount = ValueAnimator.INFINITE
            interpolator = AccelerateDecelerateInterpolator()
            addUpdateListener { animation ->
                val phase = FooterIndicatorPolicy.pulseIntensity(
                    animation.animatedValue as Float, dictationProcessing)
                micRing.setPulse(phase)
                mic.scaleX = 1f + 0.065f * phase
                mic.scaleY = 1f + 0.065f * phase
                mic.imageAlpha = (230f + 25f * phase).roundToInt()
            }
            start()
        }
    }

    private fun stopMicPulse() {
        micPulse?.cancel()
        micPulse = null
        micRing.setPulse(0f)
        if (this::ime.isInitialized) {
            microphone.scaleX = 1f
            microphone.scaleY = 1f
            microphone.imageAlpha = 255
        }
    }

    fun reset() {
        clearDictationError()
        clearContextAction()
        showDictationActive(false)
        showDictationProcessing(false)
        setDictationCancelAvailable(false)
        cancelMicGesture()
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
            FooterShortcutPolicy.Action.CLIPBOARD -> switcher.setClipboardKeyboard()
            // The normal expandable Toolbar button routes through a toggle
            // which intentionally rejects Emoji/Clipboard origins. The footer
            // shortcut is an explicit navigation command from any panel.
            FooterShortcutPolicy.Action.TOOLBAR -> switcher.setAccessPointKeyboard()
            FooterShortcutPolicy.Action.SETTINGS -> send(KeyCode.SETTINGS)
        }
    }

    private fun showShortcutArc() {
        if (windowToken == null || popup != null) return
        val colors = Settings.getValues().mColors
        val iconColor = colors.get(ColorType.KEY_ICON)
        val dark = KeyboardTheme.isDarkThemeActive(context)
        val materialColor = if (dark) 0xFF292B31.toInt() else 0xFFF5F5F8.toInt()
        val panel = FrameLayout(context).apply {
            background = rounded(
                ColorUtils.setAlphaComponent(materialColor, 248),
                dp(29).toFloat(),
                ColorUtils.setAlphaComponent(iconColor, if (dark) 46 else 30))
            clipToOutline = true
        }
        val selectedFill = ColorUtils.blendARGB(materialColor,
            colors.get(ColorType.ACTION_KEY_BACKGROUND), if (dark) 0.28f else 0.17f)
        // A floating three-action island: no nested slabs or opaque tinted
        // blocks. A generous whole-cell target, circular symbol, quiet label,
        // and subtle selection feedback match the rest of the keyboard.
        val rtl = ViewCompat.getLayoutDirection(this) == ViewCompat.LAYOUT_DIRECTION_RTL
        shortcuts.forEachIndexed { index, shortcut ->
            val button = LinearLayout(context).apply {
                contentDescription = resources.getString(shortcut.label)
                orientation = LinearLayout.VERTICAL
                gravity = Gravity.CENTER
                background = android.graphics.drawable.StateListDrawable().apply {
                    addState(intArrayOf(android.R.attr.state_selected),
                        rounded(selectedFill, dp(21).toFloat()))
                    addState(intArrayOf(), rounded(Color.TRANSPARENT, dp(21).toFloat()))
                }
                addView(FrameLayout(context).apply {
                    background = GradientDrawable().apply {
                        shape = GradientDrawable.OVAL
                        setColor(ColorUtils.setAlphaComponent(
                            colors.get(ColorType.KEY_BACKGROUND), if (dark) 212 else 232))
                        setStroke(dp(1), ColorUtils.setAlphaComponent(iconColor, 24))
                    }
                    addView(ImageView(context).apply {
                        setImageResource(shortcut.icon)
                        imageTintList = ColorStateList.valueOf(iconColor)
                        scaleType = ImageView.ScaleType.FIT_CENTER
                        setPadding(dp(2), dp(2), dp(2), dp(2))
                    }, FrameLayout.LayoutParams(dp(29), dp(29), Gravity.CENTER))
                }, LinearLayout.LayoutParams(dp(48), dp(48)))
                addView(TextView(context).apply {
                    text = resources.getString(shortcut.label)
                    setTextColor(iconColor)
                    textSize = 12f
                    gravity = Gravity.CENTER
                    KeyboardTypeface.applyToTextView(this)
                    maxLines = 1
                }, LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, dp(24)))
            }
            targets.add(button)
            val (x, y) = FooterShortcutPolicy.targetOffset(index, rtl)
            panel.addView(button, LayoutParams(dp(88), dp(94)).apply {
                leftMargin = dp(x)
                topMargin = dp(y)
            })
        }
        val popupWidth = dp(296)
        val popupHeight = dp(112)
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
            elevation = dp(18).toFloat()
            showAsDropDown(
                emojiButton,
                left - anchorX,
                -emojiButton.height - popupHeight - dp(10),
                Gravity.LEFT,
            )
        }
        if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O ||
            ValueAnimator.areAnimatorsEnabled()) {
            panel.alpha = 0f
            panel.translationY = dp(7).toFloat()
            panel.animate().alpha(1f).translationY(0f)
                .setDuration(150).setInterpolator(AccelerateDecelerateInterpolator()).start()
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
        targets.forEachIndexed { index, target ->
            val active = index == selected
            if (target.isSelected != active) {
                target.isSelected = active
                target.animate().cancel()
                if (android.os.Build.VERSION.SDK_INT < android.os.Build.VERSION_CODES.O ||
                    ValueAnimator.areAnimatorsEnabled()) {
                    target.animate().scaleX(if (active) 1.045f else 1f)
                        .scaleY(if (active) 1.045f else 1f).setDuration(100).start()
                }
            }
        }
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
