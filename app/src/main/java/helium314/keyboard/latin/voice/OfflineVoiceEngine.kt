// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.os.SystemClock
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineTransducerModelConfig
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.Future
import java.util.concurrent.TimeUnit

/**
 * Dedicated background-thread English speech decoder, directly inside brrBoard.
 * The native recognizer is lazily loaded and reused briefly across short requests; neither
 * audio nor transcript is written to disk or passed to the brrSpeak package.
 */
internal data class OfflineVoiceRecognition(
    val transcript: String,
    val modelLoadMs: Long,
    val decodeMs: Long,
    val totalMs: Long,
)

internal class OfflineVoiceEngine(
    private val context: Context,
    private val modelPack: VoiceModelPack = VoiceModelManifest.pack,
) {
    private val worker = Executors.newSingleThreadExecutor { runnable ->
        Thread({
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            runnable.run()
        }, "brrBoard-private-stt").apply { isDaemon = true }
    }
    private val handler = Handler(Looper.getMainLooper())
    private var recognizer: OfflineRecognizer? = null // worker thread only
    private var lastUsedAt = 0L // worker thread only
    private val idleUnload = Runnable {
        worker.execute {
            if (SystemClock.elapsedRealtime() - lastUsedAt >= IDLE_UNLOAD_MS) {
                recognizer?.release()
                recognizer = null
            }
        }
    }

    fun warm(): Future<*> = worker.submit {
        if (VoiceModelInstaller.isInstalled(context, modelPack)) {
            runCatching { requireRecognizer() }
            scheduleIdleUnload()
        }
    }

    /**
     * Owns [pcm16] immediately. The returned handle can cancel queued work and wipe
     * its bytes, or stop a running transcription at the next segment boundary.
     */
    fun transcribe(
        pcm16: ByteArray,
        formatTranscript: Boolean = true,
        callback: (Result<String>) -> Unit,
    ): VoiceDecodeRequest = transcribeMeasured(pcm16, formatTranscript) { answer ->
        callback(answer.map { it.transcript })
    }

    /** Same offline decoder with content-free load/decode/total timings for BR Brain A/B. */
    fun transcribeMeasured(
        pcm16: ByteArray,
        formatTranscript: Boolean = false,
        callback: (Result<OfflineVoiceRecognition>) -> Unit,
    ): VoiceDecodeRequest {
        val request = VoiceDecodeRequest(pcm16)
        worker.execute {
            if (!request.begin()) return@execute
            val result = try {
                runCatching {
                    require(pcm16.size % 2 == 0 && pcm16.isNotEmpty()) { "No speech was recorded" }
                    check(!request.isCancelled) { "Dictation was cancelled" }
                    require(VoiceModelInstaller.isInstalled(context, modelPack)) {
                        "Install this offline voice model first"
                    }
                    val totalStartedAt = SystemClock.elapsedRealtime()
                    val loadStartedAt = totalStartedAt
                    val rec = requireRecognizer()
                    val modelLoadMs = SystemClock.elapsedRealtime() - loadStartedAt
                    val decodeStartedAt = SystemClock.elapsedRealtime()
                    VoiceSessionCrashTrail.note(context, "ASR_DECODING")
                    val pieces = ArrayList<String>()
                    for (window in VoiceSegmenter.windows(pcm16)) {
                        check(!request.isCancelled) { "Dictation was cancelled" }
                        val samples = FloatArray(window.endExclusive - window.startSample)
                        try {
                            for (sampleIndex in samples.indices) {
                                val byteIndex = (window.startSample + sampleIndex) * 2
                                val lo = pcm16[byteIndex].toInt() and 0xff
                                val hi = pcm16[byteIndex + 1].toInt()
                                samples[sampleIndex] = ((hi shl 8) or lo).toShort().toFloat() / 32_768f
                            }
                            val stream = rec.createStream()
                            try {
                                stream.acceptWaveform(samples, PrivatePcmBuffer.SAMPLE_RATE)
                                rec.decode(stream)
                                pieces += rec.getResult(stream).text.trim()
                            } finally {
                                stream.release()
                            }
                        } finally {
                            samples.fill(0f)
                        }
                    }
                    lastUsedAt = SystemClock.elapsedRealtime()
                    VoiceSessionCrashTrail.note(context, "ASR_DECODED")
                    check(!request.isCancelled) { "Dictation was cancelled" }
                    val combined = VoiceTranscriptJoiner.merge(pieces)
                    val text = if (formatTranscript) VoiceTranscriptFormatter.format(combined) else combined
                    OfflineVoiceRecognition(text, modelLoadMs,
                        SystemClock.elapsedRealtime() - decodeStartedAt,
                        SystemClock.elapsedRealtime() - totalStartedAt)
                }
            } finally {
                request.finish()
            }
            if (!request.isCancelled) {
                handler.post { if (!request.isCancelled) callback(result) }
            }
            scheduleIdleUnload()
        }
        return request
    }

    /** Defer unloading while a native decode is active; release runs on the same worker. */
    fun release() {
        worker.execute {
            recognizer?.release()
            recognizer = null
        }
    }

    /** Called only by the background voice comparison worker before loading the other model. */
    fun releaseAndWait() {
        worker.submit {
            recognizer?.release()
            recognizer = null
        }.get(30, TimeUnit.SECONDS)
    }

    private fun scheduleIdleUnload() {
        handler.removeCallbacks(idleUnload)
        handler.postDelayed(idleUnload, IDLE_UNLOAD_MS)
    }

    private fun requireRecognizer(): OfflineRecognizer {
        recognizer?.let { return it }
        val model = VoiceModelInstaller.modelDir(context, modelPack)
        check(VoiceModelInstaller.isInstalled(context, modelPack)) { "Private voice model unavailable" }
        val config = OfflineRecognizerConfig(
            featConfig = FeatureConfig(sampleRate = PrivatePcmBuffer.SAMPLE_RATE, featureDim = 80),
            modelConfig = OfflineModelConfig(
                transducer = OfflineTransducerModelConfig(
                    encoder = File(model, modelPack.encoderName).absolutePath,
                    decoder = File(model, modelPack.decoderName).absolutePath,
                    joiner = File(model, modelPack.joinerName).absolutePath,
                ),
                tokens = File(model, modelPack.tokensName).absolutePath,
                numThreads = 2,
                modelType = "nemo_transducer",
            ),
        )
        VoiceSessionCrashTrail.note(context, "ASR_LOADING")
        return OfflineRecognizer(config = config).also {
            recognizer = it
            lastUsedAt = SystemClock.elapsedRealtime()
            VoiceSessionCrashTrail.note(context, "ASR_READY")
        }
    }

    companion object {
        private const val IDLE_UNLOAD_MS = 90_000L
        private val instances = HashMap<String, OfflineVoiceEngine>()

        /** Keyboard dictation prefers installed Ultra and unloads stale model workers. */
        fun shared(context: Context): OfflineVoiceEngine = forKeyboard(
            context, VoiceModelChoice.active(context) ?: VoiceModelManifest.pack)

        @Synchronized
        fun forKeyboard(context: Context, modelPack: VoiceModelPack): OfflineVoiceEngine {
            val selected = shared(context, modelPack)
            instances.forEach { (id, engine) -> if (id != modelPack.id) engine.release() }
            return selected
        }

        @Synchronized
        fun shared(context: Context, modelPack: VoiceModelPack): OfflineVoiceEngine =
            instances.getOrPut(modelPack.id) {
                OfflineVoiceEngine(context.applicationContext, modelPack)
            }

        fun ultraTest(context: Context): OfflineVoiceEngine = shared(context, UltraVoiceModelManifest.pack)
    }
}
