// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PanelGlassRecordStateTest {
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
