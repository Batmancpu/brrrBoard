// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotSame
import org.junit.Test

class VoiceCaptureAudioFocusTest {
    @Test fun focusListenersAreNotSharedBetweenRecordings() {
        assertNotSame(VoiceCaptureAudioFocus.newListener(), VoiceCaptureAudioFocus.newListener())
    }
    @Test fun eachLeaseReleasesOnlyOnce() {
        var releases = 0
        val lease = VoiceCaptureAudioFocus { releases++ }
        lease.close()
        lease.close()
        assertEquals(1, releases)
    }
    @Test fun leasesDoNotReleaseAnotherRecording() {
        var first = 0
        var second = 0
        val a = VoiceCaptureAudioFocus { first++ }
        val b = VoiceCaptureAudioFocus { second++ }
        a.close()
        assertEquals(1, first)
        assertEquals(0, second)
        b.close()
        assertEquals(1, second)
    }
}
