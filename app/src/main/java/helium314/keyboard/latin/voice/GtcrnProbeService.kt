// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.os.Handler
import android.os.Looper
import android.os.Process
import com.k2fsa.sherpa.onnx.OfflineSpeechDenoiser
import com.k2fsa.sherpa.onnx.OfflineSpeechDenoiserConfig
import com.k2fsa.sherpa.onnx.OfflineSpeechDenoiserGtcrnModelConfig
import com.k2fsa.sherpa.onnx.OfflineSpeechDenoiserModelConfig
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Child process only. A bad native model may abort this process; it must
 * never bring down the IME or compete with typing outside explicit testing.
 */
class GtcrnProbeService : Service() {
    private val launched = AtomicBoolean(false)

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!launched.compareAndSet(false, true)) return START_NOT_STICKY
        // Hard deadline even if native ONNX inference deadlocks on a device.
        // A timeout kills only :brr_voice_probe, not LatinIME or a call.
        Handler(Looper.getMainLooper()).postDelayed({
            VoiceNativeCrashProbe.stage(this, "FAILED", "TIMEOUT")
            Process.killProcess(Process.myPid())
        }, MAX_PROBE_DURATION_MS)
        Thread({
            Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
            try {
                if (assets.list("voice")?.contains("gtcrn_simple.onnx") != true) {
                    VoiceNativeCrashProbe.stage(this, "FAILED", "MISSING_MODEL")
                    return@Thread
                }
                VoiceNativeCrashProbe.stage(this, "LOADING_MODEL")
                var model: OfflineSpeechDenoiser? = null
                try {
                    model = OfflineSpeechDenoiser(
                        assetManager = assets,
                        config = OfflineSpeechDenoiserConfig(
                            model = OfflineSpeechDenoiserModelConfig(
                                gtcrn = OfflineSpeechDenoiserGtcrnModelConfig(
                                    model = "voice/gtcrn_simple.onnx",
                                ),
                                numThreads = 1,
                                provider = "cpu",
                            ),
                        ),
                    )
                    VoiceNativeCrashProbe.stage(this, "RUNNING_SYNTHETIC")
                    val input = FloatArray(16_000) { i ->
                        // Deterministic nonpersonal PCM: never opens a microphone.
                        if (i % 200 < 100) 0.01f else -0.01f
                    }
                    val output = model.run(input, 16_000)
                    val valid = output.sampleRate == 16_000 && output.samples.size == input.size &&
                        output.samples.all { it.isFinite() }
                    input.fill(0f)
                    output.samples.fill(0f)
                    VoiceNativeCrashProbe.stage(this,
                        if (valid) "PASSED" else "FAILED",
                        if (valid) "NONE" else "INVALID_OUTPUT")
                } finally {
                    runCatching { model?.release() }
                }
            } catch (_: Throwable) {
                // Deliberately discard exception messages and stack traces;
                // they can contain paths or private runtime state.
                VoiceNativeCrashProbe.stage(this, "FAILED", "MODEL_EXCEPTION")
            } finally {
                stopSelf(startId)
                // Immediate native-memory retirement. This kills only the
                // separately declared :brr_voice_probe process, never LatinIME.
                Process.killProcess(Process.myPid())
            }
        }, "brrBoard-one-shot-native-probe").apply { isDaemon = true }.start()
        return START_NOT_STICKY
    }

    companion object { private const val MAX_PROBE_DURATION_MS = 20_000L }
}
