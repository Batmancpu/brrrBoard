// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import android.content.Context
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import java.util.ArrayDeque
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.Executors
import java.util.concurrent.RejectedExecutionException

/**
 * One private dictation session: capture thread contributes only bounded PCM
 * chunks, while brrClean and the offline ASR process them sequentially in the
 * background. Optional provisional text is delivered only to the caller's guarded
 * composing-span callback; the pipeline itself never holds an editor connection.
 * All callbacks, IPC and ASR orchestration are serialized on the main looper;
 * no model ever runs there, and nothing is written to storage or sent online.
 */
internal class PrivateVoiceStreamingPipeline(
    private val context: Context,
    private val engine: OfflineVoiceEngine,
    private val mode: VoiceNoiseMode,
    private val onDone: (Result<String>) -> Unit,
    private val onPartialText: (String) -> Unit = {},
) {
    private data class Pending(val pcm: ByteArray, val enqueuedAtMs: Long)

    // Model loading, VAD and semantic inference never block AudioRecord or UI.
    // The recording duration bounds the queued PCM.
    private val captureWorker = Executors.newSingleThreadExecutor { work ->
        Thread({
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)
            work.run()
        }, "brrBoard-private-endpoint").apply { isDaemon = true }
    }
    private val endpoint = PrivateVoiceEndpointDetector(context)
    private val chunker = PrivateVoiceChunker(endpoint::speech, endpoint::thoughtComplete)
    private val handler = Handler(Looper.getMainLooper())
    private val cancelled = AtomicBoolean(false)
    private val pending = ArrayDeque<Pending>() // accessed only on main looper
    private val recognized = ArrayList<String>() // volatile memory only
    private var ended = false
    private var busy = false
    private var activeRaw: ByteArray? = null
    private var activeBridge: BrrCleanVoiceBridge? = null
    private var activeDecode: VoiceDecodeRequest? = null
    private var activeStarted = 0L

    /** Never wait for inference on AudioRecord's capture thread. */
    fun append(source: ByteArray, count: Int) {
        if (cancelled.get()) return
        if (count == 0) return
        val frame = source.copyOf(count)
        try {
            captureWorker.execute {
                try {
                    if (!cancelled.get()) chunker.append(frame, frame.size, ::enqueueChunk)
                } catch (_: Exception) {
                    handler.post { complete(Result.failure(IllegalStateException("Offline audio batching failed"))) }
                } finally {
                    frame.fill(0)
                }
            }
        } catch (_: RejectedExecutionException) { frame.fill(0) }
    }

    /** Called only after AudioRecord has stopped and yielded its last frame. */
    fun finishCapture() {
        if (cancelled.get()) return
        try {
            captureWorker.execute {
                try {
                    if (!cancelled.get()) chunker.finish(::enqueueChunk)
                    handler.post {
                        if (!cancelled.get()) {
                            ended = true
                            pump()
                        }
                    }
                } catch (_: Exception) {
                    handler.post { complete(Result.failure(IllegalStateException("Offline audio batching failed"))) }
                } finally {
                    chunker.discard()
                    endpoint.close()
                }
            }
        } catch (_: RejectedExecutionException) { /* cancellation already owns cleanup */ }
        captureWorker.shutdown()
    }

    fun cancel() {
        if (!cancelled.compareAndSet(false, true)) return
        stopCaptureWorker()
        if (Looper.myLooper() == Looper.getMainLooper()) dispose() else handler.post { dispose() }
    }

    private fun enqueueChunk(chunk: ByteArray) {
        handler.post {
            if (cancelled.get()) chunk.fill(0)
            else {
                pending.addLast(Pending(chunk, SystemClock.elapsedRealtime()))
                VoiceDenoiseMetrics.streamingBacklog(pending.size)
                pump()
            }
        }
    }

    private fun stopCaptureWorker() {
        // Queued frames see cancellation and wipe themselves. The worker alone
        // releases native state after the currently executing inference ends.
        try {
            captureWorker.execute {
                chunker.discard()
                endpoint.close()
            }
        } catch (_: RejectedExecutionException) { /* finishCapture owns cleanup */ }
        captureWorker.shutdown()
    }

    private fun pump() {
        if (cancelled.get() || busy) return
        if (pending.isEmpty()) {
            if (ended) complete(Result.success(
                VoiceTranscriptFormatter.format(VoiceTranscriptJoiner.merge(recognized))))
            return
        }
        busy = true
        val item = pending.removeFirst()
        VoiceDenoiseMetrics.streamingBacklog(pending.size)
        val pcm = item.pcm
        activeRaw = pcm
        activeStarted = item.enqueuedAtMs
        if (mode == VoiceNoiseMode.GTCRN && VoiceNoiseChoice.eligible(pcm.size)) {
            val bridge = BrrCleanVoiceBridge(context) { enhanced ->
                activeBridge = null
                val raw = activeRaw
                activeRaw = null
                if (raw == null || cancelled.get()) {
                    enhanced?.fill(0)
                    raw?.fill(0)
                } else {
                    val accepted = enhanced?.takeIf { it.size == raw.size }
                    if (accepted != null) raw.fill(0) else enhanced?.fill(0)
                    decode(accepted ?: raw)
                }
            }
            activeBridge = bridge
            if (!bridge.run(pcm) && activeBridge === bridge) {
                activeBridge = null
                activeRaw = null
                decode(pcm)
            }
        } else {
            activeRaw = null
            decode(pcm)
        }
    }

    private fun decode(pcm: ByteArray) {
        if (cancelled.get()) { pcm.fill(0); return }
        activeDecode = engine.transcribe(pcm, formatTranscript = false) { result ->
            activeDecode = null
            if (cancelled.get()) return@transcribe
            if (result.isFailure) {
                complete(Result.failure(IllegalStateException("Offline chunk transcription failed")))
            } else {
                recognized += result.getOrNull().orEmpty()
                val partial = VoiceTranscriptFormatter.format(VoiceTranscriptJoiner.merge(recognized))
                if (partial.isNotBlank()) onPartialText(partial)
                VoiceDenoiseMetrics.streamingChunkComplete(
                    SystemClock.elapsedRealtime() - activeStarted)
                busy = false
                pump()
            }
        }
    }

    private fun complete(result: Result<String>) {
        if (!cancelled.compareAndSet(false, true)) return
        stopCaptureWorker()
        dispose()
        onDone(result)
    }

    /** Main looper. Never wipe PCM while its active native decoder is reading. */
    private fun dispose() {
        activeBridge?.cancel()
        activeBridge = null
        activeRaw?.fill(0)
        activeRaw = null
        activeDecode?.cancel()
        activeDecode = null
        while (pending.isNotEmpty()) pending.removeFirst().pcm.fill(0)
        recognized.clear()
        VoiceDenoiseMetrics.streamingBacklog(0)
    }
}
