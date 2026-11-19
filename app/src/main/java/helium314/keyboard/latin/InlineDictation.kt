// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.Manifest
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.os.Bundle
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
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
import helium314.keyboard.latin.voice.VoiceModelInstaller
import java.util.Locale
import helium314.keyboard.latin.utils.Log

// The microphone starts in the foreground IME; completed audio is handed to a separate bound service.
private const val MICROPHONE_START_TIMEOUT_MS = 8_000L
private const val RECORDING_FINALIZATION_TIMEOUT_MS = 20_000L
private const val TRANSCRIPTION_RESULT_TIMEOUT_MS = 150_000L

/**
 * Two-tap record-first dictation. The keyboard captures private audio itself and transfers only
 * a completed read-only PCM file descriptor to brrSpeak's explicit authenticated Binder service.
 * Android's cross-UID RecognitionService path is intentionally no longer used.
 */
internal class InlineDictation(private val ime: LatinIME) {
    private val handler = Handler(Looper.getMainLooper())
    private val state = InlineVoiceSessionState()
    private var source: KeyboardRecordedAudioSource? = null
    private var client: BrrSpeakRecordedAudioClient? = null
    private var offlineBackup: HybridVoiceBackup? = null
    private var slowBackupPrompt: Runnable? = null
    private var footer: BoardFooterView? = null
    private var deadline: Runnable? = null
    private var generation = 0L

    fun toggle(root: View?, locale: Locale) {
        when (state.tap()) {
            InlineVoiceSessionState.Action.START -> start(root, locale)
            InlineVoiceSessionState.Action.STOP -> sendStop()
            InlineVoiceSessionState.Action.CANCEL -> cancel()
            InlineVoiceSessionState.Action.NONE -> {
                footer?.showDictationStatus(R.string.board_voice_starting)
            }
        }
    }

    fun start(root: View?, locale: Locale) {
        if (state.phase != InlineVoiceSessionState.Phase.IDLE) return
        val inputView = root ?: return
        footer = inputView.findViewById(R.id.board_footer)
        footer?.clearDictationError()
        footer?.clearContextAction()
        val editor = ime.currentInputEditorInfo ?: return
        if (!ime.isInputViewShown || !canDictate(editor) ||
            !Settings.getValues().mShowsVoiceInputKey) return
        val connection = ime.currentInputConnection ?: return
        // Do not reject an idle/cold brrSpeak process based on PackageManager's service list.
        // The authenticated no-data provider wake and explicit bind happen after capture.
        if (ime.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            report(R.string.inline_voice_permission)
            return
        }
        state.begin()
        generation++
        val currentGeneration = generation
        val backup = if (VoiceModelInstaller.isInstalled(ime)) HybridVoiceBackup(
            ime, footer, connection,
        ) { phrase ->
            if (generation == currentGeneration && isOriginalEditor(connection, editor)) {
                cancel() // End the server session before inserting locally; never duplicate words.
                ime.onPrivateVoiceTextInput(phrase)
            }
        } else null
        offlineBackup = backup
        footer?.showDictationStatus(R.string.board_voice_starting)
        footer?.setDictationCancelAvailable(true)
        val newSource = KeyboardRecordedAudioSource(
            ime,
            onRecordingStarted = {
                if (generation == currentGeneration &&
                    state.phase == InlineVoiceSessionState.Phase.STARTING) {
                    if (!isOriginalEditor(connection, editor)) {
                        cancel()
                    } else {
                        clearDeadline()
                        when (state.onAudioCaptureStarted()) {
                            InlineVoiceSessionState.Action.STOP -> sendStop()
                            else -> {
                                footer?.showDictationActive(true)
                                footer?.showDictationStatus(R.string.board_voice_cancel)
                            }
                        }
                    }
                }
            },
            onRecordingFinished = { file, bytes ->
                if (generation != currentGeneration ||
                    state.phase == InlineVoiceSessionState.Phase.IDLE ||
                    !isOriginalEditor(connection, editor)) {
                    file.delete()
                    cancel()
                } else {
                    state.onEndOfSpeech()
                    footer?.showDictationActive(false)
                    footer?.showDictationProcessing()
                    footer?.showDictationStatus(R.string.board_voice_transcribing)
                    backup?.finishCapture()
                    handoff(file, bytes, connection, editor, currentGeneration)
                }
            },
            onCaptureFailure = { reason ->
                if (generation == currentGeneration) {
                    Log.w("InlineDictation", "microphone failure stage=" + reason.name)
                    fail(R.string.inline_voice_provider_audio_failure)
                }
            },
            onPcmFrame = { frame, length ->
                // Capture a bounded RAM copy of exactly the PCM brrSpeak receives.
                // Failure of the optional backup never blocks server recording.
                runCatching { backup?.appendFrame(frame, length) }
            },
        )
        source = newSource
        scheduleDeadline(MICROPHONE_START_TIMEOUT_MS, currentGeneration) {
            fail(R.string.inline_voice_provider_audio_failure)
        }
        newSource.start()
    }

    private fun sendStop() {
        val capture = source ?: return
        val currentGeneration = generation
        footer?.showDictationActive(false)
        footer?.showDictationProcessing()
        footer?.showDictationStatus(R.string.board_voice_transcribing)
        scheduleDeadline(RECORDING_FINALIZATION_TIMEOUT_MS, currentGeneration) {
            fail(R.string.inline_voice_provider_audio_failure)
        }
        capture.stop()
    }

    private fun handoff(
        file: java.io.File,
        bytes: Long,
        connection: InputConnection,
        editor: EditorInfo,
        currentGeneration: Long,
    ) {
        if (!RecordedPcmContract.validLength(bytes)) {
            file.delete()
            fail(R.string.inline_voice_no_speech)
            return
        }
        val sessionId = (java.util.UUID.randomUUID().mostSignificantBits and Long.MAX_VALUE)
            .coerceAtLeast(1L)
        val request = BrrSpeakRecordedAudioClient(
            ime,
            sessionId,
            file,
            bytes,
            onResult = { text ->
                if (generation == currentGeneration) {
                    val originalEditorActive = isOriginalEditor(connection, editor)
                    val accepted = text.trim().takeUnless { it.isBlank() || containsHindiScript(it) }
                    val rejectedHindi = containsHindiScript(text)
                    val statusFooter = footer
                    // The local backup is never auto-inserted. The red/server result normally wins.
                    val canCommit = if (accepted != null) {
                        offlineBackup?.serverSucceeded() ?: true
                    } else false
                    val fallback = if (accepted == null) offlineBackup.also { offlineBackup = null }
                        else null
                    cancel()
                    if (originalEditorActive) {
                        when {
                            accepted != null && canCommit -> ime.onPrivateVoiceTextInput(accepted)
                            accepted != null -> Unit // User had already chosen Paste offline.
                            rejectedHindi -> report(R.string.inline_voice_hindi_result, statusFooter)
                            else -> report(R.string.inline_voice_no_speech, statusFooter)
                        }
                    }
                    fallback?.serverHasFailed()
                }
            },
            onError = { code ->
                if (generation == currentGeneration) {
                    Log.w("InlineDictation", "brrSpeak receiver failed code=" + code)
                    fail(
                        if (code == RecordedAudioWire.ERROR_BIND ||
                            code == RecordedAudioWire.ERROR_VERSION ||
                            code == RecordedAudioWire.ERROR_DISCONNECTED)
                            R.string.inline_voice_brrspeak_missing
                        else R.string.inline_voice_provider_audio_failure,
                    )
                }
            },
        )
        client = request
        scheduleDeadline(TRANSCRIPTION_RESULT_TIMEOUT_MS, currentGeneration) {
            fail(R.string.inline_voice_failure)
        }
        if (!request.start()) {
            fail(R.string.inline_voice_brrspeak_missing)
        } else {
            scheduleSlowBackup(currentGeneration)
        }
    }

    private fun isOriginalEditor(connection: InputConnection, editor: EditorInfo): Boolean =
        ime.isInputViewShown &&
            ime.currentInputConnection === connection &&
            ime.currentInputEditorInfo === editor

    fun cancel() {
        clearDeadline()
        clearSlowBackup()
        generation++
        state.reset()
        client?.close()
        client = null
        source?.close()
        source = null
        offlineBackup?.cancel()
        offlineBackup = null
        footer?.showDictationActive(false)
        footer?.showDictationProcessing(false)
        footer?.setDictationCancelAvailable(false)
        footer?.clearDictationError()
        footer = null
    }

    private fun scheduleDeadline(delayMillis: Long, currentGeneration: Long, action: () -> Unit) {
        clearDeadline()
        deadline = Runnable {
            if (state.phase != InlineVoiceSessionState.Phase.IDLE &&
                generation == currentGeneration) action()
        }.also { handler.postDelayed(it, delayMillis) }
    }

    private fun clearDeadline() {
        deadline?.let { handler.removeCallbacks(it) }
        deadline = null
    }

    private fun scheduleSlowBackup(currentGeneration: Long) {
        clearSlowBackup()
        slowBackupPrompt = Runnable {
            if (client != null && generation == currentGeneration &&
                state.phase == InlineVoiceSessionState.Phase.PROCESSING) {
                offlineBackup?.serverIsSlow()
            }
        }.also { handler.postDelayed(it, 3_000L) }
    }

    private fun clearSlowBackup() {
        slowBackupPrompt?.let { handler.removeCallbacks(it) }
        slowBackupPrompt = null
    }

    private fun fail(message: Int) {
        val statusFooter = footer
        val fallback = offlineBackup
        offlineBackup = null
        cancel()
        report(message, statusFooter)
        // Completed captured PCM can still be pasted manually after a provider failure.
        fallback?.finishCapture()
        fallback?.serverHasFailed()
    }

    private fun report(message: Int, statusFooter: BoardFooterView? = footer) {
        // IME-originated Toasts can be hidden or inconspicuous on some ColorOS versions.
        // Also show a short, scoped footer hint so a failed tap is never completely silent.
        val shortHint = when (message) {
            R.string.inline_voice_select_dictate_service -> R.string.board_voice_dictate_missing
            R.string.inline_voice_brrspeak_missing -> R.string.board_voice_brrspeak_missing
            R.string.inline_voice_permission -> R.string.board_voice_permission_missing
            R.string.inline_voice_dictate_permission -> R.string.board_voice_dictate_permission_missing
            R.string.inline_voice_provider_audio_failure -> R.string.board_voice_provider_audio_failure
            R.string.inline_voice_hindi_result -> R.string.board_voice_english_only
            else -> R.string.board_voice_failed
        }
        statusFooter?.showDictationError(shortHint)
        // A retry is meaningful for timeouts/transient recognizer/audio
        // failures. Permission, provider-selection, and language/configuration
        // errors need deliberate user action elsewhere, not a looping button.
        if (voiceFailureSupportsRetry(message)) {
            statusFooter?.showContextAction(this, R.string.board_footer_action_retry,
                R.string.board_footer_action_retry_label) {
                ime.toggleBoardDictation()
            }
        }
        val duration = if (message == R.string.inline_voice_hindi_result ||
            message == R.string.inline_voice_dictate_permission) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
        Toast.makeText(ime, message, duration).show()
    }

    companion object {
        @JvmStatic
        fun isAvailable(context: Context): Boolean =
            context.packageManager.hasSystemFeature(PackageManager.FEATURE_MICROPHONE)

        private fun containsHindiScript(text: String): Boolean =
            text.any { it in '\u0900'..'\u097F' || it in '\uA8E0'..'\uA8FF' }

        private fun canDictate(editor: EditorInfo): Boolean =
            editor.inputType != 0 &&
                !InputTypeUtils.isPasswordInputType(editor.inputType) &&
                !InputTypeUtils.isVisiblePasswordInputType(editor.inputType) &&
                editor.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING == 0
    }
}

/** Pin the fork's exact service, with no fallback to purchased Dictate or a system/OEM engine. */
internal fun resolveBrrSpeakService(available: Collection<ComponentName>): ComponentName? =
    ComponentName(BRRSPEAK_PACKAGE, BRRSPEAK_RECOGNITION_SERVICE_CLASS)
        .takeIf { it in available }

internal fun supportsBrrSpeakAudioSource(versionCode: Long?): Boolean =
    versionCode != null && versionCode >= 142_005L

/** API 33+ PCM contract shared with the v5 brrSpeak receiver, independent of system defaults. */
internal fun createBrrSpeakAudioIntent(
    locale: Locale,
    readEnd: ParcelFileDescriptor,
): Intent {
    val speechLocale = if (locale.language == "en") locale else Locale.forLanguageTag("en-IN")
    val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
        .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        .putExtra(RecognizerIntent.EXTRA_LANGUAGE, speechLocale.toLanguageTag())
        .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 3)
        .putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE, readEnd)
        .putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_CHANNEL_COUNT, KeyboardPcmAudioSource.CHANNEL_COUNT)
        .putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_ENCODING, AudioFormat.ENCODING_PCM_16BIT)
        .putExtra(RecognizerIntent.EXTRA_AUDIO_SOURCE_SAMPLING_RATE, KeyboardPcmAudioSource.SAMPLE_RATE_HZ)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
        intent.putExtra(RecognizerIntent.EXTRA_ENABLE_LANGUAGE_DETECTION, true)
    }
    return intent
}

internal fun brrSpeakFailureMessage(error: Int, localMicGranted: Boolean): Int = when {
    !localMicGranted -> R.string.inline_voice_permission
    error == SpeechRecognizer.ERROR_NO_MATCH || error == SpeechRecognizer.ERROR_SPEECH_TIMEOUT ->
        R.string.inline_voice_no_speech
    error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE -> R.string.inline_voice_no_model
    error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED -> R.string.inline_voice_unsupported_language
    error == SpeechRecognizer.ERROR_AUDIO || error == SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS ->
        R.string.inline_voice_provider_audio_failure
    else -> R.string.inline_voice_failure
}

internal const val BRRSPEAK_PACKAGE = "com.mangoloads.speak"
internal const val BRRSPEAK_RECOGNITION_SERVICE_CLASS =
    "dev.patrickgold.florisboard.dictate.recognition.DictateRecognitionService"

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

/** Only transient recognition failures may show a one-tap footer Retry. */
internal fun voiceFailureSupportsRetry(message: Int): Boolean = when (message) {
    R.string.inline_voice_failure,
    R.string.inline_voice_no_speech,
    R.string.inline_voice_provider_audio_failure -> true
    else -> false
}
