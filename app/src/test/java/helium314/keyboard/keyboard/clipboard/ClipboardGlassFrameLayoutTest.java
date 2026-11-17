// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.clipboard;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class ClipboardGlassFrameLayoutTest {
    @Test public void sharpRowsTerminateAtLiveBottomGlassNotUnderControls() {
        assertEquals(240, ClipboardGlassFrameLayout.sharpViewportBottom(240, 300));
        assertEquals(300, ClipboardGlassFrameLayout.sharpViewportBottom(0, 300));
        assertEquals(300, ClipboardGlassFrameLayout.sharpViewportBottom(400, 300));
    }
}
