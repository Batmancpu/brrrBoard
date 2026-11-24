// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Bundle
import android.os.Handler
import android.os.HandlerThread
import android.os.IBinder
import android.os.Message
import android.os.Messenger
import android.os.SystemClock
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit

/** Blocking only on the private capture worker, never on AudioRecord or main. */
internal class BrrBrainStreamBridge(private val context: Context, private val cancelled: () -> Boolean) {
    private val token = SystemClock.elapsedRealtimeNanos().coerceAtLeast(1L)
    private val connected = CountDownLatch(1)
    @Volatile private var remote: Messenger? = null
    @Volatile private var lost = false
    @Volatile private var closed = false
    @Volatile private var expectedId = 0
    private var bound = false
    private var sequence = 0
    private data class Response(val id: Int, val block: BrrBrainScoredBlock?)
    private val responses = ArrayBlockingQueue<Response>(1)
    private val thread = HandlerThread("brrBrain-stream-replies").apply { start() }
    private val reply = Messenger(Handler(thread.looper) { message ->
        val bytes = message.data.getByteArray(BrrBrainStreamWire.CLEAN)
        val a = message.data.getFloatArray(BrrBrainStreamWire.RAW_SCORE)
        val b = message.data.getFloatArray(BrrBrainStreamWire.CLEAN_SCORE)
        val result = if (bytes != null && a != null && b != null)
            BrrBrainScoredBlock(bytes, a, b)
        else { bytes?.fill(0); a?.fill(0f); b?.fill(0f); null }
        synchronized(responses) {
            if (closed || message.what != BrrBrainStreamWire.RESULT ||
                message.data.getLong(BrrBrainStreamWire.TOKEN) != token ||
                message.arg1 != expectedId || !responses.offer(Response(message.arg1, result))) {
                result?.wipe()
            }
        }
        true
    })
    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            if (!closed && binder != null) remote = Messenger(binder) else lost = true
            connected.countDown()
        }
        override fun onServiceDisconnected(name: ComponentName?) { lost = true; connected.countDown() }
        override fun onBindingDied(name: ComponentName?) { lost = true; connected.countDown() }
        override fun onNullBinding(name: ComponentName?) { lost = true; connected.countDown() }
    }
    fun process(raw: ByteArray, input: ByteArray): BrrBrainScoredBlock? {
        if (closed || cancelled()) return null
        try {
            if (!bound) {
                bound = context.bindService(Intent(context, BrrBrainStreamService::class.java),
                    connection, Context.BIND_AUTO_CREATE)
                if (!bound) return null
                val deadline = SystemClock.elapsedRealtime() + 3500L
                while (!connected.await(25, TimeUnit.MILLISECONDS)) {
                    if (cancelled() || lost || SystemClock.elapsedRealtime() >= deadline) return null
                }
            }
            val target = remote ?: return null
            if (lost || cancelled()) return null
            val id = ++sequence
            expectedId = id
            target.send(Message.obtain(null, BrrBrainStreamWire.BLOCK).apply {
                arg1 = id
                replyTo = reply
                data = Bundle().apply {
                    putLong(BrrBrainStreamWire.TOKEN, token)
                    putByteArray(BrrBrainStreamWire.RAW, raw)
                    putByteArray(BrrBrainStreamWire.INPUT, input)
                }
            })
            val deadline = SystemClock.elapsedRealtime() + if (id == 1) 4500L else 1500L
            while (!cancelled() && !lost && SystemClock.elapsedRealtime() < deadline) {
                val response = responses.poll(25, TimeUnit.MILLISECONDS) ?: continue
                if (response.id == id) return response.block
                response.block?.wipe()
            }
        } catch (_: Exception) { /* failure is transparent raw fallback */ }
        return null
    }
    fun close(abort: Boolean) {
        if (closed) return
        closed = true
        runCatching {
            remote?.send(Message.obtain(null, if (abort) BrrBrainStreamWire.CANCEL else BrrBrainStreamWire.CLOSE).apply {
                data = Bundle().apply { putLong(BrrBrainStreamWire.TOKEN, token) }
            })
        }
        if (bound) runCatching { context.unbindService(connection) }
        bound = false; remote = null
        synchronized(responses) {
            while (true) { val response = responses.poll() ?: break; response.block?.wipe() }
        }
        thread.quitSafely()
    }
}
