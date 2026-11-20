// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.util.AtomicFile
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * Opt-in ONE blue-microphone diagnostic session. The entire stored record is
 * fixed protocol codes + relative milliseconds; never PCM, speech, typed
 * words, error strings, input fields, apps, devices or exception stack traces.
 * Calls occur at major milestones only, never for each audio frame or keystroke.
 */
internal object VoiceSessionCrashTrail {
    private const val FILE_NAME = "voice-session-one-shot-v1.state"
    private const val MAX_STEPS = 20
    private val lock = Any()
    private var lastExitScanFrom = -1L
    private var lastExitScanAt = 0L
    private var cachedExit: String? = null
    private val codes = setOf(
        "REQUESTED", "MIC_INITIALIZING", "MIC_READY", "MIC_CAPTURE_STOPPED",
        "NOISE_ACTIVE", "NOISE_OFF", "NOISE_UNAVAILABLE", "NOISE_CALL_BLOCKED",
        "ASR_LOADING", "ASR_READY", "ASR_DECODING", "ASR_DECODED",
        "RESULT_CHECK", "COMPLETE", "CANCELLED", "FAILED",
    )
    private val terminal = setOf("COMPLETE", "CANCELLED", "FAILED")

    private data class Entry(val code: String, val elapsedMs: Long)
    private data class State(val startEpochMs: Long, val active: Boolean, val entries: List<Entry>)
    data class Snapshot(
        val message: String, val armed: Boolean, val finished: Boolean, val canErase: Boolean,
    )

    fun arm(context: Context): Boolean = synchronized(lock) {
        if (read(context) != null) return@synchronized false
        write(context, State(0L, false, emptyList()))
    }

    /** Ignored if no user armed a test. Never starts audio itself. */
    fun begin(context: Context) = synchronized(lock) {
        val old = read(context) ?: return@synchronized
        if (old.active || old.startEpochMs != 0L) return@synchronized
        val now = System.currentTimeMillis()
        write(context, State(now, true, listOf(Entry("REQUESTED", 0L))))
    }

    fun note(context: Context, code: String) = synchronized(lock) {
        if (code !in codes) return@synchronized
        val old = read(context) ?: return@synchronized
        if (!old.active || old.entries.lastOrNull()?.code?.let { it in terminal } == true) return@synchronized
        if (old.entries.size >= MAX_STEPS) return@synchronized
        val ms = (System.currentTimeMillis() - old.startEpochMs).coerceIn(0L, 600_000L)
        write(context, old.copy(
            active = code !in terminal,
            entries = old.entries + Entry(code, ms),
        ))
    }

    fun snapshot(context: Context): Snapshot {
        val state = synchronized(lock) { read(context) }
            ?: return Snapshot("Not armed. Arm one test, then use the blue mic normally.", false, false, false)
        if (state.startEpochMs == 0L) return Snapshot(
            "Armed. Your next blue-mic session will record only fixed processing stages and timings.",
            true, false, true)

        val stages = state.entries.joinToString(" → ") { "${it.code} (+${it.elapsedMs}ms)" }
        val reason = if (state.active) findProcessExit(context, state.startEpochMs) else null
        val exit = if (reason != null) "\nAndroid keyboard exit: $reason" else ""
        val stale = state.active && System.currentTimeMillis() - state.startEpochMs > 180_000L
        val status = if (state.active && !stale) "Last recorded stage; session may still be running or have exited."
            else if (stale) "No final callback was recorded; the app or mic session may have stopped."
            else "One-shot check finished."
        val canErase = !state.active || reason != null || stale
        return Snapshot("$status\n$stages$exit", true, canErase, canErase)
    }

    fun copy(context: Context): Boolean {
        val report = snapshot(context)
        if (!report.armed) return false
        return runCatching {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText(
                "brrBoard private voice stages", "brrBoard blue-mic one-shot diagnostic:\n${report.message}"))
            true
        }.getOrDefault(false)
    }

    fun erase(context: Context) = synchronized(lock) {
        // If a session is still processing, do not re-arm another diagnostic.
        val old = read(context) ?: return@synchronized
        if (!old.active || findProcessExit(context, old.startEpochMs) != null ||
            System.currentTimeMillis() - old.startEpochMs > 180_000L) {
            AtomicFile(file(context)).delete()
        }
    }

    private fun findProcessExit(context: Context, from: Long): String? = synchronized(lock) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || from <= 0L) return@synchronized null
        val now = System.currentTimeMillis()
        if (from == lastExitScanFrom && now - lastExitScanAt in 0L..10_000L) {
            return@synchronized cachedExit
        }
        val result = runCatching {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            manager.getHistoricalProcessExitReasons(context.packageName, 0, 12)
                .filter { it.processName == context.packageName && it.timestamp >= from }
                .maxByOrNull { it.timestamp }?.let { exit ->
                    val reason = when (exit.reason) {
                        ApplicationExitInfo.REASON_CRASH_NATIVE -> "NATIVE_CRASH"
                        ApplicationExitInfo.REASON_CRASH -> "JAVA_CRASH"
                        ApplicationExitInfo.REASON_ANR -> "ANR"
                        ApplicationExitInfo.REASON_LOW_MEMORY -> "LOW_MEMORY"
                        ApplicationExitInfo.REASON_EXIT_SELF -> "EXIT_SELF"
                        ApplicationExitInfo.REASON_SIGNALED -> "SIGNALED"
                        else -> "OTHER"
                    }
                    "$reason (status=${exit.status})"
                }
        }.getOrNull()
        lastExitScanFrom = from
        lastExitScanAt = now
        cachedExit = result
        result
    }

    private fun file(context: Context) = File(context.noBackupFilesDir, FILE_NAME)

    private fun read(context: Context): State? = runCatching {
        val line = AtomicFile(file(context)).openRead().bufferedReader(StandardCharsets.UTF_8)
            .use { it.readLine() } ?: return@runCatching null
        if (line.length > 1_024) return@runCatching null
        val parts = line.split('|')
        if (parts.size != 3 || parts[0] != "V1") return@runCatching null
        val epoch = parts[1].toLongOrNull()?.takeIf { it >= 0L } ?: return@runCatching null
        if (parts[2] == "ARMED" && epoch == 0L) return@runCatching State(0L, false, emptyList())
        if (epoch == 0L) return@runCatching null
        val entries = parts[2].split(',').map { encoded ->
            val fields = encoded.split('@')
            if (fields.size != 2 || fields[0] !in codes) return@runCatching null
            val ms = fields[1].toLongOrNull()?.takeIf { it in 0L..600_000L }
                ?: return@runCatching null
            Entry(fields[0], ms)
        }
        if (entries.isEmpty() || entries.size > MAX_STEPS) return@runCatching null
        State(epoch, entries.last().code !in terminal, entries)
    }.getOrNull()

    private fun write(context: Context, state: State): Boolean = runCatching {
        val file = AtomicFile(file(context))
        val status = if (state.startEpochMs == 0L) "ARMED"
            else state.entries.joinToString(",") { "${it.code}@${it.elapsedMs}" }
        val bytes = "V1|${state.startEpochMs}|$status\n".toByteArray(StandardCharsets.UTF_8)
        val stream = file.startWrite()
        try {
            stream.write(bytes)
            file.finishWrite(stream)
        } catch (failure: Exception) {
            file.failWrite(stream)
            throw failure
        } finally {
            bytes.fill(0)
        }
        true
    }.getOrDefault(false)
}
