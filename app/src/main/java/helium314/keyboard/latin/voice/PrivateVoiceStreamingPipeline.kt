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
 * Streaming inference/IPC waits are confined to the private capture worker;
 * editor callbacks and ASR orchestration remain on main, with no model work there.
 */
internal class PrivateVoiceStreamingPipeline(
    private val context: Context,
    private val engine: OfflineVoiceEngine,
    private val mode: VoiceNoiseMode,
    private val onDone: (Result<String>) -> Unit,
    private val onPartialText: (String) -> Unit = {},
) {
    private data class Pending(val pcm: ByteArray, val enhanced: BrrCleanResult?, val enqueuedAtMs: Long)

    // Model loading, VAD and semantic inference never block AudioRecord or UI.
    // The recording duration bounds the queued PCM.
    private val captureWorker = Executors.newSingleThreadExecutor { work ->
        Thread({
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND)
            work.run()
        }, "brrBoard-private-endpoint").apply { isDaemon = true }
    }
    private var modelsPrepared = false // capture worker only
    private var streamThoughtPrepared = false
    private val handler = Handler(Looper.getMainLooper())
    private val cancelled = AtomicBoolean(false)
    private val endpoint = PrivateVoiceEndpointDetector(context)
    private val adaptive = VoiceEndpointPreference.enabled(context)
    // Existing adaptive-batching toggle is a reversible escape hatch:
    // disabled -> original batch denoiser, enabled -> isolated streaming path.
    private val aligned = if (mode == VoiceNoiseMode.GTCRN && adaptive) AlignedVoiceBuffer() else null
    private var streamBridge: BrrBrainStreamBridge? = null // capture worker only
    private val stream = if (aligned != null) BrrBrainBlockAssembler(
        infer = { raw, input ->
            val bridge = streamBridge ?: BrrBrainStreamBridge(context, cancelled::get).also { streamBridge = it }
            val started = SystemClock.elapsedRealtime()
            bridge.process(raw, input).also { result ->
                if (result != null) {
                    VoiceDenoiseMetrics.enhanced(SystemClock.elapsedRealtime()-started)
                    // The child's two VAD states are now ready. Avoid a third
                    // VAD or competing thought-model startup before first PCM.
                    if (!streamThoughtPrepared && !cancelled.get()) {
                        streamThoughtPrepared = true
                        endpoint.prepareThought()
                    }
                }
            }
        },
        failed = {
            VoiceDenoiseMetrics.fallback()
            streamBridge?.close(true)
            streamBridge = null
        },
        emit = ::appendAligned,
    ) else null
    private val chunker = PrivateVoiceChunker(
        activity = { raw, from, to ->
            if (!adaptive) null else aligned?.activity(from, to) ?: endpoint.speech(raw, from, to)
        },
        boundaryPenalty = { from, to -> aligned?.boundaryPenalty(from,to) ?: 0.0 },
        thoughtComplete = { raw, count ->
            if (aligned == null) endpoint.thoughtComplete(raw, count)
            else {
                val protectedPcm = aligned.copyPrefix(count)
                try { endpoint.thoughtComplete(protectedPcm, count) }
                finally { protectedPcm.fill(0) }
            }
        },
    )
    private val pending = ArrayDeque<Pending>() // accessed only on main looper
    private val recognized = ArrayList<String>() // volatile memory only
    private var ended = false
    private var busy = false
    private var activeRaw: ByteArray? = null
    private var activeRecovery: ByteArray? = null
    private var activeBridge: BrrCleanVoiceBridge? = null
    private var activeDecode: VoiceDecodeRequest? = null
    private var activeStarted = 0L

    /** Never wait for inference on AudioRecord's capture thread. */
    fun append(source: ByteArray, count: Int) {
        if (cancelled.get()) return
        require(count >= 0 && count <= source.size && count % 2 == 0)
        if (count == 0) return
        val frame = source.copyOf(count)
        try {
            captureWorker.execute {
                try {
                    if (!cancelled.get()) {
                        if (!modelsPrepared) {
                            modelsPrepared = true
                            // Preserve capture-first behavior from the concurrent
                            // main update. Streaming owns VAD in its isolated child.
                            if (stream == null) endpoint.prepareActivity()
                            if (!cancelled.get()) engine.warm()
                            if (stream == null && !cancelled.get()) endpoint.prepareThought()
                        }
                        if (!cancelled.get()) {
                            if (stream != null) stream.append(frame, frame.size)
                            else chunker.append(frame, frame.size, ::enqueueChunk)
                        }
                    }
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
                    if (!cancelled.get()) {
                        stream?.finish()
                        chunker.finish(::enqueueChunk)
                    }
                    handler.post {
                        if (!cancelled.get()) {
                            ended = true
                            pump()
                        }
                    }
                } catch (_: Exception) {
                    handler.post { complete(Result.failure(IllegalStateException("Offline audio batching failed"))) }
                } finally {
                    closeCapture()
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

    private fun appendAligned(raw: ByteArray, scored: BrrBrainScoredBlock?) {
        try {
            if (!cancelled.get()) {
                checkNotNull(aligned).append(raw, scored)
                chunker.append(raw, raw.size, ::enqueueChunk)
            }
        } finally { raw.fill(0); scored?.wipe() }
    }

    private fun enqueueChunk(chunk: ByteArray) {
        val enhanced = aligned?.take(chunk)
        handler.post {
            if (cancelled.get()) { chunk.fill(0); enhanced?.pcm?.fill(0) }
            else {
                pending.addLast(Pending(chunk, enhanced, SystemClock.elapsedRealtime()))
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
                closeCapture()
            }
        } catch (_: RejectedExecutionException) { /* finishCapture owns cleanup */ }
        captureWorker.shutdown()
    }

    private fun closeCapture() {
        stream?.discard()
        streamBridge?.close(cancelled.get())
        streamBridge = null
        aligned?.clear()
        chunker.discard()
        endpoint.close()
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
        if (item.enhanced != null) {
            activeRaw = null
            decode(item.enhanced.pcm, pcm, item.enhanced.rawSpeechEvidence)
            return
        }
        if (stream != null) {
            // A failed streaming child is disabled for this session. Never
            // repeatedly respawn it or delay capture by a second denoiser.
            activeRaw = null
            decode(pcm)
            return
        }
        if (mode == VoiceNoiseMode.GTCRN && VoiceNoiseChoice.eligible(pcm.size)) {
            val bridge = BrrCleanVoiceBridge(context) { enhanced ->
                activeBridge = null
                val raw = activeRaw
                activeRaw = null
                if (raw == null || cancelled.get()) {
                    enhanced?.pcm?.fill(0)
                    raw?.fill(0)
                } else {
                    val accepted = enhanced?.takeIf { it.pcm.size == raw.size }
                    if (accepted != null) {
                        // Keep original until the protected decode is safely accepted.
                        decode(accepted.pcm, raw, accepted.rawSpeechEvidence)
                    } else {
                        enhanced?.pcm?.fill(0)
                        decode(raw)
                    }
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

    private fun decode(pcm: ByteArray, recovery: ByteArray? = null, retryBlank: Boolean = false) {
        if (cancelled.get()) { pcm.fill(0); recovery?.fill(0); return }
        activeRecovery = recovery
        activeDecode = engine.transcribe(pcm, formatTranscript = false) { result ->
            activeDecode = null
            val original = activeRecovery
            activeRecovery = null
            if (cancelled.get()) { original?.fill(0); return@transcribe }
            // One bounded retry only for failure or unexpectedly empty speech.
            // No routine second ASR pass, and no premature editor insertion.
            if (original != null && (result.isFailure || (retryBlank && result.getOrNull().isNullOrBlank()))) {
                decode(original)
                return@transcribe
            }
            original?.fill(0)
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
        activeRecovery?.fill(0)
        activeRecovery = null
        activeDecode?.cancel()
        activeDecode = null
        while (pending.isNotEmpty()) {
            val item = pending.removeFirst()
            item.pcm.fill(0); item.enhanced?.pcm?.fill(0)
        }
        recognized.clear()
        VoiceDenoiseMetrics.streamingBacklog(0)
    }
}
