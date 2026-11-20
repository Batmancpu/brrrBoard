// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class TrackpadHostSelectionTest {
    @Test public void hostCanExtendShrinkAndReverseFromFixedAnchor() {
        assertEquals(10, TrackpadHostSelection.confirmedHead(5, 5, 5, 10));
        assertEquals(7, TrackpadHostSelection.confirmedHead(5, 10, 5, 7));
        assertEquals(3, TrackpadHostSelection.confirmedHead(5, 7, 3, 5));
        assertEquals(5, TrackpadHostSelection.confirmedHead(5, 3, 5, 5));
    }

    @Test public void delayedOrUncooperativeHostNeverFabricatesPosition() {
        assertEquals(TrackpadHostSelection.WAITING,
                TrackpadHostSelection.confirmedHead(5, 10, 5, 10));
        assertEquals(TrackpadHostSelection.INVALID,
                TrackpadHostSelection.confirmedHead(5, 10, 6, 11));
        assertEquals(TrackpadHostSelection.INVALID,
                TrackpadHostSelection.confirmedHead(5, 10, -1, -1));
        assertEquals(TrackpadHostSelection.INVALID,
                TrackpadHostSelection.confirmedHead(5, 10, 9, 8));
    }
}
