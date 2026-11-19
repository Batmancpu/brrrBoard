// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.content.Context
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import helium314.keyboard.latin.utils.Log
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.util.UUID
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Foreground IME microphone -> a finished private PCM16 file.
 *
 * No pipe, network, editor text or clipboard is involved. AudioRecord and every disk write run
 * on a worker; the main thread only receives lifecycle callbacks. A stopped recording is handed
 * off as an already-finalized file, which can be opened read-only for the brrSpeak Binder receiver.
 */
internal class KeyboardRecordedAudioSource(
    context: Context,
    private val onRecordingStarted: () -> Unit,
    private val onRecordingFinished: (File, Long) -> Unit,
    private val onCaptureFailure: (Failure) -> Unit,
) {
    enum class Failure { INITIALIZE, START, READ, WRITE, EMPTY, STOP }

    private val main = Handler(Looper.getMainLooper())
    private val launched = AtomicBoolean(false)
    private val wanted = AtomicBoolean(false)
    private val cancelled = AtomicBoolean(false)
    private val finished = AtomicBoolean(false)
    private val audioFile = File(context.cacheDir, "brrboard-voice-" + UUID.randomUUID() + ".pcm")
    @Volatile private var recorder: AudioRecord? = null

    fun start() {
        if (!launched.compareAndSet(false, true) || cancelled.get()) return
        wanted.set(true)
        Thread({
            var microphone: AudioRecord? = null
            var stage = Failure.INITIALIZE
            var written = 0L
            var nonzero = false
            try {
                val minBytes = AudioRecord.getMinBufferSize(
                    RecordedPcmContract.RATE,
                    AudioFormat.CHANNEL_IN_MONO,
                    AudioFormat.ENCODING_PCM_16BIT,
                )
                if (minBytes <= 0) throw IOException("unsupported PCM")
                val bufferBytes = maxOf(minBytes * 2, 4_096)
                microphone = AudioRecord.Builder()
                    .setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                    .setAudioFormat(AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(RecordedPcmContract.RATE)
                        .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                        .build())
                    .setBufferSizeInBytes(bufferBytes)
                    .build()
                if (microphone.state != AudioRecord.STATE_INITIALIZED) throw IOException("audio init")
                recorder = microphone
                if (!wanted.get() || cancelled.get()) return@Thread
                stage = Failure.START
                microphone.startRecording()
                if (microphone.recordingState != AudioRecord.RECORDSTATE_RECORDING) {
                    throw IOException("audio start")
                }
                main.post {
                    if (!cancelled.get() && wanted.get()) onRecordingStarted()
                }
                stage = Failure.WRITE
                FileOutputStream(audioFile).use { output ->
                    val buffer = ByteArray(bufferBytes)
                    val started = SystemClock.elapsedRealtime()
                    while (!cancelled.get() && wanted.get() &&
                        SystemClock.elapsedRealtime() - started < RecordedPcmContract.MAX_MS &&
                        written < RecordedPcmContract.MAX_BYTES
                    ) {
                        val remaining = (RecordedPcmContract.MAX_BYTES - written).toInt()
                        val requestBytes = minOf(buffer.size, remaining)
                        stage = Failure.READ
                        val count = microphone.read(buffer, 0, requestBytes, AudioRecord.READ_BLOCKING)
                        if (count < 0) {
                            if (!wanted.get() || cancelled.get()) break
                            throw IOException("AudioRecord code " + count)
                        }
                        if (count == 0) continue
                        if (count % 2 != 0) throw IOException("odd PCM frame")
                        if (cancelled.get()) break
                        stage = Failure.WRITE
                        output.write(buffer, 0, count)
                        written += count
                        if (!nonzero) nonzero = (0 until count).any { buffer[it].toInt() != 0 }
                    }
                }
                if (cancelled.get()) return@Thread
                if (!RecordedPcmContract.validLength(written) || !nonzero) {
                    reportFailure(Failure.EMPTY)
                } else {
                    finished.set(true)
                    main.post {
                        if (cancelled.get()) audioFile.delete()
                        else onRecordingFinished(audioFile, written)
                    }
                }
            } catch (e: Exception) {
                Log.w("KeyboardRecordedAudio", "capture stage=" + stage.name +
                    " exception=" + e.javaClass.simpleName)
                if (!cancelled.get()) reportFailure(stage)
            } finally {
                runCatching {
                    if (microphone?.recordingState == AudioRecord.RECORDSTATE_RECORDING) microphone.stop()
                }
                runCatching { microphone?.release() }
                if (recorder === microphone) recorder = null
                if (cancelled.get() || !finished.get()) audioFile.delete()
            }
        }, "brrBoard-recorded-pcm").apply { isDaemon = true }.start()
    }

    fun stop() {
        wanted.set(false)
        val mic = recorder
        if (mic != null) {
            // Some OEM AudioRecord.stop() calls wait for the blocking read to unwind.
            Thread({
                runCatching {
                    if (mic.recordingState == AudioRecord.RECORDSTATE_RECORDING) mic.stop()
                }.onFailure {
                    Log.w("KeyboardRecordedAudio", "stop exception=" + it.javaClass.simpleName)
                }
            }, "brrBoard-stop-pcm").apply { isDaemon = true }.start()
        }
    }

    fun close() {
        if (!cancelled.compareAndSet(false, true)) return
        stop()
        audioFile.delete()
    }

    private fun reportFailure(failure: Failure) {
        main.post {
            if (!cancelled.get()) onCaptureFailure(failure)
        }
    }
}

internal object RecordedPcmContract {
    const val RATE = 16_000
    const val CHANNELS = 1
    const val ENCODING = AudioFormat.ENCODING_PCM_16BIT
    const val MAX_MS = 120_000L
    const val MAX_BYTES = RATE * 2L * (MAX_MS / 1_000L)
    const val MIN_BYTES = RATE / 10L * 2L

    fun validLength(bytes: Long): Boolean =
        bytes >= MIN_BYTES && bytes <= MAX_BYTES && bytes % 2L == 0L
}
