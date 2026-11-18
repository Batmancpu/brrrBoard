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

    @Test public void boundedSpringSettlesWithoutHeightOvershootOrFlash() {
        assertEquals(0f, EmojiPanelHeightMotion.springProgress(-1f), 0f);
        assertEquals(0f, EmojiPanelHeightMotion.springProgress(0f), 0f);
        assertEquals(1f, EmojiPanelHeightMotion.springProgress(1f), 0f);
        assertEquals(1f, EmojiPanelHeightMotion.springProgress(2f), 0f);
        float previous = 0f;
        for (int frame = 1; frame <= 120; ++frame) {
            final float progress = EmojiPanelHeightMotion.springProgress(frame / 120f);
            assertTrue("Spring must grow and shrink monotonically", progress >= previous);
            assertTrue("IME window must never overshoot", progress <= 1f);
            previous = progress;
        }
        assertTrue(EmojiPanelHeightMotion.springProgress(0.5f) > 0.80f);
    }
}
