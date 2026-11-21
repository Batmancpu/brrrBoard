// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import android.app.Service
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.Process
import com.k2fsa.sherpa.onnx.OfflineSpeechDenoiser
import com.k2fsa.sherpa.onnx.OfflineSpeechDenoiserConfig
import com.k2fsa.sherpa.onnx.OfflineSpeechDenoiserGtcrnModelConfig
import com.k2fsa.sherpa.onnx.OfflineSpeechDenoiserModelConfig
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Non-exported, throwaway child PROCESS. Native sherpa/ONNX can SIGABRT here
 * without killing LatinIME. Only PCM from an explicitly started capture enters
 * Binder memory; no microphone, Internet, file, Android effect or persistent log.
 */
class GtcrnVoiceService : Service() {
    private val started = AtomicBoolean(false)
    private val inbound = Messenger(Handler(Looper.getMainLooper()) { message ->
        if (message.what != GtcrnVoiceWire.REQUEST || !started.compareAndSet(false, true)) {
            true
        } else {
            val caller = message.replyTo
            val pcm = message.data.getByteArray(GtcrnVoiceWire.PCM)
            Handler(Looper.getMainLooper()).postDelayed({
                Process.killProcess(Process.myPid()) // child process only
            }, GtcrnVoiceWire.CHILD_TIMEOUT_MS)
            Thread({ runRequest(pcm, caller) }, "brrBoard-gtcrn-isolated").apply {
                isDaemon = true
            }.start()
            true
        }
    })

    override fun onBind(intent: Intent?): IBinder = inbound.binder

    private fun runRequest(pcm: ByteArray?, caller: Messenger?) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
        var samples: FloatArray? = null
        var result: FloatArray? = null
        var denoiser: OfflineSpeechDenoiser? = null
        var response: ByteArray? = null
        try {
            if (caller != null && pcm != null && VoiceNoiseChoice.eligible(pcm.size) &&
                assets.list("voice")?.contains("gtcrn_simple.onnx") == true) {
                val input = FloatArray(pcm.size / 2) { index ->
                    val offset = index * 2
                    (((pcm[offset + 1].toInt() shl 8) or
                        (pcm[offset].toInt() and 0xff)).toShort().toFloat() / 32768f)
                }
                samples = input
                val model = OfflineSpeechDenoiser(assetManager = assets,
                    config = OfflineSpeechDenoiserConfig(
                        model = OfflineSpeechDenoiserModelConfig(
                            gtcrn = OfflineSpeechDenoiserGtcrnModelConfig(
                                model = "voice/gtcrn_simple.onnx"),
                            numThreads = 1, provider = "cpu")))
                denoiser = model
                val nativeResult = model.run(input, 16_000)
                val output = nativeResult.samples
                result = output
                if (nativeResult.sampleRate == 16_000 &&
                    VoiceNoiseProfile.validEnhancement(input, output)) {
                    response = ByteArray(pcm.size)
                    for (index in output.indices) {
                        val value = (output[index].coerceIn(-1f, 1f) * 32767f).toInt()
                        response[index * 2] = value.toByte()
                        response[index * 2 + 1] = (value shr 8).toByte()
                    }
                }
            }
        } catch (_: Throwable) {
            // No error messages, native paths or audio are ever logged/stored.
        } finally {
            runCatching { denoiser?.release() }
            samples?.fill(0f)
            result?.fill(0f)
            pcm?.fill(0)
            try {
                if (caller != null) {
                    val reply = Message.obtain(null, GtcrnVoiceWire.RESULT)
                    reply.data = Bundle().apply {
                        if (response != null) putByteArray(GtcrnVoiceWire.PCM, response)
                    }
                    caller.send(reply)
                }
            } catch (_: Exception) {
                // Parent will take its timeout/disconnect raw-PCM fallback.
            } finally {
                response?.fill(0)
                // Release JNI memory promptly. This process owns no keyboard UI.
                Process.killProcess(Process.myPid())
            }
        }
    }
}

internal object GtcrnVoiceWire {
    const val REQUEST = 1
    const val RESULT = 2
    const val PCM = "pcm"
    const val CHILD_TIMEOUT_MS = 15_000L
    const val PARENT_TIMEOUT_MS = 17_000L
}
