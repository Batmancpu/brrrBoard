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
import java.security.MessageDigest
import java.util.concurrent.atomic.AtomicBoolean

/** Fresh native implementation. This process never opens a microphone or a network socket. */
class BrrCleanVoiceService : Service() {
    private val started = AtomicBoolean(false)
    private val handler = Handler(Looper.getMainLooper())
    private val inbound = Messenger(Handler(Looper.getMainLooper()) { message ->
        if (message.what == BrrCleanWire.REQUEST && started.compareAndSet(false, true)) {
            val pcm = message.data.getByteArray(BrrCleanWire.PCM)
            val caller = message.replyTo
            handler.postDelayed({ Process.killProcess(Process.myPid()) }, BrrCleanWire.CHILD_TIMEOUT_MS)
            Thread({ process(pcm, caller) }, "brrClean-native-worker").apply { isDaemon = true }.start()
        }
        true
    })

    override fun onBind(intent: Intent?): IBinder = inbound.binder

    private fun process(pcm: ByteArray?, caller: Messenger?) {
        Process.setThreadPriority(Process.THREAD_PRIORITY_BACKGROUND)
        var weights: ByteArray? = null
        var enhanced: ByteArray? = null
        try {
            if (caller != null && pcm != null && VoiceNoiseChoice.eligible(pcm.size)) {
                val model = assets.open("voice/brrclean_gtcrn.weights").use { it.readBytes() }
                weights = model
                // Detect incomplete or substituted build-time asset before calling C++.
                val digest = MessageDigest.getInstance("SHA-256").digest(model)
                    .joinToString("") { "%02x".format(it) }
                if (digest == EXPECTED_WEIGHTS_SHA256) {
                    val output = BrrCleanNative.process(pcm, model)
                    if (output != null && output.size == pcm.size &&
                        plausibleSpeechEnergy(pcm, output)) enhanced = output
                    else output?.fill(0)
                }
            }
        } catch (_: Throwable) {
            // Native SIGABRT is contained by the process boundary; no PCM or paths are logged.
        } finally {
            weights?.fill(0)
            pcm?.fill(0)
            try {
                if (caller != null) {
                    caller.send(Message.obtain(null, BrrCleanWire.RESULT).apply {
                        data = Bundle().apply { enhanced?.let { putByteArray(BrrCleanWire.PCM, it) } }
                    })
                }
            } catch (_: Exception) {
                // Disconnect/timeout in the parent makes a transparent raw-speech fallback.
            } finally {
                enhanced?.fill(0)
                // Allow the Binder response to flush, then release all native memory.
                handler.postDelayed({ Process.killProcess(Process.myPid()) }, 250L)
            }
        }
    }

    private fun plausibleSpeechEnergy(original: ByteArray, processed: ByteArray): Boolean {
        var before = 0.0
        var after = 0.0
        for (i in original.indices step 2) {
            val a = ((original[i + 1].toInt() shl 8) or (original[i].toInt() and 255)).toShort().toInt()
            val b = ((processed[i + 1].toInt() shl 8) or (processed[i].toInt() and 255)).toShort().toInt()
            before += a.toDouble() * a
            after += b.toDouble() * b
        }
        return before <= 1.0 || after >= before * 0.0025
    }

    companion object {
        private const val EXPECTED_WEIGHTS_SHA256 =
            "4db48ff819e5ac18f5ccad1dfdce0968dde1a209c0cbbf9f24ba9dc4d9f48ded"
    }
}

internal object BrrCleanWire {
    const val REQUEST = 1
    const val RESULT = 2
    const val PCM = "pcm"
    const val CHILD_TIMEOUT_MS = 8_000L
    const val PARENT_TIMEOUT_MS = 9_000L
}
