// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.voice

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class VoiceSessionCrashTrailTest {
    private val context: Context = ApplicationProvider.getApplicationContext()

    @Test fun noRecordingOrSessionDataExistsUntilExplicitlyArmed() {
        VoiceSessionCrashTrail.erase(context)
        VoiceSessionCrashTrail.begin(context)
        VoiceSessionCrashTrail.note(context, "ASR_LOADING")
        assertFalse(VoiceSessionCrashTrail.snapshot(context).armed)
        assertTrue(VoiceSessionCrashTrail.arm(context))
        assertFalse(VoiceSessionCrashTrail.arm(context))
        assertTrue(VoiceSessionCrashTrail.snapshot(context).message.contains("Armed"))
        VoiceSessionCrashTrail.erase(context)
    }

    @Test fun recordsOnlyOneOrderedStageTrailThenStopsAcceptingUpdates() {
        VoiceSessionCrashTrail.erase(context)
        assertTrue(VoiceSessionCrashTrail.arm(context))
        VoiceSessionCrashTrail.begin(context)
        VoiceSessionCrashTrail.note(context, "MIC_READY")
        VoiceSessionCrashTrail.note(context, "ASR_LOADING")
        VoiceSessionCrashTrail.note(context, "ASR_READY")
        VoiceSessionCrashTrail.note(context, "COMPLETE")
        VoiceSessionCrashTrail.note(context, "ASR_DECODING")
        val snapshot = VoiceSessionCrashTrail.snapshot(context)
        assertTrue(snapshot.finished)
        assertTrue(snapshot.message.contains("ASR_LOADING"))
        assertTrue(snapshot.message.contains("COMPLETE"))
        assertFalse(snapshot.message.contains("ASR_DECODING"))
        VoiceSessionCrashTrail.erase(context)
        assertFalse(VoiceSessionCrashTrail.snapshot(context).armed)
    }

    @Test fun diagnosticIgnoresUnknownPotentiallyPrivateStrings() {
        VoiceSessionCrashTrail.erase(context)
        VoiceSessionCrashTrail.arm(context)
        VoiceSessionCrashTrail.begin(context)
        VoiceSessionCrashTrail.note(context, "my private spoken sentence")
        VoiceSessionCrashTrail.note(context, "FAILED")
        assertFalse(VoiceSessionCrashTrail.snapshot(context).message.contains("private spoken"))
        VoiceSessionCrashTrail.erase(context)
    }
}
