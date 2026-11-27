// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin;

import org.junit.Test;
import static org.junit.Assert.*;

public class BackspaceSuggestionHoldTest {
    @Test public void recoverySurvivesSettleButNotANewEditorOrFreshFrame() {
        BackspaceSuggestionHold hold = new BackspaceSuggestionHold();
        assertFalse(hold.canRecover(7L, true));
        hold.noteDelete(7L);
        assertFalse(hold.canRecover(7L, true));
        hold.clear();
        assertTrue(hold.canRecover(7L, true));
        assertFalse(hold.canRecover(7L, false));
        assertFalse(hold.canRecover(8L, true));
        hold.clearRecovery();
        assertFalse(hold.canRecover(7L, true));
        assertEquals(305L, BackspaceSuggestionHold.SETTLE_MS + BackspaceSuggestionHold.RECOVERY_GRACE_MS);
    }

    @Test public void nextBurstSupersedesEarlierRecovery() {
        BackspaceSuggestionHold hold = new BackspaceSuggestionHold();
        hold.noteDelete(7L);
        hold.clear();
        hold.noteDelete(8L);
        assertFalse(hold.canRecover(7L, true));
        assertFalse(hold.canRecover(8L, true));
        hold.clear();
        assertTrue(hold.canRecover(8L, true));
    }

    @Test public void burstStaysScopedToEditorUntilSettleOrNewInput() {
        BackspaceSuggestionHold hold = new BackspaceSuggestionHold();
        assertFalse(hold.isActive(7L));
        hold.noteDelete(7L);
        for (int i = 0; i < 30; i++) {
            hold.noteDelete(7L);
            assertTrue(hold.isActive(7L));
        }
        assertFalse(hold.isActive(8L));
        hold.clear();
        assertFalse(hold.isActive(7L));
        assertEquals(185L, BackspaceSuggestionHold.SETTLE_MS);
    }
}
