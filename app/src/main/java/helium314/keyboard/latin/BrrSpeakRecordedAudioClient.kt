// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.ParcelFileDescriptor
import helium314.keyboard.latin.utils.Log
import java.io.File
import java.util.concurrent.atomic.AtomicBoolean

/**
 * One-shot explicit Binder handoff of a finalized keyboard recording.
 * The parcel carries only a read-only file descriptor, fixed PCM metadata and a random session ID.
 * brrSpeak's reply carries only final transcription text or an integer error. No RecognitionService,
 * activity, clipboard, language hint, editor context or keyboard network access is involved.
 */
internal class BrrSpeakRecordedAudioClient(
    private val context: Context,
    private val sessionId: Long,
    private val audioFile: File,
    private val audioBytes: Long,
    private val onResult: (String) -> Unit,
    private val onError: (Int) -> Unit,
) {
    private val main = Handler(Looper.getMainLooper())
    private val closed = AtomicBoolean(false)
    private var bound = false
    private var remote: Messenger? = null
    private val callback = Messenger(object : Handler(Looper.getMainLooper()) {
        override fun handleMessage(message: Message) {
            if (closed.get() ||
                message.data.getLong(RecordedAudioWire.SESSION, -1L) != sessionId) return
            when (message.what) {
                RecordedAudioWire.RESULT -> {
                    val text = message.data.getString(RecordedAudioWire.TEXT).orEmpty()
                    finish()
                    onResult(text)
                }
                RecordedAudioWire.ERROR -> {
                    val code = message.data.getInt(RecordedAudioWire.ERROR_CODE, 0)
                    finish()
                    onError(code)
                }
            }
        }
    })
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName, binder: IBinder) {
            if (closed.get()) return
            try {
                remote = Messenger(binder)
                val descriptor = ParcelFileDescriptor.open(
                    audioFile, ParcelFileDescriptor.MODE_READ_ONLY,
                )
                try {
                    val request = Message.obtain().apply {
                        what = RecordedAudioWire.REQUEST
                        replyTo = this@BrrSpeakRecordedAudioClient.callback
                        data = Bundle().apply {
                            putLong(RecordedAudioWire.SESSION, sessionId)
                            putLong(RecordedAudioWire.BYTES, audioBytes)
                            putInt(RecordedAudioWire.RATE, RecordedPcmContract.RATE)
                            putInt(RecordedAudioWire.CHANNELS, RecordedPcmContract.CHANNELS)
                            putInt(RecordedAudioWire.ENCODING, RecordedPcmContract.ENCODING)
                            putParcelable(RecordedAudioWire.AUDIO, descriptor)
                        }
                    }
                    remote!!.send(request)
                } finally {
                    runCatching { descriptor.close() }
                    // Binder already transferred a duplicated kernel descriptor. The sending file
                    // is no longer required on disk, even while the receiver transcribes it.
                    audioFile.delete()
                }
            } catch (e: Exception) {
                Log.w("BrrSpeakRecordedIPC", "handoff exception=" + e.javaClass.simpleName)
                fail(RecordedAudioWire.ERROR_TRANSPORT)
            }
        }

        override fun onServiceDisconnected(name: ComponentName) {
            if (!closed.get()) fail(RecordedAudioWire.ERROR_DISCONNECTED)
        }
        override fun onBindingDied(name: ComponentName) {
            if (!closed.get()) fail(RecordedAudioWire.ERROR_DISCONNECTED)
        }
        override fun onNullBinding(name: ComponentName) {
            if (!closed.get()) fail(RecordedAudioWire.ERROR_BIND)
        }
    }

    fun start(): Boolean {
        if (closed.get() || !RecordedPcmContract.validLength(audioBytes)) return false
        return try {
            val component = ComponentName(
                BRRSPEAK_PACKAGE, RecordedAudioWire.SERVICE_CLASS,
            )
            val boundNow = context.bindService(
                Intent().setComponent(component), connection, Context.BIND_AUTO_CREATE,
            )
            bound = boundNow
            if (!boundNow) {
                Log.w("BrrSpeakRecordedIPC", "explicit service binding returned false")
                finish()
            }
            boundNow
        } catch (e: Exception) {
            Log.w("BrrSpeakRecordedIPC", "bind exception=" + e.javaClass.simpleName)
            finish()
            false
        }
    }

    fun close() {
        if (!closed.compareAndSet(false, true)) return
        val peer = remote
        if (peer != null) {
            runCatching {
                peer.send(Message.obtain().apply {
                    what = RecordedAudioWire.CANCEL
                    data = Bundle().apply { putLong(RecordedAudioWire.SESSION, sessionId) }
                })
            }
        }
        cleanup()
    }

    private fun finish() {
        if (closed.compareAndSet(false, true)) cleanup()
    }

    private fun fail(error: Int) {
        if (closed.get()) return
        finish()
        main.post { onError(error) }
    }

    private fun cleanup() {
        remote = null
        if (bound) {
            bound = false
            runCatching { context.unbindService(connection) }
        }
        audioFile.delete()
    }
}

internal object RecordedAudioWire {
    const val SERVICE_CLASS =
        "dev.patrickgold.florisboard.dictate.recognition.BoardRecordedAudioService"
    const val MIN_BRRSPEAK_VERSION = 142_008L
    const val REQUEST = 1
    const val CANCEL = 2
    const val RESULT = 3
    const val ERROR = 4
    const val SESSION = "session"
    const val AUDIO = "audio"
    const val BYTES = "bytes"
    const val RATE = "rate"
    const val CHANNELS = "channels"
    const val ENCODING = "encoding"
    const val TEXT = "text"
    const val ERROR_CODE = "error"
    const val ERROR_BIND = 101
    const val ERROR_TRANSPORT = 102
    const val ERROR_DISCONNECTED = 103

    fun supports(versionCode: Long?): Boolean =
        versionCode != null && versionCode >= MIN_BRRSPEAK_VERSION
}
