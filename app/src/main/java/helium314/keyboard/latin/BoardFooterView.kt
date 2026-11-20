// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.annotation.SuppressLint
import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.ValueAnimator
import android.content.SharedPreferences
import android.graphics.drawable.Drawable
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
import android.view.animation.LinearInterpolator
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
import helium314.keyboard.latin.settings.SecureAppsPolicy
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.utils.prefs
import helium314.keyboard.keyboard.KeyboardTheme
import helium314.keyboard.keyboard.KeyboardSwitcher
import helium314.keyboard.keyboard.KeyboardTypeface
import helium314.keyboard.keyboard.PanelGlassOverlayView
import helium314.keyboard.keyboard.PanelMaterial
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlin.math.sin


class BoardFooterView(context: Context, attrs: AttributeSet?) : FrameLayout(context, attrs) {
    private val glass = PanelGlassOverlayView(context)
    private lateinit var ime: LatinIME
    private val emojiButton get() = findViewById<ImageButton>(R.id.board_footer_globe)
    private val microphone get() = findViewById<ImageButton>(R.id.board_footer_mic)
    private val passwordManagerButton get() = findViewById<ImageButton>(R.id.board_footer_password_manager)
    private val micUnderlight get() = findViewById<FooterMicUnderlightView>(R.id.board_footer_mic_underlight)
    private val discardGlyph get() = findViewById<FooterMicDiscardMorphView>(R.id.board_footer_discard)
    private val voiceError get() = findViewById<TextView>(R.id.board_footer_voice_error)
    private val contextAction get() = findViewById<TextView>(R.id.board_footer_context_action)
    private var popup: PopupWindow? = null
    private val targets = ArrayList<View>()
    private var fadedKeys: View? = null
    private var dictationActive = false
    private var dictationProcessing = false
    private var dictationOffline = false
    private var offlineHoldActive = false
    private var returnToLetters = false
    private var cachedPasswordManagerIntent: android.content.Intent? = null
    private var passwordIntentCheckedAt = 0L
    private val privateEmojiIcon = FooterIncognitoEmojiDrawable(context)
    private var lastSyncedPrivacy: Boolean? = null
    private var micDenialAnimator: ValueAnimator? = null
    private var protectedMicTouch = false
    private var protectedMicHeld = false
    private var protectedMicCancelled = false
    private var protectedMicDownX = 0f
    private var protectedMicDownY = 0f
    private var trackpadVisualMode = false
    private val protectedMicHold = Runnable {
        if (protectedMicTouch && isMicProtected() && micActionAvailable()) {
            protectedMicHeld = true
            microphone.isPressed = false
            showProtectedMicDenial()
        }
    }
    private var micPulse: ValueAnimator? = null
    private var micDiscardAnimator: ValueAnimator? = null
    private var micDiscardRestartTouchArmed = false
    private var pulsingMicIcon: FooterMicPulseDrawable? = null
    private var idleMicBackground: Drawable? = null
    private var cancelDictationActionId: Int? = null
    private val clearVoiceError = Runnable { voiceError.visibility = GONE }
    private val clearMicDiscardRunnable = Runnable { clearMicDiscardFeedback() }
    private val clearContextActionRunnable = Runnable { clearContextAction() }
    private var contextActionOwner: Any? = null
    private var contextActionCallback: (() -> Unit)? = null
    private var selected = -1
    private var tracking = false
    private var downX = 0f
    private var downY = 0f
    private val screenLocation = IntArray(2)
    private val openShortcuts = Runnable { if (tracking && isShown) showShortcutArc() }
    // Dispatch the first Emoji/ABC tap immediately. The next spatially close
    // tap may still form a disjoint privacy-toggle pair; no 300ms UI wait.
    private val emojiTapTimeoutMs = ViewConfiguration.getDoubleTapTimeout().toLong()
    private val emojiTapSlopPx = ViewConfiguration.get(context).scaledDoubleTapSlop.toFloat()
    private var firstEmojiTapRecognized = false
    private var firstEmojiTapOpenedPanel = false
    private var lastEmojiUpTime = 0L
    private var lastEmojiUpX = 0f
    private var lastEmojiUpY = 0f
    private var secondEmojiTap = false
    private val privacyListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        if (key == Settings.PREF_ALWAYS_INCOGNITO_MODE || key == Settings.PREF_SECURE_APPS) {
            post {
                if (ViewCompat.isAttachedToWindow(this)) {
                    syncEmojiPrivacy(animate = true)
                    requestLayout()
                }
            }
        }
    }
    /** Idle single taps wait for the double-tap switch window; an active stop is immediate. */
    private val micGestureDetector = GestureDetector(context,
        object : GestureDetector.SimpleOnGestureListener() {
            override fun onDown(e: MotionEvent): Boolean = true

            override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                if (!micActionAvailable()) return false
                microphone.performClick()
                return true
            }

            override fun onDoubleTap(e: MotionEvent): Boolean = true

            override fun onLongPress(e: MotionEvent) {
                if (!micActionAvailable() || dictationActive || dictationProcessing) return
                offlineHoldActive = true
                microphone.isPressed = true
                feedback(microphone)
                ime.startBoardOfflineDictation()
            }

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

    private fun isMicProtected(): Boolean = this::ime.isInitialized &&
        Settings.getValues().mIncognitoModeEnabled

    private fun isMicTouchExplorationEnabled(): Boolean =
        (context.getSystemService(Context.ACCESSIBILITY_SERVICE) as? AccessibilityManager)
            ?.isTouchExplorationEnabled == true

    private fun cancelMicGesture() {
        offlineHoldActive = false
        removeCallbacks(protectedMicHold)
        protectedMicTouch = false
        protectedMicHeld = false
        protectedMicCancelled = false
        // GestureDetector owns a delayed single-tap confirmation. Cancel it on
        // keyboard exit so an old tap cannot start Dictate in the next editor.
        val event = MotionEvent.obtain(0L, android.os.SystemClock.uptimeMillis(),
            MotionEvent.ACTION_CANCEL, 0f, 0f, 0)
        micGestureDetector.onTouchEvent(event)
        event.recycle()
        microphone.isPressed = false
    }

    private fun cancelEmojiGesture() {
        firstEmojiTapRecognized = false
        firstEmojiTapOpenedPanel = false
        secondEmojiTap = false
        emojiButton.isPressed = false
    }

    private fun emojiTapDown(event: MotionEvent) {
        val elapsed = android.os.SystemClock.uptimeMillis() - lastEmojiUpTime
        secondEmojiTap = FooterEmojiTapPolicy.isSecondTap(
            firstEmojiTapRecognized, elapsed,
            event.rawX - lastEmojiUpX, event.rawY - lastEmojiUpY,
            emojiTapTimeoutMs, emojiTapSlopPx)
    }

    private fun emojiTapUp(event: MotionEvent) {
        when (FooterEmojiTapPolicy.action(secondEmojiTap, firstEmojiTapOpenedPanel)) {
            FooterEmojiTapPolicy.Action.PRIVACY_AND_RETURN -> {
                cancelEmojiGesture()
                // Preserve the original double-tap incognito shortcut, ending
                // on ABC as before. No delayed first tap or double insertion.
                toggleFooterIncognito()
                send(KeyCode.ALPHA)
                return
            }
            FooterEmojiTapPolicy.Action.CONSUME_SECOND -> {
                cancelEmojiGesture() // ABC already returned on the first UP.
                return
            }
            FooterEmojiTapPolicy.Action.PRIMARY_NOW -> Unit
        }
        firstEmojiTapRecognized = true
        firstEmojiTapOpenedPanel = !returnToLetters
        lastEmojiUpTime = android.os.SystemClock.uptimeMillis()
        lastEmojiUpX = event.rawX
        lastEmojiUpY = event.rawY
        if (emojiButton.isShown && ViewCompat.isAttachedToWindow(emojiButton))
            emojiButton.performClick()
    }

    private fun toggleFooterIncognito() {
        // Protected/password fields force private behavior regardless of the
        // user's preference; never imply that a double tap disabled it there.
        val inputAttributes = Settings.getValues().mInputAttributes
        val wasForced = inputAttributes.mNoLearning || inputAttributes.mIsPasswordField ||
            SecureAppsPolicy.isSecure(context.prefs(), inputAttributes.mTargetApplicationPackageName)
        if (wasForced) {
            emojiButton.announceForAccessibility(resources.getString(R.string.board_incognito_forced))
            return
        }
        val next = !context.prefs().getBoolean(Settings.PREF_ALWAYS_INCOGNITO_MODE,
            Defaults.PREF_ALWAYS_INCOGNITO_MODE)
        // Use the established Settings code path so all user-history, swipe,
        // Emoji and clipboard guards receive the updated effective mode.
        ime.clearPendingPrivateLearning()
        Settings.getInstance().toggleAlwaysIncognitoMode()
        if (next && (dictationActive || dictationProcessing)) {
            if (dictationOffline) ime.cancelBoardOfflineDictation()
            else ime.cancelBoardDictation()
        }
        // Three tiny ticks, never the ordinary fourth key-press tick or sound.
        AudioAndHapticFeedbackManager.getInstance().performPrivacyModeConfirmation()
        syncEmojiPrivacy(animate = true)
        emojiButton.announceForAccessibility(resources.getString(
            if (next) R.string.board_incognito_on else R.string.board_incognito_off))
    }

    private fun syncEmojiPrivacy(animate: Boolean) {
        val privateMode = Settings.getValues().mIncognitoModeEnabled
        if (lastSyncedPrivacy != null && lastSyncedPrivacy != privateMode) {
            // Flush delayed normal-mic double-tap callbacks before a privacy switch.
            cancelMicGesture()
            if (!privateMode) stopProtectedMicDenial()
        }
        lastSyncedPrivacy = privateMode
        privateEmojiIcon.setThemeInk(Settings.getValues().mColors.get(ColorType.KEY_ICON))
        privateEmojiIcon.setIncognito(privateMode, animated = animate && isShown && !returnToLetters)
        if (!returnToLetters) {
            emojiButton.contentDescription = resources.getString(
                if (privateMode) R.string.board_emoji_private_desc else R.string.board_emoji_normal_desc)
        }
        ViewCompat.setStateDescription(emojiButton,
            resources.getString(if (privateMode) R.string.board_incognito_on else R.string.board_incognito_off))
    }

    /**
     * Genuine iOS-profile trackpad mode: the entire 48dp hitboxes stay laid
     * out, while Emoji exits left and the mic/shield exits right together.
     * Reversing during entry continues from the current rendered translation.
     */
    fun setTrackpadMode(active: Boolean) {
        val desired = active && KeyboardMotionSpec.isIos27(context)
        if (trackpadVisualMode == desired) return
        trackpadVisualMode = desired
        if (desired) {
            cancelEmojiGesture()
            dismissShortcutArc()
            clearMicDiscardFeedback()
        }
        emojiButton.isEnabled = !desired
        microphone.isEnabled = !desired
        passwordManagerButton.isEnabled = !desired
        val slide = dp(64).toFloat()
        val smooth = KeyboardMotionSpec.easing(context)
        val duration = if (desired) KeyboardMotionSpec.TRACKPAD_FOOTER_EXIT_MS
            else KeyboardMotionSpec.TRACKPAD_FOOTER_RETURN_MS
        val controls = arrayOf(emojiButton, microphone, micUnderlight, passwordManagerButton)
        controls.forEachIndexed { index, view ->
            val target = if (!desired) 0f else if (index == 0) -slide else slide
            view.animate().cancel()
            if (!KeyboardMotionSpec.enabled() || !isShown) {
                view.translationX = target
                view.alpha = if (desired) 0f else 1f
            } else {
                val animation = view.animate().translationX(target)
                    .alpha(if (desired) 0f else 1f).setDuration(duration)
                if (smooth != null) animation.setInterpolator(smooth)
                animation.start()
            }
        }
    }

    private fun resetTrackpadVisuals() {
        trackpadVisualMode = false
        arrayOf(emojiButton, microphone, micUnderlight, passwordManagerButton).forEach { view ->
            view.animate().cancel()
            view.translationX = 0f
            view.alpha = 1f
        }
        emojiButton.isEnabled = true
        microphone.isEnabled = true
        passwordManagerButton.isEnabled = true
        // The effect layer is intentionally disabled/noninteractive in XML.
        micUnderlight.isEnabled = false
    }

    /** A silent, non-recording rejection: the emoji glances down and flashes red. */
    private fun showProtectedMicDenial() {
        AudioAndHapticFeedbackManager.getInstance().performPrivacyMicDenial()
        val previous = micDenialAnimator
        micDenialAnimator = null
        previous?.cancel()
        privateEmojiIcon.setDenialFraction(0f)
        emojiButton.scaleX = 1f
        emojiButton.scaleY = 1f
        if (!KeyboardMotionSpec.enabled() || returnToLetters || !emojiButton.isShown) return
        micDenialAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            // Exactly the duration of the rejection haptic waveform.
            duration = AudioAndHapticFeedbackManager.PRIVACY_MIC_DENIAL_MILLIS
            interpolator = LinearInterpolator()
            addUpdateListener {
                val pulse = sin(Math.PI * (it.animatedValue as Float)).toFloat().coerceAtLeast(0f)
                privateEmojiIcon.setDenialFraction(pulse)
                emojiButton.scaleX = 1f + .075f * pulse
                emojiButton.scaleY = 1f + .075f * pulse
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (micDenialAnimator === animation) {
                        micDenialAnimator = null
                        privateEmojiIcon.setDenialFraction(0f)
                        emojiButton.scaleX = 1f
                        emojiButton.scaleY = 1f
                    }
                }
            })
            start()
        }
    }

    private fun stopProtectedMicDenial() {
        val old = micDenialAnimator
        micDenialAnimator = null
        old?.cancel()
        privateEmojiIcon.setDenialFraction(0f)
        emojiButton.scaleX = 1f
        emojiButton.scaleY = 1f
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
        // Emoji, ABC and the mic keep their original unframed touch surfaces.
        // Recording/processing illuminate only the exact mic silhouette below.
        idleMicBackground = microphone.background
        emojiButton.setImageDrawable(privateEmojiIcon)
        privateEmojiIcon.setFrameListener { pose -> micUnderlight.setPrivacyShield(pose.shield) }
        microphone.drawable?.let { original ->
            pulsingMicIcon = FooterMicPulseDrawable(original.mutate()).also {
                microphone.setImageDrawable(it)
            }
        }
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
        microphone.setOnClickListener {
            // A direct/accessibility click during discard has the same immediate
            // restart semantics as a physical tap, without waiting for the morph.
            if (discardGlyph.visibility == VISIBLE) clearMicDiscardFeedback()
            feedback(it)
            ime.toggleBoardOfflineDictation()
        }
        // TalkBack's explicit long-click bypasses the raw touch detector.
        microphone.setOnLongClickListener {
            if (isMicProtected()) {
                showProtectedMicDenial()
                true
            } else false
        }
        passwordManagerButton.setOnClickListener {
            if (!Settings.getValues().mInputAttributes.mIsPasswordField) return@setOnClickListener
            cachedPasswordManagerIntent?.let { target ->
                if (PasswordManagerAction.open(context, target)) feedback(it)
            }
        }
        // Touch-exploration double-taps activate an accessibility click, not the
        // IME-switch gesture. The dedicated accessibility switch action stays below.
        microphone.setOnTouchListener { view, event ->
            if (isMicTouchExplorationEnabled()) return@setOnTouchListener false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> view.isPressed = true
                MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL,
                MotionEvent.ACTION_POINTER_DOWN -> view.isPressed = false
            }
            // Cancellation is a visual event, not an input lock. Interrupt its
            // animation on DOWN, then start a fresh recording on a valid UP.
            // A dragged-away or cancelled gesture must never start the mic.
            if (event.actionMasked == MotionEvent.ACTION_DOWN &&
                discardGlyph.visibility == VISIBLE) {
                clearMicDiscardFeedback()
                // A held protected mic must still deny even during a discard morph.
                micDiscardRestartTouchArmed = !isMicProtected()
            }
            if (micDiscardRestartTouchArmed) {
                when (event.actionMasked) {
                    MotionEvent.ACTION_UP -> {
                        micDiscardRestartTouchArmed = false
                        if (micActionAvailable() && event.x >= 0f && event.x < view.width &&
                            event.y >= 0f && event.y < view.height) view.performClick()
                    }
                    MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_DOWN ->
                        micDiscardRestartTouchArmed = false
                }
                return@setOnTouchListener true
            }
            // Private mic taps still use RAM-only offline transcription. No long
            // hold or double tap can launch recording or switch input methods.
            // The protected path must run BEFORE the active/processing fast-tap
            // branch, otherwise a long hold during recording would stop it.
            if (protectedMicTouch ||
                (event.actionMasked == MotionEvent.ACTION_DOWN && isMicProtected())) {
                when (event.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        protectedMicTouch = true
                        protectedMicHeld = false
                        protectedMicCancelled = false
                        protectedMicDownX = event.x
                        protectedMicDownY = event.y
                        postDelayed(protectedMicHold,
                            ViewConfiguration.getLongPressTimeout().toLong())
                    }
                    MotionEvent.ACTION_MOVE -> {
                        val slop = ViewConfiguration.get(context).scaledTouchSlop.toFloat()
                        if (abs(event.x - protectedMicDownX) > slop ||
                            abs(event.y - protectedMicDownY) > slop) {
                            protectedMicCancelled = true
                            removeCallbacks(protectedMicHold)
                            view.isPressed = false
                        }
                    }
                    MotionEvent.ACTION_UP -> {
                        removeCallbacks(protectedMicHold)
                        if (!protectedMicHeld && !protectedMicCancelled && micActionAvailable() &&
                            event.x >= 0f && event.x < view.width &&
                            event.y >= 0f && event.y < view.height) view.performClick()
                        protectedMicTouch = false
                        protectedMicHeld = false
                        protectedMicCancelled = false
                    }
                    MotionEvent.ACTION_POINTER_DOWN -> {
                        removeCallbacks(protectedMicHold)
                        protectedMicCancelled = true
                        view.isPressed = false
                    }
                    MotionEvent.ACTION_CANCEL -> {
                        removeCallbacks(protectedMicHold)
                        protectedMicTouch = false
                        protectedMicHeld = false
                        protectedMicCancelled = false
                        view.isPressed = false
                    }
                }
                return@setOnTouchListener true
            }
            // A held mic ends on release; a single tap toggles the SAME private engine.
            // Neither gesture may bind to brrSpeak or accidentally switch keyboards.
            if (offlineHoldActive) {
                when (event.actionMasked) {
                    MotionEvent.ACTION_UP -> {
                        offlineHoldActive = false
                        ime.stopBoardOfflineDictation()
                    }
                    MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_DOWN -> {
                        offlineHoldActive = false
                        ime.cancelBoardOfflineDictation()
                    }
                }
                return@setOnTouchListener true
            }
            // Once capture really begins (or transcription is pending), the next tap
            // stops/cancels without the idle double-tap confirmation delay. A quick
            // idle double-tap still switches keyboards without ever opening a mic.
            if (FooterMicTapPolicy.isImmediate(dictationActive, dictationProcessing)) {
                if (event.actionMasked == MotionEvent.ACTION_UP && micActionAvailable()) {
                    view.performClick()
                }
                return@setOnTouchListener true
            }
            micGestureDetector.onTouchEvent(event)
            true
        }
        ViewCompat.addAccessibilityAction(microphone, resources.getString(R.string.board_system_keyboard_picker)) { _, _ ->
            if (isMicProtected()) showProtectedMicDenial()
            else ime.switchBoardInputMethod()
            true
        }
        ViewCompat.addAccessibilityAction(microphone, resources.getString(R.string.board_voice_offline_accessibility)) { _, _ ->
            ime.toggleBoardOfflineDictation()
            true
        }
        ViewCompat.addAccessibilityAction(emojiButton, resources.getString(R.string.board_system_keyboard_picker)) { _, _ ->
            ime.showInputPickerDialog()
            true
        }
        ViewCompat.addAccessibilityAction(emojiButton, resources.getString(R.string.board_toggle_incognito)) { _, _ ->
            toggleFooterIncognito()
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
            if (isMicTouchExplorationEnabled()) return@setOnTouchListener false
            when (event.actionMasked) {
                MotionEvent.ACTION_DOWN -> {
                    emojiTapDown(event)
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
                        if (secondEmojiTap) cancelEmojiGesture()
                        removeCallbacks(openShortcuts)
                        view.isPressed = false
                    }
                }
                MotionEvent.ACTION_UP -> {
                    val hadPopup = popup != null
                    if (hadPopup) updateSelection(event.rawX, event.rawY)
                    val actionToPerform = shortcuts.getOrNull(selected)?.action
                    val tap = tracking && !hadPopup &&
                        event.x >= 0f && event.x < view.width &&
                        event.y >= 0f && event.y < view.height
                    dismissShortcutArc()
                    if (actionToPerform != null) {
                        cancelEmojiGesture()
                        performShortcut(actionToPerform)
                    } else if (tap) emojiTapUp(event)
                }
                MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_DOWN -> {
                    cancelEmojiGesture()
                    dismissShortcutArc()
                }
            }
            true
        }
    }

    fun render(show: Boolean, showMicrophone: Boolean, returnToAbc: Boolean) {
        visibility = if (show) VISIBLE else GONE
        returnToLetters = returnToAbc
        if (returnToAbc) emojiButton.setImageResource(R.drawable.ic_board_abc)
        else emojiButton.setImageDrawable(privateEmojiIcon)
        emojiButton.contentDescription = resources.getString(
            if (returnToAbc) R.string.board_return_to_letters else R.string.board_language_and_shortcuts,
        )
        if (!show) {
            reset()
            return
        }
        val tint = ColorStateList.valueOf(Settings.getValues().mColors.get(ColorType.KEY_ICON))
        val dark = KeyboardTheme.isDarkThemeActive(context)
        emojiButton.imageTintList = if (returnToAbc) tint else null
        syncEmojiPrivacy(animate = false)
        voiceError.setTextColor(Settings.getValues().mColors.get(ColorType.KEY_ICON))
        if (dictationProcessing) showDictationProcessing()
        else showDictationActive(dictationActive)
        microphone.visibility = if (showMicrophone) VISIBLE else INVISIBLE
        micUnderlight.setPrivacyShieldVisible(showMicrophone)
        // Do not add a password icon on ordinary text, email, incognito, etc.
        // Password fields naturally hide the mic, so reuse precisely that slot.
        val passwordField = Settings.getValues().mInputAttributes.mIsPasswordField
        if (passwordField && !showMicrophone) {
            val now = android.os.SystemClock.uptimeMillis()
            if (passwordIntentCheckedAt == 0L || now - passwordIntentCheckedAt > 5_000L) {
                cachedPasswordManagerIntent = PasswordManagerAction.launchIntent(context)
                passwordIntentCheckedAt = now
            }
        }
        passwordManagerButton.visibility = if (passwordField && !showMicrophone &&
            cachedPasswordManagerIntent != null) VISIBLE else GONE
        passwordManagerButton.imageTintList = tint
    }

    fun showDictationActive(active: Boolean, offline: Boolean = false) {
        // A layout/theme re-render must not erase the live voice status.
        if (active) {
            clearMicDiscardFeedback()
            if (!dictationActive) clearDictationError()
        }
        dictationActive = active
        dictationProcessing = false
        dictationOffline = active && offline
        val dark = KeyboardTheme.isDarkThemeActive(context)
        micUnderlight.setPhase(if (active) FooterMicUnderlightView.Phase.RECORDING
            else FooterMicUnderlightView.Phase.IDLE, dark)
        val colors = Settings.getValues().mColors
        microphone.imageTintList = ColorStateList.valueOf(
            if (active) FooterIndicatorPolicy.voiceTint(dark)
            else colors.get(ColorType.KEY_ICON),
        )
        microphone.contentDescription = resources.getString(
            if (active) R.string.board_dictation_stop else R.string.board_inline_dictation,
        )
        // No disk, ring or badge: the silhouette-shaped glow belongs to the
        // actual keyboard surface below the existing 48dp mic touch target.
        microphone.background = idleMicBackground
        if (active) startMicPulse() else stopMicPulse()
    }

    fun showDictationProcessing(processing: Boolean = true, offline: Boolean = dictationOffline) {
        dictationProcessing = processing
        if (processing) {
            clearMicDiscardFeedback()
            showDictationActive(false)
            dictationProcessing = true
            dictationOffline = offline
            // Processing is communicated solely by the turquoise pulsing mic;
            // old Cancel/Transcribing text must never survive until insertion.
            clearDictationError()
            micUnderlight.setPhase(FooterMicUnderlightView.Phase.PROCESSING,
                KeyboardTheme.isDarkThemeActive(context))
            microphone.background = idleMicBackground
            microphone.imageTintList = ColorStateList.valueOf(
                FooterIndicatorPolicy.processingTint(KeyboardTheme.isDarkThemeActive(context)))
            microphone.contentDescription = resources.getString(R.string.board_dictation_processing)
            // The turquoise glyph and its mic-shaped underlight breathe together.
            startMicPulse()
        }
    }

    /** Called only after an explicit user Cancel of an active private voice session. */
    fun showDictationDiscarded(wasProcessing: Boolean = false) {
        clearMicDiscardFeedback()
        if (!isShown || microphone.visibility != VISIBLE) return
        clearDictationError()
        val glyph = discardGlyph
        val dark = KeyboardTheme.isDarkThemeActive(context)
        glyph.setStartingMicTint(if (wasProcessing) FooterIndicatorPolicy.processingTint(dark)
            else FooterIndicatorPolicy.voiceTint(dark))
        glyph.setIdleTint(Settings.getValues().mColors.get(ColorType.KEY_ICON))
        glyph.setProgress(0f)
        glyph.visibility = VISIBLE
        microphone.announceForAccessibility(resources.getString(R.string.board_voice_discarded))
        microphone.imageAlpha = 0
        if (!KeyboardMotionSpec.enabled()) {
            glyph.setProgress(.75f)
            postDelayed(clearMicDiscardRunnable, 220L)
            return
        }
        micDiscardAnimator = ValueAnimator.ofFloat(0f, 1f).apply {
            duration = FooterMicMorphPolicy.DURATION_MS
            interpolator = LinearInterpolator()
            addUpdateListener { frame ->
                val p = frame.animatedValue as Float
                glyph.setProgress(p)
                // The morph's white mic and real idle mic crossfade in one place.
                microphone.imageAlpha = (FooterMicMorphPolicy.idleRevealFraction(p) * 255f).roundToInt()
            }
            addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationEnd(animation: Animator) {
                    if (micDiscardAnimator === animation) clearMicDiscardFeedback()
                }
            })
            start()
        }
    }

    private fun clearMicDiscardFeedback() {
        removeCallbacks(clearMicDiscardRunnable)
        val running = micDiscardAnimator
        micDiscardAnimator = null
        running?.cancel()
        discardGlyph.visibility = GONE
        discardGlyph.setProgress(0f)
        microphone.imageAlpha = 255
    }

    fun setDictationCancelAvailable(available: Boolean, offline: Boolean = dictationOffline) {
        val mic = microphone
        if (available && cancelDictationActionId == null) {
            cancelDictationActionId = ViewCompat.addAccessibilityAction(
                mic, resources.getString(R.string.board_cancel_dictation)
            ) { _, _ ->
                if (offline) ime.cancelBoardOfflineDictation() else ime.cancelBoardDictation()
                true
            }
        } else if (!available) {
            cancelDictationActionId?.let { ViewCompat.removeAccessibilityAction(mic, it) }
            cancelDictationActionId = null
        }
    }

    fun showDictationError(message: Int) {
        removeCallbacks(clearVoiceError)
        configureVoiceStatusAction(false)
        voiceError.setText(message)
        voiceError.visibility = VISIBLE
        voiceError.announceForAccessibility(resources.getString(message))
        postDelayed(clearVoiceError, 4_000L)
    }

    /** Stable footer status; changing state never changes the keyboard's measured height. */
    fun showDictationStatus(message: Int) {
        if (dictationProcessing) {
            clearDictationError()
            return
        }
        removeCallbacks(clearVoiceError)
        val blueRecording = message == R.string.board_voice_offline_listening
        val visibleMessage = if (blueRecording) R.string.board_voice_cancel else message
        val label = resources.getString(visibleMessage)
        val changed = voiceError.text.toString() != label || voiceError.visibility != VISIBLE
        configureVoiceStatusAction(visibleMessage == R.string.board_voice_cancel,
            offline = blueRecording || dictationOffline)
        voiceError.setText(visibleMessage)
        voiceError.visibility = VISIBLE
        if (changed) voiceError.announceForAccessibility(label)
    }

    fun clearDictationError() {
        removeCallbacks(clearVoiceError)
        configureVoiceStatusAction(false)
        voiceError.visibility = GONE
    }

    /** The center Cancel label stops and discards recording; a second MIC tap still stops/sends. */
    private fun configureVoiceStatusAction(cancel: Boolean, offline: Boolean = false) {
        val status = voiceError
        if (cancel && this::ime.isInitialized) {
            status.setOnClickListener { view ->
                feedback(view)
                if (offline) ime.cancelBoardOfflineDictation() else ime.cancelBoardDictation()
            }
            status.contentDescription = resources.getString(R.string.board_cancel_dictation)
            status.isClickable = true
            status.isFocusable = true
        } else {
            status.setOnClickListener(null)
            status.contentDescription = null
            status.isClickable = false
            status.isFocusable = false
        }
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
                .setDuration(if (KeyboardMotionSpec.isIos27(context)) 175L
                    else KeyboardMotionSpec.FOOTER_ACTION_MS).start()
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
                // Animate the mic glyph and the matching underlight, not its touch target.
                micUnderlight.setPulse(phase)
                pulsingMicIcon?.setPulse(phase)
                mic.imageAlpha = (230f + 25f * phase).roundToInt()
            }
            start()
        }
    }

    private fun stopMicPulse() {
        micPulse?.cancel()
        micPulse = null
        micUnderlight.setPulse(0f)
        pulsingMicIcon?.setPulse(0f)
        if (this::ime.isInitialized) {
            microphone.scaleX = 1f
            microphone.scaleY = 1f
            microphone.imageAlpha = 255
        }
    }

    fun reset() {
        resetTrackpadVisuals()
        micDiscardRestartTouchArmed = false
        clearMicDiscardFeedback()
        clearDictationError()
        clearContextAction()
        showDictationActive(false)
        showDictationProcessing(false)
        setDictationCancelAvailable(false)
        cancelMicGesture()
        stopProtectedMicDenial()
        cancelEmojiGesture()
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
        context.prefs().unregisterOnSharedPreferenceChangeListener(privacyListener)
        reset()
        privateEmojiIcon.stopAnimation()
        super.onDetachedFromWindow()
    }

    override fun onAttachedToWindow() {
        super.onAttachedToWindow()
        context.prefs().registerOnSharedPreferenceChangeListener(privacyListener)
        if (this::ime.isInitialized) syncEmojiPrivacy(animate = false)
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
