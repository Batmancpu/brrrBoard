// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import android.graphics.Color;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class GestureTrailVisualPolicyTest {
    @Test public void previousDefaultRetainsExactExistingValues() {
        assertEquals(10f, GestureTrailVisualPolicy.startWidth(false, 10f, 2f), 0f);
        assertEquals(2.5f, GestureTrailVisualPolicy.endWidth(false, 2.5f, 2f), 0f);
        assertEquals(20, GestureTrailVisualPolicy.updateInterval(false, 20));
        assertEquals(Color.BLUE, GestureTrailVisualPolicy.color(false, Color.BLUE, Color.WHITE));
    }

    @Test public void iosTaperHasBroadTipHairlineTailAndThemeAwareGray() {
        assertEquals(40f, GestureTrailVisualPolicy.startWidth(true, 10f, 2f), 0f);
        assertEquals(3.2f, GestureTrailVisualPolicy.endWidth(true, 2.5f, 2f), .001f);
        assertEquals(16, GestureTrailVisualPolicy.updateInterval(true, 20));
        assertTrue(GestureTrailVisualPolicy.startWidth(true, 10f, 2f)
                > GestureTrailVisualPolicy.endWidth(true, 2.5f, 2f));
        assertEquals(104, Color.alpha(GestureTrailVisualPolicy.color(true, Color.BLUE, Color.WHITE)));
        assertEquals(Color.BLACK & 0xffffff,
                GestureTrailVisualPolicy.color(true, Color.BLUE, Color.BLACK) & 0xffffff);
    }
}
