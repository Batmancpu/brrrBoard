// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.inputlogic;

import org.junit.Test;
import static org.junit.Assert.*;

public class CorrectionRejectionPolicyTest {
    @Test public void rejectionOnlySuppressesSameCorrectionOnceInSameEditor() {
        final var policy = new CorrectionRejectionPolicy();
        final var editor = new Object();
        final var connection = new Object();
        policy.reject("teh", "the", editor, connection, 100L);
        assertTrue(policy.consumeIfMatches("teh", "the", editor, connection, 200L));
        assertFalse(policy.consumeIfMatches("teh", "the", editor, connection, 201L));
    }

    @Test public void differentEditorInvalidatesButUnrelatedCommitPreservesReceipt() {
        final var policy = new CorrectionRejectionPolicy();
        final var editor = new Object();
        final var connection = new Object();
        policy.reject("teh", "the", editor, connection, 100L);
        assertFalse(policy.consumeIfMatches("teh", "the", new Object(), connection, 200L));
        assertFalse(policy.consumeIfMatches("teh", "the", editor, connection, 201L));
        policy.reject("teh", "the", editor, connection, 300L);
        assertFalse(policy.consumeIfMatches("other", "better", editor, connection, 310L));
        assertTrue(policy.hasReceipt());
        assertFalse(policy.consumeIfMatches("teh", "their", editor, connection, 311L));
        assertTrue(policy.consumeIfMatches("teh", "the", editor, connection, 312L));
        assertFalse(policy.hasReceipt());
    }

    @Test public void expiryAndClearNeverKeepSuppressionAlive() {
        final var policy = new CorrectionRejectionPolicy();
        final var editor = new Object();
        final var connection = new Object();
        policy.reject("teh", "the", editor, connection, 100L);
        assertFalse(policy.consumeIfMatches("teh", "the", editor, connection, 30_100L));
        assertFalse(policy.hasReceipt());
        policy.reject("teh", "the", editor, connection, 100L);
        assertTrue(policy.consumeIfMatches("teh", "the", editor, connection, 30_099L));
        policy.reject("teh", "the", editor, connection, 100L);
        assertFalse(policy.consumeIfMatches("teh", "the", editor, connection, 99L));
        policy.reject("teh", "the", editor, connection, 100L);
        policy.clear();
        assertFalse(policy.consumeIfMatches("teh", "the", editor, connection, 110L));
    }

    @Test public void multiplePairsAreConsumedIndependentlyWithoutRefreshingDuplicateExpiry() {
        final var policy = new CorrectionRejectionPolicy();
        final var editor = new Object();
        final var connection = new Object();
        policy.reject("teh", "the", editor, connection, 100L);
        policy.reject("wurld", "world", editor, connection, 200L);
        assertEquals(2, policy.receiptCount());
        policy.reject("teh", "the", editor, connection, 20_000L);
        assertEquals(30_100L, policy.nextExpiryAt());
        assertTrue(policy.consumeIfMatches("teh", "the", editor, connection, 30_099L));
        assertEquals(1, policy.receiptCount());
        assertFalse(policy.consumeIfMatches("wurld", "world", editor, connection, 30_200L));
        assertFalse(policy.hasReceipt());
    }

    @Test public void receiptCountIsBoundedAndEvictsTheOldestPair() {
        final var policy = new CorrectionRejectionPolicy();
        final var editor = new Object();
        final var connection = new Object();
        for (int i = 0; i < CorrectionRejectionPolicy.MAX_RECEIPTS + 1; i++) {
            policy.reject("typed" + i, "fixed" + i, editor, connection, 100L + i);
        }
        assertEquals(CorrectionRejectionPolicy.MAX_RECEIPTS, policy.receiptCount());
        assertFalse(policy.consumeIfMatches("typed0", "fixed0", editor, connection, 200L));
        for (int i = 1; i <= CorrectionRejectionPolicy.MAX_RECEIPTS; i++) {
            assertTrue(policy.consumeIfMatches("typed" + i, "fixed" + i, editor, connection, 200L));
        }
        assertFalse(policy.hasReceipt());
    }

    @Test public void unrelatedAttemptsDoNotRefreshOriginalExpiry() {
        final var policy = new CorrectionRejectionPolicy();
        final var editor = new Object();
        final var connection = new Object();
        policy.reject("teh", "the", editor, connection, 1_000L);
        assertFalse(policy.consumeIfMatches("other", "word", editor, connection, 20_000L));
        assertFalse(policy.consumeIfMatches("teh", "their", editor, connection, 29_000L));
        assertFalse(policy.consumeIfMatches("teh", "the", editor, connection, 31_000L));
        assertFalse(policy.hasReceipt());
    }
}
