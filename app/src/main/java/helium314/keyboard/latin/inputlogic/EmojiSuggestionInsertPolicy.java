// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.inputlogic;

/**
 * Word-preserving, editor-independent spacing rule for suggestion-strip Emoji.
 * It is intentionally more conservative than word-completion/auto-correction:
 * the Emoji action inserts content and never chooses a replacement word.
 */
public final class EmojiSuggestionInsertPolicy {
    private EmojiSuggestionInsertPolicy() {}

    public static boolean needsSpace(final CharSequence textBeforeCursor,
            final boolean automaticSpacingEnabled) {
        if (!automaticSpacingEnabled || textBeforeCursor == null || textBeforeCursor.length() == 0) {
            return false;
        }
        final int last = Character.codePointBefore(textBeforeCursor, textBeforeCursor.length());
        // Preserve existing space/punctuation; don't introduce a leading space.
        final int category = Character.getType(last);
        return Character.isLetterOrDigit(last)
                || category == Character.NON_SPACING_MARK
                || category == Character.COMBINING_SPACING_MARK;
    }
}
