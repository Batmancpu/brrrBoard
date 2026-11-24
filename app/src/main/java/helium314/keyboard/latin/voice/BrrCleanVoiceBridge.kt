// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.Message
import android.os.Messenger
import android.os.SystemClock
import java.util.concurrent.atomic.AtomicBoolean

/** One bounded request; native failure can never crash LatinIME or lose its raw PCM. */
internal class BrrCleanVoiceBridge(
    private val context: Context,
    private val onDone: (BrrCleanResult?) -> Unit,
) {
    private val handler = Handler(Looper.getMainLooper())
    private val finished = AtomicBoolean(false)
    private val started = SystemClock.elapsedRealtime()
    private var bound = false
    private var remote: Messenger? = null
    private var reply: Messenger? = null
    private var pending: ByteArray? = null
    private val timeout = Runnable { complete(null) }
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            if (finished.get()) return
            try {
                if (binder == null) { complete(null); return }
                val message = Message.obtain(null, BrrCleanWire.REQUEST)
                message.replyTo = reply
                message.data = Bundle().apply { putByteArray(BrrCleanWire.PCM, pending) }
                remote = Messenger(binder).also { it.send(message) }
            } catch (_: Exception) { complete(null) }
            finally { pending?.fill(0); pending = null }
        }

        override fun onServiceDisconnected(name: ComponentName?) { complete(null) }
        override fun onBindingDied(name: ComponentName?) { complete(null) }
        override fun onNullBinding(name: ComponentName?) { complete(null) }
    }

    fun run(pcm: ByteArray): Boolean {
        if (finished.get() || !VoiceNoiseChoice.eligible(pcm.size)) return false
        pending = pcm.copyOf()
        reply = Messenger(Handler(Looper.getMainLooper()) { message ->
            if (message.what == BrrCleanWire.RESULT) {
                complete(message.data.getByteArray(BrrCleanWire.PCM)?.let {
                    BrrCleanResult(it, message.data.getBoolean(BrrCleanWire.RAW_SPEECH))
                })
            }
            true
        })
        handler.postDelayed(timeout, BrrCleanWire.PARENT_TIMEOUT_MS)
        return try {
            bound = context.bindService(Intent(context, BrrCleanVoiceService::class.java),
                connection, Context.BIND_AUTO_CREATE)
            if (!bound) complete(null)
            bound
        } catch (_: Exception) { complete(null); false }
    }

    fun cancel() { complete(null, notify = false) }

    private fun complete(result: BrrCleanResult?, notify: Boolean = true) {
        val bytes = result?.pcm
        if (!finished.compareAndSet(false, true)) {
            bytes?.fill(0)
            return
        }
        handler.removeCallbacks(timeout)
        pending?.fill(0)
        pending = null
        // Owner cancel/timeout kills the child, so stale native work cannot
        // continue eating CPU while the user has resumed normal typing.
        if (!notify || bytes == null) runCatching {
            remote?.send(Message.obtain(null, BrrCleanWire.CANCEL))
        }
        remote = null
        if (bound) runCatching { context.unbindService(connection) }
        bound = false
        reply = null
        if (notify) {
            if (bytes == null) VoiceDenoiseMetrics.fallback()
            else VoiceDenoiseMetrics.enhanced(SystemClock.elapsedRealtime() - started)
            onDone(result)
        } else bytes?.fill(0)
    }
}
