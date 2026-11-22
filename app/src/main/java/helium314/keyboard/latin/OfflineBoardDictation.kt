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
import helium314.keyboard.latin.voice.VoiceGesturePreferences

/**
 * The blue press-and-hold path belongs wholly to brrBoard. No SpeechRecognizer, IPC, WAV,
 * network request or other dictation app is involved. A held mic captures once; release
 * ends it, and a private background worker returns one English transcript to this editor.
 * Pause-bounded PCM windows are cleaned and decoded while capture continues. Outside
 * Incognito, verified batches update one composing span in the captured editor; Incognito
 * receives only the final transcript after Stop.
 */
internal class OfflineBoardDictation(private val ime: LatinIME) {
    private enum class Phase { IDLE, STARTING, CAPTURING, DECODING }
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
    private var autoSendOnComplete = false
    private var composingVoiceDraft = ""
    private var generation = 0L

    fun toggle(root: View?) {
        when (phase) {
            Phase.IDLE -> start(root)
            Phase.STARTING, Phase.CAPTURING -> stop()
            Phase.DECODING -> cancelFromUser() // A tap explicitly discards the pending result.
        }
    }

    fun start(root: View?, holdToTalk: Boolean = false) {
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
        autoSendOnComplete = holdToTalk && VoiceGesturePreferences.isHoldAutoSendEnabled(ime) &&
            !Settings.getValues().mIncognitoModeEnabled
        footer = currentFooter
        composingVoiceDraft = ""
        val pipeline = PrivateVoiceStreamingPipeline(ime, selectedEngine,
            VoiceNoiseChoice.selected(ime), onDone = { result ->
            if (generation == id && phase == Phase.DECODING) {
                onTranscriptionCompleted(result, id, connection)
            }
        }, onPartialText = { partial ->
            if (!Settings.getValues().mIncognitoModeEnabled) updateComposingDraft(connection, partial)
        })
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
            Phase.IDLE, Phase.DECODING -> return
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
        if (phase != Phase.DECODING) {
            cancel()
            return
        }
        VoiceSessionCrashTrail.note(ime, "MIC_CAPTURE_STOPPED")
        source = null
        streaming?.finishCapture()
    }

    private fun onTranscriptionCompleted(answer: Result<String>, id: Long, connection: InputConnection) {
            if (id != generation || phase != Phase.DECODING) return
            val validEditor = sameEditor(connection)
            val phrase = answer.getOrNull().orEmpty().trim()
            val validDestination = validEditor &&
                (destinationUnchanged() || composingDraftStillAtOriginalCursor())
            val outcome = VoiceTranscriptDeliveryPolicy.choose(
                detached = false,
                sameEditor = validEditor,
                sameSelection = validDestination,
                validTranscript = phrase.isNotEmpty() && !containsDevanagari(phrase),
            )
            VoiceSessionCrashTrail.note(ime, "RESULT_CHECK")
            // Completion means the private recognition pipeline returned;
            // it does not record the transcript, editor, or insertion contents.
            VoiceSessionCrashTrail.note(ime, if (answer.isSuccess) "COMPLETE" else "FAILED")
            val statusFooter = footer
            val hadLiveDraft = composingVoiceDraft.isNotEmpty()
            val shouldSend = autoSendOnComplete && validDestination &&
                ((originalEditorInfo?.imeOptions ?: 0) and EditorInfo.IME_MASK_ACTION) == EditorInfo.IME_ACTION_SEND
            finish(OfflineDictationExit.OTHER, clearDraft = outcome != VoiceTranscriptDeliveryPolicy.Outcome.INSERT)
            if (outcome == VoiceTranscriptDeliveryPolicy.Outcome.INSERT) {
                ime.onPrivateVoiceTextInput(phrase, hadLiveDraft)
                if (shouldSend) connection.performEditorAction(EditorInfo.IME_ACTION_SEND)
            } else if (validEditor) {
                statusFooter?.showDictationError(R.string.board_voice_offline_failed)
            }
    }

    /** Only an explicit Cancel/mic-discard gesture may play the discard feedback. */
    fun cancelFromUser() = finish(OfflineDictationExit.USER_CANCEL)

    /** Editor loss, IME dismissal, failures and successful commits never imply Cancel. */
    fun cancel() = finish(OfflineDictationExit.OTHER)

    /** A changed or detached editor discards all pending audio and transcript data. */
    fun onEditorLost() {
        cancel()
    }

    private fun finish(reason: OfflineDictationExit, clearDraft: Boolean = true) {
        val wasActive = phase != Phase.IDLE
        val wasProcessing = phase == Phase.DECODING
        if (wasActive && reason == OfflineDictationExit.USER_CANCEL)
            VoiceSessionCrashTrail.note(ime, "CANCELLED")
        val statusFooter = footer
        if (clearDraft && composingVoiceDraft.isNotEmpty() && originalConnection != null &&
            sameEditor(originalConnection!!) && composingDraftStillAtOriginalCursor()) {
            runCatching {
                originalConnection?.setComposingText("", 1)
                originalConnection?.finishComposingText()
            }
        }
        composingVoiceDraft = ""
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
        autoSendOnComplete = false
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

    private fun composingDraftStillAtOriginalCursor(): Boolean {
        if (composingVoiceDraft.isEmpty()) return false
        val rich = ime.mInputLogic.mConnection
        val expected = originalSelectionStart + composingVoiceDraft.length
        if (rich.expectedSelectionStart != expected || rich.expectedSelectionEnd != expected) return false
        // A same-length manual edit can leave the caret at the expected offset.
        // Verify the actual composing text before clearing/replacing it.
        return originalConnection?.getTextBeforeCursor(composingVoiceDraft.length, 0)?.toString() ==
            composingVoiceDraft
    }

    private fun updateComposingDraft(connection: InputConnection, text: String) {
        if (phase != Phase.CAPTURING || text == composingVoiceDraft || !sameEditor(connection)) return
        val destinationSafe = if (composingVoiceDraft.isEmpty()) destinationUnchanged()
            else composingDraftStillAtOriginalCursor()
        if (!destinationSafe) {
            cancel()
            return
        }
        val accepted = runCatching { connection.setComposingText(text, 1) }.getOrDefault(false)
        if (accepted) composingVoiceDraft = text
    }

    private fun isAllowed(info: EditorInfo): Boolean =
        info.inputType != 0 &&
            !InputTypeUtils.isPasswordInputType(info.inputType) &&
            !InputTypeUtils.isVisiblePasswordInputType(info.inputType)

    private fun containsDevanagari(text: String): Boolean = text.any {
        it in '\u0900'..'\u097F' || it in '\uA8E0'..'\uA8FF'
    }

}
