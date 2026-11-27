// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji;

/** Visibility contract for the two fixed-height Search-only siblings above Emoji Browse. */
final class EmojiSearchSurfacePolicy {
    private EmojiSearchSurfacePolicy() {}

    static boolean needsResultGrid(boolean searchMode, boolean initialized, boolean dirty,
            boolean widthMatches, boolean heightMatches) {
        return searchMode && (!initialized || dirty || !widthMatches || !heightMatches);
    }

    static boolean resultsVisible(final boolean searchMode, final boolean noMatches) {
        return searchMode && !noMatches;
    }

    static boolean emptyVisible(final boolean searchMode, final boolean noMatches) {
        return searchMode && noMatches;
    }

    static boolean canReuseResults(boolean searchMode,boolean entering,boolean finalAnswer,
            boolean changed,boolean widthMatches,boolean surfaceMatches,boolean keysMatch) {
        return searchMode && !entering && finalAnswer && !changed && widthMatches && surfaceMatches && keysMatch;
    }

    static float replacementStartAlpha(float departingAlpha) {
        return 1f - Math.max(0f,Math.min(1f,departingAlpha));
    }

    static boolean shouldMorphResults(final boolean searchMode, final boolean entering,
            final boolean changed, final boolean finalAnswer) {
        return searchMode && !entering && changed && finalAnswer;
    }
}
