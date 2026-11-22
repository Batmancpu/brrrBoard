// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.Debug
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.Process
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/** Experimental 0.6B model only. A native OOM/crash cannot take down the keyboard process. */
internal class UltraVoiceTestService : Service() {
    private val worker = Executors.newSingleThreadExecutor { runnable ->
        Thread({
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            runnable.run()
        }, "brrBoard-ultra-test").apply { isDaemon = true }
    }
    private val active = AtomicBoolean(false)
    private val cancelRequested = AtomicBoolean(false)
    @Volatile private var decodeRequest: VoiceDecodeRequest? = null
    @Volatile private var completion: CountDownLatch? = null
    private val messenger by lazy { Messenger(IncomingHandler()) }

    override fun onBind(intent: Intent?): IBinder = messenger.binder

    override fun onUnbind(intent: Intent?): Boolean {
        cancelActive()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        cancelActive()
        worker.shutdownNow()
        OfflineVoiceEngine.ultraTest(this).release()
        super.onDestroy()
    }

    private inner class IncomingHandler : Handler(Looper.getMainLooper()) {
        override fun handleMessage(message: Message) {
            if (message.what != MSG_TRANSCRIBE) return
            val pcm = message.data.getByteArray(KEY_PCM)
            val reply = message.replyTo
            if (pcm == null || pcm.isEmpty() || pcm.size % 2 != 0 ||
                pcm.size > SAMPLE_RATE * BYTES_PER_SAMPLE * MAX_SECONDS) {
                replyError(reply, "Audio must be a single non-empty take of at most 12 seconds.")
                return
            }
            if (!active.compareAndSet(false, true)) {
                pcm.fill(0)
                replyError(reply, "Ultra test decoder is already busy.")
                return
            }
            val done = CountDownLatch(1)
            cancelRequested.set(false)
            completion = done
            try {
                worker.execute { decodeOneTake(pcm, reply, done) }
            } catch (_: RuntimeException) {
                pcm.fill(0)
                active.set(false)
                completion = null
                replyError(reply, "Ultra test worker could not start; the 110M model remains unchanged.")
            }
        }
    }

    private fun decodeOneTake(pcm: ByteArray, reply: Messenger, done: CountDownLatch) {
        var request: VoiceDecodeRequest? = null
        val sampling = AtomicBoolean(true)
        val peakPssKb = AtomicLong(0L)
        val sampler = Thread({
            while (sampling.get()) {
                updatePeak(peakPssKb, Debug.getPss().toLong())
                try { Thread.sleep(100) } catch (_: InterruptedException) { break }
            }
        }, "brrBoard-ultra-memory-sample").apply { isDaemon = true }
        try {
            check(!cancelRequested.get()) { "Ultra test cancelled." }
            check(VoiceModelInstaller.isInstalled(this, UltraVoiceModelManifest.pack)) {
                "Parakeet Ultra test model is not installed."
            }
            val engine = OfflineVoiceEngine.ultraTest(this)
            // Only one recognizer is resident at a time in this process. Main 110M
            // unload is coordinated by the lab before this service receives audio.
            engine.releaseAndWait()
            check(!cancelRequested.get()) { "Ultra test cancelled." }
            sampler.start()
            request = engine.transcribeMeasured(pcm, formatTranscript = false) { result ->
                result.fold(
                    onSuccess = { measured ->
                        val message = Message.obtain(null, MSG_RESULT).apply {
                            data = Bundle().apply {
                                putBoolean(KEY_OK, true)
                                putString(KEY_TRANSCRIPT, measured.transcript)
                                putLong(KEY_MODEL_LOAD_MS, measured.modelLoadMs)
                                putLong(KEY_DECODE_MS, measured.decodeMs)
                                putLong(KEY_TOTAL_MS, measured.totalMs)
                                putLong(KEY_PEAK_PSS_KB, peakPssKb.get())
                            }
                        }
                        sendReply(reply, message)
                    },
                    onFailure = { replyError(reply,
                        "Parakeet Ultra could not decode this take; the 110M model remains unchanged.") },
                )
                done.countDown()
            }
            decodeRequest = request
            if (cancelRequested.get()) request.cancel()
            if (done.await(MAX_DECODE_WAIT_SECONDS, TimeUnit.SECONDS).not()) {
                request.cancel()
                replyError(reply, "Parakeet Ultra test timed out; the 110M model remains unchanged.")
            }
        } catch (_: Exception) {
            replyError(reply, "Parakeet Ultra test failed; the 110M model remains unchanged.")
        } finally {
            pcm.fill(0)
            sampling.set(false)
            if (sampler.isAlive) sampler.join(250)
            runCatching { OfflineVoiceEngine.ultraTest(this).releaseAndWait() }
            decodeRequest = null
            completion = null
            active.set(false)
            done.countDown()
        }
    }

    private fun cancelActive() {
        cancelRequested.set(true)
        decodeRequest?.cancel()
        completion?.countDown()
    }

    private fun updatePeak(peak: AtomicLong, value: Long) {
        while (true) {
            val current = peak.get()
            if (value <= current || peak.compareAndSet(current, value)) return
        }
    }

    private fun replyError(reply: Messenger?, error: String) {
        sendReply(reply, Message.obtain(null, MSG_RESULT).apply {
            data = Bundle().apply { putBoolean(KEY_OK, false); putString(KEY_ERROR, error.take(180)) }
        })
    }

    private fun sendReply(reply: Messenger?, message: Message) {
        if (reply == null) return
        runCatching { reply.send(message) }
    }

    companion object {
        const val MSG_TRANSCRIBE = 1
        const val MSG_RESULT = 2
        const val KEY_PCM = "pcm"
        const val KEY_OK = "ok"
        const val KEY_ERROR = "error"
        const val KEY_TRANSCRIPT = "transcript"
        const val KEY_MODEL_LOAD_MS = "model_load_ms"
        const val KEY_DECODE_MS = "decode_ms"
        const val KEY_TOTAL_MS = "total_ms"
        const val KEY_PEAK_PSS_KB = "peak_pss_kb"
        const val SAMPLE_RATE = 16_000
        const val BYTES_PER_SAMPLE = 2
        const val MAX_SECONDS = 12
        const val MAX_DECODE_WAIT_SECONDS = 180L
    }
}

internal data class UltraVoiceTestResult(
    val transcript: String,
    val modelLoadMs: Long,
    val decodeMs: Long,
    val totalMs: Long,
    val peakPssKb: Long,
)

internal class UltraVoiceTestClient(private val context: android.content.Context) {
    private val appContext = context.applicationContext
    private val done = AtomicBoolean(false)
    @Volatile private var connection: android.content.ServiceConnection? = null
    @Volatile private var callback: ((Result<UltraVoiceTestResult>) -> Unit)? = null

    fun transcribe(pcm16: ByteArray, result: (Result<UltraVoiceTestResult>) -> Unit) {
        if (done.get()) {
            result(Result.failure(IllegalStateException("Ultra comparison was cancelled.")))
            return
        }
        callback = result
        val responseHandler = Handler(Looper.getMainLooper()) { message ->
            if (message.what != UltraVoiceTestService.MSG_RESULT || done.get()) return@Handler true
            val data = message.data
            if (data.getBoolean(UltraVoiceTestService.KEY_OK)) {
                finish(Result.success(UltraVoiceTestResult(
                    data.getString(UltraVoiceTestService.KEY_TRANSCRIPT).orEmpty(),
                    data.getLong(UltraVoiceTestService.KEY_MODEL_LOAD_MS),
                    data.getLong(UltraVoiceTestService.KEY_DECODE_MS),
                    data.getLong(UltraVoiceTestService.KEY_TOTAL_MS),
                    data.getLong(UltraVoiceTestService.KEY_PEAK_PSS_KB),
                )))
            } else finish(Result.failure(IllegalStateException(
                data.getString(UltraVoiceTestService.KEY_ERROR) ?: "Ultra model test failed."
            )))
            true
        }
        val responseMessenger = Messenger(responseHandler)
        val serviceConnection = object : android.content.ServiceConnection {
            override fun onServiceConnected(name: android.content.ComponentName?, binder: IBinder?) {
                if (done.get() || binder == null) return
                val audio = pcm16.copyOf()
                try {
                    Messenger(binder).send(Message.obtain(null, UltraVoiceTestService.MSG_TRANSCRIBE).apply {
                        replyTo = responseMessenger
                        data = Bundle().apply { putByteArray(UltraVoiceTestService.KEY_PCM, audio) }
                    })
                } catch (error: Exception) {
                    finish(Result.failure(error))
                } finally {
                    // Messenger marshals the byte array into Binder before send returns.
                    audio.fill(0)
                }
            }
            override fun onServiceDisconnected(name: android.content.ComponentName?) {
                finish(Result.failure(IllegalStateException(
                    "Ultra test process stopped. The regular 110M recognizer is still available."
                )))
            }
        }
        connection = serviceConnection
        val bound = runCatching {
            appContext.bindService(
                Intent(appContext, UltraVoiceTestService::class.java), serviceConnection,
                android.content.Context.BIND_AUTO_CREATE,
            )
        }.getOrDefault(false)
        if (!bound) finish(Result.failure(IllegalStateException("Could not start isolated Ultra test process.")))
    }

    fun cancel() = finish(Result.failure(IllegalStateException("Ultra comparison cancelled.")))

    private fun finish(answer: Result<UltraVoiceTestResult>) {
        if (!done.compareAndSet(false, true)) return
        connection?.let { runCatching { appContext.unbindService(it) } }
        connection = null
        callback?.invoke(answer)
        callback = null
    }
}
