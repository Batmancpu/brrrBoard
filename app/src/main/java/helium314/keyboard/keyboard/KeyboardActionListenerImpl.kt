// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard

import android.text.InputType
import android.os.SystemClock
import android.util.SparseArray
import android.view.KeyCharacterMap
import android.view.KeyEvent
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.view.inputmethod.InputMethodSubtype
import androidx.core.util.forEach
import helium314.keyboard.event.Event
import helium314.keyboard.event.HangulEventDecoder
import helium314.keyboard.event.HapticEvent
import helium314.keyboard.event.HardwareEventDecoder
import helium314.keyboard.event.HardwareKeyboardEventDecoder
import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode
import helium314.keyboard.latin.AudioAndHapticFeedbackManager
import helium314.keyboard.latin.EmojiAltPhysicalKeyDetector
import helium314.keyboard.latin.LatinIME
import helium314.keyboard.latin.RichInputMethodManager
import helium314.keyboard.latin.common.Constants
import helium314.keyboard.latin.common.InputPointers
import helium314.keyboard.latin.common.combiningRange
import helium314.keyboard.latin.common.moveStepsToCharCount
import helium314.keyboard.latin.define.ProductionFlags
import helium314.keyboard.latin.inputlogic.InputLogic
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.SubtypeSettings
import kotlin.math.abs

class KeyboardActionListenerImpl(private val latinIME: LatinIME, private val inputLogic: InputLogic) : KeyboardActionListener {

    private val connection = inputLogic.mConnection
    private val emojiAltPhysicalKeyDetector by lazy { EmojiAltPhysicalKeyDetector(latinIME.resources) }

    // We expect to have only one decoder in almost all cases, hence the default capacity of 1.
    // If it turns out we need several, it will get grown seamlessly.
    private val hardwareEventDecoders: SparseArray<HardwareEventDecoder> = SparseArray(1)

    private val keyboardSwitcher = KeyboardSwitcher.getInstance()
    private val settings = Settings.getInstance()
    private val audioAndHapticFeedbackManager = AudioAndHapticFeedbackManager.getInstance()

    // This is an editor-local gesture, never saved as a preference or a text receipt.
    // RichInputConnection normalizes its cached selection bounds, so keep the
    // directed head separately to support shrinking and reversing a selection.
    private var trackpadSelectionAnchor = -1
    private var trackpadSelectionHead = -1
    private var trackpadSelectionEditor: EditorInfo? = null
    private var trackpadSelectionConnection: InputConnection? = null
    // A host-directed vertical move is asynchronous. Do not issue a second
    // movement until onUpdateSelection has confirmed the fixed anchor.
    private var trackpadVerticalPending = false

    // language slide state
    private var initialSubtype: InputMethodSubtype? = null
    private var subtypeSwitchCount = 0

    override fun editingGestureSession(): Long =
        if (metaState == 0 && settings.current.mEditingGestureConfig.enabled &&
            !TouchpadHandler.isTouchpadModeRequested() && latinIME.isInputViewShown &&
            keyboardSwitcher.visibleKeyboardView === keyboardSwitcher.mainKeyboardView &&
            latinIME.mayUseTransientCorrectionMemory(settings.current)) latinIME.editorGeneration() else -1L

    override fun dispatchEditingGesture(session: Long, code: Int): Boolean {
        if (session < 0 || session != editingGestureSession() || !EditingGestureConfig.allowedCode(code)) return false
        latinIME.hapticAndAudioFeedback(KeyCode.TEXT_TOOLS,0,HapticEvent.KEY_PRESS)
        onCodeInput(code,Constants.NOT_A_COORDINATE,Constants.NOT_A_COORDINATE,false)
        return true
    }

    override fun onPressKey(primaryCode: Int, repeatCount: Int, isSinglePointer: Boolean, hapticEvent: HapticEvent) {
        if (TouchpadHandler.isTouchpadModeRequested()) return
        metaOnPressKey(primaryCode)
        keyboardSwitcher.onPressKey(primaryCode, isSinglePointer, latinIME.currentAutoCapsState, latinIME.currentRecapitalizeState)
        // we need to use LatinIME for handling of key-down audio and haptics
        latinIME.hapticAndAudioFeedback(primaryCode, repeatCount, hapticEvent)
    }

    override fun onLongPressKey(primaryCode: Int) {
        metaOnLongPressKey(primaryCode)
        performHapticFeedback(HapticEvent.KEY_LONG_PRESS)
    }

    override fun onReleaseKey(primaryCode: Int, withSliding: Boolean) {
        if (TouchpadHandler.isTouchpadModeRequested()) return
        metaOnReleaseKey(primaryCode)
        keyboardSwitcher.onReleaseKey(primaryCode, withSliding, latinIME.currentAutoCapsState, latinIME.currentRecapitalizeState)
    }

    override fun onKeyUp(keyCode: Int, keyEvent: KeyEvent): Boolean {
        emojiAltPhysicalKeyDetector.onKeyUp(keyEvent)
        if (!ProductionFlags.IS_HARDWARE_KEYBOARD_SUPPORTED)
            return false

        val keyIdentifier = keyEvent.deviceId.toLong() shl 32 + keyEvent.keyCode
        return inputLogic.mCurrentlyPressedHardwareKeys.remove(keyIdentifier)
    }

    override fun onKeyDown(keyCode: Int, keyEvent: KeyEvent): Boolean {
        emojiAltPhysicalKeyDetector.onKeyDown(keyEvent)
        if (!ProductionFlags.IS_HARDWARE_KEYBOARD_SUPPORTED)
            return false

        val event: Event
        if (settings.current.mLocale.language == "ko") { // todo: this does not appear to be the right place
            val subtype = keyboardSwitcher.keyboard?.mId?.mSubtype ?: RichInputMethodManager.getInstance().currentSubtype
            event = HangulEventDecoder.decodeHardwareKeyEvent(subtype, keyEvent) {
                getHardwareKeyEventDecoder(keyEvent.deviceId).decodeHardwareKey(keyEvent)
            }
        } else {
            event = getHardwareKeyEventDecoder(keyEvent.deviceId).decodeHardwareKey(keyEvent)
        }

        if (event.isHandled) {
            inputLogic.onCodeInput(
                settings.current, event,
                keyboardSwitcher.getKeyboardShiftMode(), // TODO: this is not necessarily correct for a hardware keyboard right now
                keyboardSwitcher.getCurrentKeyboardScript(),
                latinIME.mHandler
            )
            return true
        }
        return false
    }

    override fun onTapCodeInput(keyboard: Keyboard, primaryCode: Int, x: Int, y: Int) {
        val mkv = keyboardSwitcher.mainKeyboardView
        if (metaState != 0 || mkv.keyboard !== keyboard || TouchpadHandler.isTouchpadModeRequested()) {
            onCodeInput(primaryCode, x, y, false)
            return
        }
        val adjusted = inputLogic.beginAdaptiveTap(keyboard, primaryCode, mkv.getKeyX(x), mkv.getKeyY(y))
        try { onCodeInput(adjusted, x, y, false) }
        finally { inputLogic.endAdaptiveTap(adjusted) }
    }

    override fun onCodeInput(primaryCode: Int, x: Int, y: Int, isKeyRepeat: Boolean) {
        // A second finger must not type an arbitrary key into the editor while
        // the first finger is steering the caret across the blank trackpad.
        if (TouchpadHandler.isTouchpadModeRequested() && primaryCode != KeyCode.ARROW_LEFT &&
            primaryCode != KeyCode.ARROW_RIGHT && primaryCode != KeyCode.ARROW_UP &&
            primaryCode != KeyCode.ARROW_DOWN) return
        when (primaryCode) {
            KeyCode.TOGGLE_AUTOCORRECT -> return settings.toggleAutoCorrect()
            KeyCode.TOGGLE_INCOGNITO_MODE -> return settings.toggleAlwaysIncognitoMode()
            KeyCode.RESIZE_KEYBOARD -> return KeyboardSwitcher.getInstance().enterResizeMode()
            KeyCode.TEXT_TOOLS -> return latinIME.requestContextualTextActions()
        }
        val mkv = keyboardSwitcher.mainKeyboardView

        // checking if the character is a combining accent
        val event = if (primaryCode in combiningRange) { // todo: should this be done later, maybe in inputLogic?
            Event.createSoftwareDeadEvent(primaryCode, 0, metaState, mkv.getKeyX(x), mkv.getKeyY(y), null)
        } else {
            // todo:
            //  setting meta shift should only be done for arrow and similar cursor movement keys
            //  should only be enabled once it works more reliably (currently depends on app for some reason)
//            if (mkv.keyboard?.mId?.isAlphabetShiftedManually == true)
//                Event.createSoftwareKeypressEvent(primaryCode, metaState or KeyEvent.META_SHIFT_ON, mkv.getKeyX(x), mkv.getKeyY(y), isKeyRepeat)
//            else Event.createSoftwareKeypressEvent(primaryCode, metaState, mkv.getKeyX(x), mkv.getKeyY(y), isKeyRepeat)
            Event.createSoftwareKeypressEvent(primaryCode, metaState, mkv.getKeyX(x), mkv.getKeyY(y), isKeyRepeat)
        }
        latinIME.onEvent(event)
        metaAfterCodeInput(primaryCode)
    }

    override fun onTextInput(text: String?) {
        if (!TouchpadHandler.isTouchpadModeRequested()) latinIME.onTextInput(text)
    }

    override fun onStartBatchInput() = latinIME.onStartBatchInput()

    override fun onUpdateBatchInput(batchPointers: InputPointers?) = latinIME.onUpdateBatchInput(batchPointers)

    override fun onEndBatchInput(batchPointers: InputPointers?) = latinIME.onEndBatchInput(batchPointers)

    override fun onCancelBatchInput() = latinIME.onCancelBatchInput()

    // User released a finger outside any key
    override fun onCancelInput() { if (deleteAnchor >= 0) onCancelDeletePointer() }

    override fun onFinishSlidingInput() =
        keyboardSwitcher.onFinishSlidingInput(latinIME.currentAutoCapsState, latinIME.currentRecapitalizeState)

    override fun onCustomRequest(requestCode: Int): Boolean {
        if (requestCode == Constants.CUSTOM_CODE_SHOW_INPUT_METHOD_PICKER) {
            return latinIME.showInputPickerDialog()
        }
        if (requestCode == Constants.CODE_TOUCHPAD_ON) {
            val view = keyboardSwitcher.mainKeyboardView
            if (view != null && helium314.keyboard.latin.KeyboardMotionSpec.isIos27(view.context)) {
                view.alpha = 1f
                view.setTrackpadVisualMode(true)
                keyboardSwitcher.setFooterTrackpadMode(true)
            } else {
                view?.alpha = 0.5f
            }
            return true
        }
        if (requestCode == Constants.CODE_TOUCHPAD_OFF) {
            clearTrackpadSelection()
            keyboardSwitcher.mainKeyboardView?.setTrackpadVisualMode(false)
            keyboardSwitcher.mainKeyboardView?.alpha = 1.0f
            keyboardSwitcher.setFooterTrackpadMode(false)
            return true
        }
        if (requestCode == Constants.CODE_PERFORM_HAPTIC) {
            performHapticFeedback(HapticEvent.KEY_LONG_PRESS)
            return true
        }
        return false
    }

    override fun onHorizontalSpaceSwipe(steps: Int): Boolean = when (Settings.getValues().mSpaceSwipeHorizontal) {
        KeyboardActionListener.SwipeAction.MOVE_CURSOR -> onMoveCursorHorizontally(steps)
        KeyboardActionListener.SwipeAction.SWITCH_LANGUAGE -> onLanguageSlide(steps)
        KeyboardActionListener.SwipeAction.TOGGLE_NUMPAD -> toggleNumpad(false, false)
        else -> false
    }

    override fun onTouchpadCursorMove(horizontalSteps: Int, verticalSteps: Int) {
        if (!TouchpadHandler.isTouchpadModeRequested()) return
        if (trackpadSelectionAnchor >= 0) {
            if (!isTrackpadSelectionEditorValid()) {
                clearTrackpadSelection()
                return
            }
            if (trackpadVerticalPending) {
                when (val confirmed = TrackpadHostSelection.confirmedHead(
                    trackpadSelectionAnchor, trackpadSelectionHead,
                    connection.expectedSelectionStart, connection.expectedSelectionEnd)) {
                    TrackpadHostSelection.WAITING -> return
                    TrackpadHostSelection.INVALID -> {
                        clearTrackpadSelection()
                        return
                    }
                    else -> {
                        trackpadSelectionHead = confirmed
                        trackpadVerticalPending = false
                    }
                }
            }
            if (!isTrackpadSelectionValid()) {
                clearTrackpadSelection()
                return
            }
            if (horizontalSteps != 0) moveTrackpadSelectionHorizontally(horizontalSteps)
            if (verticalSteps != 0 && !trackpadVerticalPending)
                requestTrackpadSelectionVertically(verticalSteps)
            return
        }
        // One selection update can cover several characters. Avoid routing
        // every tiny motion step through the full key-event/decoder pipeline.
        if (horizontalSteps != 0) onMoveCursorHorizontally(horizontalSteps)
        if (verticalSteps != 0) repeat(abs(verticalSteps)) {
            onMoveCursorVertically(if (verticalSteps > 0) 1 else -1)
        }
    }

    override fun onTouchpadSelectionChanged(selecting: Boolean) {
        clearTrackpadSelection()
        if (!selecting || !TouchpadHandler.isTouchpadModeRequested()) return
        val start = connection.expectedSelectionStart
        val end = connection.expectedSelectionEnd
        val editor = latinIME.currentInputEditorInfo ?: return
        val ic = latinIME.currentInputConnection ?: return
        if (start < 0 || end < start) return
        inputLogic.finishInput()
        // Composing teardown may itself move the caret; sample after it.
        val updatedStart = connection.expectedSelectionStart
        val updatedEnd = connection.expectedSelectionEnd
        if (updatedStart < 0 || updatedEnd < updatedStart) return
        trackpadSelectionAnchor = updatedStart
        trackpadSelectionHead = updatedEnd
        trackpadSelectionEditor = editor
        trackpadSelectionConnection = ic
    }

    private fun clearTrackpadSelection() {
        trackpadSelectionAnchor = -1
        trackpadSelectionHead = -1
        trackpadSelectionEditor = null
        trackpadSelectionConnection = null
        trackpadVerticalPending = false
    }

    private fun isTrackpadSelectionEditorValid(): Boolean {
        val anchor = trackpadSelectionAnchor
        val head = trackpadSelectionHead
        return anchor >= 0 && head >= 0
            && latinIME.currentInputEditorInfo === trackpadSelectionEditor
            && latinIME.currentInputConnection === trackpadSelectionConnection
    }

    private fun isTrackpadSelectionValid(): Boolean {
        val anchor = trackpadSelectionAnchor
        val head = trackpadSelectionHead
        return isTrackpadSelectionEditorValid() && !trackpadVerticalPending
            && connection.expectedSelectionStart == minOf(anchor, head)
            && connection.expectedSelectionEnd == maxOf(anchor, head)
    }

    private fun requestTrackpadSelectionVertically(rawSteps: Int) {
        val editor = trackpadSelectionEditor ?: return
        // Only ordinary multiline text fields. The host, not the IME, owns
        // visual line wraps, RTL glyph positions and WebView layout.
        if ((editor.inputType and InputType.TYPE_MASK_CLASS) != InputType.TYPE_CLASS_TEXT ||
            (editor.inputType and InputType.TYPE_TEXT_FLAG_MULTI_LINE) == 0) return
        val ic = trackpadSelectionConnection ?: return
        if (!isTrackpadSelectionValid()) return
        val key = if (rawSteps < 0) KeyEvent.KEYCODE_DPAD_UP else KeyEvent.KEYCODE_DPAD_DOWN
        val now = SystemClock.uptimeMillis()
        val down = KeyEvent(now, now, KeyEvent.ACTION_DOWN, key, 0,
            KeyEvent.META_SHIFT_ON, KeyCharacterMap.VIRTUAL_KEYBOARD, 0,
            KeyEvent.FLAG_SOFT_KEYBOARD)
        val up = KeyEvent.changeAction(down, KeyEvent.ACTION_UP)
        // sendKeyEvent is best-effort and asynchronous. Never treat its return
        // value as a confirmed selection or infer UTF-16 offsets from pixels.
        try {
            if (ic.sendKeyEvent(down)) {
                trackpadVerticalPending = true
                ic.sendKeyEvent(up)
            }
        } catch (_: RuntimeException) {
            // A stale/limited InputConnection must never cause a text edit.
            clearTrackpadSelection()
        }
    }

    private fun moveTrackpadSelectionHorizontally(rawSteps: Int) {
        val rtl = RichInputMethodManager.getInstance().currentSubtype.isRtlSubtype
        val steps = if (rtl) -rawSteps else rawSteps
        if (steps == 0) return
        val anchor = trackpadSelectionAnchor
        val head = trackpadSelectionHead
        // In a selection, getTextBefore/AfterCursor addresses the outside of
        // the selected range. Moving the head *toward* the anchor must instead
        // measure a grapheme from the selected range itself.
        val text = (if ((steps < 0 && head > anchor) || (steps > 0 && head < anchor)) {
            connection.getSelectedText(0)
        } else if (steps < 0) {
            connection.getTextBeforeCursor(-steps * 8, 0)
        } else {
            connection.getTextAfterCursor(steps * 8, 0)
        }) ?: return
        // Editors may return unexpectedly huge or partial selection snapshots.
        // Bail out, preserving the existing selection, rather than allocating
        // or placing a head in a guessed position.
        if (text.length > 4096) return
        val codeUnits = moveStepsToCharCount(text, steps)
        if (codeUnits == 0) return
        val candidate = head + codeUnits
        if (candidate < 0 || !isTrackpadSelectionValid()) return
        if (connection.setSelection(anchor, candidate)) {
            trackpadSelectionHead = candidate
            if (steps < 0) gestureMoveBackHaptics() else gestureMoveForwardHaptics()
        } else {
            clearTrackpadSelection()
        }
    }

    override fun onVerticalSpaceSwipe(steps: Int): Boolean = when (Settings.getValues().mSpaceSwipeVertical) {
        KeyboardActionListener.SwipeAction.MOVE_CURSOR -> onMoveCursorVertically(steps)
        KeyboardActionListener.SwipeAction.SWITCH_LANGUAGE -> onLanguageSlide(steps)
        KeyboardActionListener.SwipeAction.TOGGLE_NUMPAD -> toggleNumpad(false, false)
        KeyboardActionListener.SwipeAction.HIDE_KEYBOARD -> {
            latinIME.requestHideSelf(0)
            true
        }
        KeyboardActionListener.SwipeAction.TOUCHPAD_MODE -> {
            // Activate touchpad mode - the actual cursor movement will be handled in PointerTracker

            // Activation and ensure enough room for navigation.
            val requiredSteps = 8

            if (abs(steps) >= requiredSteps) {
                TouchpadHandler.setTouchpadModeActive(true)
                true
            } else {
                false
            }
        }
        else -> false
    }

    override fun onEndSpaceSwipe(){
        initialSubtype = null
        subtypeSwitchCount = 0
    }

    override fun toggleNumpad(withSliding: Boolean, forceReturnToAlpha: Boolean): Boolean {
        keyboardSwitcher.toggleNumpad(withSliding, latinIME.currentAutoCapsState, latinIME.currentRecapitalizeState, forceReturnToAlpha)
        return true
    }

    // Own only the preview created by this gesture. Text proof is held only in eligible editors.
    private var deleteAnchor = -1
    private var deleteHead = -1
    private var deleteEpoch = -1L
    private var deleteConnection: InputConnection? = null
    private var deleteStarted = 0L
    private var deleteProof: String? = null
    private var deleteCanJournal = false
    private val deleteTimeout = Runnable { onCancelDeletePointer() }

    private fun deleteOwnerValid(): Boolean = deleteAnchor >= 0 &&
        deleteConnection === latinIME.currentInputConnection && deleteEpoch == latinIME.editorGeneration() &&
        connection.expectedSelectionStart == deleteHead && connection.expectedSelectionEnd == deleteAnchor

    override fun onMoveDeletePointer(steps: Int) {
        if (steps == 0) return
        if (deleteAnchor < 0) {
            if (steps > 0 || connection.hasSelection()) return
            inputLogic.finishInput()
            val ic = latinIME.currentInputConnection ?: return
            val anchor = connection.expectedSelectionEnd
            if (anchor < 0 || connection.expectedSelectionStart != anchor) return
            deleteAnchor = anchor; deleteHead = anchor
            deleteEpoch = latinIME.editorGeneration(); deleteConnection = ic
            deleteStarted = SystemClock.elapsedRealtime()
            deleteCanJournal = latinIME.mayUseTransientCorrectionMemory(settings.current)
            latinIME.mHandler.postDelayed(deleteTimeout, 30_000)
        }
        if (!deleteOwnerValid() || SystemClock.elapsedRealtime() - deleteStarted >= 30_000) {
            onCancelDeletePointer()
            return
        }
        val bounded = steps.coerceIn(-128, 128)
        val text = if (bounded > 0) connection.getSelectedText(0)
            else connection.getTextBeforeCursor(-bounded * 4, 0)
        if (text == null) return
        val actual = moveStepsToCharCount(text, bounded)
        val next = deleteHead + actual
        // Do not clamp through a grapheme boundary; ignore oversized moves.
        if (next < (deleteAnchor - 2048).coerceAtLeast(0) || next > deleteAnchor || next == deleteHead) return
        if (connection.setSelection(next, deleteAnchor)) {
            deleteHead = next
            if (deleteCanJournal) {
                if (!latinIME.mayUseTransientCorrectionMemory(settings.current)) { onCancelDeletePointer(); return }
                deleteProof = if (next == deleteAnchor) "" else runCatching { deleteConnection?.getSelectedText(0)?.toString() }.getOrNull()
                if (deleteProof?.length != deleteAnchor - next) { onCancelDeletePointer(); return }
            }
            gestureMoveBackHaptics()
        } else onCancelDeletePointer()
    }

    private fun clearDeletePreview() {
        latinIME.mHandler.removeCallbacks(deleteTimeout)
        deleteAnchor = -1; deleteHead = -1; deleteEpoch = -1L; deleteConnection = null
        deleteProof = null; deleteCanJournal = false
    }

    override fun onCancelDeletePointer() {
        if (deleteOwnerValid()) connection.setSelection(deleteAnchor, deleteAnchor)
        clearDeletePreview()
    }

    override fun onUpWithDeletePointerActive() {
        if (!deleteOwnerValid() || deleteHead == deleteAnchor || SystemClock.elapsedRealtime() - deleteStarted >= 30_000) {
            onCancelDeletePointer()
            return
        }
        if (deleteCanJournal) {
            val proof = deleteProof
            val ic = deleteConnection
            if (proof == null || ic == null || !latinIME.mayUseTransientCorrectionMemory(settings.current) ||
                proof != runCatching { ic.getSelectedText(0)?.toString() }.getOrNull()) {
                onCancelDeletePointer()
                return
            }
            val start = deleteHead; val end = deleteAnchor; val epoch = deleteEpoch
            clearDeletePreview()
            if (!inputLogic.commitVerifiedSelectionEdit(proof, "", start, end, epoch, ic) &&
                ic === latinIME.currentInputConnection && epoch == latinIME.editorGeneration() &&
                connection.expectedSelectionStart == start && connection.expectedSelectionEnd == end)
                connection.setSelection(end, end)
        } else {
            // No additional text capture/journal for a protected editor.
            clearDeletePreview()
            inputLogic.finishInput()
            onCodeInput(KeyCode.DELETE, Constants.NOT_A_COORDINATE, Constants.NOT_A_COORDINATE, false)
        }
    }

    override fun resetMetaState() {
        metaState = 0
    }

    private fun onLanguageSlide(steps: Int): Boolean {
        if (abs(steps) < settings.current.mLanguageSwipeDistance) return false
        val subtypes = SubtypeSettings.getEnabledSubtypes(true)
        if (subtypes.size <= 1) { // only allow if we have more than one subtype
            return false
        }
        // decide next or previous dependent on up or down
        val current = RichInputMethodManager.getInstance().currentSubtype.rawSubtype
        var wantedIndex = subtypes.indexOf(current) + if (steps > 0) 1 else -1
        wantedIndex %= subtypes.size
        if (wantedIndex < 0) {
            wantedIndex += subtypes.size
        }
        val newSubtype = subtypes[wantedIndex]

        // do not switch if we would switch to the initial subtype after cycling all other subtypes
        if (initialSubtype == null) initialSubtype = current
        if (initialSubtype == newSubtype) {
            if ((subtypeSwitchCount > 0 && steps > 0) || (subtypeSwitchCount < 0 && steps < 0)) {
                return true
            }
        }
        if (steps > 0) subtypeSwitchCount++ else subtypeSwitchCount--

        keyboardSwitcher.switchToSubtype(newSubtype)
        return true
    }

    private fun onMoveCursorVertically(steps: Int): Boolean {
        if (steps == 0) return false
        val code = if (steps < 0) {
            gestureMoveBackHaptics()
            KeyCode.ARROW_UP
        } else {
            gestureMoveForwardHaptics()
            KeyCode.ARROW_DOWN
        }
        onCodeInput(code, Constants.NOT_A_COORDINATE, Constants.NOT_A_COORDINATE, false)
        return true
    }

    private fun onMoveCursorHorizontally(rawSteps: Int): Boolean {
        if (rawSteps == 0) return false
        // for RTL languages we want to invert pointer movement
        val rtl = RichInputMethodManager.getInstance().currentSubtype.isRtlSubtype
        val steps = if (rtl) -rawSteps else rawSteps
        val moveSteps: Int
        if (steps < 0) {
            val text = connection.getTextBeforeCursor(-steps * 4, 0) ?: return false
            moveSteps = moveStepsToCharCount(text, steps)
            if (moveSteps == 0) {
                // some apps don't return any text via input connection, and the cursor can't be moved
                // we fall back to virtually pressing the left/right key one or more times instead
                repeat(-steps) {
                    onCodeInput(if (rtl) KeyCode.ARROW_RIGHT else KeyCode.ARROW_LEFT, Constants.NOT_A_COORDINATE,
                        Constants.NOT_A_COORDINATE, false)
                }
                if (text.isNotEmpty()) {
                    gestureMoveBackHaptics()
                }
                return true
            }
            gestureMoveBackHaptics()
        } else {
            val text = connection.getTextAfterCursor(steps * 4, 0) ?: return false
            moveSteps = moveStepsToCharCount(text, steps)
            if (moveSteps == 0) {
                // some apps don't return any text via input connection, and the cursor can't be moved
                // we fall back to virtually pressing the left/right key one or more times instead
                repeat(steps) {
                    onCodeInput(if (rtl) KeyCode.ARROW_LEFT else KeyCode.ARROW_RIGHT, Constants.NOT_A_COORDINATE,
                        Constants.NOT_A_COORDINATE, false)
                }
                if (text.isNotEmpty()) {
                    gestureMoveForwardHaptics(true)
                }
                return true
            }
            gestureMoveForwardHaptics(text.isNotEmpty())
        }

        // the shortcut below causes issues due to horrible handling of text fields by Firefox and forks
        // issues:
        //  * setSelection "will cause the editor to call onUpdateSelection", see: https://developer.android.com/reference/android/view/inputmethod/InputConnection#setSelection(int,%20int)
        //     but Firefox is simply not doing this within the same word... WTF?
        //     https://github.com/HeliBorg/HeliBoard/issues/1139#issuecomment-2588169384
        //  * inputType is NOT of variant InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT (variant appears to always be 0)
        //     -> this is "fixed" now using AppWorkarounds.adjustInputType
        val variation = InputType.TYPE_MASK_VARIATION and Settings.getValues().mInputAttributes.mInputType
        if (variation != InputType.TYPE_TEXT_VARIATION_WEB_EDIT_TEXT
                && inputLogic.moveCursorByAndReturnIfInsideComposingWord(moveSteps)) {
            // no need to finish input and restart suggestions if we're still in the word
            // this is a noticeable performance improvement when moving through long words
            val newPosition = connection.expectedSelectionStart + moveSteps
            connection.setSelection(newPosition, newPosition)
            return true
        }

        inputLogic.finishInput()
        val newPosition = connection.expectedSelectionStart + moveSteps
        connection.setSelection(newPosition, newPosition)
        inputLogic.restartSuggestionsOnWordTouchedByCursor(settings.current, keyboardSwitcher.currentKeyboardScript)
        return true
    }

    private fun gestureMoveBackHaptics() {
        if (connection.canDeleteCharacters()) {
            performHapticFeedback(HapticEvent.GESTURE_MOVE)
        }
    }

    // hasTextAfterCursor is used because text before the cursor is cached, going through the InputConnection can be slow
    private fun gestureMoveForwardHaptics(hasTextAfterCursor: Boolean? = null) {
        if (hasTextAfterCursor ?: connection.hasTextAfterCursor()) {
            performHapticFeedback(HapticEvent.GESTURE_MOVE)
        }
    }

    private fun performHapticFeedback(hapticEvent: HapticEvent) {
        audioAndHapticFeedbackManager.performHapticFeedback(keyboardSwitcher.visibleKeyboardView, hapticEvent)
    }

    private fun getHardwareKeyEventDecoder(deviceId: Int): HardwareEventDecoder {
        hardwareEventDecoders.get(deviceId)?.let { return it }

        // TODO: create the decoder according to the specification
        val newDecoder = HardwareKeyboardEventDecoder(deviceId)
        hardwareEventDecoders.put(deviceId, newDecoder)
        return newDecoder
    }

    // -------------------------- meta state handling -----------------------------

    // current state
    // press enables meta
    // release keeps meta enabled, unless there was a onCodeInput for a different key in between
    // onCodeInput ends the meta if it was enabled
    // long press on meta key also ends meta so popups are handled properly
    // sliding from a meta key to some other words too, though this was not intended (and there are no sliding key input graphics)

    // todo: move meta state tracking to KeyboardState? seems more suitable, also for handling sliding input
    //  but the issue is that meta state is used in Event to determine whether it's a functional Event (does not add a character)
    //  (and also it's in the hardware keyEvents which are handled by onKeyUp/Down, but that should be manageable)

    /** actual Android metaState like in KeyEvent */
    private var metaState = 0

    /** keeps track of the state of meta keys by (HeliBoard) KeyCodes */
    private val metaPressStates = SparseArray<MetaPressState>(4)

    // todo: lock and non-lock versions interact badly: when any of them is released, the meta state is removed
    //  this is not wanted, especially because the state of the other key is not affected (still looks pressed)
    private fun metaOnPressKey(primaryCode: Int) {
        val metaCode = primaryCode.toMetaState() ?: return
        if (primaryCode.isMetaLock()) {
            // if unset -> lock, otherwise set to UNSET_ON_RELEASE so it's unset on release
            if (metaPressStates[primaryCode] != MetaPressState.LOCKED) {
                metaPressStates[primaryCode] = MetaPressState.LOCKED
                keyboardSwitcher.mainKeyboardView?.updateLockState(primaryCode, true)
                metaState = metaState or metaCode
            } else {
                metaPressStates[primaryCode] = MetaPressState.UNSET_ON_RELEASE
            }
            return
        }
        if (metaPressStates[primaryCode] == MetaPressState.RELEASED_BUT_ACTIVE) {
            // meta key is pressed again without other input -> should be disabled on release
            metaPressStates[primaryCode] = MetaPressState.UNSET_ON_RELEASE
        } else {
            // otherwise just press it normally
            metaPressStates[primaryCode] = MetaPressState.PRESSED
        }
        metaState = metaState or metaCode
        // pressed graphics are set anyway, no need to lock it
    }

    // looks like this is not called if there are no popups
    private fun metaOnLongPressKey(primaryCode: Int) {
        if (metaPressStates[primaryCode] != MetaPressState.PRESSED) return
        // we long-pressed a meta key that has popups -> disable so the meta state is not used for the popup
        metaPressStates[primaryCode] = MetaPressState.UNSET
        keyboardSwitcher.mainKeyboardView?.updateLockState(primaryCode, false)
        val metaCode = primaryCode.toMetaState() ?: return
        metaState = metaState and metaCode.inv()
    }

    private fun metaOnReleaseKey(primaryCode: Int) {
        val metaCode = primaryCode.toMetaState() ?: return
        val metaPressState = metaPressStates[primaryCode]
        if (metaPressState == MetaPressState.UNSET_ON_RELEASE) {
            metaPressStates[primaryCode] = MetaPressState.UNSET
            metaState = metaState and metaCode.inv()
            keyboardSwitcher.mainKeyboardView?.updateLockState(primaryCode, false)
        } else if (metaPressState == MetaPressState.PRESSED) {
            metaPressStates[primaryCode] = MetaPressState.RELEASED_BUT_ACTIVE
            keyboardSwitcher.mainKeyboardView?.updateLockState(primaryCode, true)
        }
    }

    private fun metaAfterCodeInput(primaryCode: Int) {
        val metaCode = primaryCode.toMetaState()
        if (metaCode != null) {
            // meta key might be a popup key, we just toggle between set and unset
            val metaPressState = metaPressStates[primaryCode] ?: MetaPressState.UNSET
            if (metaPressState == MetaPressState.UNSET) {
                metaPressStates[primaryCode] = MetaPressState.SET
                metaState = metaState or metaCode
                keyboardSwitcher.mainKeyboardView?.updateLockState(primaryCode, true)
            } else if (metaPressState == MetaPressState.SET) {
                metaPressStates[primaryCode] = MetaPressState.UNSET
                metaState = metaState and metaCode.inv()
                keyboardSwitcher.mainKeyboardView?.updateLockState(primaryCode, false)
            }
        } else if (metaState != 0) {
            // non-meta key -> unset all set / released_but_active, and mark pressed as UNSET_ON_RELEASE
            metaPressStates.forEach { key, value ->
                if (value == MetaPressState.RELEASED_BUT_ACTIVE || value == MetaPressState.SET) {
                    metaPressStates[key] = MetaPressState.UNSET
                    keyboardSwitcher.mainKeyboardView?.updateLockState(key, false)
                    val metaCode = key.toMetaState() ?: return@forEach
                    metaState = metaState and metaCode.inv()
                } else if (value == MetaPressState.PRESSED) {
                    metaPressStates[key] = MetaPressState.UNSET_ON_RELEASE
                }
            }
        }
    }

    companion object {
        private enum class MetaPressState {
            UNSET, // default state, not active
            SET, // enabled without onPressKey (e.g. in popup)
            PRESSED, // key is pressed
            UNSET_ON_RELEASE, // key is pressed, but state will be unset on release
            RELEASED_BUT_ACTIVE, // key was released without UNSET_ON_RELEASE state, meta state is still set
            LOCKED, // key is locked and will be released only by pressing the same key again
        }

        private fun Int.toMetaState() = when (this) {
            KeyCode.CTRL, KeyCode.CTRL_LOCK -> KeyEvent.META_CTRL_ON
            KeyCode.CTRL_LEFT               -> KeyEvent.META_CTRL_LEFT_ON
            KeyCode.CTRL_RIGHT              -> KeyEvent.META_CTRL_RIGHT_ON
            KeyCode.ALT, KeyCode.ALT_LOCK   -> KeyEvent.META_ALT_ON
            KeyCode.ALT_LEFT                -> KeyEvent.META_ALT_LEFT_ON
            KeyCode.ALT_RIGHT               -> KeyEvent.META_ALT_RIGHT_ON
            KeyCode.FN, KeyCode.FN_LOCK     -> KeyEvent.META_FUNCTION_ON
            KeyCode.META, KeyCode.META_LOCK -> KeyEvent.META_META_ON
            KeyCode.META_LEFT               -> KeyEvent.META_META_LEFT_ON
            KeyCode.META_RIGHT              -> KeyEvent.META_META_RIGHT_ON
            else -> null
        }

        private fun Int.isMetaLock() = this == KeyCode.CTRL_LOCK || this == KeyCode.ALT_LOCK || this == KeyCode.FN_LOCK || this == KeyCode.META_LOCK
    }
}
