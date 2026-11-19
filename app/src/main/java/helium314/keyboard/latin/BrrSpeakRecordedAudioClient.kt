// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.content.ComponentName
import android.content.ContentProviderClient
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
    private var wakeClient: ContentProviderClient? = null
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
            // Binder is connected: it now holds brrSpeak's process alive. Release the brief
            // stable provider reference before transferring the completed PCM descriptor.
            releaseWakeClient()
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

    private val started = AtomicBoolean(false)

    fun start(): Boolean {
        if (!started.compareAndSet(false, true) || closed.get() ||
            !RecordedPcmContract.validLength(audioBytes)) return false
        // This worker makes no microphone request and transfers no data. Acquiring the exported
        // UID-verified ContentProvider starts a cold brrSpeak process on Android/ColorOS, without
        // requiring its activity to stay open or a permanent foreground/background service.
        Thread({
            var client: ContentProviderClient? = null
            val awake = runCatching {
                // A stable provider client keeps the cold process alive through the subsequent
                // main-thread bind, avoiding a short OEM kill race between call() and bindService().
                client = context.contentResolver.acquireContentProviderClient(
                    android.net.Uri.parse("content://" + RecordedAudioWire.WAKE_AUTHORITY),
                )
                client?.call(RecordedAudioWire.WAKE_METHOD, null, null)
                    ?.getBoolean(RecordedAudioWire.WAKE_READY, false) == true
            }.onFailure {
                Log.w("BrrSpeakRecordedIPC", "cold-start provider exception=" +
                    it.javaClass.simpleName)
            }.getOrDefault(false)
            val retained = client
            main.post {
                if (closed.get()) {
                    runCatching { retained?.release() }
                } else {
                    wakeClient = retained
                    bindReceiver(awake)
                }
            }
        }, "brrBoard-brrSpeak-cold-start").apply { isDaemon = true }.start()
        return true
    }

    private fun bindReceiver(awake: Boolean) {
        if (closed.get()) return
        // Some older builds lack the provider; still try the legacy direct bind. An explicitly
        // installed old protocol version, however, cannot safely accept this request.
        @Suppress("DEPRECATION")
        val installed = runCatching {
            context.packageManager.getPackageInfo(BRRSPEAK_PACKAGE, 0)
        }.getOrNull()
        if (installed != null) {
            val version = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.P) {
                installed.longVersionCode
            } else {
                @Suppress("DEPRECATION")
                installed.versionCode.toLong()
            }
            if (!RecordedAudioWire.supports(version)) {
                fail(RecordedAudioWire.ERROR_VERSION)
                return
            }
        }
        try {
            val component = ComponentName(BRRSPEAK_PACKAGE, RecordedAudioWire.SERVICE_CLASS)
            bound = context.bindService(
                Intent().setComponent(component), connection, Context.BIND_AUTO_CREATE,
            )
            if (!bound) {
                Log.w("BrrSpeakRecordedIPC", "explicit bind false after cold-start ack=" + awake)
                fail(RecordedAudioWire.ERROR_BIND)
            }
        } catch (e: Exception) {
            Log.w("BrrSpeakRecordedIPC", "bind exception=" + e.javaClass.simpleName +
                " providerAck=" + awake)
            fail(RecordedAudioWire.ERROR_BIND)
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

    private fun releaseWakeClient() {
        val client = wakeClient
        wakeClient = null
        @Suppress("DEPRECATION")
        runCatching { client?.release() }
    }

    private fun cleanup() {
        releaseWakeClient()
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
    const val MIN_BRRSPEAK_VERSION = 142_009L
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
    const val ERROR_VERSION = 104
    const val WAKE_AUTHORITY = "com.mangoloads.speak.boardaudio"
    const val WAKE_METHOD = "prepare-recorded-audio"
    const val WAKE_READY = "ready"

    fun supports(versionCode: Long?): Boolean =
        versionCode != null && versionCode >= MIN_BRRSPEAK_VERSION
}
