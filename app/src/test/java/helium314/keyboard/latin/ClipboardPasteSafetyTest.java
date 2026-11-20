package helium314.keyboard.latin;

import org.junit.Test;
import static org.junit.Assert.*;

public class ClipboardPasteSafetyTest {
    @Test public void privateAndSensitivePastesAlwaysRequireExplicitConfirmation() {
        assertEquals(ClipboardPasteSafety.Action.INSERT,
                ClipboardPasteSafety.action(true, true, false, false));
        assertEquals(ClipboardPasteSafety.Action.CONFIRM,
                ClipboardPasteSafety.action(true, true, true, false));
        assertEquals(ClipboardPasteSafety.Action.CONFIRM,
                ClipboardPasteSafety.action(true, true, false, true));
        assertEquals(ClipboardPasteSafety.Action.REJECT,
                ClipboardPasteSafety.action(false, true, false, false));
        assertEquals(ClipboardPasteSafety.Action.REJECT,
                ClipboardPasteSafety.action(true, false, false, false));
    }

    @Test public void foreignOrChangedPrimaryClipCannotBeRemovedByHistoryClear() {
        assertFalse(ClipboardPasteSafety.mayClearOwned(true, false));
        assertFalse(ClipboardPasteSafety.mayClearOwned(false, true));
        assertTrue(ClipboardPasteSafety.mayClearOwned(true, true));
    }
}
