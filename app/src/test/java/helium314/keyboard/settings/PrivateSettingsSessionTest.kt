package helium314.keyboard.settings

import org.junit.Assert.*
import org.junit.Test

class PrivateSettingsSessionTest {
    @Test fun expiresAtExactDeadlineAndRejectsBackwardClock() {
        val deadline = PrivateSettingsSession.deadline(1000L)
        assertTrue(PrivateSettingsSession.active(deadline,1000L))
        assertTrue(PrivateSettingsSession.active(deadline,deadline-1))
        assertFalse(PrivateSettingsSession.active(deadline,deadline))
        assertFalse(PrivateSettingsSession.active(deadline,999L))
        assertFalse(PrivateSettingsSession.active(0L,1000L))
    }
}
