// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import com.k2fsa.sherpa.onnx.FeatureConfig
import com.k2fsa.sherpa.onnx.OfflineModelConfig
import com.k2fsa.sherpa.onnx.OfflineRecognizer
import com.k2fsa.sherpa.onnx.OfflineRecognizerConfig
import com.k2fsa.sherpa.onnx.OfflineTransducerModelConfig
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.Future

/**
 * Dedicated background-thread English speech decoder, directly inside brrBoard.
 * The native recognizer is lazily loaded and reused briefly across short requests; neither
 * audio nor transcript is written to disk or passed to the brrSpeak package.
 */
internal class OfflineVoiceEngine(private val context: Context) {
    private val worker = Executors.newSingleThreadExecutor { runnable ->
        Thread(runnable, "brrBoard-private-stt").apply { isDaemon = true }
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
        if (VoiceModelInstaller.isInstalled(context)) {
            runCatching { requireRecognizer() }
            scheduleIdleUnload()
        }
    }

    /**
     * Owns [pcm16] immediately. The returned handle can cancel queued work and wipe
     * its bytes, or stop a running transcription at the next segment boundary.
     */
    fun transcribe(pcm16: ByteArray, callback: (Result<String>) -> Unit): VoiceDecodeRequest {
        val request = VoiceDecodeRequest(pcm16)
        worker.execute {
            if (!request.begin()) return@execute
            val result = try {
                runCatching {
                    require(pcm16.size % 2 == 0 && pcm16.isNotEmpty()) { "No speech was recorded" }
                    check(!request.isCancelled) { "Dictation was cancelled" }
                    require(VoiceModelInstaller.isInstalled(context)) { "Install the one-time offline voice model first" }
                    val rec = requireRecognizer()
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
                    VoiceTranscriptFormatter.format(VoiceTranscriptJoiner.merge(pieces))
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

    private fun scheduleIdleUnload() {
        handler.removeCallbacks(idleUnload)
        handler.postDelayed(idleUnload, IDLE_UNLOAD_MS)
    }

    private fun requireRecognizer(): OfflineRecognizer {
        recognizer?.let { return it }
        val model = VoiceModelInstaller.modelDir(context)
        check(VoiceModelInstaller.isInstalled(context)) { "Private voice model unavailable" }
        val config = OfflineRecognizerConfig(
            featConfig = FeatureConfig(sampleRate = PrivatePcmBuffer.SAMPLE_RATE, featureDim = 80),
            modelConfig = OfflineModelConfig(
                transducer = OfflineTransducerModelConfig(
                    encoder = File(model, "encoder.onnx").absolutePath,
                    decoder = File(model, "decoder.onnx").absolutePath,
                    joiner = File(model, "joiner.onnx").absolutePath,
                ),
                tokens = File(model, "tokens.txt").absolutePath,
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
        @Volatile private var instance: OfflineVoiceEngine? = null

        /** One native ASR model/worker shared by blue dictation and the red-mode backup. */
        @Synchronized
        fun shared(context: Context): OfflineVoiceEngine = instance ?: OfflineVoiceEngine(
            context.applicationContext,
        ).also { instance = it }
    }
}
