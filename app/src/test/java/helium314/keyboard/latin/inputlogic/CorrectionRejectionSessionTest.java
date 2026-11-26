// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.inputlogic;

import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.LooperMode;

import java.util.concurrent.TimeUnit;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.robolectric.Shadows.shadowOf;

@RunWith(RobolectricTestRunner.class)
@LooperMode(LooperMode.Mode.PAUSED)
public class CorrectionRejectionSessionTest {
    @Test public void unrelatedAttemptsDoNotCancelOrExtendOriginalHandlerDeadline() {
        final var looper = shadowOf(Looper.getMainLooper());
        final var session = new CorrectionRejectionSession(new Handler(Looper.getMainLooper()));
        final Object editor = new Object();
        final Object connection = new Object();
        session.reject("teh", "the", editor, connection, SystemClock.elapsedRealtime());

        looper.idleFor(20_000L, TimeUnit.MILLISECONDS);
        long now = SystemClock.elapsedRealtime();
        assertFalse(session.consumeIfMatches("other", "word", editor, connection, now));
        assertFalse(session.consumeIfMatches("teh", "their", editor, connection, now));
        assertTrue(session.hasReceipt());

        looper.idleFor(9_999L, TimeUnit.MILLISECONDS);
        assertTrue(session.hasReceipt());
        looper.idleFor(1L, TimeUnit.MILLISECONDS);
        assertFalse(session.hasReceipt());
    }

    @Test public void exactMatchAndPrivacyOrConnectionBoundariesClearScheduledReceipt() {
        final var looper = shadowOf(Looper.getMainLooper());
        final var session = new CorrectionRejectionSession(new Handler(Looper.getMainLooper()));
        final Object editor = new Object();
        final Object connection = new Object();

        session.reject("teh", "the", editor, connection, SystemClock.elapsedRealtime());
        assertFalse(session.consumeIfMatches("teh", "the", editor, new Object(),
                SystemClock.elapsedRealtime()));
        assertFalse(session.hasReceipt());
        looper.idleFor(31_000L, TimeUnit.MILLISECONDS);
        assertFalse(session.hasReceipt());

        session.reject("teh", "the", editor, connection, SystemClock.elapsedRealtime());
        assertTrue(session.consumeIfMatches("teh", "the", editor, connection,
                SystemClock.elapsedRealtime()));
        looper.idleFor(31_000L, TimeUnit.MILLISECONDS);
        assertFalse(session.hasReceipt());

        session.reject("teh", "the", editor, connection, SystemClock.elapsedRealtime());
        session.clear(); // Same operation used by incognito, no-learning and editor teardown.
        looper.idleFor(31_000L, TimeUnit.MILLISECONDS);
        assertFalse(session.hasReceipt());
    }
}
