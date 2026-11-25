// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.brain

import android.content.Context
import android.os.Build
import android.os.UserManager
import android.util.AtomicFile
import helium314.keyboard.latin.settings.Settings
import helium314.keyboard.latin.utils.prefs
import java.io.File
import java.nio.ByteBuffer
import java.util.concurrent.Executors
import java.util.concurrent.ScheduledFuture
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/** Bounded category counts only, encrypted in CE no-backup storage. No content or app identities. */
object SmartPasteLearning {
    private const val MAGIC = 0x53504C31 // SPL1, enum order is part of this version
    private val counts = IntArray(SmartPastePolicy.Kind.values().size * SmartPastePolicy.Destination.values().size * 2)
    private val worker by lazy { Executors.newSingleThreadScheduledExecutor() }
    private val generation = AtomicLong()
    private var context: Context? = null
    private var loaded = false
    private var loading = false
    private var readOnly = false
    private var pending: ScheduledFuture<*>? = null
    private fun index(kind: SmartPastePolicy.Kind, dest: SmartPastePolicy.Destination) =
        (kind.ordinal * SmartPastePolicy.Destination.values().size + dest.ordinal) * 2

    private fun available(c: Context): Boolean =
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.N && !c.isDeviceProtectedStorage &&
            c.getSystemService(UserManager::class.java)?.isUserUnlocked == true
    private fun enabled(c: Context) = c.prefs().getBoolean(Settings.PREF_KEY_USE_PERSONALIZED_DICTS, true)
    private fun file(c: Context) = AtomicFile(File(c.noBackupFilesDir, "smart_paste_learning_v1.enc"))

    @Synchronized fun initialize(c: Context) {
        val app = c.applicationContext
        if (context == null) context = app
        if (loaded || loading || readOnly || !available(app) || !enabled(app)) return
        loading = true
        val token = generation.get()
        worker.execute {
            var plain: ByteArray? = null
            try {
                if (!available(app) || !enabled(app) || token != generation.get()) return@execute
                val target = file(app)
                val values = if (target.baseFile.exists() || File(target.baseFile.path + ".bak").exists()) {
                    val sealed = target.readFully()
                    if (sealed.size > 4096) throw IllegalStateException("record too large")
                    plain = BrainLearningVault.open(sealed)
                    val data = ByteBuffer.wrap(plain)
                    if (data.remaining() != (counts.size+1)*4 || data.int != MAGIC)
                        throw IllegalStateException("invalid record")
                    IntArray(counts.size) { data.int.also { n -> if (n !in 0..32) throw IllegalStateException("invalid counter") } }
                } else IntArray(counts.size)
                synchronized(this) {
                    if (token == generation.get()) {
                        values.copyInto(counts)
                        loaded = true
                    }
                }
                values.fill(0)
            } catch (_: Exception) {
                synchronized(this) {
                    if (token == generation.get()) { readOnly = true; counts.fill(0) }
                }
            } finally {
                plain?.fill(0)
                synchronized(this) { loading = false }
            }
        }
    }

    @Synchronized fun bias(kind: SmartPastePolicy.Kind, dest: SmartPastePolicy.Destination): Int {
        if (kind == SmartPastePolicy.Kind.OTP || dest == SmartPastePolicy.Destination.OTP) return 0
        val i = index(kind,dest); val a = counts[i]; val d = counts[i+1]
        return if (a+d < 4) 0 else ((a-d)*5/(a+d+4)).coerceIn(-5,5)
    }

    @Synchronized fun record(kind: SmartPastePolicy.Kind, dest: SmartPastePolicy.Destination, accepted: Boolean) {
        if (kind == SmartPastePolicy.Kind.OTP || dest == SmartPastePolicy.Destination.OTP ||
            readOnly || (context != null && !loaded)) return
        val i = index(kind,dest)
        if (counts[i]+counts[i+1] >= 32) { counts[i] /= 2; counts[i+1] /= 2 }
        counts[i + if (accepted) 0 else 1]++
        val app = context ?: return // pure JVM tests, or transient before initialization
        pending?.cancel(false)
        val token = generation.get()
        pending = worker.schedule({ persist(app, token) }, 500L, TimeUnit.MILLISECONDS)
    }

    private fun persist(c: Context, token: Long) {
        if (token != generation.get() || !available(c) || !enabled(c)) return
        val data = synchronized(this) { counts.clone() }
        val plain = ByteBuffer.allocate((data.size+1)*4).putInt(MAGIC).apply { data.forEach { putInt(it) } }.array()
        data.fill(0)
        try {
            val sealed = BrainLearningVault.seal(plain)
            if (token != generation.get() || !available(c) || !enabled(c)) return
            val target = file(c)
            val stream = target.startWrite()
            try {
                stream.write(sealed)
                if (token == generation.get() && available(c) && enabled(c)) target.finishWrite(stream)
                else target.failWrite(stream)
            } catch (_: Exception) { target.failWrite(stream) }
        } catch (_: Exception) {
            // No plaintext fallback, no diagnostic payload.
        } finally { plain.fill(0) }
    }

    /** Stop scheduled writes at privacy/opt-out transitions; preserve earlier eligible aggregates. */
    @JvmStatic @Synchronized fun pause() {
        generation.incrementAndGet()
        pending?.cancel(false)
        pending = null
    }

    @JvmStatic @Synchronized fun reset() {
        pause()
        counts.fill(0)
        val app = context ?: return
        val token = generation.get()
        loaded = false
        loading = true
        worker.execute {
            var success = false
            try {
                if (available(app) && token == generation.get()) {
                    val target = file(app)
                    target.delete()
                    success = !target.baseFile.exists() && !File(target.baseFile.path + ".bak").exists()
                }
            } catch (_: Exception) {
            } finally {
                synchronized(this) {
                    if (token == generation.get()) { loaded = success; readOnly = !success }
                    loading = false
                }
            }
        }
    }

    @JvmStatic @Synchronized fun reset(c: Context) {
        context = c.applicationContext
        reset()
    }
}
