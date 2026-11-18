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
import helium314.keyboard.latin.utils.Log

// The default service may use a network provider, so allow longer final-result processing.
private const val SPEECH_SERVICE_START_TIMEOUT_MS = 8_000L
private const val TRANSCRIPTION_RESULT_TIMEOUT_MS = 30_000L

/** Dictation uses Android's selected RecognitionService in the current IME and commits final results only. */
internal class InlineDictation(private val ime: LatinIME) {
    private val handler = Handler(Looper.getMainLooper())
    private val state = InlineVoiceSessionState()
    private var recognizer: SpeechRecognizer? = null
    private var footer: BoardFooterView? = null
    private var deadline: Runnable? = null
    private var generation = 0L
    private var detectedSpeechLanguage: String? = null

    fun toggle(root: View?, locale: Locale) {
        when (state.tap()) {
            InlineVoiceSessionState.Action.START -> start(root, locale)
            InlineVoiceSessionState.Action.STOP -> sendStop()
            InlineVoiceSessionState.Action.CANCEL -> cancel()
            InlineVoiceSessionState.Action.NONE -> {
                // A second tap during startup must not call stopListening yet.
                // Dictate may announce readiness before opening its microphone.
                footer?.showDictationStatus(R.string.board_voice_waiting_for_audio)
            }
        }
    }

    fun start(root: View?, locale: Locale) {
        if (recognizer != null) return
        val inputView = root ?: return
        footer = inputView.findViewById(R.id.board_footer)
        footer?.clearDictationError()
        val editor = ime.currentInputEditorInfo ?: return
        if (!ime.isInputViewShown || !canDictate(editor) || !Settings.getValues().mShowsVoiceInputKey) return
        val connection = ime.currentInputConnection ?: return
        val serviceSelection = selectedDictateRecognitionService(ime)
        val dictateService = serviceSelection.component
        if (dictateService == null) {
            // Tap never opens system/OEM settings. Configuration is a distinct,
            // deliberate action in brrBoard Settings > About.
            Log.w("InlineDictation", "Dictate service unavailable: ${serviceSelection.failure}")
            report(R.string.inline_voice_select_dictate_service)
            return
        }
        if (ime.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            report(R.string.inline_voice_permission)
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
            state.begin()
            generation++
            recognizer = session
            detectedSpeechLanguage = null
            footer?.showDictationStatus(R.string.board_voice_starting)
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
        val sessionGeneration = generation
        fun isCurrent(): Boolean = recognizer === session && generation == sessionGeneration
        try {
            session.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    if (!isCurrent()) return
                    if (!ime.isInputViewShown || ime.currentInputConnection !== connection) {
                        cancel()
                        return
                    }
                    // Dictate currently emits readyForSpeech before AudioRecord is initialized.
                    // Keep the startup watchdog until there is evidence of speech so a stalled
                    // recorder cannot leave a fake recording indicator indefinitely.
                    // Keep the truthful Starting label: this callback is sent before
                    // Dictate's AudioRecord starts, not evidence of microphone capture.
                }
                override fun onBeginningOfSpeech() {
                    if (!isCurrent()) return
                    if (!ime.isInputViewShown || ime.currentInputConnection !== connection) {
                        cancel()
                        return
                    }
                    val wasStarting = state.phase == InlineVoiceSessionState.Phase.STARTING
                    val action = state.onBeginningOfSpeech()
                    if (wasStarting) clearDeadline()
                    when (action) {
                        InlineVoiceSessionState.Action.STOP -> sendStop()
                        else -> {
                            if (state.phase == InlineVoiceSessionState.Phase.LISTENING) {
                                footer?.showDictationActive(true)
                                footer?.showDictationStatus(R.string.board_voice_listening)
                            }
                        }
                    }
                }
                override fun onLanguageDetection(results: Bundle) {
                    if (!isCurrent()) return
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
                    if (!isCurrent()) return
                    if (!ime.isInputViewShown || ime.currentInputConnection !== connection) {
                        cancel()
                        return
                    }
                    state.onEndOfSpeech()
                    footer?.showDictationProcessing()
                    footer?.showDictationStatus(R.string.board_voice_transcribing)
                    scheduleDeadline(TRANSCRIPTION_RESULT_TIMEOUT_MS, sessionGeneration) {
                        fail(R.string.inline_voice_failure)
                    }
                }
                override fun onError(error: Int) {
                    if (!isCurrent()) return
                    // An ERROR_INSUFFICIENT_PERMISSIONS callback comes from Android's
                    // recognizer/provider, after brrBoard passed its own permission gate.
                    // It is not proof that the user denied brrBoard's runtime permission.
                    val locallyGranted = ime.checkSelfPermission(Manifest.permission.RECORD_AUDIO) ==
                        PackageManager.PERMISSION_GRANTED
                    // ColorOS can show brrBoard granted while Dictate has no microphone grant.
                    // A grant still cannot rule out Android's AppOps/attribution denial.
                    val dictateGranted = if (error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS) {
                        runCatching {
                            ime.packageManager.checkPermission(
                                Manifest.permission.RECORD_AUDIO, DICTATE_SERVICE_PACKAGE
                            ) == PackageManager.PERMISSION_GRANTED
                        }.getOrNull()
                    } else null
                    Log.w("InlineDictation", "Recognition callback failed: code=$error " +
                        "localMicPermission=$locallyGranted dictateMicPermission=$dictateGranted")
                    fail(recognitionFailureMessage(error, locallyGranted, dictateGranted))
                }
                override fun onResults(results: Bundle?) {
                    if (!isCurrent()) return
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
                    val statusFooter = footer
                    cancel()
                    if (ime.isInputViewShown && ime.currentInputConnection === connection) {
                        if (text.isNotEmpty()) {
                            ime.onTextInput(text)
                        } else if (rejectedNonEnglish) {
                            report(R.string.inline_voice_hindi_result, statusFooter)
                        }
                    }
                }
            })
            scheduleDeadline(SPEECH_SERVICE_START_TIMEOUT_MS, sessionGeneration) {
                fail(R.string.inline_voice_no_speech)
            }
            session.startListening(intent)
        } catch (_: RuntimeException) {
            fail(R.string.inline_voice_failure)
        }
    }

    private fun sendStop() {
        val session = recognizer ?: return
        val sessionGeneration = generation
        footer?.showDictationProcessing()
        footer?.showDictationStatus(R.string.board_voice_transcribing)
        scheduleDeadline(TRANSCRIPTION_RESULT_TIMEOUT_MS, sessionGeneration) {
            fail(R.string.inline_voice_failure)
        }
        runCatching { session.stopListening() }.onFailure {
            fail(R.string.inline_voice_failure)
        }
    }

    fun cancel() {
        clearDeadline()
        val session = recognizer
        recognizer = null
        generation++
        state.reset()
        detectedSpeechLanguage = null
        footer?.showDictationActive(false)
        footer?.showDictationProcessing(false)
        footer?.setDictationCancelAvailable(false)
        footer?.clearDictationError()
        footer = null
        if (session != null) {
            runCatching { session.cancel() }
            runCatching { session.destroy() }
        }
    }

    private fun scheduleDeadline(delayMillis: Long, sessionGeneration: Long, action: () -> Unit) {
        clearDeadline()
        deadline = Runnable {
            if (recognizer != null && generation == sessionGeneration) action()
        }.also { handler.postDelayed(it, delayMillis) }
    }

    private fun clearDeadline() {
        deadline?.let { handler.removeCallbacks(it) }
        deadline = null
    }

    private fun fail(message: Int) {
        val statusFooter = footer
        cancel()
        report(message, statusFooter)
    }

    private fun report(message: Int, statusFooter: BoardFooterView? = footer) {
        // IME-originated Toasts can be hidden or inconspicuous on some ColorOS versions.
        // Also show a short, scoped footer hint so a failed tap is never completely silent.
        val shortHint = when (message) {
            R.string.inline_voice_select_dictate_service -> R.string.board_voice_dictate_missing
            R.string.inline_voice_permission -> R.string.board_voice_permission_missing
            R.string.inline_voice_dictate_permission -> R.string.board_voice_dictate_permission_missing
            R.string.inline_voice_provider_audio_failure -> R.string.board_voice_provider_audio_failure
            R.string.inline_voice_hindi_result -> R.string.board_voice_english_only
            else -> R.string.board_voice_failed
        }
        statusFooter?.showDictationError(shortHint)
        val duration = if (message == R.string.inline_voice_hindi_result ||
            message == R.string.inline_voice_dictate_permission) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
        Toast.makeText(ime, message, duration).show()
    }

    companion object {
        @JvmStatic
        fun isAvailable(context: Context): Boolean =
            runCatching { SpeechRecognizer.isRecognitionAvailable(context) }.getOrDefault(false)

        private fun selectedDictateRecognitionService(context: Context): DictateServiceSelection {
            val selected = AndroidSettings.Secure.getString(
                context.contentResolver, VOICE_RECOGNITION_SERVICE_SETTING
            )
            val query = Intent(RecognitionService.SERVICE_INTERFACE).setPackage(DICTATE_PACKAGE_NAME)
            val services = runCatching {
                context.packageManager.queryIntentServices(query, 0).mapNotNull { info ->
                    info.serviceInfo?.let { ComponentName(it.packageName, it.name) }
                }
            }.getOrDefault(emptyList())
            return resolveDictateServiceSelection(selected, services)
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

        private const val VOICE_RECOGNITION_SERVICE_SETTING = "voice_recognition_service"
        private const val DICTATE_PACKAGE_NAME = "net.devemperor.dictate"
    }
}

/** Pure Dictate-only service-resolution policy. No other recognizer is launched. */
internal data class DictateServiceSelection(
    val component: ComponentName?,
    val failure: Failure? = null,
) {
    enum class Failure { NO_SELECTION, MALFORMED, WRONG_PROVIDER, SERVICE_MISSING }
}

internal fun resolveDictateServiceSelection(
    selected: String?, available: Collection<ComponentName>,
): DictateServiceSelection {
    val selectedComponent = selected?.let { ComponentName.unflattenFromString(it) }
    val verifiedDictateServices = available.filter { it.packageName == DICTATE_SERVICE_PACKAGE }
    // ColorOS may direct the general system recognizer to another service even when Dictate
    // handles voice-search activities. brrBoard's explicit mic contract is Dictate-only, so
    // choose its discoverable RecognitionService without depending on the OEM secure setting.
    if (selectedComponent != null && selectedComponent in verifiedDictateServices)
        return DictateServiceSelection(selectedComponent)
    val canonical = ComponentName(DICTATE_SERVICE_PACKAGE, DICTATE_RECOGNITION_SERVICE_CLASS)
    if (canonical in verifiedDictateServices) return DictateServiceSelection(canonical)
    if (verifiedDictateServices.size == 1) return DictateServiceSelection(verifiedDictateServices.single())

    val failure = when {
        selected.isNullOrBlank() -> DictateServiceSelection.Failure.NO_SELECTION
        selectedComponent == null -> DictateServiceSelection.Failure.MALFORMED
        selectedComponent.packageName != DICTATE_SERVICE_PACKAGE -> DictateServiceSelection.Failure.WRONG_PROVIDER
        else -> DictateServiceSelection.Failure.SERVICE_MISSING
    }
    return DictateServiceSelection(null, failure)
}

private const val DICTATE_SERVICE_PACKAGE = "net.devemperor.dictate"
// Confirmed by the Dictate maintainer in DevEmperor/DictateKeyboard issue #359.
private const val DICTATE_RECOGNITION_SERVICE_CLASS =
    "dev.patrickgold.florisboard.dictate.recognition.DictateRecognitionService"

/** Distinguish brrBoard's local permission gate from a provider's asynchronous error. */
internal fun recognitionFailureMessage(
    error: Int,
    localMicPermissionGranted: Boolean,
    dictateMicPermissionGranted: Boolean? = null,
): Int = when (error) {
    SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> R.string.inline_voice_no_model
    SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> R.string.inline_voice_unsupported_language
    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
        when {
            !localMicPermissionGranted -> R.string.inline_voice_permission
            dictateMicPermissionGranted == false -> R.string.inline_voice_dictate_permission
            else -> R.string.inline_voice_provider_audio_failure
        }
    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> R.string.inline_voice_no_speech
    else -> R.string.inline_voice_failure
}
