// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.utils

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import android.os.SystemClock
import android.view.View
import helium314.keyboard.keyboard.EmojiPanelKeyMorph
import helium314.keyboard.keyboard.KeyboardView
import helium314.keyboard.latin.BuildConfig
import helium314.keyboard.latin.KeyboardMotionSpec
import helium314.keyboard.latin.R

/**
 * Bounded RAM-only diagnostic trace of keyboard-owned UI geometry.
 * No editor identity, text, glyphs, screenshots, input events, clipboard or audio.
 * Capture only real draw traversals in a short window following Emoji transitions.
 */
object TransitionDiagnostics {
    enum class Event { ENTER_BROWSE, ENTER_SEARCH, EXIT_BROWSE, EXIT_SEARCH, EXIT_EMOJI,
        PANEL_COMMITTED, MORPH_OVERLAY, MORPH_RELEASE }
    private const val CAPACITY = 96
    private const val WINDOW_MS = 650L
    private const val MAX_FRAMES = 48
    private val trace = ArrayDeque<String>(CAPACITY)
    private var startMs = -1L
    private var previousFrame = -1L
    private var frames = 0
    private var measures = 0
    private var serial = 0L

    @JvmStatic @Synchronized fun begin(event: Event) {
        val now = SystemClock.uptimeMillis()
        // Direct Search begins in the switcher, then enters the panel in the
        // same traversal. Preserve both events in the same capture window.
        if (event == Event.ENTER_SEARCH && startMs >= 0 && now - startMs < 40 &&
            frames == 0 && trace.lastOrNull()?.contains("ENTER_SEARCH") == true) return
        startMs = now
        previousFrame = -1L
        frames = 0
        measures = 0
        serial++
        add("transition=$serial start=${event.name}")
    }

    @JvmStatic @Synchronized fun event(event: Event) {
        val now = SystemClock.uptimeMillis()
        if (active(now)) add("t=${now - startMs}ms event=${event.name}")
    }

    @JvmStatic @Synchronized fun measure() {
        if (active(SystemClock.uptimeMillis())) measures++
    }

    private fun active(now: Long) = startMs >= 0 && now - startMs <= WINDOW_MS
    private fun add(line: String) {
        if (trace.size >= CAPACITY) trace.removeFirst()
        trace.addLast(line)
    }

    /** Values from fixed view IDs only, never text, content descriptions or drawables. */
    private val views = arrayOf(
        "typing" to R.id.keyboard_view, "emoji" to R.id.emoji_palettes_view,
        "bottom" to R.id.bottom_row_keyboard, "strip" to R.id.strip_container,
        "tabs" to R.id.emoji_tab_strip, "bar" to R.id.emoji_search_bar_container,
        "back" to R.id.emoji_search_back_button, "search" to R.id.emoji_search_icon,
        "clear" to R.id.emoji_search_clear_button, "expand" to R.id.emoji_search_expand_button,
        "results" to R.id.emoji_search_results_scroll, "footer" to R.id.board_footer,
        "abc" to R.id.board_footer_globe, "mic" to R.id.board_footer_mic
    )

    @JvmStatic @Synchronized fun frame(root: View) {
        val now = SystemClock.uptimeMillis()
        if (!active(now) || frames >= MAX_FRAMES) return
        val gap = if (previousFrame < 0) 0 else now - previousFrame
        previousFrame = now
        frames++
        val state = buildString {
            append("t=${now - startMs}ms draw=$frames gap=${gap}ms measure=$measures ")
            append("root=${root.width}x${root.height} hw=${root.isHardwareAccelerated} ")
            append("overlay=${EmojiPanelKeyMorph.overlayActive()}")
            for ((name, id) in views) {
                val v = root.findViewById<View>(id) ?: continue
                append(" $name=${if (v.isShown) 1 else 0}/${(v.alpha * 100).toInt()}")
                append("@${v.left},${v.top},${v.width},${v.height}")
                if (v is KeyboardView) {
                    append("/hideS=${if (v.isMorphSearchHidden) 1 else 0}")
                    append("/hideD=${if (v.isMorphDeleteHidden) 1 else 0}")
                }
            }
        }
        add(state)
    }

    @Synchronized fun traceText(): String =
        if (trace.isEmpty()) "No recent Emoji transition captured. Reproduce the issue before opening About."
        else trace.joinToString("\n")

    fun reportText(context: Context): String = buildString {
        appendLine("brrBoard diagnostic report v1")
        appendLine("app=${BuildConfig.VERSION_NAME} android_api=${Build.VERSION.SDK_INT}")
        appendLine("device=${Build.MANUFACTURER.take(64)} ${Build.MODEL.take(64)}")
        appendLine("motion_enabled=${KeyboardMotionSpec.enabled()} ios_profile=${KeyboardMotionSpec.isIos27(context)}")
        appendLine("density=${context.resources.displayMetrics.density} orientation=${context.resources.configuration.orientation}")
        appendLine("Draw gaps are app traversal intervals, NOT physical display FPS.")
        appendLine("Fields: shown/alpha_percent@left,top,width,height; hideS/hideD=native key suppression.")
        appendLine("--- latest voice timing (RAM only; -1 = unavailable) ---")
        val stop = helium314.keyboard.latin.voice.VoiceStopMetrics.snapshot()
        val voice = helium314.keyboard.latin.voice.VoiceDenoiseMetrics.snapshot()
        appendLine("voice_startup_ms=${helium314.keyboard.latin.voice.VoiceDenoiseMetrics.lastMicrophoneStartupMs()}")
        appendLine("stop_to_capture_release_ms=${stop.captureReleaseMs} stop_to_first_drain_partial_ms=${stop.firstDrainPartialMs}")
        appendLine("stop_to_result_ms=${stop.resultReadyMs} stop_to_commit_dispatch_return_ms=${stop.commitDispatchReturnedMs}")
        appendLine("voice_chunks=${voice.streamingChunks} last_chunk_ms=${voice.lastChunkLatencyMs} queued_chunks=${voice.queuedChunks}")
        appendLine("denoise_last_ms=${voice.lastEnhancementMs} enhanced=${voice.enhancedWindows} fallback=${voice.fallbacks}")
        appendLine("--- recent transitions (RAM only) ---")
        appendLine(traceText())
        appendLine("--- safe crash and configuration ---")
        appendLine(SafeCrashReport.copyText(context))
    }

    internal fun reportClip(text: String): ClipData =
        ClipData.newPlainText("brrBoard diagnostic report", text).apply {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                description.extras = PersistableBundle().apply {
                    putBoolean("android.content.extra.IS_SENSITIVE", true)
                }
            }
        }

    fun copyReport(context: Context): Boolean {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager ?: return false
        clipboard.setPrimaryClip(reportClip(reportText(context)))
        return true
    }
}
