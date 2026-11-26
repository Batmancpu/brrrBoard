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

    static boolean matchesSuffix(final CharSequence actual, final CharSequence committed,
            final CharSequence separator) {
        if (actual == null || committed == null || separator == null
                || actual.length() < committed.length() + separator.length()) return false;
        final int start = actual.length() - committed.length() - separator.length();
        for (int i = 0; i < committed.length(); i++) {
            if (actual.charAt(start + i) != committed.charAt(i)) return false;
        }
        for (int i = 0; i < separator.length(); i++) {
            if (actual.charAt(start + committed.length() + i) != separator.charAt(i)) return false;
        }
        return true;
    }

    static boolean matchesPrefixAfterDeletion(final CharSequence before, final CharSequence after,
            final CharSequence committed, final CharSequence separator) {
        if (!matchesSuffix(before, committed, separator) || after == null) return false;
        final int prefixLength = before.length() - committed.length() - separator.length();
        if (after.length() != prefixLength) return false;
        for (int i = 0; i < prefixLength; i++) {
            if (before.charAt(i) != after.charAt(i)) return false;
        }
        return true;
    }

    /** Confirms the restored suffix and untouched preceding editor text after the transaction. */
    static boolean confirmsRestoration(final CharSequence before, final CharSequence after,
            final CharSequence committed, final CharSequence separator, final CharSequence replacement,
            final boolean sameInputConnection, final boolean sameEditor) {
        if (!sameInputConnection || !sameEditor || replacement == null
                || !matchesSuffix(before, committed, separator) || after == null) return false;
        final int prefixLength = before.length() - committed.length() - separator.length();
        if (after.length() != prefixLength + replacement.length()) return false;
        for (int i = 0; i < prefixLength; i++) {
            if (before.charAt(i) != after.charAt(i)) return false;
        }
        for (int i = 0; i < replacement.length(); i++) {
            if (after.charAt(prefixLength + i) != replacement.charAt(i)) return false;
        }
        return true;
    }
}
