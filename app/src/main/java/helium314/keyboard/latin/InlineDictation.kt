// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognitionService
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.provider.Settings as AndroidSettings
import android.view.View
import android.view.inputmethod.InputConnection
import android.view.inputmethod.EditorInfo
import android.widget.Toast
import helium314.keyboard.latin.utils.InputTypeUtils
import helium314.keyboard.latin.settings.Settings
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
    private var detectedSpeechLanguage: String? = null

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
        if (!ime.isInputViewShown || !canDictate(editor) || !Settings.getValues().mShowsVoiceInputKey) return
        val connection = ime.currentInputConnection ?: return
        val dictateService = selectedDictateRecognitionService(ime)
        if (dictateService == null) {
            report(R.string.inline_voice_select_dictate_service)
            if (!openVoiceInputSettings(ime)) {
                report(R.string.inline_voice_open_settings_failed)
            }
            return
        }
        if (ime.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            report(R.string.inline_voice_permission)
            return
        }
        if (!isAvailable(ime)) {
            report(R.string.inline_voice_unavailable)
            return
        }

        // Request English for the selected Android RecognitionService. Dictate currently ignores
        // caller language extras and starts its saved Active language, so its own language setting
        // still has to be English. Other services may provide Android language-detection metadata,
        // which is checked before final text is inserted.
        val speechLocale = if (locale.language == "en") locale else Locale.forLanguageTag("en-IN")
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, speechLocale.toLanguageTag())
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            intent.putExtra(RecognizerIntent.EXTRA_ENABLE_LANGUAGE_DETECTION, true)
        }

        try {
            // Use Android's selected RecognitionService (Dictate on the owner's device). This
            // keeps Dictate's configured provider in control without opening its voice-input IME.
            // Bind to Dictate explicitly so Android/OEM resolution cannot fall back to another recognizer.
            val session = SpeechRecognizer.createSpeechRecognizer(ime, dictateService)
            recognizer = session
            detectedSpeechLanguage = null
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
                override fun onLanguageDetection(results: Bundle) {
                    if (recognizer !== session) return
                    val language = results.getString(SpeechRecognizer.DETECTED_LANGUAGE) ?: return
                    val confidence = results.getInt(
                        SpeechRecognizer.LANGUAGE_DETECTION_CONFIDENCE_LEVEL,
                        SpeechRecognizer.LANGUAGE_DETECTION_CONFIDENCE_LEVEL_UNKNOWN,
                    )
                    if (confidence == SpeechRecognizer.LANGUAGE_DETECTION_CONFIDENCE_LEVEL_CONFIDENT ||
                        confidence == SpeechRecognizer.LANGUAGE_DETECTION_CONFIDENCE_LEVEL_HIGHLY_CONFIDENT) {
                        detectedSpeechLanguage = language
                    }
                }
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
                    val candidates = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        .orEmpty()
                    val resultLanguage = (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                        results?.getString(SpeechRecognizer.DETECTED_LANGUAGE)
                    } else null) ?: detectedSpeechLanguage
                    val detectedNonEnglish = resultLanguage != null && !isEnglishLanguageTag(resultLanguage)
                    // Dictate currently returns one candidate and no language tag. Android services
                    // that provide a confident language tag are filtered here; Devanagari Hindi is
                    // also rejected even when a service omits that metadata.
                    val text = if (detectedNonEnglish) "" else candidates
                        .firstOrNull { it.isNotBlank() && !containsHindiScript(it) }
                        ?.trim().orEmpty()
                    val rejectedNonEnglish = candidates.isNotEmpty() && text.isEmpty() &&
                        (detectedNonEnglish || candidates.any(::containsHindiScript))
                    cancel()
                    if (ime.isInputViewShown && ime.currentInputConnection === connection) {
                        if (text.isNotEmpty()) {
                            ime.onTextInput(text)
                        } else if (rejectedNonEnglish) {
                            report(R.string.inline_voice_hindi_result)
                        }
                    }
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
        detectedSpeechLanguage = null
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
        val duration = if (message == R.string.inline_voice_hindi_result) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
        Toast.makeText(ime, message, duration).show()
    }

    companion object {
        @JvmStatic
        fun isAvailable(context: Context): Boolean =
            runCatching { SpeechRecognizer.isRecognitionAvailable(context) }.getOrDefault(false)

        private fun selectedDictateRecognitionService(context: Context): ComponentName? {
            val service = AndroidSettings.Secure.getString(
                context.contentResolver, VOICE_RECOGNITION_SERVICE_SETTING
            ) ?: return null
            val component = ComponentName.unflattenFromString(service) ?: return null
            if (component.packageName != DICTATE_PACKAGE_NAME) return null
            val query = Intent(RecognitionService.SERVICE_INTERFACE).setPackage(component.packageName)
            val isAvailable = runCatching {
                context.packageManager.queryIntentServices(query, 0).any { resolveInfo ->
                    val info = resolveInfo.serviceInfo ?: return@any false
                    info.packageName == component.packageName && info.name == component.className
                }
            }.getOrDefault(false)
            return component.takeIf { isAvailable }
        }

        // This rejects Hindi-script output because the owner requested English only. Dictate's
        // saved Active language remains authoritative; this does not identify Romanized Hindi.
        private fun containsHindiScript(text: String): Boolean =
            text.any { it in '\u0900'..'\u097F' || it in '\uA8E0'..'\uA8FF' }

        private fun isEnglishLanguageTag(languageTag: String): Boolean =
            Locale.forLanguageTag(languageTag).language.equals(Locale.ENGLISH.language, ignoreCase = true)

        private fun canDictate(editor: EditorInfo): Boolean =
            editor.inputType != 0 &&
                !InputTypeUtils.isPasswordInputType(editor.inputType) &&
                !InputTypeUtils.isVisiblePasswordInputType(editor.inputType) &&
                editor.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING == 0

        private fun openVoiceInputSettings(context: Context): Boolean = runCatching {
            context.startActivity(
                Intent(AndroidSettings.ACTION_VOICE_INPUT_SETTINGS)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            )
        }.isSuccess

        private const val VOICE_RECOGNITION_SERVICE_SETTING = "voice_recognition_service"
        private const val DICTATE_PACKAGE_NAME = "net.devemperor.dictate"
    }
}
