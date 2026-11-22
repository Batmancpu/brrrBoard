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
import android.os.Build
import android.os.Debug
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong

/**
 * One user-controlled, RAM-only microphone comparison lab. It is never called by
 * LatinIME's recorder, and never queries AudioRecord.routedDevice (the prior crash suspect).
 * No microphone fusion, auto-record, log, file or network path is present.
 */
internal class VoiceNoiseLab(context: Context) {
    enum class Input(val title: String) { AUTO("Android auto"), PHONE("Phone mic"), EARBUDS("Earbuds mic") }

    data class Key(val input: Input, val mode: VoiceNoiseMode) {
        val suppression: Boolean get() = mode == VoiceNoiseMode.ANDROID
        val enhance: Boolean get() = mode == VoiceNoiseMode.GTCRN
    }
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
        val enhancedTranscript: String? = null,
        val enhancedWordErrorPercent: Int? = null,
        val enhanceMs: Long? = null,
        val enhanceStatus: String = "Not requested",
        val modelComparison: ModelComparison? = null,
    )
    data class ModelScore(
        val transcript: String,
        val wordErrorPercent: Int?,
        val modelLoadMs: Long,
        val decodeMs: Long,
        val totalMs: Long,
        val peakPssKb: Long,
        val wallMs: Long,
    )
    data class ModelComparison(
        val reference: String,
        val current110m: ModelScore?,
        val ultra: ModelScore?,
        val firstDecoded: String,
        val failure: String? = null,
    )
    data class Snapshot(
        val status: String,
        val recording: Boolean,
        val playing: Boolean,
        val decoding: Boolean,
        val enhancing: Boolean,
        val activeKey: Key?,
        val levelPercent: Int,
        val takes: List<Take>,
    )

    private val appContext = context.applicationContext
    private val gate = Any()
    private val busy = AtomicBoolean(false)
    private val playBusy = AtomicBoolean(false)
    private val decodeBusy = AtomicBoolean(false)
    private val enhanceBusy = AtomicBoolean(false)
    private val wanted = AtomicBoolean(false)
    private var closed = false
    @Volatile private var microphone: AudioRecord? = null
    @Volatile private var player: AudioTrack? = null
    @Volatile private var level = 0
    @Volatile private var activeKey: Key? = null
    @Volatile private var status = "Ready. Nothing is being recorded."
    private val clips = LinkedHashMap<Key, ByteArray>()
    private val enhancedClips = LinkedHashMap<Key, ByteArray>()
    private val takes = LinkedHashMap<Key, Take>()
    private var decodeRequest: VoiceDecodeRequest? = null
    private var ultraTestClient: UltraVoiceTestClient? = null
    private var comparisonLatch: CountDownLatch? = null
    private var comparisonOrder = 0
    private var enhanceBridge: BrrCleanVoiceBridge? = null
    private var enhanceInput: ByteArray? = null

    fun snapshot(): Snapshot = synchronized(gate) {
        Snapshot(status, busy.get(), playBusy.get(), decodeBusy.get(), enhanceBusy.get(), activeKey,
            level, takes.values.toList())
    }

    fun record(key: Key): Boolean {
        synchronized(gate) {
            if (closed || busy.get() || playBusy.get() || decodeBusy.get() || enhanceBusy.get()) return false
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
            clips.values.forEach { it.fill(0) }
            enhancedClips.values.forEach { it.fill(0) }
            clips.clear()
            enhancedClips.clear()
            takes.clear()
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
        var audioManager: AudioManager? = null
        var previousMode: Int? = null
        var communicationRouteOwned = false
        val collected = ByteArray(MAX_AUDIO_BYTES)
        var written = 0
        try {
            if (!wanted.get()) return
            val manager = appContext.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            audioManager = manager
            // A preferred AudioRecord device alone doesn't open the Bluetooth HFP/LE
            // microphone. Android 12+ must accept the matching communication route.
            if (key.input == Input.EARBUDS) {
                check(Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    "Earbuds recording requires Android 12+ communication routing"
                }
                check(!VoiceCaptureNoiseReduction.callIsActive(appContext)) {
                    "A call is already using the communication microphone"
                }
                val communication = manager.availableCommunicationDevices.firstOrNull {
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO ||
                        it.type == AudioDeviceInfo.TYPE_BLE_HEADSET
                }
                check(communication != null) { "Bluetooth mic is not a communication device" }
                previousMode = manager.mode
                manager.mode = AudioManager.MODE_IN_COMMUNICATION
                check(manager.setCommunicationDevice(communication)) {
                    "Android refused the Bluetooth communication route"
                }
                communicationRouteOwned = true
                check(manager.communicationDevice?.id == communication.id) {
                    "Android did not activate the Bluetooth communication route"
                }
            }
            val device = chooseInput(manager, key.input)
            if (key.input == Input.EARBUDS) check(device != null) {
                "Android exposes no Bluetooth microphone"
            }
            val minBytes = AudioRecord.getMinBufferSize(SAMPLE_RATE, AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT)
            check(minBytes > 0) { "Unsupported microphone format" }
            val bufferSize = maxOf(minBytes * 2, 4096)
            val mic = AudioRecord.Builder()
                .setAudioSource(if (key.input == Input.EARBUDS)
                    MediaRecorder.AudioSource.VOICE_COMMUNICATION else
                    MediaRecorder.AudioSource.VOICE_RECOGNITION)
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
            if (key.input == Input.EARBUDS) check(accepted) {
                "Android refused Bluetooth microphone selection"
            }
            effect = VoiceCaptureNoiseReduction.attachForLab(mic, key.suppression)
            val ns = when {
                key.enhance -> "Android extra suppression OFF · brrClean after recording"
                !key.suppression -> "OFF · raw microphone; no denoiser"
                effect != null -> "Android effect ACTIVE"
                else -> "Android effect UNAVAILABLE"
            }
            // We changed MODE_IN_COMMUNICATION ourselves for Bluetooth, so the
            // normal call guard here would otherwise block this legitimate lab test.
            if (!wanted.get() || (key.input != Input.EARBUDS &&
                    VoiceCaptureNoiseReduction.callIsActive(appContext))) return
            mic.startRecording()
            check(mic.recordingState == AudioRecord.RECORDSTATE_RECORDING) { "Could not record" }
            if (Build.VERSION.SDK_INT >= 29) check(!isCaptureSilenced(manager, mic.audioSessionId)) {
                "Android silenced this capture"
            }
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
                // A headset route silently returning zeros must not appear as a valid take.
                val audible = saved.indices.step(2).any { i ->
                    val value = ((saved[i + 1].toInt() shl 8) or
                        (saved[i].toInt() and 0xff)).toShort().toInt()
                    value > 32 || value < -32
                }
                if (!audible) {
                    saved.fill(0)
                    synchronized(gate) { status = "Silence received. Android may not have routed the earbuds microphone; reconnect or use Phone mic." }
                    return
                }
                if (key.input != Input.AUTO && activeRouteDoesNotMatch(manager, mic.audioSessionId, key.input)) {
                    saved.fill(0)
                    synchronized(gate) { status = "The selected microphone was not confirmed by Android. This recording was discarded, not mislabeled." }
                    return
                }
                val take = Take(key, route, ns, written * 1000 / (SAMPLE_RATE * 2),
                    signal.levelPercent, signal.clippingPercent)
                synchronized(gate) {
                    if (!closed) {
                        clips[key] = saved
                        takes[key] = take
                        status = if (key.enhance) "Take saved. brrClean processes the same PCM in an isolated process."
                            else "Take saved in memory with ${if (key.suppression) "Android effect" else "OFF"}. Replay or transcribe."
                    } else saved.fill(0)
                }
                if (key.enhance) Handler(Looper.getMainLooper()).postDelayed({
                    enhance(key)
                }, 80L) // capture.finally must release its recording gate first
            } else if (!closed) {
                synchronized(gate) { status = "No usable sample recorded. Try again." }
            }
        } catch (_: Exception) {
            synchronized(gate) { if (!closed) status = "Mic/communication route unavailable or rejected. Try Phone mic, reconnect earbuds, and check Android permissions." }
        } finally {
            wanted.set(false)
            level = 0
            runCatching { if (recorder?.recordingState == AudioRecord.RECORDSTATE_RECORDING) recorder.stop() }
            runCatching { effect?.release() }
            runCatching { recorder?.release() }
            // Never leave the system in headset communication mode after the test.
            if (communicationRouteOwned && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
                runCatching { audioManager?.clearCommunicationDevice() }
            previousMode?.let { mode -> runCatching { audioManager?.mode = mode } }
            microphone = null
            collected.fill(0)
            busy.set(false)
            activeKey = null
        }
    }

    fun play(key: Key, enhanced: Boolean = false): Boolean {
        val pcm = synchronized(gate) {
            if (closed || busy.get() || decodeBusy.get() || enhanceBusy.get() ||
                !playBusy.compareAndSet(false, true))
                return false
            (if (enhanced) enhancedClips[key] else clips[key])?.copyOf()
                ?: run { playBusy.set(false); return false }
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

    fun transcribe(key: Key, reference: String, enhanced: Boolean = false): Boolean {
        val pcm = synchronized(gate) {
            if (closed || busy.get() || playBusy.get() || enhanceBusy.get() ||
                !decodeBusy.compareAndSet(false, true))
                return false
            if (VoiceModelChoice.active(appContext) == null) {
                status = "Install a verified offline model to score transcription."
                decodeBusy.set(false)
                return false
            }
            (if (enhanced) enhancedClips[key] else clips[key])?.copyOf()
                ?: run { decodeBusy.set(false); return false }
        }
        val started = SystemClock.elapsedRealtime()
        synchronized(gate) { status = "Transcribing this take with the selected keyboard model…" }
        // VoiceDecodeRequest owns and wipes its copy on success, failure or cancellation.
        val request = OfflineVoiceEngine.shared(appContext).transcribe(pcm) { answer ->
            synchronized(gate) {
                decodeBusy.set(false)
                decodeRequest = null
                if (!closed) {
                    val previous = takes[key]
                    if (previous != null && answer.isSuccess) {
                        val text = answer.getOrNull().orEmpty()
                        takes[key] = if (enhanced) previous.copy(enhancedTranscript = text,
                            enhancedWordErrorPercent = VoiceLabScore.wordErrorPercent(reference, text),
                            scoredReference = reference)
                        else previous.copy(transcript = text, scoredReference = reference,
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

    /** Decodes one identical volatile PCM take, sequentially and cold, on both ASR models. */
    fun compareModels(key: Key, reference: String): Boolean {
        val pcm = synchronized(gate) {
            if (closed || busy.get() || playBusy.get() || enhanceBusy.get() ||
                !decodeBusy.compareAndSet(false, true)) return false
            if (!VoiceModelInstaller.isInstalled(appContext)) {
                status = "The existing Parakeet 110M model must be installed before comparison."
                decodeBusy.set(false)
                return false
            }
            if (!VoiceModelInstaller.isInstalled(appContext, UltraVoiceModelManifest.pack)) {
                status = "Install the bundled Ultra test pack before comparing ASR models."
                decodeBusy.set(false)
                return false
            }
            clips[key]?.copyOf() ?: run { decodeBusy.set(false); return false }
        }
        val order = synchronized(gate) { comparisonOrder++ and 1 }
        synchronized(gate) {
            takes[key]?.let { takes[key] = it.copy(modelComparison = null) }
            status = "Comparing both offline models on the same audio take…"
        }
        Thread({
            var oldScore: ModelScore? = null
            var ultraScore: ModelScore? = null
            val currentFirst = order == 0
            try {
                if (currentFirst) {
                    oldScore = decodeCurrentModel(pcm, reference)
                    ultraScore = decodeUltraModel(pcm, reference)
                } else {
                    ultraScore = decodeUltraModel(pcm, reference)
                    oldScore = decodeCurrentModel(pcm, reference)
                }
                synchronized(gate) {
                    val previous = takes[key]
                    if (!closed && previous != null && oldScore != null && ultraScore != null) {
                        takes[key] = previous.copy(modelComparison = ModelComparison(reference,
                            oldScore!!, ultraScore!!, if (currentFirst) "110M" else "Ultra"))
                        status = "Both models finished. The same RAM-only take was decoded one at a time."
                    }
                }
            } catch (error: Exception) {
                synchronized(gate) {
                    if (!closed) {
                        takes[key]?.let { take ->
                            if (oldScore != null || ultraScore != null) {
                                takes[key] = take.copy(modelComparison = ModelComparison(reference,
                                    oldScore, ultraScore, if (currentFirst) "110M" else "Ultra",
                                    "The comparison stopped before both models finished."))
                            }
                        }
                        status = error.message?.take(180)
                            ?: "Model comparison failed. Your selected keyboard model is unchanged."
                    }
                }
            } finally {
                pcm.fill(0)
                runCatching { OfflineVoiceEngine.shared(appContext, VoiceModelManifest.pack).releaseAndWait() }
                synchronized(gate) {
                    decodeRequest = null
                    ultraTestClient = null
                    comparisonLatch = null
                    decodeBusy.set(false)
                }
            }
        }, "brrBoard-voice-model-comparison").apply { isDaemon = true }.start()
        return true
    }

    private fun decodeCurrentModel(pcm: ByteArray, reference: String): ModelScore {
        val engine = OfflineVoiceEngine.shared(appContext, VoiceModelManifest.pack)
        val wallStarted = SystemClock.elapsedRealtime()
        engine.releaseAndWait()
        val peakPss = AtomicLong(Debug.getPss().toLong())
        val sampling = AtomicBoolean(true)
        val sampler = startPssSampler(peakPss, sampling)
        val latch = CountDownLatch(1)
        var answer: Result<OfflineVoiceRecognition>? = null
        val request = engine.transcribeMeasured(pcm.copyOf(), formatTranscript = false) {
            answer = it
            latch.countDown()
        }
        synchronized(gate) {
            if (closed) {
                request.cancel()
                latch.countDown()
            } else {
                decodeRequest = request
                comparisonLatch = latch
            }
        }
        try {
            check(latch.await(MAX_COMPARISON_SECONDS, TimeUnit.SECONDS)) {
                request.cancel()
                "110M comparison timed out."
            }
            val measured = requireNotNull(answer).getOrThrow()
            return ModelScore(measured.transcript,
                VoiceLabScore.wordErrorPercent(reference, measured.transcript),
                measured.modelLoadMs, measured.decodeMs, measured.totalMs, peakPss.get(),
                SystemClock.elapsedRealtime() - wallStarted)
        } finally {
            sampling.set(false)
            sampler.join(250)
            engine.releaseAndWait()
        }
    }

    private fun decodeUltraModel(pcm: ByteArray, reference: String): ModelScore {
        val wallStarted = SystemClock.elapsedRealtime()
        val latch = CountDownLatch(1)
        var answer: Result<UltraVoiceTestResult>? = null
        val client = UltraVoiceTestClient(appContext)
        synchronized(gate) { ultraTestClient = client; comparisonLatch = latch }
        client.transcribe(pcm) {
            answer = it
            latch.countDown()
        }
        check(latch.await(MAX_COMPARISON_SECONDS, TimeUnit.SECONDS)) {
            client.cancel()
            "Ultra comparison timed out; the main 110M process is unaffected."
        }
        val measured = requireNotNull(answer).getOrThrow()
        return ModelScore(measured.transcript,
            VoiceLabScore.wordErrorPercent(reference, measured.transcript),
            measured.modelLoadMs, measured.decodeMs, measured.totalMs, measured.peakPssKb,
            SystemClock.elapsedRealtime() - wallStarted)
    }

    private fun startPssSampler(peakPss: AtomicLong, sampling: AtomicBoolean): Thread =
        Thread({
            while (sampling.get()) {
                updatePeak(peakPss, Debug.getPss().toLong())
                try { Thread.sleep(PSS_SAMPLE_INTERVAL_MS) }
                catch (_: InterruptedException) { break }
            }
        }, "brrBoard-110m-memory-sample").apply { isDaemon = true; start() }

    private fun updatePeak(peak: AtomicLong, value: Long) {
        while (true) {
            val current = peak.get()
            if (value <= current || peak.compareAndSet(current, value)) return
        }
    }

    /** The *same* stored microphone take is enhanced without reopening AudioRecord. */
    private fun enhance(key: Key) {
        val original = synchronized(gate) {
            if (closed || busy.get() || playBusy.get() || decodeBusy.get() ||
                !enhanceBusy.compareAndSet(false, true)) return
            clips[key]?.copyOf() ?: run { enhanceBusy.set(false); return }
        }
        val started = SystemClock.elapsedRealtime()
        enhanceInput = original
        val bridge = BrrCleanVoiceBridge(appContext) { enhanced ->
            synchronized(gate) {
                enhanceBusy.set(false)
                enhanceBridge = null
                if (!closed) {
                    val previous = takes[key]
                    if (previous != null) {
                        val valid = enhanced != null && enhanced.size == original.size
                        if (valid) {
                            enhancedClips.remove(key)?.fill(0)
                            enhancedClips[key] = enhanced!!
                            takes[key] = previous.copy(enhanceStatus = "brrClean native GTCRN processed",
                                enhanceMs = SystemClock.elapsedRealtime() - started)
                            status = "brrClean finished. Replay original and enhanced from ONE recording."
                        } else {
                            enhanced?.fill(0)
                            takes[key] = previous.copy(enhanceStatus = "brrClean failed or timed out · raw safe")
                            status = "brrClean could not process this take. Original audio is intact; keyboard is safe."
                        }
                    } else enhanced?.fill(0)
                } else enhanced?.fill(0)
            }
            original.fill(0)
            enhanceInput = null
        }
        enhanceBridge = bridge
        if (!bridge.run(original) && enhanceBridge === bridge) {
            enhanceBridge = null
            original.fill(0)
            enhanceInput = null
            enhanceBusy.set(false)
        }
    }

    fun clear() {
        synchronized(gate) {
            if (closed || busy.get() || playBusy.get() || decodeBusy.get() || enhanceBusy.get()) return
            clips.values.forEach { it.fill(0) }
            enhancedClips.values.forEach { it.fill(0) }
            clips.clear()
            enhancedClips.clear()
            takes.clear()
            status = "All trial audio and transcripts erased from memory."
        }
    }

    fun close() {
        synchronized(gate) {
            closed = true
            wanted.set(false)
            enhanceBridge?.cancel()
            enhanceBridge = null
            enhanceInput?.fill(0)
            enhanceInput = null
            decodeRequest?.cancel()
            ultraTestClient?.cancel()
            comparisonLatch?.countDown()
            clips.values.forEach { it.fill(0) }
            enhancedClips.values.forEach { it.fill(0) }
            clips.clear()
            enhancedClips.clear()
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

    private fun isCaptureSilenced(manager: AudioManager, sessionId: Int): Boolean = runCatching {
        manager.activeRecordingConfigurations.firstOrNull { it.clientAudioSessionId == sessionId }
            ?.isClientSilenced ?: false
    }.getOrDefault(false)

    private fun activeRouteDoesNotMatch(manager: AudioManager, sessionId: Int, input: Input): Boolean =
        runCatching {
            val type = manager.activeRecordingConfigurations.firstOrNull {
                it.clientAudioSessionId == sessionId
            }?.audioDevice?.type ?: return@runCatching input == Input.EARBUDS
            when (input) {
                Input.PHONE -> type != AudioDeviceInfo.TYPE_BUILTIN_MIC
                Input.EARBUDS -> type != AudioDeviceInfo.TYPE_BLUETOOTH_SCO &&
                    type != AudioDeviceInfo.TYPE_BLE_HEADSET
                Input.AUTO -> false
            }
        }.getOrDefault(false)

    companion object {
        private const val SAMPLE_RATE = 16_000
        private const val MAX_SECONDS = 12
        private const val MAX_AUDIO_BYTES = SAMPLE_RATE * 2 * MAX_SECONDS
        private const val MAX_COMPARISON_SECONDS = 240L
        private const val PSS_SAMPLE_INTERVAL_MS = 100L
    }
}
