// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioFocusRequest
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import java.util.concurrent.atomic.AtomicBoolean

/**
 * One exclusive, temporary focus lease per physical microphone recording.
 * No stream-volume mutations, media-key injection, DND, routing changes or new permissions.
 * A denied request is non-fatal: Android and the playback app ultimately control audio focus.
 */
internal class VoiceCaptureAudioFocus(private val release: () -> Unit) : AutoCloseable {
    private val closed = AtomicBoolean(false)
    override fun close() {
        if (closed.compareAndSet(false, true)) runCatching { release() }
    }

    companion object {
        // A non-capturing SAM lambda can be a singleton. AudioManager keys focus by
        // listener identity, so every physical recording needs a fresh instance.
        internal fun newListener(): AudioManager.OnAudioFocusChangeListener =
            object : AudioManager.OnAudioFocusChangeListener {
                override fun onAudioFocusChange(focusChange: Int) = Unit
            }

        @Suppress("DEPRECATION")
        fun acquire(context: Context): VoiceCaptureAudioFocus? = runCatching {
            val manager = context.getSystemService(Context.AUDIO_SERVICE) as? AudioManager
                ?: return@runCatching null
            val listener = newListener()
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val request = AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                    .setAudioAttributes(AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build())
                    .setOnAudioFocusChangeListener(listener, Handler(Looper.getMainLooper()))
                    .setAcceptsDelayedFocusGain(false)
                    .build()
                if (manager.requestAudioFocus(request) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
                    VoiceCaptureAudioFocus { manager.abandonAudioFocusRequest(request) }
                else null
            } else {
                if (manager.requestAudioFocus(listener, AudioManager.STREAM_MUSIC,
                        AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE) == AudioManager.AUDIOFOCUS_REQUEST_GRANTED)
                    VoiceCaptureAudioFocus { manager.abandonAudioFocus(listener) }
                else null
            }
        }.getOrNull()
    }
}
