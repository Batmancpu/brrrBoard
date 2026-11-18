// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji;

/** The first Emoji row must not inherit a partly scrolled Search/Browse viewport. */
final class EmojiRowViewportPolicy {
    private EmojiRowViewportPolicy() {}

    static boolean firstRowNeedsAlignment(final int firstRowTop, final int viewportPaddingTop) {
        // Ignore a one-pixel rounding difference across density/font scales.
        return Math.abs(firstRowTop - viewportPaddingTop) > 1;
    }
}
