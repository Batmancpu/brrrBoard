// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.AudioDeviceInfo
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.AudioTrack
import android.media.MediaRecorder
import android.media.audiofx.NoiseSuppressor
import android.os.SystemClock
import java.util.concurrent.atomic.AtomicBoolean

/**
 * One user-controlled, RAM-only microphone comparison lab. It is never called by
 * LatinIME's recorder, and never queries AudioRecord.routedDevice (the prior crash suspect).
 * No microphone fusion, auto-record, log, file or network path is present.
 */
internal class VoiceNoiseLab(context: Context) {
    enum class Input(val title: String) { AUTO("Android auto"), PHONE("Phone mic"), EARBUDS("Earbuds mic") }

    data class Key(val input: Input, val suppression: Boolean)
    data class Take(
        val key: Key,
        val source: String,
        val suppressionStatus: String,
        val durationMs: Int,
        val signalLevelPercent: Int,
        val clippingPercent: Double,
        val transcript: String? = null,
        val scoredReference: String? = null,
        val wordErrorPercent: Int? = null,
        val decodeMs: Long? = null,
    )
    data class Snapshot(
        val status: String,
        val recording: Boolean,
        val playing: Boolean,
        val decoding: Boolean,
        val activeKey: Key?,
        val levelPercent: Int,
        val takes: List<Take>,
    )

    private val appContext = context.applicationContext
    private val gate = Any()
    private val busy = AtomicBoolean(false)
    private val playBusy = AtomicBoolean(false)
    private val decodeBusy = AtomicBoolean(false)
    private val wanted = AtomicBoolean(false)
    private var closed = false
    @Volatile private var microphone: AudioRecord? = null
    @Volatile private var player: AudioTrack? = null
    @Volatile private var level = 0
    @Volatile private var activeKey: Key? = null
    @Volatile private var status = "Ready. Nothing is being recorded."
    private val clips = LinkedHashMap<Key, ByteArray>()
    private val takes = LinkedHashMap<Key, Take>()
    private var decodeRequest: VoiceDecodeRequest? = null

    fun snapshot(): Snapshot = synchronized(gate) {
        Snapshot(status, busy.get(), playBusy.get(), decodeBusy.get(), activeKey,
            level, takes.values.toList())
    }

    fun record(key: Key): Boolean {
        synchronized(gate) {
            if (closed || busy.get() || playBusy.get() || decodeBusy.get()) return false
            if (appContext.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
                status = "Allow microphone access in Android settings before recording."
                return false
            }
            if (VoiceCaptureNoiseReduction.callIsActive(appContext)) {
                status = "Recording blocked while Android reports a call."
                return false
            }
            busy.set(true)
            wanted.set(true)
            activeKey = key
            level = 0
            clips.remove(key)?.fill(0)
            takes.remove(key)
            status = "Opening ${key.input.title.lowercase()}…"
        }
        Thread({ capture(key) }, "brrBoard-voice-lab-record").apply { isDaemon = true }.start()
        return true
    }

    fun stopRecording() {
        wanted.set(false)
        // ColorOS can block on AudioRecord.stop while a read is in progress.
        val current = microphone ?: return
        Thread({ runCatching {
            if (current.recordingState == AudioRecord.RECORDSTATE_RECORDING) current.stop()
        } }, "brrBoard-voice-lab-stop").apply { isDaemon = true }.start()
    }

    private fun capture(key: Key) {
        var recorder: AudioRecord? = null
        var effect: NoiseSuppressor? = null
        val collected = ByteArray(MAX_AUDIO_BYTES)
        var written = 0
        try {
            if (!wanted.get()) return
            val manager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val device = chooseInput(manager, key.input)
            val minBytes = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT)
            check(minBytes > 0) { "Unsupported microphone format" }
            val bufferSize = maxOf(minBytes * 2, 4096)
            val mic = AudioRecord.Builder()
                .setAudioSource(MediaRecorder.AudioSource.VOICE_RECOGNITION)
                .setAudioFormat(AudioFormat.Builder()
                    .setSampleRate(SAMPLE_RATE)
                    .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                    .setChannelMask(AudioFormat.CHANNEL_IN_MONO)
                    .build())
                .setBufferSizeInBytes(bufferSize)
                .build()
            recorder = mic
            check(mic.state == AudioRecord.STATE_INITIALIZED) { "Microphone unavailable" }
            microphone = mic
            val accepted = if (device == null) false else mic.setPreferredDevice(device)
            effect = VoiceCaptureNoiseReduction.attachForLab(mic, key.suppression)
            val ns = if (!key.suppression) "App effect OFF" else if (effect != null)
                "Android effect ACTIVE" else "Effect unavailable"
            if (!wanted.get() || VoiceCaptureNoiseReduction.callIsActive(appContext)) return
            mic.startRecording()
            check(mic.recordingState == AudioRecord.RECORDSTATE_RECORDING) { "Could not record" }
            val actual = activeRoute(manager, mic.audioSessionId)
            var checkedRoute = actual != null
            var route = if (actual != null) actual else when {
                key.input == Input.AUTO -> "Android-controlled · route not reported"
                !accepted -> "Request rejected · Android auto (unverified)"
                else -> "Requested ${key.input.title} · actual route unverified"
            }
            synchronized(gate) { if (!closed) status = "$route · $ns · recording (12 s maximum)" }
            val frame = ByteArray(bufferSize)
            val started = SystemClock.elapsedRealtime()
            try {
                while (wanted.get() && !closed && written < MAX_AUDIO_BYTES &&
                    SystemClock.elapsedRealtime() - started < MAX_SECONDS * 1000L) {
                    val available = minOf(frame.size, MAX_AUDIO_BYTES - written)
                    val count = mic.read(frame, 0, available, AudioRecord.READ_BLOCKING)
                    if (count < 0) {
                        if (!wanted.get()) break
                        error("Microphone read failed")
                    }
                    if (count > 0 && wanted.get() && !closed) {
                        System.arraycopy(frame, 0, collected, written, count)
                        written += count
                        level = VoiceLabScore.signal(frame, count).levelPercent
                        // Some OEMs report the actual route only after audio has flowed.
                        // Check once after 500ms, never per audio frame or in the IME.
                        if (!checkedRoute && written >= SAMPLE_RATE) {
                            checkedRoute = true
                            activeRoute(manager, mic.audioSessionId)?.let { confirmed ->
                                route = confirmed
                                synchronized(gate) {
                                    if (!closed) status = "$route · $ns · recording (12 s maximum)"
                                }
                            }
                        }
                    }
                }
            } finally { frame.fill(0) }
            if (!closed && written >= SAMPLE_RATE * 2 / 4) {
                val saved = collected.copyOf(written)
                val signal = VoiceLabScore.signal(saved)
                val take = Take(key, route, ns, written * 1000 / (SAMPLE_RATE * 2),
                    signal.levelPercent, signal.clippingPercent)
                synchronized(gate) {
                    if (!closed) {
                        clips[key] = saved
                        takes[key] = take
                        status = "Take saved in memory. Replay or transcribe; compare another mode."
                    } else saved.fill(0)
                }
            } else if (!closed) {
                synchronized(gate) { status = "No usable sample recorded. Try again." }
            }
        } catch (_: Exception) {
            synchronized(gate) { if (!closed) status = "Microphone test failed. Try Android auto or reconnect the earbuds." }
        } finally {
            wanted.set(false)
            level = 0
            runCatching { if (recorder?.recordingState == AudioRecord.RECORDSTATE_RECORDING) recorder.stop() }
            runCatching { effect?.release() }
            runCatching { recorder?.release() }
            microphone = null
            collected.fill(0)
            busy.set(false)
            activeKey = null
        }
    }

    fun play(key: Key): Boolean {
        val pcm = synchronized(gate) {
            if (closed || busy.get() || decodeBusy.get() || !playBusy.compareAndSet(false, true))
                return false
            clips[key]?.copyOf() ?: run { playBusy.set(false); return false }
        }
        Thread({
            var track: AudioTrack? = null
            try {
                val minBytes = AudioTrack.getMinBufferSize(SAMPLE_RATE,
                    AudioFormat.CHANNEL_OUT_MONO, AudioFormat.ENCODING_PCM_16BIT)
                check(minBytes > 0)
                val playbackTrack = AudioTrack.Builder()
                    .setAudioAttributes(AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                    .setAudioFormat(AudioFormat.Builder()
                        .setSampleRate(SAMPLE_RATE)
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build())
                    .setTransferMode(AudioTrack.MODE_STREAM)
                    .setBufferSizeInBytes(maxOf(minBytes, 4096))
                    .build()
                track = playbackTrack
                player = playbackTrack
                check(playbackTrack.state == AudioTrack.STATE_INITIALIZED)
                playbackTrack.play()
                var offset = 0
                while (offset < pcm.size && !closed && playBusy.get()) {
                    val count = playbackTrack.write(pcm, offset, pcm.size - offset,
                        AudioTrack.WRITE_BLOCKING)
                    if (count <= 0) break
                    offset += count
                }
            } catch (_: Exception) {
                synchronized(gate) { if (!closed) status = "Replay unavailable on this audio output." }
            } finally {
                runCatching { track?.stop() }
                runCatching { track?.release() }
                player = null
                pcm.fill(0)
                playBusy.set(false)
            }
        }, "brrBoard-voice-lab-replay").apply { isDaemon = true }.start()
        return true
    }

    fun transcribe(key: Key, reference: String): Boolean {
        val pcm = synchronized(gate) {
            if (closed || busy.get() || playBusy.get() || !decodeBusy.compareAndSet(false, true))
                return false
            if (!VoiceModelInstaller.isInstalled(appContext)) {
                status = "The existing offline model must be installed to score transcription."
                decodeBusy.set(false)
                return false
            }
            clips[key]?.copyOf() ?: run { decodeBusy.set(false); return false }
        }
        val started = SystemClock.elapsedRealtime()
        synchronized(gate) { status = "Transcribing this take offline with the existing model…" }
        // VoiceDecodeRequest owns and wipes its copy on success, failure or cancellation.
        val request = OfflineVoiceEngine.shared(appContext).transcribe(pcm) { answer ->
            synchronized(gate) {
                decodeBusy.set(false)
                decodeRequest = null
                if (!closed) {
                    val previous = takes[key]
                    if (previous != null && answer.isSuccess) {
                        val text = answer.getOrNull().orEmpty()
                        takes[key] = previous.copy(transcript = text,
                            scoredReference = reference,
                            wordErrorPercent = VoiceLabScore.wordErrorPercent(reference, text),
                            decodeMs = SystemClock.elapsedRealtime() - started)
                        status = "Offline recognition complete. Compare the result with the displayed phrase."
                    } else status = "Offline recognition failed; recorded audio remains local."
                }
            }
        }
        synchronized(gate) { if (closed) request.cancel() else decodeRequest = request }
        return true
    }

    fun clear() {
        synchronized(gate) {
            if (closed || busy.get() || playBusy.get() || decodeBusy.get()) return
            clips.values.forEach { it.fill(0) }
            clips.clear()
            takes.clear()
            status = "All trial audio and transcripts erased from memory."
        }
    }

    fun close() {
        synchronized(gate) {
            closed = true
            wanted.set(false)
            decodeRequest?.cancel()
            clips.values.forEach { it.fill(0) }
            clips.clear()
            takes.clear()
        }
        stopRecording()
        playBusy.set(false)
        val currentPlayer = player
        if (currentPlayer != null) Thread({
            runCatching { currentPlayer.pause() }
            runCatching { currentPlayer.flush() }
        }, "brrBoard-voice-lab-stop-replay").apply { isDaemon = true }.start()
    }

    private fun chooseInput(manager: AudioManager, input: Input): AudioDeviceInfo? {
        if (input == Input.AUTO) return null
        return manager.getDevices(AudioManager.GET_DEVICES_INPUTS).firstOrNull {
            when (input) {
                Input.PHONE -> it.type == AudioDeviceInfo.TYPE_BUILTIN_MIC
                Input.EARBUDS -> it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                    it.type == AudioDeviceInfo.TYPE_BLE_HEADSET
                Input.AUTO -> false
            }
        }
    }

    /** Uses Android's own recording configuration, never the suspected routedDevice JNI probe. */
    private fun activeRoute(manager: AudioManager, sessionId: Int): String? = runCatching {
        val config = manager.activeRecordingConfigurations.firstOrNull {
            it.clientAudioSessionId == sessionId
        } ?: return@runCatching null
        val actualDevice = config.audioDevice ?: return@runCatching null
        when (actualDevice.type) {
            AudioDeviceInfo.TYPE_BUILTIN_MIC -> "Phone microphone (confirmed)"
            AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "Bluetooth headset mic (confirmed)"
            AudioDeviceInfo.TYPE_BLE_HEADSET -> "Bluetooth LE headset mic (confirmed)"
            AudioDeviceInfo.TYPE_WIRED_HEADSET -> "Wired headset mic (confirmed)"
            else -> "Android audio input (confirmed type ${actualDevice.type})"
        }
    }.getOrNull()

    companion object {
        private const val SAMPLE_RATE = 16_000
        private const val MAX_SECONDS = 12
        private const val MAX_AUDIO_BYTES = SAMPLE_RATE * 2 * MAX_SECONDS
    }
}
