// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.view.inputmethod.InputConnection
import helium314.keyboard.latin.voice.OfflineVoiceEngine
import helium314.keyboard.latin.voice.VoiceModelChoice
import helium314.keyboard.latin.voice.UltraVoiceModelManifest
import helium314.keyboard.latin.voice.PrivatePcmBuffer
import helium314.keyboard.latin.voice.VoiceResultArbiter
import helium314.keyboard.latin.voice.VoiceDecodeRequest

/**
 * Ephemeral offline transcription of the *same* audio sent to brrSpeak. Never gets the
 * text of the host editor and never creates a file. The server remains the automatic
 * winner; only a slow/failed server shows a user-controlled Paste offline action.
 */
internal class HybridVoiceBackup(
    private val ime: LatinIME,
    private val footer: BoardFooterView?,
    private val connection: InputConnection,
    private val onLocalPaste: (String) -> Unit,
) {
    private val modelPack = VoiceModelChoice.active(ime)
    private val engine = OfflineVoiceEngine.forKeyboard(ime, modelPack ?: UltraVoiceModelManifest.pack)
    private val buffer = PrivatePcmBuffer()
    private val arbiter = VoiceResultArbiter()
    private var decodeRequest: VoiceDecodeRequest? = null
    private var cancelled = false // main thread only
    private var captureFinished = false
    private var localPasted = false
    private var serverFailed = false
    private var serverSlow = false
    private var localText: String? = null

    init {
        if (modelPack != null) engine.warm()
    }

    /** Called synchronously on the mic thread; this is a bounded RAM copy, never a decode. */
    fun appendFrame(bytes: ByteArray, length: Int) = buffer.append(bytes, 0, length)

    /** Called after the pipe-writer has closed, on the main thread. Idempotent on error. */
    fun finishCapture() {
        if (captureFinished || cancelled) return
        captureFinished = true
        val bytes = runCatching { buffer.drain() }.getOrNull() ?: return
        if (bytes.isEmpty() || modelPack == null) {
            bytes.fill(0)
            return
        }
        decodeRequest = engine.transcribe(bytes) { answer ->
            decodeRequest = null
            if (cancelled || localPasted) return@transcribe
            localText = answer.getOrNull()?.trim()?.takeIf {
                it.isNotBlank() && !containsDevanagari(it)
            }
            maybeOffer()
        }
    }

    fun serverIsSlow() {
        if (cancelled) return
        serverSlow = true
        maybeOffer()
    }

    fun serverHasFailed() {
        if (cancelled) return
        serverFailed = true
        maybeOffer()
    }

    /** Returns false if the user has already chosen the local transcript. */
    fun serverSucceeded(): Boolean {
        val canCommitServer = arbiter.acceptServer()
        cancel()
        return canCommitServer
    }

    fun cancel() {
        cancelled = true
        decodeRequest?.cancel()
        decodeRequest = null
        localText = null
        buffer.discard()
        footer?.clearContextAction(this)
    }

    private fun maybeOffer() {
        if (cancelled || localPasted || (!serverFailed && !serverSlow)) return
        val text = localText ?: return
        if (!sameEditor()) return
        footer?.showContextAction(
            this,
            R.string.board_voice_offline_ready,
            R.string.board_footer_action_paste_offline,
            10_000L,
        ) {
            if (!cancelled && !localPasted && sameEditor() && arbiter.acceptOffline()) {
                localPasted = true
                onLocalPaste(text)
                cancel()
            }
        }
    }

    private fun sameEditor(): Boolean =
        ime.isInputViewShown && ime.currentInputConnection === connection

    private fun containsDevanagari(text: String): Boolean = text.any {
        it in '\u0900'..'\u097F' || it in '\uA8E0'..'\uA8FF'
    }
}
