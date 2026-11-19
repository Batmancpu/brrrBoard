// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class InlineVoiceSessionStateTest {
    private val state = InlineVoiceSessionState()

    @Test fun ordinaryStartSpeakStopThenCancelPendingResult() {
        assertEquals(InlineVoiceSessionState.Action.START, state.tap())
        state.begin()
        assertEquals(InlineVoiceSessionState.Phase.STARTING, state.phase)
        assertEquals(InlineVoiceSessionState.Action.NONE, state.onBeginningOfSpeech())
        assertEquals(InlineVoiceSessionState.Phase.LISTENING, state.phase)
        assertEquals(InlineVoiceSessionState.Action.STOP, state.tap())
        assertEquals(InlineVoiceSessionState.Phase.PROCESSING, state.phase)
        assertEquals(InlineVoiceSessionState.Action.CANCEL, state.tap())
    }

    @Test fun quickSecondTapWaitsForActualSpeechThenStopsExactlyOnce() {
        state.begin()
        assertEquals(InlineVoiceSessionState.Action.NONE, state.tap())
        assertEquals(InlineVoiceSessionState.Action.NONE, state.tap())
        assertEquals(InlineVoiceSessionState.Phase.STARTING, state.phase)
        assertEquals(InlineVoiceSessionState.Action.STOP, state.onAudioCaptureStarted())
        assertEquals(InlineVoiceSessionState.Action.NONE, state.onAudioCaptureStarted())
        assertEquals(InlineVoiceSessionState.Phase.PROCESSING, state.phase)
    }

    @Test fun automaticEndOfSpeechAndLateCallbacksCannotRestartListening() {
        state.begin()
        state.onEndOfSpeech()
        assertEquals(InlineVoiceSessionState.Phase.PROCESSING, state.phase)
        assertEquals(InlineVoiceSessionState.Action.NONE, state.onBeginningOfSpeech())
        state.reset()
        assertEquals(InlineVoiceSessionState.Phase.IDLE, state.phase)
        assertEquals(InlineVoiceSessionState.Action.NONE, state.onBeginningOfSpeech())
        assertEquals(InlineVoiceSessionState.Action.START, state.tap())
    }

    @Test fun unsuccessfulStartDoesNotLeaveSessionStuck() {
        assertEquals(InlineVoiceSessionState.Action.START, state.tap())
        // Caller enters STARTING only after service checks and recognizer creation succeed.
        assertEquals(InlineVoiceSessionState.Phase.IDLE, state.phase)
        assertEquals(InlineVoiceSessionState.Action.START, state.tap())
    }

    @Test fun retryChipOnlyForTransientRecognizerFailures() {
        assertTrue(voiceFailureSupportsRetry(R.string.inline_voice_failure))
        assertTrue(voiceFailureSupportsRetry(R.string.inline_voice_no_speech))
        assertTrue(voiceFailureSupportsRetry(R.string.inline_voice_provider_audio_failure))
        assertFalse(voiceFailureSupportsRetry(R.string.inline_voice_permission))
        assertFalse(voiceFailureSupportsRetry(R.string.inline_voice_dictate_permission))
        assertFalse(voiceFailureSupportsRetry(R.string.inline_voice_select_dictate_service))
        assertFalse(voiceFailureSupportsRetry(R.string.inline_voice_unsupported_language))
    }
}
