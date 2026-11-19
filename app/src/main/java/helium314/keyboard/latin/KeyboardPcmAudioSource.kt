// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.os.ParcelFileDescriptor
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Foreground IME-owned microphone source for brrSpeak's EXTRA_AUDIO_SOURCE receiver.
 *
 * The keyboard owns both the permission and AudioRecord. brrSpeak only reads PCM from the
 * other end of the pipe; it must never open another microphone in this mode. A separate
 * worker owns the pipe writer, so service backpressure cannot block keyboard touch input.
 */
internal class KeyboardPcmAudioSource(
    private val onRecordingStarted: () -> Unit,
    private val onCaptureFailure: () -> Unit,
) {
    private val mainHandler = Handler(Looper.getMainLooper())
    private val wanted = AtomicBoolean(false)
    private val launched = AtomicBoolean(false)
    private val closed = AtomicBoolean(false)
    private val pipe = ParcelFileDescriptor.createPipe()
    val readEnd: ParcelFileDescriptor = pipe[0]
    private val writeEnd: ParcelFileDescriptor = pipe[1]
    @Volatile private var recorder: AudioRecord? = null

    /** Call only after brrSpeak signals readiness, to avoid filling the pipe while it boots. */
    fun start() {
        if (closed.get() || !launched.compareAndSet(false, true)) return
        wanted.set(true)
        Thread({
            var microphone: AudioRecord? = null
            try {
                if (!wanted.get() || closed.get()) return@Thread
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
                if (!wanted.get() || closed.get()) return@Thread
                microphone.startRecording()
                if (microphone.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                    throw IOException("Microphone could not start")
                }
                mainHandler.post {
                    if (wanted.get() && !closed.get()) onRecordingStarted()
                }
                ParcelFileDescriptor.AutoCloseOutputStream(writeEnd).use { output ->
                    val samples = ByteArray(bufferSize)
                    val startedAt = android.os.SystemClock.elapsedRealtime()
                    while (wanted.get() && !closed.get() &&
                        android.os.SystemClock.elapsedRealtime() - startedAt < MAX_CAPTURE_MS) {
                        val count = microphone.read(samples, 0, samples.size, AudioRecord.READ_BLOCKING)
                        if (count < 0) {
                            if (!wanted.get() || closed.get()) break
                            throw IOException("AudioRecord read failed: $count")
                        }
                        // Keep the final completed audio buffer when the user taps Stop during read.
                        if (count > 0 && !closed.get()) output.write(samples, 0, count)
                    }
                }
            } catch (_: Exception) {
                if (wanted.getAndSet(false) && !closed.get()) {
                    mainHandler.post { if (!closed.get()) onCaptureFailure() }
                }
            } finally {
                runCatching { writeEnd.close() } // EOF makes brrSpeak transcribe the full PCM stream.
                runCatching {
                    if (microphone?.recordingState == AudioRecord.RECORDSTATE_RECORDING)
                        microphone.stop()
                }
                runCatching { microphone?.release() }
                if (recorder === microphone) recorder = null
            }
        }, "brrBoard-pcm-microphone").apply { isDaemon = true }.start()
    }

    /** Natural end of speech: stop capture and let writer's close signal EOF. No stopListening(). */
    fun stop() {
        wanted.set(false)
        runCatching {
            recorder?.let { if (it.recordingState == AudioRecord.RECORDSTATE_RECORDING) it.stop() }
        }
    }

    /** Cancellation, IME dismissal or stale session: close both endpoints immediately. */
    fun close() {
        if (!closed.compareAndSet(false, true)) return
        stop()
        runCatching { writeEnd.close() }
        runCatching { readEnd.close() }
    }

    companion object {
        const val SAMPLE_RATE_HZ = 16_000
        const val CHANNEL_COUNT = 1
        const val MAX_CAPTURE_MS = 170_000L // brrSpeak has a 180-second absolute session cap.
    }
}
