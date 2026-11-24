// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import android.content.res.AssetManager
import com.k2fsa.sherpa.onnx.SileroVadModelConfig
import com.k2fsa.sherpa.onnx.Vad
import com.k2fsa.sherpa.onnx.VadModelConfig
import java.security.MessageDigest

/** Only called inside the expendable denoiser process, never on the IME/UI. */
internal object BrrCleanSpeechProtection {
    fun apply(assets: AssetManager, raw: ByteArray, clean: ByteArray): ByteArray? {
        var vad: Vad? = null
        return try {
            val bytes = assets.open(PATH).use { it.readBytes() }
            val valid = try {
                bytes.size == 2327524 && MessageDigest.getInstance("SHA-256").digest(bytes)
                    .joinToString("") { "%02x".format(it) } == SHA
            } finally { bytes.fill(0) }
            if (!valid) return null
            val model = Vad(assets, VadModelConfig(
                sileroVadModelConfig = SileroVadModelConfig(model = PATH, windowSize = 512),
                sampleRate = 16000, numThreads = 1, provider = "cpu"))
            vad = model
            val rawScores = ProtectedVoiceAudio.scores(raw, model::compute)
            // Independent recurrent history, one set of loaded weights.
            model.reset()
            val cleanScores = ProtectedVoiceAudio.scores(clean, model::compute)
            ProtectedVoiceAudio.protect(raw, clean, rawScores, cleanScores)
        } catch (_: Throwable) {
            null // fail closed to original PCM, never unprotected denoising
        } finally { vad?.let { runCatching { it.release() } } }
    }

    private const val PATH = "voice/silero_vad_v6_2_3.onnx"
    private const val SHA = "1a153a22f4509e292a94e67d6f9b85e8deb25b4988682b7e174c65279d8788e3"
}
