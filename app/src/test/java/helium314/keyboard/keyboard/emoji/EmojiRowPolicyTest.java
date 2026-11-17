// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji;

import org.junit.Test;
import static org.junit.Assert.*;

public class EmojiRowPolicyTest {
    @Test public void everyEmojiAppearsExactlyOnceAcrossVirtualRows() {
        for (int count = 0; count <= 980; count += 7) {
            for (int columns = 1; columns <= 12; columns++) {
                final boolean[] found = new boolean[count];
                for (int row = 0; row < EmojiRowPolicy.rowCount(count, columns); row++) {
                    for (int col = 0; col < columns; col++) {
                        final int index = EmojiRowPolicy.keyIndex(count, columns, row, col);
                        if (index < 0) continue;
                        assertFalse(found[index]);
                        found[index] = true;
                    }
                }
                for (boolean present : found) assertTrue(present);
            }
        }
    }

    @Test public void incompleteLastRowAndZeroCountDoNotLosePositions() {
        assertEquals(4, EmojiRowPolicy.rowCount(25, 8));
        assertEquals(24, EmojiRowPolicy.keyIndex(25, 8, 3, 0));
        assertEquals(-1, EmojiRowPolicy.keyIndex(25, 8, 3, 1));
        assertEquals(1, EmojiRowPolicy.rowCount(0, 8));
        assertEquals(-1, EmojiRowPolicy.keyIndex(0, 8, 0, 0));
    }
}
