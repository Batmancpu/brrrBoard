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
import helium314.keyboard.latin.voice.PrivatePcmBuffer
import helium314.keyboard.latin.voice.VoiceModelInstaller

/**
 * The blue press-and-hold path belongs wholly to brrBoard. No SpeechRecognizer, IPC, WAV,
 * network request or other dictation app is involved. A held mic captures once; release
 * ends it, and a private background worker returns one English transcript to this editor.
 */
internal class OfflineBoardDictation(private val ime: LatinIME) {
    private enum class Phase { IDLE, STARTING, CAPTURING, DECODING }
    // LatinIME constructs this member before Service.attachBaseContext(). Initializing the
    // singleton here dereferences a null applicationContext in Robolectric and cold startup.
    // Resolve it on first offline action, after the IME has an attached Android Context.
    private val engine by lazy(LazyThreadSafetyMode.NONE) { OfflineVoiceEngine.shared(ime) }
    private var phase = Phase.IDLE
    private var source: KeyboardPcmAudioSource? = null
    private var pcmBuffer: PrivatePcmBuffer? = null
    private var footer: BoardFooterView? = null
    private var originalConnection: InputConnection? = null
    private var generation = 0L

    fun toggle(root: View?) {
        if (phase == Phase.IDLE) start(root) else stop()
    }

    fun start(root: View?) {
        if (phase != Phase.IDLE) return
        val info = ime.currentInputEditorInfo ?: return
        val connection = ime.currentInputConnection ?: return
        val currentFooter = root?.findViewById<BoardFooterView>(R.id.board_footer) ?: return
        if (!ime.isInputViewShown || !Settings.getValues().mShowsVoiceInputKey || !isAllowed(info)) return
        if (ime.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            currentFooter.showDictationError(R.string.board_voice_permission_missing)
            return
        }
        if (!VoiceModelInstaller.isInstalled(ime)) {
            currentFooter.showDictationError(R.string.board_voice_offline_model_missing)
            Toast.makeText(ime, R.string.board_voice_offline_model_missing, Toast.LENGTH_LONG).show()
            return
        }
        generation++
        val id = generation
        originalConnection = connection
        footer = currentFooter
        val buffer = PrivatePcmBuffer()
        pcmBuffer = buffer
        phase = Phase.STARTING
        currentFooter.clearContextAction()
        currentFooter.clearDictationError()
        currentFooter.showDictationStatus(R.string.board_voice_starting)
        currentFooter.setDictationCancelAvailable(true)
        val capture = KeyboardPcmAudioSource(
            onRecordingStarted = {
                if (generation == id && phase == Phase.STARTING) {
                    if (!sameEditor(connection)) cancel() else {
                        phase = Phase.CAPTURING
                        footer?.showDictationActive(true, offline = true)
                        footer?.showDictationStatus(R.string.board_voice_offline_listening)
                    }
                }
            },
            onCaptureFailure = {
                if (generation == id) fail(R.string.board_voice_offline_failed)
            },
            onPcmFrame = { bytes, count -> buffer.append(bytes, 0, count) },
            onCaptureFinished = { if (generation == id) finishCapture(connection) },
            toBrrSpeak = false,
        )
        source = capture
        engine.warm() // Model loading overlaps with the held recording, never with UI touch.
        capture.start()
    }

    fun stop() {
        when (phase) {
            Phase.IDLE, Phase.DECODING -> return
            Phase.STARTING, Phase.CAPTURING -> {
                phase = Phase.DECODING
                footer?.showDictationProcessing(offline = true)
                footer?.showDictationStatus(R.string.board_voice_offline_processing)
                source?.stop()
            }
        }
    }

    private fun finishCapture(connection: InputConnection) {
        if (phase != Phase.DECODING) {
            cancel()
            return
        }
        val id = generation
        val bytes = pcmBuffer?.drain()
        pcmBuffer = null
        source = null
        if (bytes == null || bytes.isEmpty()) {
            bytes?.fill(0)
            fail(R.string.board_voice_offline_failed)
            return
        }
        engine.transcribe(bytes) { answer ->
            if (id != generation || phase != Phase.DECODING) return@transcribe
            val validEditor = sameEditor(connection)
            val phrase = answer.getOrNull().orEmpty().trim()
            val canCommit = validEditor && phrase.isNotEmpty() && !containsDevanagari(phrase)
            val statusFooter = footer
            cancel()
            if (canCommit) {
                ime.onPrivateVoiceTextInput(phrase)
            } else if (validEditor) {
                statusFooter?.showDictationError(R.string.board_voice_offline_failed)
            }
        }
    }

    fun cancel() {
        generation++
        phase = Phase.IDLE
        source?.close()
        source = null
        pcmBuffer?.discard()
        pcmBuffer = null
        originalConnection = null
        footer?.showDictationActive(false)
        footer?.showDictationProcessing(false)
        footer?.setDictationCancelAvailable(false)
        footer = null
    }

    private fun fail(message: Int) {
        val f = footer
        cancel()
        f?.showDictationError(message)
    }

    private fun sameEditor(connection: InputConnection): Boolean =
        ime.isInputViewShown && ime.currentInputConnection === connection &&
            originalConnection === connection

    private fun isAllowed(info: EditorInfo): Boolean =
        info.inputType != 0 &&
            !InputTypeUtils.isPasswordInputType(info.inputType) &&
            !InputTypeUtils.isVisiblePasswordInputType(info.inputType) &&
            info.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING == 0

    private fun containsDevanagari(text: String): Boolean = text.any {
        it in '\u0900'..'\u097F' || it in '\uA8E0'..'\uA8FF'
    }
}
