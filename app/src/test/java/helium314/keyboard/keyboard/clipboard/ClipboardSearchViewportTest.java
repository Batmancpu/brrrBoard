// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.clipboard;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/** A Search result card must not occupy the alphabet or shared ABC/mic footer. */
public class ClipboardSearchViewportTest {
    @Test public void searchViewportStopsAtMeasuredAlphabetTop() {
        assertEquals(600, ClipboardGlassFrameLayout.searchViewportBottom(900, 600, 400));
    }
    @Test public void unavailableTopUsesKeyboardHeightFallback() {
        assertEquals(500, ClipboardGlassFrameLayout.searchViewportBottom(900, 0, 400));
    }
    @Test public void smallWindowIsSafelyClamped() {
        assertEquals(0, ClipboardGlassFrameLayout.searchViewportBottom(900, -1, 1000));
        assertEquals(900, ClipboardGlassFrameLayout.searchViewportBottom(900, 0, 0));
    }
}
