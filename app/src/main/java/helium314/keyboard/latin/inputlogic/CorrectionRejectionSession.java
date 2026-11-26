// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.inputlogic;

import android.os.Handler;
import android.os.SystemClock;

/** Owns bounded session-local rejection receipts and their original Handler deadlines. */
final class CorrectionRejectionSession {
    private final CorrectionRejectionPolicy policy = new CorrectionRejectionPolicy();
    private final Handler handler;
    private long expiresAtElapsedRealtime;
    private final Runnable expiry = this::expireAtOriginalDeadline;

    CorrectionRejectionSession(final Handler handler) {
        this.handler = handler;
    }

    boolean hasReceipt() {
        return policy.hasReceipt();
    }

    void clear() {
        policy.clear();
        expiresAtElapsedRealtime = 0L;
        handler.removeCallbacks(expiry);
    }

    void reject(String typed, String replacement, Object editor, Object connection, long now) {
        policy.reject(typed, replacement, editor, connection, now);
        scheduleExpiry();
    }

    /** Applies the live privacy eligibility decision and clears receipts when ineligible. */
    boolean consumeIfEligible(boolean eligible, String typed, String replacement, Object editor,
            Object connection, long now) {
        if (!eligible) {
            clear();
            return false;
        }
        return consumeIfMatches(typed, replacement, editor, connection, now);
    }

    boolean consumeIfMatches(String typed, String replacement, Object editor, Object connection, long now) {
        final boolean rejected = policy.consumeIfMatches(typed, replacement, editor, connection, now);
        scheduleExpiry();
        return rejected;
    }

    private void scheduleExpiry() {
        handler.removeCallbacks(expiry);
        expiresAtElapsedRealtime = policy.nextExpiryAt();
        if (expiresAtElapsedRealtime == 0L) return;
        final long remaining = expiresAtElapsedRealtime - SystemClock.elapsedRealtime();
        handler.postDelayed(expiry, Math.max(0L, remaining));
    }

    private void expireAtOriginalDeadline() {
        policy.expire(SystemClock.elapsedRealtime());
        scheduleExpiry();
    }
}
