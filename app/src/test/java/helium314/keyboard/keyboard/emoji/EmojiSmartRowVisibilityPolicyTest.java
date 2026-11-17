// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import org.junit.Test;

public class EmojiSmartRowVisibilityPolicyTest {
    @Test public void leavingRecentsRemovesReservedRowDuringScroll() {
        assertFalse(EmojiSmartRowVisibilityPolicy.deferAppearance(true, false, true));
        assertFalse(EmojiSmartRowVisibilityPolicy.deferAppearance(true, false, false));
    }

    @Test public void newContextRowWaitsUntilScrollingStops() {
        assertTrue(EmojiSmartRowVisibilityPolicy.deferAppearance(true, true, false));
        assertFalse(EmojiSmartRowVisibilityPolicy.deferAppearance(false, true, false));
        assertFalse(EmojiSmartRowVisibilityPolicy.deferAppearance(true, true, true));
    }
}
