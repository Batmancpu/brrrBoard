// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EmojiRowViewportPolicyTest {
    @Test public void firstRowMustNotStartPartlyAboveTheViewport() {
        assertTrue(EmojiRowViewportPolicy.firstRowNeedsAlignment(-40, 16));
        assertTrue(EmojiRowViewportPolicy.firstRowNeedsAlignment(0, 16));
    }

    @Test public void firstRowMustNotLeaveAnExtraBlankBand() {
        assertTrue(EmojiRowViewportPolicy.firstRowNeedsAlignment(100, 16));
    }

    @Test public void alreadyAlignedRowsAreNotMoved() {
        assertFalse(EmojiRowViewportPolicy.firstRowNeedsAlignment(16, 16));
        assertFalse(EmojiRowViewportPolicy.firstRowNeedsAlignment(15, 16));
        assertFalse(EmojiRowViewportPolicy.firstRowNeedsAlignment(17, 16));
    }
}
