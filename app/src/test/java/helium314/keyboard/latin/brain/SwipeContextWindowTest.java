// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.brain;
import org.junit.Test;
import static org.junit.Assert.*;
public class SwipeContextWindowTest {
    @Test public void idleBoundaryNeverCarriesOldPredictionContext() {
        assertFalse(SwipeContextWindow.expired(0, 900000));
        assertFalse(SwipeContextWindow.expired(100, 300100));
        assertTrue(SwipeContextWindow.expired(100, 300101));
        assertTrue(SwipeContextWindow.expired(900000, 100));
    }
}
