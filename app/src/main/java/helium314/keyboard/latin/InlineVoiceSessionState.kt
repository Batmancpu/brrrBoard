// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

/**
 * The caller-side lifecycle of one invisible Dictate RecognitionService request.
 *
 * Dictate may emit onReadyForSpeech before its recorder has acquired the microphone.
 * Therefore readiness alone must never trigger a pending stop: an early second tap is
 * queued until onBeginningOfSpeech (detected audio activity) or a terminal callback.
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
            // user's intent without issuing a premature stop that Dictate may discard.
            stopOnSpeech = true
            Action.NONE
        }
        Phase.LISTENING -> {
            phase = Phase.PROCESSING
            Action.STOP
        }
        Phase.PROCESSING -> Action.CANCEL
    }

    fun onBeginningOfSpeech(): Action {
        if (phase != Phase.STARTING) return Action.NONE
        phase = if (stopOnSpeech) Phase.PROCESSING else Phase.LISTENING
        return if (stopOnSpeech) Action.STOP else Action.NONE
    }

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
