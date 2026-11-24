// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import android.content.Context
import com.k2fsa.sherpa.onnx.SileroVadModelConfig
import com.k2fsa.sherpa.onnx.Vad
import com.k2fsa.sherpa.onnx.VadModelConfig
import helium314.keyboard.latin.utils.prefs
import java.security.MessageDigest

internal object VoiceEndpointPreference {
    private const val KEY = "brr_voice_adaptive_batching_v1"
    fun enabled(context: Context) = context.prefs().getBoolean(KEY, true)
    fun setEnabled(context: Context, enabled: Boolean) {
        context.prefs().edit().putBoolean(KEY, enabled).apply()
    }
}

/** Confined to the per-session audio worker; no audio/text is written to disk. */
internal class PrivateVoiceEndpointDetector(private val context: Context) {
    private val enabled = VoiceEndpointPreference.enabled(context)
    private var vad: Vad? = null
    private var vadAttempted = false
    private var turn = 0L
    private var turnAttempted = false

    private fun verifiedAsset(path: String, bytes: Int, sha: String): ByteArray? =
        runCatching {
            val data = context.assets.open(path).use { it.readBytes() }
            val hash = MessageDigest.getInstance("SHA-256").digest(data)
                .joinToString("") { "%02x".format(it) }
            if (data.size == bytes && hash == sha) data else { data.fill(0); null }
        }.getOrNull()

    fun speech(pcm: ByteArray, from: Int, to: Int): Boolean? {
        if (!enabled) return null
        if (!vadAttempted) {
            vadAttempted = true
            try {
                val verified = verifiedAsset(VAD_PATH, 2327524, VAD_SHA) ?: return null
                verified.fill(0)
                vad = Vad(context.assets, VadModelConfig(
                    sileroVadModelConfig = SileroVadModelConfig(
                        model = VAD_PATH, threshold = .5f, minSilenceDuration = .10f,
                        minSpeechDuration = .064f, windowSize = 512, maxSpeechDuration = 20f),
                    sampleRate = 16000, numThreads = 1, provider = "cpu"))
            } catch (_: Throwable) { vad = null }
        }
        val detector = vad ?: return null
        val samples = pcmFloats(pcm, from, to)
        return try {
            detector.acceptWaveform(samples)
            val speaking = detector.isSpeechDetected()
            while (!detector.empty()) detector.pop()
            speaking
        } catch (_: Throwable) {
            runCatching { detector.release() }
            vad = null
            null
        } finally { samples.fill(0f) }
    }

    fun thoughtComplete(pcm: ByteArray, count: Int): Boolean? {
        if (!enabled) return null
        if (!turnAttempted) {
            turnAttempted = true
            val weights = verifiedAsset(TURN_PATH, 8679182, TURN_SHA) ?: return null
            try { turn = BrrVoiceTurnNative.create(weights) }
            catch (_: Throwable) { turn = 0L }
            finally { weights.fill(0) }
        }
        if (turn == 0L) return null
        val samples = pcmFloats(pcm, 0, count)
        val window = WhisperFeatures.lastWindow(samples)
        val flat = FloatArray(WhisperFeatures.N_MELS * WhisperFeatures.N_FRAMES)
        var mel: Array<FloatArray>? = null
        return try {
            val values = WhisperFeatures.logMel(window)
            mel = values
            values.forEachIndexed { index, row -> row.copyInto(flat, index * WhisperFeatures.N_FRAMES) }
            val probability = BrrVoiceTurnNative.score(turn, flat)
            if (probability.isFinite() && probability in 0f..1f) probability >= .65f
            else { disableTurn(); null }
        } catch (_: Throwable) {
            disableTurn()
            null
        } finally {
            samples.fill(0f)
            window.fill(0f)
            flat.fill(0f)
            mel?.forEach { it.fill(0f) }
        }
    }

    private fun disableTurn() {
        if (turn != 0L) runCatching { BrrVoiceTurnNative.release(turn) }
        turn = 0L
    }

    fun close() {
        vad?.let { runCatching { it.release() } }
        vad = null
        disableTurn()
    }

    private fun pcmFloats(pcm: ByteArray, from: Int, to: Int) =
        FloatArray((to - from) / 2) { i ->
            val p = from + i * 2
            ((pcm[p + 1].toInt() shl 8) or (pcm[p].toInt() and 255)).toShort() / 32768f
        }

    companion object {
        private const val VAD_PATH = "voice/silero_vad_v6_2_3.onnx"
        private const val VAD_SHA = "1a153a22f4509e292a94e67d6f9b85e8deb25b4988682b7e174c65279d8788e3"
        private const val TURN_PATH = "voice/smart-turn-v3.2-cpu.onnx"
        private const val TURN_SHA = "2bb026316b14a660486a75b1733cd3fbab8c2fd0314dc9af7be49f8cca967e4f"
    }
}

internal object BrrVoiceTurnNative {
    init { System.loadLibrary("brrvoiceendpoint") }
    external fun create(model: ByteArray): Long
    external fun score(handle: Long, features: FloatArray): Float
    external fun release(handle: Long)
}
