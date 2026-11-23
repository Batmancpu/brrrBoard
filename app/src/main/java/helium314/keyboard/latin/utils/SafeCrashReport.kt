// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.utils

import android.content.Context
import android.Manifest
import android.os.Build
import android.util.AtomicFile
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import helium314.keyboard.latin.voice.UltraVoiceModelManifest
import helium314.keyboard.latin.voice.VoiceModelChoice
import helium314.keyboard.latin.voice.VoiceModelInstaller
import helium314.keyboard.latin.voice.VoiceModelManifest

/** Stores only crash type and stack frames. Exception messages and user content are never recorded. */
object SafeCrashReport {
    private const val FILE_NAME = "safe_crash_report.v1"
    private const val MAX_REPORT_BYTES = 12 * 1024
    private const val MAX_FRAMES = 24

    fun install(context: Context) {
        val previous = Thread.getDefaultUncaughtExceptionHandler()
        if (previous is RecordingHandler) return
        Thread.setDefaultUncaughtExceptionHandler(RecordingHandler(context.applicationContext, previous))
    }

    fun copyText(context: Context): String {
        val crash = runCatching {
            val file = AtomicFile(reportFile(context))
            val bytes = file.openRead().use { input -> readBounded(input, MAX_REPORT_BYTES) }
            try {
                String(bytes, Charsets.UTF_8)
            } finally {
                bytes.fill(0)
            }
        }.getOrElse { "No uncaught app crash has been captured since this install." }
        val liveState = runCatching {
            val active = VoiceModelChoice.active(context)?.id ?: "none"
            val toolbar = getEnabledToolbarKeys(context.prefs()).joinToString(",") { it.name }
            val biometricPermission = if (context.checkSelfPermission(Manifest.permission.USE_BIOMETRIC) ==
                android.content.pm.PackageManager.PERMISSION_GRANTED) "granted" else "missing"
            "voice_active=$active\n" +
                "voice_110m_verified=${VoiceModelInstaller.isInstalled(context, VoiceModelManifest.pack)}\n" +
                "voice_ultra_verified=${VoiceModelInstaller.isInstalled(context, UltraVoiceModelManifest.pack)}\n" +
                "toolbar_visible_keys=$toolbar\n" +
                "biometric_permission=$biometricPermission"
        }.getOrDefault("live_state=unavailable")
        return "$crash\n\ncurrent_safe_state\n$liveState"
    }

    internal fun format(
        timestampMs: Long,
        appVersion: String,
        api: Int,
        manufacturer: String,
        model: String,
        threadName: String,
        error: Throwable,
    ): String = buildString {
        appendLine("brrBoard safe crash report v1")
        appendLine("timestamp_ms=$timestampMs")
        appendLine("app_version=${safeField(appVersion)}")
        appendLine("android_api=$api")
        appendLine("device=${safeField(manufacturer)}_${safeField(model)}")
        appendLine("thread=${safeField(threadName)}")
        var cause: Throwable? = error
        var depth = 0
        while (cause != null && depth < 4) {
            appendLine("exception_${depth + 1}=${cause.javaClass.name}")
            cause.stackTrace.take(MAX_FRAMES).forEach { frame ->
                val file = safeField(frame.fileName ?: "unknown")
                appendLine(" at ${frame.className}.${frame.methodName}($file:${frame.lineNumber})")
            }
            cause = cause.cause
            depth++
        }
    }.take(MAX_REPORT_BYTES).trimEnd()

    private fun safeField(value: String) = value.replace(Regex("[^A-Za-z0-9_.:$-]"), "_").take(96)

    private fun readBounded(input: FileInputStream, limit: Int): ByteArray {
        val output = java.io.ByteArrayOutputStream()
        val buffer = ByteArray(1024)
        while (output.size() < limit) {
            val count = input.read(buffer, 0, minOf(buffer.size, limit - output.size()))
            if (count <= 0) break
            output.write(buffer, 0, count)
        }
        buffer.fill(0)
        return output.toByteArray()
    }

    private fun reportFile(context: Context): File {
        val storage = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            context.createDeviceProtectedStorageContext() else context
        return File(storage.noBackupFilesDir, FILE_NAME)
    }

    private fun write(context: Context, report: String) {
        val target = AtomicFile(reportFile(context))
        var output: FileOutputStream? = null
        try {
            output = target.startWrite()
            output.write(report.toByteArray(Charsets.UTF_8))
            output.fd.sync()
            target.finishWrite(output)
            output = null
        } finally {
            if (output != null) target.failWrite(output)
        }
    }

    private class RecordingHandler(
        private val context: Context,
        private val previous: Thread.UncaughtExceptionHandler?,
    ) : Thread.UncaughtExceptionHandler {
        override fun uncaughtException(thread: Thread, error: Throwable) {
            runCatching {
                val version = runCatching {
                    @Suppress("DEPRECATION")
                    context.packageManager.getPackageInfo(context.packageName, 0).versionName ?: "unknown"
                }.getOrDefault("unknown")
                write(context, format(
                    System.currentTimeMillis(), version, Build.VERSION.SDK_INT,
                    Build.MANUFACTURER, Build.MODEL, thread.name, error,
                ))
            }
            previous?.uncaughtException(thread, error)
                ?: android.os.Process.killProcess(android.os.Process.myPid())
        }
    }
}
