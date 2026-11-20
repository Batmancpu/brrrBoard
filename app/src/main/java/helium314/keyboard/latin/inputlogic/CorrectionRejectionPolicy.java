// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.inputlogic;

/**
 * One-use, in-memory suppression of an immediately rejected autocorrection.
 * It never persists words, logs their contents, or changes dictionary scores.
 */
final class CorrectionRejectionPolicy {
    private static final long MAX_AGE_MS = 15_000L;
    private String original;
    private String corrected;
    private Object editor;
    private Object connection;
    private long rejectedAt;

    void reject(String typed, String replacement, Object currentEditor,
            Object currentConnection, long now) {
        clear();
        if (typed == null || replacement == null || typed.isEmpty() || typed.length() > 64
                || replacement.length() > 64 || currentEditor == null || currentConnection == null
                || typed.equals(replacement)) return;
        original = typed;
        corrected = replacement;
        editor = currentEditor;
        connection = currentConnection;
        rejectedAt = now;
    }

    /** Always consumes the receipt, including when the next word is unrelated. */
    boolean consumeIfMatches(String typed, String replacement, Object currentEditor,
            Object currentConnection, long now) {
        final boolean matches = original != null && now >= rejectedAt
                && now - rejectedAt <= MAX_AGE_MS
                && editor == currentEditor && connection == currentConnection
                && original.equals(typed) && corrected.equals(replacement);
        clear();
        return matches;
    }

    void clear() {
        original = null;
        corrected = null;
        editor = null;
        connection = null;
        rejectedAt = 0L;
    }
}
