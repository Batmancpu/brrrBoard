// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FooterRenderWorkPolicyTest {
    @Test fun visibleVoicePhaseAloneOwnsPulse() {
        assertTrue(FooterRenderWorkPolicy.canPulse(true,true,false,true,false,true))
        assertTrue(FooterRenderWorkPolicy.canPulse(true,true,false,false,true,true))
        assertFalse(FooterRenderWorkPolicy.canPulse(false,true,false,true,false,true))
        assertFalse(FooterRenderWorkPolicy.canPulse(true,false,false,true,false,true))
        assertFalse(FooterRenderWorkPolicy.canPulse(true,true,true,true,false,true))
        assertFalse(FooterRenderWorkPolicy.canPulse(true,true,false,false,false,true))
        assertFalse(FooterRenderWorkPolicy.canPulse(true,true,false,true,false,false))
    }
    @Test fun unchangedAbsoluteDeadlineNeverRepostsOrRenews() {
        assertTrue(FooterRenderWorkPolicy.needsDeadlinePost(0L,30000L))
        assertFalse(FooterRenderWorkPolicy.needsDeadlinePost(30000L,30000L))
        assertFalse(FooterRenderWorkPolicy.needsDeadlinePost(30000L,0L))
        assertTrue(FooterRenderWorkPolicy.needsDeadlinePost(30000L,45000L))
    }
}
