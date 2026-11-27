// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.internal;

import org.junit.Test;
import static org.junit.Assert.*;

public class GestureTrailWorkPolicyTest {
    @Test public void noBufferBeforeFirstSampleOrAfterFinalClear() {
        assertFalse(GestureTrailWorkPolicy.shouldDraw(false,false,100,100));
        assertTrue(GestureTrailWorkPolicy.shouldDraw(true,false,100,100));
        assertTrue(GestureTrailWorkPolicy.shouldDraw(false,true,100,100));
        assertFalse(GestureTrailWorkPolicy.shouldDraw(false,false,100,100));
        assertFalse(GestureTrailWorkPolicy.shouldDraw(true,true,0,100));
        assertFalse(GestureTrailWorkPolicy.shouldDraw(true,true,100,0));
    }
}
