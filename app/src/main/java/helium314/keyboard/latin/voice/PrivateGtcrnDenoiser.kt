// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import android.content.Context
import com.k2fsa.sherpa.onnx.OfflineSpeechDenoiser
import com.k2fsa.sherpa.onnx.OfflineSpeechDenoiserConfig
import com.k2fsa.sherpa.onnx.OfflineSpeechDenoiserGtcrnModelConfig
import com.k2fsa.sherpa.onnx.OfflineSpeechDenoiserModelConfig

/**
 * Strictly private GTCRN speech enhancement. Weights live inside the APK,
 * loaded through AssetManager by the already-pinned sherpa-onnx runtime.
 * No external app, socket, cache file, download or separate model installation.
 *
 * The owning OfflineVoiceEngine serial worker owns this instance. Every JNI
 * failure, bad output and clean/no-speech window falls back to original PCM.
 */
internal class PrivateGtcrnDenoiser(private val context: Context) {
    private var native: OfflineSpeechDenoiser? = null
    private var unavailable = false

    fun warm() {
        if (unavailable || native != null) return
        runCatching {
            check(context.assets.list(ASSET_DIR)?.contains(MODEL_FILE) == true) {
                "The bundled GTCRN model was not packaged"
            }
            OfflineSpeechDenoiser(
                assetManager = context.assets,
                config = OfflineSpeechDenoiserConfig(
                    model = OfflineSpeechDenoiserModelConfig(
                        gtcrn = OfflineSpeechDenoiserGtcrnModelConfig(model = "$ASSET_DIR/$MODEL_FILE"),
                        numThreads = 1,
                        provider = "cpu",
                    ),
                ),
            )
        }.onSuccess { native = it }.onFailure {
            // Keep the original voice recorder/Parakeet functional on devices
            // where enhancement is unsupported; do not log private audio.
            unavailable = true
            release()
        }
    }

    /** Returns a fresh enhanced array or null to use unmodified input. */
    fun enhanceIfNoisy(samples: FloatArray): FloatArray? {
        if (!VoiceNoiseProfile.shouldDenoise(samples)) {
            VoiceDenoiseMetrics.bypassed()
            return null
        }
        if (unavailable) {
            VoiceDenoiseMetrics.fallback()
            return null
        }
        warm()
        val model = native ?: run {
            VoiceDenoiseMetrics.fallback()
            return null
        }
        val started = System.nanoTime()
        return runCatching {
            val result = model.run(samples, PrivatePcmBuffer.SAMPLE_RATE)
            val enhanced = result.samples
            if (result.sampleRate != PrivatePcmBuffer.SAMPLE_RATE ||
                !VoiceNoiseProfile.validEnhancement(samples, enhanced)) {
                if (enhanced !== samples) enhanced.fill(0f)
                VoiceDenoiseMetrics.fallback()
                null
            } else {
                VoiceDenoiseMetrics.enhanced((System.nanoTime() - started) / 1_000_000)
                enhanced
            }
        }.getOrElse {
            unavailable = true
            VoiceDenoiseMetrics.fallback()
            release()
            null
        }
    }

    fun release() {
        runCatching { native?.release() }
        native = null
    }

    companion object {
        private const val ASSET_DIR = "voice"
        private const val MODEL_FILE = "gtcrn_simple.onnx"
    }
}
