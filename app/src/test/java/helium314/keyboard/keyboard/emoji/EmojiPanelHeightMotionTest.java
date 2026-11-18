// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EmojiPanelHeightMotionTest {
    @Test public void onlyVisibleDeliberateModeHeightChangesAnimate() {
        assertTrue(EmojiPanelHeightMotion.canAnimate(300, 470, 600, true, true));
        assertTrue(EmojiPanelHeightMotion.canAnimate(470, 300, 600, true, true));
        assertFalse(EmojiPanelHeightMotion.canAnimate(0, 470, 600, true, true));
        assertFalse(EmojiPanelHeightMotion.canAnimate(300, 300, 600, true, true));
        assertFalse(EmojiPanelHeightMotion.canAnimate(300, 470, 600, false, true));
        assertFalse(EmojiPanelHeightMotion.canAnimate(300, 470, 600, true, false));
    }

    @Test public void constrainedWindowsDoNotAnimateBeyondAvailableHeight() {
        assertFalse(EmojiPanelHeightMotion.canAnimate(300, 470, 350, true, true));
        assertEquals(350, EmojiPanelHeightMotion.bound(470, 350));
        assertEquals(1, EmojiPanelHeightMotion.bound(-4, 0));
    }
}
