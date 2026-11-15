// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.View
import android.view.inputmethod.InputConnection
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import helium314.keyboard.latin.utils.InputTypeUtils
import java.util.Locale

// The default service may use a network provider, so allow longer final-result processing.
private const val SPEECH_SERVICE_START_TIMEOUT_MS = 8_000L
private const val TRANSCRIPTION_RESULT_TIMEOUT_MS = 30_000L

/** Dictation uses Android's selected RecognitionService in the current IME and commits final results only. */
internal class InlineDictation(private val ime: LatinIME) {
    private val handler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var footer: BoardFooterView? = null
    private var deadline: Runnable? = null
    private var recognitionStarted = false
    private var stopRequested = false

    fun toggle(root: View?, locale: Locale) {
        if (recognizer != null) {
            // First tap starts, the next tap stops, and a tap while the result is
            // processing cancels it. This keeps the footer as the only visible control.
            if (stopRequested || !recognitionStarted) cancel() else stop()
            return
        }
        start(root, locale)
    }

    fun start(root: View?, locale: Locale) {
        if (recognizer != null) return
        val inputView = root ?: return
        val editor = ime.currentInputEditorInfo ?: return
        if (!ime.isInputViewShown || !canDictate(editor)) return
        val connection = ime.currentInputConnection ?: return
        if (ime.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            report(R.string.inline_voice_permission)
            return
        }
        if (!isAvailable(ime)) {
            report(R.string.inline_voice_unavailable)
            return
        }

        val romanizedHinglish = locale.language == "hi" && locale.script == "Latn"
        val speechLocale = if (romanizedHinglish)
            Locale.forLanguageTag("hi-IN") else locale
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, speechLocale.toLanguageTag())
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)

        try {
            // Use Android's selected RecognitionService (Dictate on the owner's device). This
            // keeps Dictate's configured provider in control without opening its voice-input IME.
            val session = SpeechRecognizer.createSpeechRecognizer(ime)
            recognizer = session
            recognitionStarted = false
            stopRequested = false
            footer = inputView.findViewById(R.id.board_footer)
            footer?.showDictationActive(true)
            footer?.setDictationCancelAvailable(true)
            listen(session, intent, connection)
        } catch (_: RuntimeException) {
            fail(R.string.inline_voice_failure)
        }
    }

    private fun listen(
        session: SpeechRecognizer,
        intent: Intent,
        connection: InputConnection,
    ) {
        try {
            session.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    if (recognizer !== session) return
                    if (!ime.isInputViewShown || ime.currentInputConnection !== connection) {
                        cancel()
                        return
                    }
                    clearDeadline()
                }
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
                override fun onEndOfSpeech() {
                    if (recognizer !== session) return
                    stopRequested = true
                    footer?.showDictationProcessing()
                    scheduleDeadline(TRANSCRIPTION_RESULT_TIMEOUT_MS) { fail(R.string.inline_voice_failure) }
                }
                override fun onError(error: Int) {
                    if (recognizer !== session) return
                    fail(when (error) {
                        SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> R.string.inline_voice_no_model
                        SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> R.string.inline_voice_unsupported_language
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> R.string.inline_voice_permission
                        SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> R.string.inline_voice_no_speech
                        else -> R.string.inline_voice_failure
                    })
                }
                override fun onResults(results: Bundle?) {
                    if (recognizer !== session) return
                    val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()?.trim().orEmpty()
                    cancel()
                    if (text.isNotEmpty() && ime.isInputViewShown &&
                        ime.currentInputConnection === connection) ime.onTextInput(text)
                }
            })
            scheduleDeadline(SPEECH_SERVICE_START_TIMEOUT_MS) { fail(R.string.inline_voice_failure) }
            recognitionStarted = true
            session.startListening(intent)
        } catch (_: RuntimeException) {
            fail(R.string.inline_voice_failure)
        }
    }

    fun stop() {
        val session = recognizer ?: return
        if (stopRequested) return
        if (!recognitionStarted) {
            cancel()
            return
        }
        stopRequested = true
        footer?.showDictationProcessing()
        scheduleDeadline(TRANSCRIPTION_RESULT_TIMEOUT_MS) { fail(R.string.inline_voice_failure) }
        runCatching { session.stopListening() }.onFailure {
            fail(R.string.inline_voice_failure)
        }
    }

    fun cancel() {
        clearDeadline()
        val session = recognizer
        recognizer = null
        recognitionStarted = false
        stopRequested = false
        footer?.showDictationActive(false)
        footer?.showDictationProcessing(false)
        footer?.setDictationCancelAvailable(false)
        footer = null
        if (session != null) runCatching { session.destroy() }
    }

    private fun scheduleDeadline(delayMillis: Long, action: () -> Unit) {
        clearDeadline()
        deadline = Runnable(action).also { handler.postDelayed(it, delayMillis) }
    }

    private fun clearDeadline() {
        deadline?.let { handler.removeCallbacks(it) }
        deadline = null
    }

    private fun fail(message: Int) {
        cancel()
        report(message)
    }

    private fun report(message: Int) {
        Toast.makeText(ime, message, Toast.LENGTH_SHORT).show()
    }

    companion object {
        @JvmStatic
        fun isAvailable(context: Context): Boolean =
            runCatching { SpeechRecognizer.isRecognitionAvailable(context) }.getOrDefault(false)

        private fun canDictate(editor: EditorInfo): Boolean =
            editor.inputType != 0 &&
                !InputTypeUtils.isPasswordInputType(editor.inputType) &&
                !InputTypeUtils.isVisiblePasswordInputType(editor.inputType) &&
                editor.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING == 0
    }
}
