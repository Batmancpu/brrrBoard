// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognitionSupport
import android.speech.RecognitionSupportCallback
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.View
import android.view.inputmethod.InputConnection
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.graphics.ColorUtils
import helium314.keyboard.latin.common.ColorType
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.InputTypeUtils
import java.util.Locale

/** Dictation stays in the current IME window and commits only final results to the active editor. */
internal class InlineDictation(private val ime: LatinIME) {
    private val handler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var panel: LinearLayout? = null
    private var footer: BoardFooterView? = null
    private var deadline: Runnable? = null

    fun start(root: View?, locale: Locale) {
        if (recognizer != null) return
        val frame = root as? FrameLayout ?: return
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
            .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)

        try {
            val session = SpeechRecognizer.createOnDeviceSpeechRecognizer(ime)
            recognizer = session
            footer = frame.findViewById(R.id.board_footer)
            footer?.showDictationActive(true)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                checkSupport(session, intent, speechLocale, romanizedHinglish, frame, connection)
            } else {
                listen(session, intent, frame, connection)
            }
        } catch (_: RuntimeException) {
            fail(R.string.inline_voice_failure)
        }
    }

    @androidx.annotation.RequiresApi(Build.VERSION_CODES.TIRAMISU)
    private fun checkSupport(
        session: SpeechRecognizer,
        intent: Intent,
        locale: Locale,
        romanizedHinglish: Boolean,
        frame: FrameLayout,
        connection: InputConnection,
    ) {
        var checking = true
        fun proceed(installed: List<String> = emptyList()) {
            if (!checking || recognizer !== session) return
            checking = false
            val requested = locale.toLanguageTag()
            // Some providers install only one regional model for a given language.
            // A Latin-script Hinglish keyboard can use an installed offline English
            // model when Hindi is not installed; never make that substitution for
            // a Hindi-script keyboard or a language other than Hinglish.
            if (installed.none { it.equals(requested, ignoreCase = true) }) {
                val sameLanguage = installed.firstOrNull {
                    val model = Locale.forLanguageTag(it)
                    model.language == locale.language &&
                        (model.script.isEmpty() || locale.script.isEmpty() || model.script == locale.script)
                }
                val englishFallback = if (romanizedHinglish && sameLanguage == null) {
                    installed.firstOrNull { it.equals("en-IN", ignoreCase = true) }
                        ?: installed.firstOrNull { it.equals("en-US", ignoreCase = true) }
                        ?: installed.firstOrNull { Locale.forLanguageTag(it).language == "en" }
                } else null
                (sameLanguage ?: englishFallback)?.let {
                    intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, it)
                }
            }
            listen(session, intent, frame, connection)
        }
        // A provider may omit this optional check or fail to call its callback.
        scheduleDeadline(1500) { proceed() }
        session.checkRecognitionSupport(intent, ime.mainExecutor, object : RecognitionSupportCallback {
            override fun onSupportResult(support: RecognitionSupport) = proceed(support.installedOnDeviceLanguages)
            override fun onError(error: Int) = proceed()
        })
    }

    private fun listen(
        session: SpeechRecognizer,
        intent: Intent,
        frame: FrameLayout,
        connection: InputConnection,
    ) {
        var status: TextView? = null
        var stop: Button? = null
        try {
            session.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) {
                    if (recognizer !== session || panel != null) return
                    if (!ime.isInputViewShown || ime.currentInputConnection !== connection) {
                        cancel()
                        return
                    }
                    clearDeadline()
                    val display = showPanel(frame, session)
                    status = display.first
                    stop = display.second
                }
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
                override fun onEndOfSpeech() {
                    if (recognizer !== session) return
                    status?.setText(R.string.inline_voice_processing)
                    stop?.isEnabled = false
                    scheduleDeadline(8000) { fail(R.string.inline_voice_failure) }
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
            scheduleDeadline(8000) { fail(R.string.inline_voice_failure) }
            session.startListening(intent)
        } catch (_: RuntimeException) {
            fail(R.string.inline_voice_failure)
        }
    }

    private fun showPanel(frame: FrameLayout, session: SpeechRecognizer): Pair<TextView, Button> {
        val colors = Settings.getValues().mColors
        val foreground = colors.get(ColorType.SUGGESTED_WORD)
        // The glass strip itself is transparent; use the board's base color for contrast.
        val background = colors.get(if (colors.isFrosted) ColorType.MAIN_BACKGROUND else ColorType.STRIP_BACKGROUND)
        val status = TextView(ime).apply {
            setText(R.string.inline_voice_listening)
            setTextColor(foreground)
            gravity = Gravity.CENTER
            accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
        }
        val stop = Button(ime, null, android.R.attr.borderlessButtonStyle).apply {
            setText(R.string.inline_voice_stop)
            setTextColor(foreground)
            setOnClickListener {
                isEnabled = false
                status.setText(R.string.inline_voice_processing)
                scheduleDeadline(8000) { fail(R.string.inline_voice_failure) }
                runCatching { session.stopListening() }.onFailure {
                    fail(R.string.inline_voice_failure)
                }
            }
        }
        val cancelButton = Button(ime, null, android.R.attr.borderlessButtonStyle).apply {
            setText(R.string.inline_voice_cancel)
            setTextColor(foreground)
            setOnClickListener { cancel() }
        }
        val stripHeight = frame.findViewById<View>(R.id.suggestion_strip_view)?.height ?: 0
        panel = LinearLayout(ime).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = Gravity.CENTER_VERTICAL
            setBackgroundColor(ColorUtils.setAlphaComponent(background, 255))
            isClickable = true
            addView(status, LinearLayout.LayoutParams(0, -1, 1f))
            addView(stop, LinearLayout.LayoutParams(-2, -1))
            addView(cancelButton, LinearLayout.LayoutParams(-2, -1))
            frame.addView(this, FrameLayout.LayoutParams(-1, if (stripHeight > 0) stripHeight else -2, Gravity.TOP))
        }
        return status to stop
    }

    fun cancel() {
        clearDeadline()
        val session = recognizer
        recognizer = null
        panel?.let { (it.parent as? FrameLayout)?.removeView(it) }
        panel = null
        footer?.showDictationActive(false)
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
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S &&
                runCatching { SpeechRecognizer.isOnDeviceRecognitionAvailable(context) }.getOrDefault(false)

        private fun canDictate(editor: EditorInfo): Boolean =
            editor.inputType != 0 &&
                !InputTypeUtils.isPasswordInputType(editor.inputType) &&
                !InputTypeUtils.isVisiblePasswordInputType(editor.inputType) &&
                editor.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING == 0
    }
}
