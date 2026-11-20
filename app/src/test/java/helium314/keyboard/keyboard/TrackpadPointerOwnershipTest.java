// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard;

import org.junit.Test;
import static org.junit.Assert.*;

public class TrackpadPointerOwnershipTest {
    @Test public void secondFingerSelectsAndThirdFingerCannotStealSelection() {
        final TrackpadPointerOwnership state = new TrackpadPointerOwnership();
        state.activate(5);
        assertTrue(state.isPrimary(5));
        assertTrue(state.addSecondary(8));
        assertTrue(state.isSelecting());
        assertFalse(state.addSecondary(2));
        assertFalse(state.removeSecondary(2));
        assertTrue(state.isSecondary(8));
        assertTrue(state.removeSecondary(8));
        assertFalse(state.isSelecting());
        assertTrue(state.isPrimary(5));
    }

    @Test public void releaseAndReuseCannotResurrectOldSelection() {
        final TrackpadPointerOwnership state = new TrackpadPointerOwnership();
        state.activate(1);
        assertTrue(state.addSecondary(3));
        state.clear();
        assertFalse(state.isPrimary(1));
        assertFalse(state.isSelecting());
        assertFalse(state.addSecondary(4));
        state.activate(3);
        assertFalse(state.addSecondary(3));
        assertTrue(state.addSecondary(1));
    }
}
