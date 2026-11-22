// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.Manifest
import android.content.pm.PackageManager
import android.view.View
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputConnection
import android.widget.Toast
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.InputTypeUtils
import helium314.keyboard.latin.voice.OfflineVoiceEngine
import helium314.keyboard.latin.voice.PrivateVoiceStreamingPipeline
import helium314.keyboard.latin.voice.VoiceModelChoice
import helium314.keyboard.latin.voice.VoiceTranscriptDeliveryPolicy
import helium314.keyboard.latin.voice.VoiceCaptureNoiseReduction
import helium314.keyboard.latin.voice.VoiceSessionCrashTrail
import helium314.keyboard.latin.voice.VoiceNoiseChoice

/**
 * The blue press-and-hold path belongs wholly to brrBoard. No SpeechRecognizer, IPC, WAV,
 * network request or other dictation app is involved. A held mic captures once; release
 * ends it, and a private background worker returns one English transcript to this editor.
 * Five-to-six second PCM windows are cleaned and decoded while capture continues;
 * partial text is never inserted into an unverified or changing input field.
 */
internal class OfflineBoardDictation(private val ime: LatinIME) {
    private enum class Phase { IDLE, STARTING, CAPTURING, DECODING, RECOVERING }
    // LatinIME constructs this member before Service.attachBaseContext(). Initializing the
    // singleton here dereferences a null applicationContext in Robolectric and cold startup.
    // Resolve it on first offline action, after the IME has an attached Android Context.
    private var phase = Phase.IDLE
    private var source: KeyboardPcmAudioSource? = null
    private var streaming: PrivateVoiceStreamingPipeline? = null
    private var footer: BoardFooterView? = null
    private var originalConnection: InputConnection? = null
    private var originalEditorInfo: EditorInfo? = null
    private var originalEditorGeneration = -1L
    private var originalSelectionStart = -1
    private var originalSelectionEnd = -1
    private var generation = 0L

    fun toggle(root: View?) {
        when (phase) {
            Phase.IDLE -> start(root)
            Phase.STARTING, Phase.CAPTURING -> stop()
            Phase.DECODING -> cancelFromUser() // A tap explicitly discards the pending result.
            Phase.RECOVERING -> {
                // A new mic tap must not silently do nothing or replace the
                // previous in-flight recognition before its recovery decision.
                root?.findViewById<BoardFooterView>(R.id.board_footer)
                    ?.showDictationError(R.string.board_voice_finishing_previous)
            }
        }
    }

    fun start(root: View?) {
        if (phase != Phase.IDLE) return
        val info = ime.currentInputEditorInfo ?: return
        val connection = ime.currentInputConnection ?: return
        val start = ime.mInputLogic.mConnection.expectedSelectionStart
        val end = ime.mInputLogic.mConnection.expectedSelectionEnd
        val currentFooter = root?.findViewById<BoardFooterView>(R.id.board_footer) ?: return
        if (!ime.isInputViewShown || !Settings.getValues().mShowsVoiceInputKey || !isAllowed(info)) return
        // A delayed result must never overwrite selected text or commit into
        // an editor that cannot report a stable caret position.
        if (start < 0 || start != end) {
            currentFooter.showDictationError(R.string.board_voice_destination_changed)
            return
        }
        if (ime.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            currentFooter.showDictationError(R.string.board_voice_permission_missing)
            return
        }
        // Never start a second audio capture or warm an ASR model while Android
        // has reported an ongoing voice/video call. OEM microphone isolation is
        // not a reliable contract on every device.
        if (VoiceCaptureNoiseReduction.callIsActive(ime)) {
            currentFooter.showDictationError(R.string.board_voice_offline_failed)
            return
        }
        val modelPack = VoiceModelChoice.active(ime)
        if (modelPack == null) {
            val message = R.string.board_voice_offline_model_missing
            currentFooter.showDictationError(message)
            Toast.makeText(ime, message, Toast.LENGTH_LONG).show()
            return
        }
        val selectedEngine = OfflineVoiceEngine.forKeyboard(ime, modelPack)
        generation++
        val id = generation
        originalConnection = connection
        originalEditorInfo = info
        originalEditorGeneration = ime.editorGeneration()
        originalSelectionStart = start
        originalSelectionEnd = end
        footer = currentFooter
        val pipeline = PrivateVoiceStreamingPipeline(ime, selectedEngine,
            VoiceNoiseChoice.selected(ime)) { result ->
            if (generation == id && (phase == Phase.DECODING || phase == Phase.RECOVERING)) {
                onTranscriptionCompleted(result, id, connection)
            }
        }
        streaming = pipeline
        phase = Phase.STARTING
        VoiceSessionCrashTrail.begin(ime) // No-op unless the owner armed one real-mic test.
        currentFooter.clearContextAction()
        currentFooter.clearDictationError()
        currentFooter.showDictationStatus(R.string.board_voice_starting)
        currentFooter.setDictationCancelAvailable(true, offline = true)
        val capture = KeyboardPcmAudioSource(
            context = ime,
            onRecordingStarted = {
                if (generation == id && phase == Phase.STARTING) {
                    if (!sameEditor(connection)) cancel() else {
                        VoiceSessionCrashTrail.note(ime, "MIC_READY")
                        phase = Phase.CAPTURING
                        footer?.showDictationActive(true, offline = true)
                        footer?.showDictationStatus(R.string.board_voice_offline_listening)
                    }
                }
            },
            onCaptureFailure = {
                if (generation == id) {
                    VoiceSessionCrashTrail.note(ime, "FAILED")
                    fail(R.string.board_voice_offline_failed)
                }
            },
            onPcmFrame = { bytes, count -> pipeline.append(bytes, count) },
            onCaptureFinished = { if (generation == id) finishCapture() },
            toBrrSpeak = false,
        )
        source = capture
        selectedEngine.warm() // Model loading overlaps with the held recording, never with UI touch.
        capture.start()
    }

    fun stop() {
        when (phase) {
            Phase.IDLE, Phase.DECODING, Phase.RECOVERING -> return
            Phase.STARTING, Phase.CAPTURING -> {
                phase = Phase.DECODING
                footer?.showDictationProcessing(offline = true)
                source?.stop()
            }
        }
    }

    private fun finishCapture() {
        if (phase == Phase.CAPTURING) {
            // The 170s recording cap is a normal end, not a discarded take.
            phase = Phase.DECODING
            footer?.showDictationProcessing(offline = true)
        }
        if (phase != Phase.DECODING && phase != Phase.RECOVERING) {
            cancel()
            return
        }
        VoiceSessionCrashTrail.note(ime, "MIC_CAPTURE_STOPPED")
        source = null
        streaming?.finishCapture()
    }

    private fun onTranscriptionCompleted(answer: Result<String>, id: Long, connection: InputConnection) {
            if (id != generation || (phase != Phase.DECODING && phase != Phase.RECOVERING)) return
            val validEditor = sameEditor(connection)
            val phrase = answer.getOrNull().orEmpty().trim()
            val validDestination = validEditor && destinationUnchanged()
            val outcome = VoiceTranscriptDeliveryPolicy.choose(
                detached = phase == Phase.RECOVERING,
                sameEditor = validEditor,
                sameSelection = validDestination,
                validTranscript = phrase.isNotEmpty() && !containsDevanagari(phrase),
            )
            VoiceSessionCrashTrail.note(ime, "RESULT_CHECK")
            // Completion means the private recognition pipeline returned;
            // it does not record the transcript, editor, or insertion contents.
            VoiceSessionCrashTrail.note(ime, if (answer.isSuccess) "COMPLETE" else "FAILED")
            val statusFooter = footer
            cancel()
            if (outcome == VoiceTranscriptDeliveryPolicy.Outcome.INSERT) {
                ime.onPrivateVoiceTextInput(phrase)
            } else if (outcome == VoiceTranscriptDeliveryPolicy.Outcome.RECOVER &&
                ime.clipboardHistoryManager.saveUndeliveredTranscript(phrase)) {
                val visibleFooter = ime.mKeyboardSwitcher.wrapperView
                    ?.findViewById<BoardFooterView>(R.id.board_footer)?.takeIf { it.isShown }
                if (visibleFooter != null) {
                    visibleFooter.showContextAction(recoveryNoticeOwner,
                        R.string.board_voice_saved_for_recovery, R.string.board_voice_open_clipboard) {
                        ime.mKeyboardSwitcher.setClipboardKeyboard()
                    }
                } else {
                    Toast.makeText(ime, R.string.board_voice_saved_for_recovery, Toast.LENGTH_SHORT).show()
                }
            } else if (outcome == VoiceTranscriptDeliveryPolicy.Outcome.RECOVER) {
                if (validEditor) statusFooter?.showDictationError(R.string.board_voice_recovery_unavailable)
                else Toast.makeText(ime, R.string.board_voice_recovery_unavailable, Toast.LENGTH_SHORT).show()
            } else if (validEditor) {
                statusFooter?.showDictationError(R.string.board_voice_offline_failed)
            }
    }

    /** Only an explicit Cancel/mic-discard gesture may play the discard feedback. */
    fun cancelFromUser() = finish(OfflineDictationExit.USER_CANCEL)

    /** Editor loss, IME dismissal, failures and successful commits never imply Cancel. */
    fun cancel() = finish(OfflineDictationExit.OTHER)

    /**
     * The destination disappeared *after* recording was stopped. The recognizer
     * may finish, but is permanently forbidden from committing into an editor.
     * Only explicit Cancel, a privacy boundary or service destruction destroys
     * the pending result. This state is bounded to one in-flight recognition.
     */
    fun onEditorLost() {
        if (phase != Phase.DECODING && phase != Phase.RECOVERING) {
            cancel()
            return
        }
        phase = Phase.RECOVERING
        val oldFooter = footer
        footer = null
        oldFooter?.showDictationActive(false)
        oldFooter?.showDictationProcessing(false)
        oldFooter?.setDictationCancelAvailable(false)
        oldFooter?.clearDictationError()
    }

    private fun finish(reason: OfflineDictationExit) {
        val wasActive = phase != Phase.IDLE
        if (wasActive) VoiceSessionCrashTrail.note(ime, "CANCELLED")
        val wasProcessing = phase == Phase.DECODING
        val statusFooter = footer
        generation++
        phase = Phase.IDLE
        streaming?.cancel()
        streaming = null
        source?.close()
        source = null
        originalConnection = null
        originalEditorInfo = null
        originalEditorGeneration = -1L
        originalSelectionStart = -1
        originalSelectionEnd = -1
        statusFooter?.showDictationActive(false)
        statusFooter?.showDictationProcessing(false)
        statusFooter?.setDictationCancelAvailable(false)
        // The old Cancel/Transcribing label was never dismissed on success or
        // cancel. Clear it synchronously before the next editor can be shown.
        statusFooter?.clearDictationError()
        footer = null
        if (OfflineDictationFeedbackPolicy.showDiscard(reason, wasActive)) {
            statusFooter?.showDictationDiscarded(wasProcessing)
        }
    }

    private fun fail(message: Int) {
        val f = footer
        VoiceSessionCrashTrail.note(ime, "FAILED")
        cancel()
        f?.showDictationError(message)
    }

    private fun sameEditor(connection: InputConnection): Boolean =
        ime.isInputViewShown && ime.currentInputConnection === connection &&
            originalConnection === connection &&
            ime.currentInputEditorInfo === originalEditorInfo &&
            ime.editorGeneration() == originalEditorGeneration

    private fun destinationUnchanged(): Boolean {
        val rich = ime.mInputLogic.mConnection
        return rich.expectedSelectionStart == originalSelectionStart &&
            rich.expectedSelectionEnd == originalSelectionEnd
    }

    private fun isAllowed(info: EditorInfo): Boolean =
        info.inputType != 0 &&
            !InputTypeUtils.isPasswordInputType(info.inputType) &&
            !InputTypeUtils.isVisiblePasswordInputType(info.inputType)

    private fun containsDevanagari(text: String): Boolean = text.any {
        it in '\u0900'..'\u097F' || it in '\uA8E0'..'\uA8FF'
    }

    private val recoveryNoticeOwner = Any()
}
