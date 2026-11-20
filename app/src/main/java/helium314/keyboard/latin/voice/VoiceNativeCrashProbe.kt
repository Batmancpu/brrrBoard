// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import android.app.ActivityManager
import android.app.ApplicationExitInfo
import android.media.AudioManager
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.AtomicFile
import java.io.File
import java.nio.charset.StandardCharsets

/**
 * Single-use, opt-in, offline native GTCRN crash check. The real keyboard
 * microphone/Parakeet path is NEVER changed or touched by this probe.
 *
 * Persist only fixed protocol codes and a timestamp in noBackupFilesDir.
 * No microphone, typing, transcript, app, contact, trace or exception message
 * enters this file. Android's raw tombstone is deliberately never read.
 */
internal object VoiceNativeCrashProbe {
    private const val FILE_NAME = "voice-gtcrn-one-shot-v1.state"
    private const val PROBE_PROCESS = ":brr_voice_probe"
    private val stages = setOf("REQUESTED", "LOADING_MODEL", "RUNNING_SYNTHETIC", "PASSED", "FAILED", "START_FAILED")
    private val details = setOf("NONE", "MISSING_MODEL", "MODEL_EXCEPTION", "INVALID_OUTPUT", "SERVICE_ERROR", "UNAVAILABLE", "TIMEOUT")
    private val lock = Any()

    data class Snapshot(val message: String, val started: Boolean, val finished: Boolean)

    private data class State(val stage: String, val time: Long, val detail: String)

    fun start(context: Context): Boolean = synchronized(lock) {
        if (read(context) != null) return@synchronized false // One-shot until explicit erasure.
        // Do not compete with a WhatsApp/Instagram/phone call for CPU or audio.
        val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (audio.mode == AudioManager.MODE_IN_CALL ||
            audio.mode == AudioManager.MODE_IN_COMMUNICATION) return@synchronized false
        val timestamp = System.currentTimeMillis()
        if (!write(context, State("REQUESTED", timestamp, "NONE"))) return@synchronized false
        val started = runCatching {
            context.startService(Intent(context, GtcrnProbeService::class.java)) != null
        }.getOrDefault(false)
        if (!started) write(context, State("START_FAILED", timestamp, "SERVICE_ERROR"))
        started
    }

    /** Called exclusively by the short-lived, separate Android process. */
    fun stage(context: Context, stage: String, detail: String = "NONE") = synchronized(lock) {
        if (stage !in stages || detail !in details) return@synchronized
        val prior = read(context) ?: return@synchronized
        write(context, State(stage, prior.time, detail))
    }

    fun snapshot(context: Context): Snapshot {
        val s = synchronized(lock) { read(context) }
            ?: return Snapshot("Not run. One tap tests the bundled GTCRN model with generated audio in a separate process.", false, false)
        if (s.stage == "PASSED") return Snapshot(
            "PASS · GTCRN loaded and processed synthetic samples. This does NOT prove real microphone accuracy or device stability.", true, true)
        if (s.stage == "FAILED" || s.stage == "START_FAILED") return Snapshot(
            "FAILED · stage=${s.stage} detail=${s.detail}. No private data collected.", true, true)

        // Android 11+ records the exit type for our own processes. We do NOT
        // request the raw stack trace, native tombstone or process description.
        val exit = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) runCatching {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            manager.getHistoricalProcessExitReasons(context.packageName, 0, 12)
                .filter { it.processName?.endsWith(PROBE_PROCESS) == true && it.timestamp >= s.time }
                .maxByOrNull { it.timestamp }
        }.getOrNull() else null
        if (exit != null) return Snapshot(
            "PROCESS EXIT · last stage=${s.stage}, reason=${reasonCode(exit.reason)}, status=${exit.status}. " +
                "No recording or raw tombstone was accessed.", true, true)
        if (System.currentTimeMillis() - s.time > 45_000L) return Snapshot(
            "NO RESULT · last stage=${s.stage}; process-exit reason unavailable. This can mean a timeout or OS termination, not necessarily a native crash.", true, true)
        return Snapshot("Running once · last stage=${s.stage}. Please wait; the keyboard is unaffected.", true, false)
    }

    /** Read-only, one-time snapshot of a *previous* keyboard process death.
     * This can reveal the v1.0.195 incident without reproducing it.
     * No private tombstone, exit description, process memory or typed content.
     */
    fun recentKeyboardExit(context: Context): String {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return "Android exit categories unavailable before Android 11."
        return runCatching {
            val manager = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
            val record = manager.getHistoricalProcessExitReasons(context.packageName, 0, 20)
                .filter { it.processName == context.packageName }
                .filter { it.reason == ApplicationExitInfo.REASON_CRASH_NATIVE ||
                    it.reason == ApplicationExitInfo.REASON_CRASH ||
                    it.reason == ApplicationExitInfo.REASON_ANR ||
                    it.reason == ApplicationExitInfo.REASON_LOW_MEMORY }
                .filter { System.currentTimeMillis() - it.timestamp in 0L..604_800_000L }
                .maxByOrNull { it.timestamp }
            if (record == null) "No recent keyboard crash category available in Android history."
            else "Recent keyboard process exit: reason=${reasonCode(record.reason)}, status=${record.status}. Stage inside old releases is unknown."
        }.getOrDefault("Android exit history unavailable on this device.")
    }

    fun copyReport(context: Context): Boolean {
        val report = snapshot(context)
        if (!report.started) return false
        return runCatching {
            val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            clipboard.setPrimaryClip(ClipData.newPlainText(
                "brrBoard one-shot GTCRN result", "brrBoard private GTCRN probe v1: " + report.message + "\n" + recentKeyboardExit(context)))
            true
        }.getOrDefault(false)
    }

    /** Explicit user erasure. No automatic retry or recurring collection. */
    fun erase(context: Context) = synchronized(lock) {
        if (snapshot(context).finished) {
            runCatching { AtomicFile(file(context)).delete() }
        }
    }

    private fun reasonCode(reason: Int): String = when (reason) {
        ApplicationExitInfo.REASON_CRASH_NATIVE -> "NATIVE_CRASH"
        ApplicationExitInfo.REASON_CRASH -> "JAVA_CRASH"
        ApplicationExitInfo.REASON_LOW_MEMORY -> "LOW_MEMORY"
        ApplicationExitInfo.REASON_ANR -> "ANR"
        ApplicationExitInfo.REASON_SIGNALED -> "SIGNALED"
        ApplicationExitInfo.REASON_EXIT_SELF -> "EXIT_SELF"
        ApplicationExitInfo.REASON_USER_REQUESTED -> "USER_STOP"
        else -> "OTHER"
    }

    private fun file(context: Context) = File(context.noBackupFilesDir, FILE_NAME)

    private fun read(context: Context): State? = runCatching {
        val text = AtomicFile(file(context)).openRead().bufferedReader(StandardCharsets.UTF_8).use {
            it.readLine()
        } ?: return@runCatching null
        val fields = text.split('|')
        if (fields.size != 3) return@runCatching null
        val stage = fields[0].takeIf { it in stages } ?: return@runCatching null
        val timestamp = fields[1].toLongOrNull()?.takeIf { it > 0 } ?: return@runCatching null
        val detail = fields[2].takeIf { it in details } ?: return@runCatching null
        State(stage, timestamp, detail)
    }.getOrNull()

    private fun write(context: Context, state: State): Boolean = runCatching {
        val atomic = AtomicFile(file(context))
        val bytes = "${state.stage}|${state.time}|${state.detail}\n".toByteArray(StandardCharsets.UTF_8)
        val stream = atomic.startWrite()
        try {
            stream.write(bytes)
            atomic.finishWrite(stream)
        } catch (failure: Exception) {
            atomic.failWrite(stream)
            throw failure
        } finally {
            bytes.fill(0)
        }
        true
    }.getOrDefault(false)
}
