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

    fun warm(): Future<*> = worker.submit {
        if (VoiceModelInstaller.isInstalled(context)) runCatching { requireRecognizer() }
    }

    /** Takes ownership of [pcm16] and wipes its bytes after this attempt, including failures. */
    fun transcribe(pcm16: ByteArray, callback: (Result<String>) -> Unit): Future<*> = worker.submit {
        val result = runCatching {
            require(pcm16.size % 2 == 0 && pcm16.isNotEmpty()) { "No speech was recorded" }
            require(VoiceModelInstaller.isInstalled(context)) { "Install the one-time offline voice model first" }
            val rec = requireRecognizer()
            val windows = VoiceSegmenter.windows(pcm16)
            val pieces = windows.map { window ->
                val samples = FloatArray(window.endExclusive - window.startSample)
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
                    rec.getResult(stream).text.trim()
                } finally {
                    samples.fill(0f)
                    stream.release()
                }
            }
            lastUsedAt = SystemClock.elapsedRealtime()
            VoiceTranscriptJoiner.merge(pieces)
        }
        pcm16.fill(0)
        handler.post { callback(result) }
        scheduleIdleUnload()
    }

    /** Defer unloading while a native decode is active; release runs on the same worker. */
    fun release() {
        worker.execute {
            recognizer?.release()
            recognizer = null
        }
    }

    private fun scheduleIdleUnload() {
        handler.postDelayed({
            worker.execute {
                if (SystemClock.elapsedRealtime() - lastUsedAt >= IDLE_UNLOAD_MS) {
                    recognizer?.release()
                    recognizer = null
                }
            }
        }, IDLE_UNLOAD_MS)
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
        return OfflineRecognizer(config = config).also {
            recognizer = it
            lastUsedAt = SystemClock.elapsedRealtime()
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
