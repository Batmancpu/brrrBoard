// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin;

import org.junit.Test;
import static org.junit.Assert.*;

public class BackspaceSuggestionHoldTest {
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
