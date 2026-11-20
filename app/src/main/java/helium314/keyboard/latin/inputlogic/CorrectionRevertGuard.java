// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.inputlogic;

/** Fail closed when the editor no longer contains the committed correction. */
final class CorrectionRevertGuard {
    private CorrectionRevertGuard() { }

    static boolean matches(final CharSequence actual, final CharSequence committed,
            final CharSequence separator) {
        if (actual == null || committed == null || separator == null) return false;
        final int wordLength = committed.length();
        if (actual.length() != wordLength + separator.length()) return false;
        for (int i = 0; i < wordLength; i++) {
            if (actual.charAt(i) != committed.charAt(i)) return false;
        }
        for (int i = 0; i < separator.length(); i++) {
            if (actual.charAt(wordLength + i) != separator.charAt(i)) return false;
        }
        return true;
    }
}
