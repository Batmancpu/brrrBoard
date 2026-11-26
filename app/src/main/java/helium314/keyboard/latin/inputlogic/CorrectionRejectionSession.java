// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.inputlogic;

import android.os.Handler;
import android.os.SystemClock;

/** Owns the one session-local rejection receipt and its original Handler expiry deadline. */
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
        if (!policy.hasReceipt()) {
            clear();
            return;
        }
        handler.removeCallbacks(expiry);
        expiresAtElapsedRealtime = now + CorrectionRejectionPolicy.MAX_AGE_MS;
        scheduleExpiry();
    }

    boolean consumeIfMatches(String typed, String replacement, Object editor, Object connection, long now) {
        final boolean rejected = policy.consumeIfMatches(typed, replacement, editor, connection, now);
        if (!policy.hasReceipt()) {
            expiresAtElapsedRealtime = 0L;
            handler.removeCallbacks(expiry);
        }
        return rejected;
    }

    private void scheduleExpiry() {
        final long remaining = expiresAtElapsedRealtime - SystemClock.elapsedRealtime();
        handler.postDelayed(expiry, Math.max(0L, remaining));
    }

    private void expireAtOriginalDeadline() {
        if (!policy.hasReceipt()) {
            expiresAtElapsedRealtime = 0L;
            return;
        }
        if (SystemClock.elapsedRealtime() >= expiresAtElapsedRealtime) {
            clear();
        } else {
            // Handler uses uptimeMillis, which pauses during deep sleep. Recheck elapsed time
            // after wake and keep the receipt only until its original elapsed-realtime deadline.
            scheduleExpiry();
        }
    }
}
