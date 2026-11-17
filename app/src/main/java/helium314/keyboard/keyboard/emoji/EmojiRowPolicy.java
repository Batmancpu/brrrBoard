// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji;

/** B10V: vertically virtualized, row-major Emoji grid mapping. */
final class EmojiRowPolicy {
    private EmojiRowPolicy() {}

    static int rowCount(final int keyCount, final int columns) {
        return Math.max(1, (Math.max(0, keyCount) + Math.max(1, columns) - 1)
                / Math.max(1, columns));
    }

    static int keyIndex(final int keyCount, final int columns,
            final int row, final int column) {
        if (row < 0 || column < 0 || column >= Math.max(1, columns)) return -1;
        final long index = (long) row * Math.max(1, columns) + column;
        return index >= keyCount ? -1 : (int) index;
    }
}
