// SPDX-License-Identifier: Apache-2.0 AND GPL-3.0-only
package helium314.keyboard.keyboard.emoji;

/** Pure row-major page-to-column mapping for the horizontally scrolling Emoji grid. */
final class EmojiColumnPolicy {
    private EmojiColumnPolicy() {}

    static int pageCount(final int keyCount, final int columns, final int rows) {
        final int capacity = Math.max(1, columns) * Math.max(1, rows);
        return Math.max(1, (Math.max(0, keyCount) + capacity - 1) / capacity);
    }

    static int columnCount(final int keyCount, final int columns, final int rows, final int page) {
        final int safeColumns = Math.max(1, columns);
        final int capacity = safeColumns * Math.max(1, rows);
        final int remaining = Math.max(0, keyCount - Math.max(0, page) * capacity);
        return Math.max(1, Math.min(safeColumns, remaining));
    }

    static int keyIndex(final int keyCount, final int columns, final int rows,
            final int page, final int column, final int row) {
        final int safeColumns = Math.max(1, columns);
        final int capacity = safeColumns * Math.max(1, rows);
        final int start = Math.max(0, page) * capacity;
        final int count = Math.min(capacity, Math.max(0, keyCount - start));
        final int activeColumns = columnCount(keyCount, safeColumns, rows, page);
        if (column < 0 || column >= activeColumns || row < 0 || row >= rows) {
            return -1;
        }
        final int offset = row * activeColumns + column;
        return offset < count ? start + offset : -1;
    }

    static int columnWidth(final int fullWidth, final int columns, final int column) {
        final int safeColumns = Math.max(1, columns);
        final int safeWidth = Math.max(safeColumns, fullWidth);
        return (int) (((long) (column + 1) * safeWidth) / safeColumns
                - ((long) column * safeWidth) / safeColumns);
    }
}
