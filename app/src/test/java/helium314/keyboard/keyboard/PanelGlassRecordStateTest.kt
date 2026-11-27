// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PanelGlassRecordStateTest {
    @Test fun unchangedConfigurationDoesNotDirtyAValidRecording() {
        val state = PanelGlassRecordState()
        state.recordingCompleted()
        assertFalse(state.configurationChanged(false,false,false))
        assertFalse(state.needsRecording(true))
        assertTrue(state.configurationChanged(true,false,false))
        assertTrue(state.needsRecording(true))
        state.recordingCompleted()
        assertTrue(state.configurationChanged(false,true,false))
        assertTrue(state.needsRecording(true))
        state.recordingCompleted()
        assertTrue(state.configurationChanged(false,false,true))
        assertTrue(state.needsRecording(true))
    }
    @Test fun repeatedInvalidationsShareOneFrameButKeepContentDirty() {
        val state = PanelGlassRecordState()
        state.recordingCompleted()
        state.invalidate()
        assertTrue(state.requestFrame())
        state.invalidate()
        assertFalse(state.requestFrame())
        assertTrue(state.needsRecording(true))
        state.frameStarted()
        state.recordingCompleted()
        assertFalse(state.needsRecording(true))
        assertTrue(state.requestFrame())
    }
    @Test fun hiddenSurfaceCancelsScheduledFrameAndMustRecordOnReturn() {
        val state = PanelGlassRecordState()
        state.recordingCompleted()
        assertTrue(state.requestFrame())
        // The view drops its display list, marks it dirty and clears the callback.
        state.invalidate()
        state.frameStarted()
        assertTrue(state.needsRecording(hasDisplayList = false))
        assertTrue(state.requestFrame())
        state.frameStarted()
        state.recordingCompleted()
        assertFalse(state.needsRecording(hasDisplayList = true))
    }
    @Test fun unchangedFramesReuseTheRecordingButInvalidationsRefreshIt() {
        val state = PanelGlassRecordState()
        assertTrue(state.needsRecording(hasDisplayList = false))
        state.recordingCompleted()
        assertFalse(state.needsRecording(hasDisplayList = true))

        state.invalidate()
        assertTrue(state.needsRecording(hasDisplayList = true))
        state.recordingCompleted()
        assertTrue(state.needsRecording(hasDisplayList = false))
    }
}
