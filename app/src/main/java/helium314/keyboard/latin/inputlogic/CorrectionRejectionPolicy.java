// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.inputlogic;

import java.util.ArrayList;
import java.util.Iterator;

/**
 * Bounded, one-use in-memory suppression of explicitly rejected autocorrections.
 * It never persists words, logs their contents, or changes dictionary scores.
 */
final class CorrectionRejectionPolicy {
    static final long MAX_AGE_MS = 30_000L;
    static final int MAX_RECEIPTS = 4;

    private static final class Receipt {
        final String original;
        final String corrected;
        final Object editor;
        final Object connection;
        final long rejectedAt;

        Receipt(String original, String corrected, Object editor, Object connection, long rejectedAt) {
            this.original = original;
            this.corrected = corrected;
            this.editor = editor;
            this.connection = connection;
            this.rejectedAt = rejectedAt;
        }

        long expiryAt() {
            return rejectedAt > Long.MAX_VALUE - MAX_AGE_MS
                    ? Long.MAX_VALUE : rejectedAt + MAX_AGE_MS;
        }
    }

    private final ArrayList<Receipt> receipts = new ArrayList<>(MAX_RECEIPTS);

    void reject(String typed, String replacement, Object currentEditor,
            Object currentConnection, long now) {
        expire(now);
        if (currentEditor == null || currentConnection == null) {
            clear();
            return;
        }
        if (!receipts.isEmpty() && (receipts.get(0).editor != currentEditor
                || receipts.get(0).connection != currentConnection)) {
            clear();
        }
        if (typed == null || replacement == null || typed.isEmpty() || typed.length() > 64
                || replacement.length() > 64 || typed.equals(replacement)) return;

        // A duplicate rejection must not restart an earlier receipt's lifetime.
        for (Receipt receipt : receipts) {
            if (receipt.original.equals(typed) && receipt.corrected.equals(replacement)) return;
        }
        if (receipts.size() == MAX_RECEIPTS) receipts.remove(0);
        receipts.add(new Receipt(typed, replacement, currentEditor, currentConnection, now));
    }

    /** Keeps unrelated pairs until their original expiry or a genuine editor boundary. */
    boolean consumeIfMatches(String typed, String replacement, Object currentEditor,
            Object currentConnection, long now) {
        expire(now);
        if (receipts.isEmpty()) return false;
        final Receipt first = receipts.get(0);
        if (first.editor != currentEditor || first.connection != currentConnection) {
            clear();
            return false;
        }
        for (Iterator<Receipt> iterator = receipts.iterator(); iterator.hasNext();) {
            final Receipt receipt = iterator.next();
            if (receipt.original.equals(typed) && receipt.corrected.equals(replacement)) {
                iterator.remove();
                return true;
            }
        }
        return false;
    }

    /** Removes receipts that reached their original monotonic deadline. */
    void expire(long now) {
        for (Iterator<Receipt> iterator = receipts.iterator(); iterator.hasNext();) {
            final Receipt receipt = iterator.next();
            if (now < receipt.rejectedAt || now - receipt.rejectedAt >= MAX_AGE_MS) {
                iterator.remove();
            }
        }
    }

    long nextExpiryAt() {
        long earliest = Long.MAX_VALUE;
        for (Receipt receipt : receipts) earliest = Math.min(earliest, receipt.expiryAt());
        return receipts.isEmpty() ? 0L : earliest;
    }

    boolean hasReceipt() { return !receipts.isEmpty(); }

    int receiptCount() { return receipts.size(); }

    void clear() {
        receipts.clear();
    }
}
