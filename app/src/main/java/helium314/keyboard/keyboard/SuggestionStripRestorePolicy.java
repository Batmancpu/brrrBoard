// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard;

/** Restores typing suggestions only when an ordinary alphabet view owns the strip. */
public final class SuggestionStripRestorePolicy {
    private SuggestionStripRestorePolicy() {}
    public static boolean shouldRestore(final boolean stripConfigured,
            final boolean containerVisible, final boolean stripVisible,
            final boolean searchPanelActive) {
        return stripConfigured && !searchPanelActive && (!containerVisible || !stripVisible);
    }

    /** A posted Emoji Browse callback must never steal the alphabet's strip. */
    public static boolean canEmojiBrowseHideStrip(final boolean emojiPanelVisible,
            final boolean emojiSearchActive) {
        return emojiPanelVisible && !emojiSearchActive;
    }
}
