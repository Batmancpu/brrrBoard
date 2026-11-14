// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.Button
import android.widget.FrameLayout
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import helium314.keyboard.latin.utils.InputTypeUtils
import java.util.Locale

/** Dictation stays in the current IME window and commits only final results to the active editor. */
internal class InlineDictation(private val ime: LatinIME) {
    private var recognizer: SpeechRecognizer? = null
    private var panel: LinearLayout? = null

    fun start(root: View?, locale: Locale) {
        if (recognizer != null) return
        val frame = root as? FrameLayout ?: return
        val editor = ime.currentInputEditorInfo ?: return
        if (!ime.isInputViewShown || !canDictate(editor)) return
        val connection = ime.currentInputConnection ?: return
        if (!isAvailable(ime)) {
            report(R.string.inline_voice_unavailable)
            return
        }
        if (ime.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            report(R.string.inline_voice_permission)
            return
        }

        val speechLocale = if (locale.language == "hi" && locale.script == "Latn")
            Locale.forLanguageTag("hi-IN") else locale
        val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            .putExtra(RecognizerIntent.EXTRA_LANGUAGE, speechLocale.toLanguageTag())
            .putExtra(RecognizerIntent.EXTRA_PREFER_OFFLINE, true)
            .putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)

        try {
            val session = SpeechRecognizer.createOnDeviceSpeechRecognizer(ime)
            recognizer = session
            val status = TextView(ime).apply {
                setText(R.string.inline_voice_listening)
                accessibilityLiveRegion = View.ACCESSIBILITY_LIVE_REGION_POLITE
            }
            val stop = Button(ime).apply {
                setText(R.string.inline_voice_stop)
                setOnClickListener {
                    isEnabled = false
                    status.setText(R.string.inline_voice_processing)
                    runCatching { session.stopListening() }.onFailure {
                        cancel()
                        report(R.string.inline_voice_failure)
                    }
                }
            }
            val cancelButton = Button(ime).apply {
                setText(R.string.inline_voice_cancel)
                setOnClickListener { cancel() }
            }
            val colors = ime.obtainStyledAttributes(intArrayOf(android.R.attr.colorBackground))
            val background = colors.getColor(0, 0xffeeeeee.toInt())
            colors.recycle()
            panel = LinearLayout(ime).apply {
                orientation = LinearLayout.HORIZONTAL
                gravity = Gravity.CENTER_VERTICAL
                setBackgroundColor(background)
                isClickable = true
                status.gravity = Gravity.CENTER
                addView(status, LinearLayout.LayoutParams(0, -2, 1f))
                addView(stop)
                addView(cancelButton)
                frame.addView(this, FrameLayout.LayoutParams(-1, -2, Gravity.TOP))
            }
            session.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = Unit
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onPartialResults(partialResults: Bundle?) = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit
                override fun onEndOfSpeech() {
                    if (recognizer !== session) return
                    status.setText(R.string.inline_voice_processing)
                    stop.isEnabled = false
                }
                override fun onError(error: Int) {
                    if (recognizer !== session) return
                    cancel()
                    val message = if (error == SpeechRecognizer.ERROR_LANGUAGE_NOT_SUPPORTED
                        || error == SpeechRecognizer.ERROR_LANGUAGE_UNAVAILABLE)
                        R.string.inline_voice_no_model else R.string.inline_voice_failure
                    report(message)
                }
                override fun onResults(results: Bundle?) {
                    if (recognizer !== session) return
                    val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()?.trim().orEmpty()
                    cancel()
                    if (text.isNotEmpty() && ime.isInputViewShown
                        && ime.currentInputConnection === connection) ime.onTextInput(text)
                }
            })
            session.startListening(intent)
        } catch (_: RuntimeException) {
            cancel()
            report(R.string.inline_voice_failure)
        }
    }

    fun cancel() {
        val session = recognizer
        recognizer = null
        panel?.let { (it.parent as? FrameLayout)?.removeView(it) }
        panel = null
        if (session != null) runCatching { session.destroy() }
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
