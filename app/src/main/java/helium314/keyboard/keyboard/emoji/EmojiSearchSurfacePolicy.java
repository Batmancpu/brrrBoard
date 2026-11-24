// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji;

/** Visibility contract for the two fixed-height Search-only siblings above Emoji Browse. */
final class EmojiSearchSurfacePolicy {
    private EmojiSearchSurfacePolicy() {}

    static boolean resultsVisible(final boolean searchMode, final boolean noMatches) {
        return searchMode && !noMatches;
    }

    static boolean emptyVisible(final boolean searchMode, final boolean noMatches) {
        return searchMode && noMatches;
    }

    static boolean shouldMorphResults(final boolean searchMode, final boolean entering,
            final boolean changed, final boolean finalAnswer) {
        return searchMode && !entering && changed && finalAnswer;
    }
}
