// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji;

import org.junit.Test;
import static org.junit.Assert.*;

public class EmojiAxisGesturePolicyTest {
    @Test public void verticalScrollAndDiagonalsAreNeverCategorySwipes() {
        assertFalse(EmojiAxisGesturePolicy.shouldCapture(18, 160, 8));
        assertFalse(EmojiAxisGesturePolicy.shouldSwitch(120, 100, 1700, 900, 390, 8));
        assertFalse(EmojiAxisGesturePolicy.shouldSwitch(40, 5, 80, 0, 390, 8));
    }

    @Test public void deliberateLeftAndRightGesturesAdvanceOneCategory() {
        assertTrue(EmojiAxisGesturePolicy.shouldSwitch(-130, 18, -110, 0, 390, 8));
        assertTrue(EmojiAxisGesturePolicy.shouldSwitch(130, -18, 110, 0, 390, 8));
        assertTrue(EmojiAxisGesturePolicy.shouldSwitch(-50, 2, -990, 80, 390, 8));
    }
}
