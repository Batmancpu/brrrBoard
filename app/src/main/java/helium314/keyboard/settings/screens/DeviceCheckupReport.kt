// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.settings.screens

import android.content.Context
import android.content.pm.PackageInfo
import android.os.Build
import helium314.keyboard.latin.voice.VoiceNoiseLab
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** Volatile, metrics-only bridge from the Voice Lab back to the checkup page. */
internal object DeviceCheckupSession {
    data class ModelMetrics(
        val werPercent: Int?,
        val loadMs: Long,
        val decodeMs: Long,
        val totalMs: Long,
        val wallMs: Long,
        val peakPssKb: Long,
    )
    data class VoiceMetrics(
        val audioDurationMs: Int,
        val firstDecoded: String,
        val current110m: ModelMetrics?,
        val ultra: ModelMetrics?,
    )

    private var active = false
    private var startedAt = 0L
    val startTimeMs: Long get() = if (active) startedAt else 0L
    var voiceMetrics by mutableStateOf<VoiceMetrics?>(null)
        private set

    fun beginIfNeeded(nowMs: Long = System.currentTimeMillis()) {
        if (!active || nowMs - startedAt > MAX_SESSION_AGE_MS) {
            active = true
            startedAt = nowMs
            voiceMetrics = null
        }
    }

    fun record(comparison: VoiceNoiseLab.ModelComparison, audioDurationMs: Int) {
        if (!active) return
        fun safe(score: VoiceNoiseLab.ModelScore?) = score?.let {
            ModelMetrics(
                werPercent = it.wordErrorPercent?.coerceIn(0, 10_000),
                loadMs = it.modelLoadMs.coerceIn(0, MAX_METRIC_MS),
                decodeMs = it.decodeMs.coerceIn(0, MAX_METRIC_MS),
                totalMs = it.totalMs.coerceIn(0, MAX_METRIC_MS),
                wallMs = it.wallMs.coerceIn(0, MAX_METRIC_MS),
                peakPssKb = it.peakPssKb.coerceIn(0, MAX_PSS_KB),
            )
        }
        voiceMetrics = VoiceMetrics(
            audioDurationMs = audioDurationMs.coerceIn(0, MAX_AUDIO_MS),
            firstDecoded = if (comparison.firstDecoded == "Ultra 0.6B") "ultra" else "110m",
            current110m = safe(comparison.current110m),
            ultra = safe(comparison.ultra),
        )
    }

    fun finish() {
        active = false
        startedAt = 0L
        voiceMetrics = null
    }

    private const val MAX_SESSION_AGE_MS = 30 * 60 * 1000L
    private const val MAX_METRIC_MS = 24 * 60 * 60 * 1000L
    private const val MAX_PSS_KB = 16L * 1024L * 1024L
    private const val MAX_AUDIO_MS = 12_000
}

internal enum class DeviceCheckOutcome(val token: String) {
    PENDING("pending"),
    GOOD("good"),
    ISSUE("issue"),
    SKIPPED("skipped"),
}

/** Builds a strict plain-text allowlist. It accepts no free-form diagnostic strings. */
internal object DeviceCheckupReport {
    data class CheckResults(
        val emojiGap: DeviceCheckOutcome,
        val smartEmojiAndMotion: DeviceCheckOutcome,
        val toolbar: DeviceCheckOutcome,
        val clipboardGlass: DeviceCheckOutcome,
        val voiceLab: DeviceCheckOutcome,
    ) {
        fun complete() = listOf(emojiGap, smartEmojiAndMotion, toolbar, clipboardGlass, voiceLab)
            .none { it == DeviceCheckOutcome.PENDING }
    }

    fun format(
        context: Context,
        results: CheckResults,
        voice: DeviceCheckupSession.VoiceMetrics?,
        freshGeometry: String?,
    ): String {
        val display = context.resources.displayMetrics
        val appInfo = runCatching {
            val info = context.packageManager.getPackageInfo(context.packageName, 0)
            val versionCode = versionCode(info)
            "${safe(info.versionName ?: "unknown", 32)}($versionCode)"
        }.getOrDefault("unknown")
        val manufacturer = safe(Build.MANUFACTURER, 40)
        val model = safe(Build.MODEL, 48)
        val release = safe(Build.VERSION.RELEASE ?: "unknown", 24)
        val orientation = when (context.resources.configuration.orientation) {
            android.content.res.Configuration.ORIENTATION_LANDSCAPE -> "landscape"
            android.content.res.Configuration.ORIENTATION_PORTRAIT -> "portrait"
            else -> "unknown"
        }
        val timestamp = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.ROOT).apply {
            timeZone = TimeZone.getTimeZone("UTC")
        }.format(Date())
        return buildString(16_384) {
            appendLine("brrBoard device checkup v1")
            appendLine("timestamp_utc=$timestamp")
            appendLine("device=${manufacturer}_$model")
            appendLine("android_release=$release")
            appendLine("android_api=${Build.VERSION.SDK_INT}")
            appendLine("app_version=$appInfo")
            appendLine("screen_px=${display.widthPixels}x${display.heightPixels}")
            appendLine("density_dpi=${display.densityDpi}")
            appendLine("density=${"%.2f".format(Locale.US, display.density)}")
            appendLine("orientation=$orientation")
            appendLine("check_emoji_gap=${results.emojiGap.token}")
            appendLine("check_smart_emoji_suggestion_motion=${results.smartEmojiAndMotion.token}")
            appendLine("check_toolbar=${results.toolbar.token}")
            appendLine("check_clipboard_glass=${results.clipboardGlass.token}")
            appendLine("check_voice_ab=${results.voiceLab.token}")
            appendLine("voice_ab_metrics=${if (voice == null) "not_captured" else "captured"}")
            voice?.let { metrics ->
                appendLine("voice_audio_duration_ms=${metrics.audioDurationMs}")
                appendLine("voice_first_decoded=${metrics.firstDecoded}")
                appendModel("voice_110m", metrics.current110m)
                appendModel("voice_ultra", metrics.ultra)
            }
            if (freshGeometry.isNullOrBlank()) {
                appendLine("emoji_geometry=not_captured")
            } else {
                appendLine("emoji_geometry_begin")
                appendLine(freshGeometry.trim())
                appendLine("emoji_geometry_end")
            }
        }.take(MAX_REPORT_CHARS)
    }

    private fun StringBuilder.appendModel(prefix: String, metrics: DeviceCheckupSession.ModelMetrics?) {
        appendLine("${prefix}_result=${if (metrics == null) "unavailable" else "captured"}")
        if (metrics == null) return
        appendLine("${prefix}_wer_percent=${metrics.werPercent ?: "unknown"}")
        appendLine("${prefix}_load_ms=${metrics.loadMs}")
        appendLine("${prefix}_decode_ms=${metrics.decodeMs}")
        appendLine("${prefix}_total_ms=${metrics.totalMs}")
        appendLine("${prefix}_wall_ms=${metrics.wallMs}")
        appendLine("${prefix}_peak_pss_kb=${metrics.peakPssKb}")
    }

    private fun safe(value: String, max: Int): String = value
        .replace(Regex("[^A-Za-z0-9._-]"), "_")
        .take(max)
        .ifBlank { "unknown" }

    @Suppress("DEPRECATION")
    private fun versionCode(info: PackageInfo): Long =
        if (Build.VERSION.SDK_INT >= 28) info.longVersionCode else info.versionCode.toLong()

    private const val MAX_REPORT_CHARS = 16_384
}
