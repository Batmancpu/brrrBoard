// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

/**
 * The caller-side lifecycle of one invisible speech RecognitionService request.
 *
 * brrSpeak signals ready before brrBoard starts the foreground microphone.
 * Therefore readiness alone must never trigger a pending stop: an early second tap is
 * queued until AudioRecord confirms capture (or a terminal callback).
 * There is exactly one STOP action per request and a tap while processing cancels.
 */
internal class InlineVoiceSessionState {
    enum class Phase { IDLE, STARTING, LISTENING, PROCESSING }
    enum class Action { NONE, START, STOP, CANCEL }

    var phase: Phase = Phase.IDLE
        private set

    private var stopOnSpeech = false

    fun begin() {
        check(phase == Phase.IDLE)
        stopOnSpeech = false
        phase = Phase.STARTING
    }

    fun tap(): Action = when (phase) {
        Phase.IDLE -> Action.START
        Phase.STARTING -> {
            // The service can be ready before AudioRecord is initialized. Remember the
            // user's intent without issuing a premature EOF while the sender starts.
            stopOnSpeech = true
            Action.NONE
        }
        Phase.LISTENING -> {
            phase = Phase.PROCESSING
            Action.STOP
        }
        Phase.PROCESSING -> Action.CANCEL
    }

    fun onAudioCaptureStarted(): Action {
        if (phase != Phase.STARTING) return Action.NONE
        phase = if (stopOnSpeech) Phase.PROCESSING else Phase.LISTENING
        return if (stopOnSpeech) Action.STOP else Action.NONE
    }

    // Older tests/callers used the remote service's speech callback as a proxy for capture.
    // The brrBoard PCM path instead invokes onAudioCaptureStarted after AudioRecord starts.
    fun onBeginningOfSpeech(): Action = onAudioCaptureStarted()

    fun onEndOfSpeech() {
        if (phase == Phase.STARTING || phase == Phase.LISTENING) {
            phase = Phase.PROCESSING
        }
    }

    fun reset() {
        stopOnSpeech = false
        phase = Phase.IDLE
    }
}
