// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.content.Context
import android.media.AudioFormat
import android.media.AudioDeviceInfo
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.audiofx.NoiseSuppressor
import android.os.Handler
import android.os.Build
import android.os.Looper
import android.os.ParcelFileDescriptor
import helium314.keyboard.latin.voice.VoiceCaptureNoiseReduction
import helium314.keyboard.latin.voice.VoiceDenoiseMetrics
import helium314.keyboard.latin.voice.VoiceSessionCrashTrail
import java.io.IOException
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Foreground IME-owned microphone source for brrSpeak's EXTRA_AUDIO_SOURCE receiver.
 *
 * The keyboard owns both the permission and AudioRecord. brrSpeak only reads PCM from the
 * other end of the pipe; it must never open another microphone in this mode. A separate
 * worker owns the pipe writer, so service backpressure cannot block keyboard touch input.
 */
internal class KeyboardPcmAudioSource(
    private val context: Context,
    private val onRecordingStarted: () -> Unit,
    private val onCaptureFailure: () -> Unit,
    private val onPcmFrame: ((ByteArray, Int) -> Unit)? = null,
    private val onCaptureFinished: (() -> Unit)? = null,
    private val toBrrSpeak: Boolean = true,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val wanted = AtomicBoolean(false)
    private val launched = AtomicBoolean(false)
    private val closed = AtomicBoolean(false)
    private val pipe = if (toBrrSpeak) ParcelFileDescriptor.createPipe() else null
    val readEnd: ParcelFileDescriptor
        get() = requireNotNull(pipe) { "Offline-only recording must never open a remote pipe" }[0]
    private val writeEnd: ParcelFileDescriptor? = pipe?.get(1)
    private val outgoingFrames = if (toBrrSpeak) ArrayBlockingQueue<ByteArray>(96) else null
    private val endOfStream = ByteArray(0)
    private val writerStarted = AtomicBoolean(false)
    private val endQueued = AtomicBoolean(false)
    @Volatile private var recorder: AudioRecord? = null

    /** Call only after brrSpeak signals readiness, to avoid filling the pipe while it boots. */
    fun start() {
        if (closed.get() || !launched.compareAndSet(false, true)) return
        wanted.set(true)
        Thread({
            var microphone: AudioRecord? = null
            var suppression: NoiseSuppressor? = null
            var audioFocus: VoiceCaptureAudioFocus? = null
            try {
                if (!wanted.get() || closed.get()) return@Thread
                // Capture must outrank background model initialization and inference.
                if (!toBrrSpeak) runCatching {
                    android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO)
                }
                if (!toBrrSpeak && VoiceCaptureNoiseReduction.callIsActive(context)) {
                    throw IOException("Microphone unavailable during a call")
                }
                if (!toBrrSpeak) VoiceSessionCrashTrail.note(context, "MIC_INITIALIZING")
                val minBytes = AudioRecord.getMinBufferSize(
                    SAMPLE_RATE_HZ, AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT,
                )
                if (minBytes <= 0) throw IOException("Unsupported microphone PCM format")
                val bufferSize = maxOf(minBytes * 2, 4_096)
                microphone = AudioRecord.Builder()
                    .setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                    .setAudioFormat(AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(SAMPLE_RATE_HZ)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .build())
                    .setBufferSizeInBytes(bufferSize)
                    .build()
                if (microphone.state != AudioRecord.STATE_INITIALIZED) {
                    throw IOException("Microphone did not initialize")
                }
                recorder = microphone
                if (!toBrrSpeak) {
                    // Normal blue-mic dictation always prefers the phone's own
                    // microphone array. Bluetooth communication routing is
                    // deliberately confined to the explicit BR Brain lab.
                    val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    val phone = audio.getDevices(AudioManager.GET_DEVICES_INPUTS)
                        .firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_MIC }
                    val requested = phone != null && microphone.setPreferredDevice(phone)
                    VoiceDenoiseMetrics.inputRoute(if (requested)
                        "Phone microphone preferred (Android route not yet verified)"
                        else "Phone preference unavailable; Android controls route")
                    suppression = VoiceCaptureNoiseReduction.attach(context, microphone)
                    val noiseStage = if (!VoiceCaptureNoiseReduction.enabled(context)) "NOISE_OFF"
                        else if (suppression != null) "NOISE_ACTIVE" else "NOISE_UNAVAILABLE"
                    VoiceSessionCrashTrail.note(context, noiseStage)
                }
                if (!wanted.get() || closed.get()) return@Thread
                if (!toBrrSpeak && VoiceCaptureNoiseReduction.callIsActive(context)) {
                    throw IOException("Call took microphone ownership")
                }
                audioFocus = VoiceCaptureAudioFocus.acquire(context)
                if (!wanted.get() || closed.get()) return@Thread
                microphone.startRecording()
                if (microphone.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                    throw IOException("Microphone could not start")
                }
                if (!toBrrSpeak && Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val audio = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                    val actual = runCatching {
                        audio.activeRecordingConfigurations.firstOrNull {
                            it.clientAudioSessionId == microphone.audioSessionId
                        }
                    }.getOrNull()
                    if (actual?.isClientSilenced == true) throw IOException("Android silenced microphone")
                    val type = actual?.audioDevice?.type
                    if (type != null && type != AudioDeviceInfo.TYPE_BUILTIN_MIC)
                        throw IOException("Android routed offline dictation away from phone mic")
                    if (type == AudioDeviceInfo.TYPE_BUILTIN_MIC)
                        VoiceDenoiseMetrics.inputRoute("Phone microphone (Android confirmed)")
                }
                if (!toBrrSpeak) VoiceDenoiseMetrics.blueCaptureStarted()
                startPipeWriter()
                mainHandler.post {
                    if (wanted.get() && !closed.get()) onRecordingStarted()
                }
                // Keep the hardware buffer roomy, but deliver offline PCM in 40 ms frames.
                val samples = ByteArray(if (toBrrSpeak) bufferSize else SAMPLE_RATE_HZ * 2 * 40 / 1000)
                try {
                    val startedAt = android.os.SystemClock.elapsedRealtime()
                    var nextCallCheck = startedAt + 2_000L
                    while (wanted.get() && !closed.get() &&
                        android.os.SystemClock.elapsedRealtime() - startedAt < MAX_CAPTURE_MS) {
                        val now = android.os.SystemClock.elapsedRealtime()
                        if (!toBrrSpeak && now >= nextCallCheck) {
                            nextCallCheck = now + 2_000L
                            if (VoiceCaptureNoiseReduction.callIsActive(context))
                                throw IOException("Call claimed the microphone during dictation")
                        }
                        val count = microphone.read(samples, 0, samples.size, AudioRecord.READ_BLOCKING)
                        if (count < 0) {
                            if (!wanted.get() || closed.get()) break
                            throw IOException("AudioRecord read failed: $count")
                        }
                        // Keep the final completed audio buffer when the user taps Stop during read.
                        if (count > 0 && !closed.get()) {
                            // Fan out once. The remote pipe has its own bounded writer; slow
                            // brrSpeak reads cannot stall AudioRecord or the local PCM backup.
                            onPcmFrame?.invoke(samples, count)
                            if (outgoingFrames != null &&
                                !outgoingFrames.offer(samples.copyOf(count))) {
                                throw IOException("brrSpeak pipe writer stopped draining PCM")
                            }
                        }
                    }
                } finally {
                    samples.fill(0)
                    finishPipeWriter()
                }
            } catch (_: Exception) {
                if (wanted.getAndSet(false) && !closed.get()) {
                    mainHandler.post { if (!closed.get()) onCaptureFailure() }
                }
            } finally {
                finishPipeWriter()
                runCatching {
                    if (microphone?.recordingState == AudioRecord.RECORDSTATE_RECORDING)
                        microphone.stop()
                }
                runCatching { suppression?.release() }
                runCatching { microphone?.release() }
                audioFocus?.close()
                if (!toBrrSpeak) VoiceDenoiseMetrics.blueCaptureStopped()
                if (recorder === microphone) recorder = null
                if (!closed.get()) mainHandler.post {
                    if (!closed.get()) onCaptureFinished?.invoke()
                }
            }
        }, "brrBoard-pcm-microphone").apply { isDaemon = true }.start()
    }

    private fun startPipeWriter() {
        val queue = outgoingFrames ?: return
        val descriptor = writeEnd ?: return
        if (!writerStarted.compareAndSet(false, true)) return
        Thread({
            try {
                ParcelFileDescriptor.AutoCloseOutputStream(descriptor).use { output ->
                    while (!closed.get()) {
                        val frame = queue.take()
                        if (frame === endOfStream) break
                        try { output.write(frame) } finally { frame.fill(0) }
                    }
                }
            } catch (_: Exception) {
                if (wanted.getAndSet(false) && !closed.get()) {
                    mainHandler.post { if (!closed.get()) onCaptureFailure() }
                }
            } finally {
                while (true) {
                    val pending = queue.poll() ?: break
                    pending.fill(0)
                }
                runCatching { descriptor.close() }
            }
        }, "brrBoard-pcm-pipe-writer").apply { isDaemon = true }.start()
    }

    private fun finishPipeWriter() {
        val queue = outgoingFrames ?: return
        if (!endQueued.compareAndSet(false, true)) return
        if (!writerStarted.get()) {
            runCatching { writeEnd?.close() }
            return
        }
        // On normal EOF the writer must flush every completed buffer before closing. If the
        // receiver stalls so badly that the queue is full, fail this server path explicitly.
        if (!queue.offer(endOfStream)) runCatching { writeEnd?.close() }
    }

    /** Natural end of speech: stop capture and let writer's close signal EOF. No stopListening(). */
    fun stop() {
        wanted.set(false)
        val mic = recorder ?: return
        // ColorOS may synchronously wait for a blocking AudioRecord.read() to unwind.
        // The mic footer must stay responsive while capture finishes on its own thread.
        Thread({
            runCatching {
                if (mic.recordingState == AudioRecord.RECORDSTATE_RECORDING) mic.stop()
            }
        }, "brrBoard-stop-private-mic").apply { isDaemon = true }.start()
    }

    /** Cancellation, IME dismissal or stale session: close both endpoints immediately. */
    fun close() {
        if (!closed.compareAndSet(false, true)) return
        stop()
        runCatching { writeEnd?.close() }
        runCatching { pipe?.get(0)?.close() }
        outgoingFrames?.let { queue ->
            while (true) {
                val pending = queue.poll() ?: break
                pending.fill(0)
            }
        }
    }

    companion object {
        const val SAMPLE_RATE_HZ = 16_000
        const val CHANNEL_COUNT = 1
        const val MAX_CAPTURE_MS = 170_000L // brrSpeak has a 180-second absolute session cap.
    }
}
