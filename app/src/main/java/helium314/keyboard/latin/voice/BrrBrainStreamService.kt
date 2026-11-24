// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import android.app.Service
import android.content.Intent
import android.content.res.AssetManager
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.Process
import com.k2fsa.sherpa.onnx.SileroVadModelConfig
import com.k2fsa.sherpa.onnx.Vad
import com.k2fsa.sherpa.onnx.VadModelConfig
import java.security.MessageDigest
import java.util.concurrent.Executors

internal object BrrCleanStreamNative {
    init { System.loadLibrary("brrclean") }
    @JvmStatic external fun create(weights: ByteArray): Long
    @JvmStatic external fun step(handle: Long, pcm: ByteArray): ByteArray?
    @JvmStatic external fun release(handle: Long)
}

internal object BrrBrainStreamWire {
    const val BLOCK = 11
    const val RESULT = 12
    const val CLOSE = 13
    const val CANCEL = 14
    const val TOKEN = "token"
    const val RAW = "raw"
    const val INPUT = "input"
    const val CLEAN = "clean"
    const val RAW_SCORE = "raw_score"
    const val CLEAN_SCORE = "clean_score"
    const val BLOCK_BYTES = 20 * ProtectedVoiceAudio.HOP * 2 // 640 ms
    const val LOOK_BYTES = 512 // 16 ms GTCRN/STFT latency
}
internal data class BrrBrainScoredBlock(val clean: ByteArray, val rawScore: FloatArray, val cleanScore: FloatArray) {
    fun wipe() { clean.fill(0); rawScore.fill(0f); cleanScore.fill(0f) }
}

/** Persistent, session-scoped, non-exported child; the IME never loads GTCRN here. */
class BrrBrainStreamService : Service() {
    private val handler = Handler(Looper.getMainLooper())
    private val worker = Executors.newSingleThreadExecutor { task ->
        Thread({
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            task.run()
        }, "brrBrain-stream-worker").apply { isDaemon = true }
    }
    private var session: BrrBrainNativeSession? = null // worker-confined
    private var token = 0L // main looper
    private var busy = false
    private val watchdog = Runnable { Process.killProcess(Process.myPid()) }
    private val inbound = Messenger(Handler(Looper.getMainLooper()) { message ->
        val incoming = message.data.getLong(BrrBrainStreamWire.TOKEN)
        when (message.what) {
            BrrBrainStreamWire.BLOCK -> {
                val caller = message.replyTo
                val id = message.arg1
                val raw = message.data.getByteArray(BrrBrainStreamWire.RAW)
                val input = message.data.getByteArray(BrrBrainStreamWire.INPUT)
                if (incoming == 0L || (token != 0L && incoming != token) || busy ||
                    raw == null || raw.size != BrrBrainStreamWire.BLOCK_BYTES || input == null ||
                    (input.size != BrrBrainStreamWire.BLOCK_BYTES &&
                     input.size != BrrBrainStreamWire.BLOCK_BYTES + BrrBrainStreamWire.LOOK_BYTES)) {
                    raw?.fill(0); input?.fill(0)
                    respond(caller, id, incoming, null)
                } else {
                    token = incoming
                    busy = true
                    handler.postDelayed(watchdog, 6000L)
                    worker.execute {
                        val result = try {
                            val active = session ?: BrrBrainNativeSession(assets).also { session = it }
                            active.process(raw, input)
                        } catch (_: Throwable) {
                            session?.close(); session = null
                            null
                        } finally { raw.fill(0); input.fill(0) }
                        handler.post {
                            handler.removeCallbacks(watchdog)
                            busy = false
                            respond(caller, id, incoming, result)
                        }
                    }
                }
            }
            BrrBrainStreamWire.CANCEL -> if (incoming == token) Process.killProcess(Process.myPid())
            BrrBrainStreamWire.CLOSE -> if (incoming == token && !busy) releaseSession()
        }
        true
    })

    private fun respond(caller: Messenger?, id: Int, incoming: Long, result: BrrBrainScoredBlock?) {
        try {
            caller?.send(Message.obtain(null, BrrBrainStreamWire.RESULT).apply {
                arg1 = id
                data = Bundle().apply {
                    putLong(BrrBrainStreamWire.TOKEN, incoming)
                    result?.let {
                        putByteArray(BrrBrainStreamWire.CLEAN, it.clean)
                        putFloatArray(BrrBrainStreamWire.RAW_SCORE, it.rawScore)
                        putFloatArray(BrrBrainStreamWire.CLEAN_SCORE, it.cleanScore)
                    }
                }
            })
        } catch (_: Exception) { /* owner falls back to original */ }
        finally { result?.wipe() }
    }

    private fun releaseSession() {
        token = 0L
        worker.execute { session?.close(); session = null }
    }
    override fun onBind(intent: Intent?): IBinder = inbound.binder
    override fun onUnbind(intent: Intent?): Boolean {
        if (busy) Process.killProcess(Process.myPid()) else releaseSession()
        return false
    }
    override fun onDestroy() {
        handler.removeCallbacks(watchdog)
        if (busy) Process.killProcess(Process.myPid())
        else {
            worker.execute { session?.close(); session = null }
            worker.shutdown()
        }
        super.onDestroy()
    }
}

private class BrrBrainNativeSession(assets: AssetManager) {
    private var native = 0L
    private var rawVad: Vad? = null
    private var cleanVad: Vad? = null
    private val rawContext = FloatArray(64)
    private val cleanContext = FloatArray(64)
    private var first = true
    init {
        try {
            val model = verified(assets, "voice/brrclean_gtcrn.weights", 211220,
                "4db48ff819e5ac18f5ccad1dfdce0968dde1a209c0cbbf9f24ba9dc4d9f48ded")
            try { native = BrrCleanStreamNative.create(model) } finally { model.fill(0) }
            check(native != 0L)
            verified(assets, VAD_PATH, 2327524,
                "1a153a22f4509e292a94e67d6f9b85e8deb25b4988682b7e174c65279d8788e3").fill(0)
            fun detector() = Vad(assets, VadModelConfig(
                sileroVadModelConfig = SileroVadModelConfig(model = VAD_PATH, windowSize = 512),
                sampleRate = 16000, numThreads = 1, provider = "cpu"))
            rawVad = detector()
            cleanVad = detector()
        } catch (failure: Throwable) { close(); throw failure }
    }
    fun process(raw: ByteArray, input: ByteArray): BrrBrainScoredBlock {
        check(input.size == raw.size + if (first) BrrBrainStreamWire.LOOK_BYTES else 0)
        val output = checkNotNull(BrrCleanStreamNative.step(native, input))
        try {
            check(output.size == raw.size)
            first = false
            val a = score(raw, checkNotNull(rawVad), rawContext)
            val b = score(output, checkNotNull(cleanVad), cleanContext)
            return BrrBrainScoredBlock(output, a, b)
        } catch (failure: Throwable) { output.fill(0); throw failure }
    }
    private fun score(pcm: ByteArray, detector: Vad, context: FloatArray): FloatArray {
        val window = FloatArray(576)
        val result = FloatArray(pcm.size / 1024)
        try {
            for (f in result.indices) {
                context.copyInto(window)
                for (i in 0 until 512)
                    window[64+i] = ProtectedVoiceAudio.sample(pcm, f*512+i) / 32768f
                result[f] = detector.compute(window)
                check(result[f].isFinite() && result[f] in 0f..1f)
                window.copyInto(context, 0, 512, 576)
            }
            return result
        } finally { window.fill(0f) }
    }
    fun close() {
        rawVad?.let { runCatching { it.reset() }; runCatching { it.release() } }; rawVad = null
        cleanVad?.let { runCatching { it.reset() }; runCatching { it.release() } }; cleanVad = null
        if (native != 0L) runCatching { BrrCleanStreamNative.release(native) }
        native = 0L
        rawContext.fill(0f); cleanContext.fill(0f)
    }
    private fun verified(assets: AssetManager, path: String, size: Int, sha: String): ByteArray {
        val bytes = assets.open(path).use { it.readBytes() }
        val valid = bytes.size == size && MessageDigest.getInstance("SHA-256").digest(bytes)
            .joinToString("") { "%02x".format(it) } == sha
        if (!valid) { bytes.fill(0); error("Invalid pinned audio model") }
        return bytes
    }
    companion object { private const val VAD_PATH = "voice/silero_vad_v6_2_3.onnx" }
}
