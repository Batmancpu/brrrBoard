// SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
package helium314.keyboard.keyboard.emoji;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class EmojiColumnPolicyTest {
    @Test public void fullPagePreservesRowMajorOrderAcrossEightColumns() {
        for (int column = 0; column < 8; column++) {
            assertEquals(column, EmojiColumnPolicy.keyIndex(24, 8, 3, 0, column, 0));
            assertEquals(column + 8, EmojiColumnPolicy.keyIndex(24, 8, 3, 0, column, 1));
            assertEquals(column + 16, EmojiColumnPolicy.keyIndex(24, 8, 3, 0, column, 2));
        }
    }

    @Test public void partialPageDoesNotRepeatOrDropEmoji() {
        assertEquals(2, EmojiColumnPolicy.pageCount(29, 8, 3));
        assertEquals(5, EmojiColumnPolicy.columnCount(29, 8, 3, 1));
        for (int column = 0; column < 5; column++) {
            assertEquals(24 + column, EmojiColumnPolicy.keyIndex(29, 8, 3, 1, column, 0));
            assertEquals(-1, EmojiColumnPolicy.keyIndex(29, 8, 3, 1, column, 1));
        }
        assertEquals(-1, EmojiColumnPolicy.keyIndex(29, 8, 3, 1, 5, 0));
    }

    @Test public void landscapeTwoRowsMakesRemainingEmojiReachable() {
        assertEquals(3, EmojiColumnPolicy.pageCount(32, 7, 2));
        assertEquals(4, EmojiColumnPolicy.columnCount(32, 7, 2, 2));
        assertEquals(31, EmojiColumnPolicy.keyIndex(32, 7, 2, 2, 3, 0));
    }

    @Test public void columnsFillWidthExactlyAndEmptyHistoryKeepsAPlace() {
        int sum = 0;
        for (int column = 0; column < 8; column++) {
            sum += EmojiColumnPolicy.columnWidth(393, 8, column);
        }
        assertEquals(393, sum);
        assertEquals(1, EmojiColumnPolicy.pageCount(0, 8, 3));
        assertEquals(1, EmojiColumnPolicy.columnCount(0, 8, 3, 0));
        assertEquals(-1, EmojiColumnPolicy.keyIndex(0, 8, 3, 0, 0, 0));
    }

    @Test public void everyInputEmojiIsReachableExactlyOnceAcrossPartialPages() {
        for (int rows = 2; rows <= 3; rows++) {
            for (int columns = 5; columns <= 9; columns++) {
                for (int count = 1; count <= 81; count++) {
                    final boolean[] visited = new boolean[count];
                    int seen = 0;
                    for (int page = 0; page < EmojiColumnPolicy.pageCount(count, columns, rows); page++) {
                        final int visible = EmojiColumnPolicy.columnCount(count, columns, rows, page);
                        for (int col = 0; col < visible; col++) {
                            for (int row = 0; row < rows; row++) {
                                final int index = EmojiColumnPolicy.keyIndex(count, columns, rows,
                                        page, col, row);
                                if (index >= 0) {
                                    assertFalse(visited[index]);
                                    visited[index] = true;
                                    seen++;
                                }
                            }
                        }
                    }
                    assertEquals(count, seen);
                }
            }
        }
    }
}
