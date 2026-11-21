// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.brain;

import org.junit.After;
import org.junit.Test;

import java.lang.reflect.Field;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class BrainPerformanceMetricsTest {
    @After public void reset() { BrainPerformanceMetrics.clearSession(); }

    @Test public void percentilesUseRecentBoundedSamplesOnly() {
        BrainPerformanceMetrics.clearSession();
        for (int i = 1; i <= 256; ++i) {
            BrainPerformanceMetrics.record(BrainPerformanceMetrics.Stage.NEXT_WORD, i * 1_000_000L);
        }
        final BrainPerformanceMetrics.Snapshot result =
                BrainPerformanceMetrics.snapshot(BrainPerformanceMetrics.Stage.NEXT_WORD);
        assertEquals(256, result.totalRequests);
        assertEquals(128, result.windowRequests);
        assertEquals(256.0, result.lastMs, 0.001);
        assertEquals(192.5, result.averageMs, 0.001);
        assertEquals(250.0, result.p95Ms, 0.001);
        assertEquals(256.0, result.worstMs, 0.001);
    }

    @Test public void onlyAggregateCountsAndNoWordOrAppFieldsExist() {
        BrainPerformanceMetrics.clearSession();
        BrainPerformanceMetrics.recordSuggestionBatch(5);
        BrainPerformanceMetrics.recordSuggestionBatch(99); // capped per native list size
        BrainPerformanceMetrics.recordManualTap(false);
        BrainPerformanceMetrics.recordManualTap(true);
        final BrainPerformanceMetrics.Activity result = BrainPerformanceMetrics.activity();
        assertEquals(2, result.suggestionBatches);
        assertEquals(23, result.candidatesReturned);
        assertEquals(1, result.textSuggestionTaps);
        assertEquals(1, result.emojiSuggestionTaps);
        for (final Field field : BrainPerformanceMetrics.class.getDeclaredFields()) {
            assertFalse("No raw strings may be retained", field.getType().equals(String.class));
        }
    }

    @Test public void badAndHugeTimingsDoNotSkewDashboard() {
        BrainPerformanceMetrics.clearSession();
        BrainPerformanceMetrics.record(BrainPerformanceMetrics.Stage.SWIPE, -1);
        BrainPerformanceMetrics.record(BrainPerformanceMetrics.Stage.SWIPE, 100_000_000_000L);
        assertEquals(0, BrainPerformanceMetrics.snapshot(
                BrainPerformanceMetrics.Stage.SWIPE).totalRequests);
        BrainPerformanceMetrics.record(BrainPerformanceMetrics.Stage.SWIPE, 2_000_000L);
        assertTrue(BrainPerformanceMetrics.snapshot(
                BrainPerformanceMetrics.Stage.SWIPE).p95Ms > 0);
        BrainPerformanceMetrics.clearSession();
        assertEquals(0, BrainPerformanceMetrics.snapshot(
                BrainPerformanceMetrics.Stage.SWIPE).totalRequests);
    }

    @Test public void keyToPreDrawSampleIsConsumedOnce() {
        BrainPerformanceMetrics.beginKeyToSuggestionPreDraw();
        BrainPerformanceMetrics.recordKeyToSuggestionPreDraw();
        BrainPerformanceMetrics.recordKeyToSuggestionPreDraw();
        assertEquals(1, BrainPerformanceMetrics.snapshot(
                BrainPerformanceMetrics.Stage.KEY_TO_SUGGESTION_PREDRAW).totalRequests);
    }

    @Test public void editorBoundaryCancelsPendingKeyTiming() {
        BrainPerformanceMetrics.beginKeyToSuggestionPreDraw();
        BrainPerformanceMetrics.cancelPendingKeyToSuggestionPreDraw();
        BrainPerformanceMetrics.recordKeyToSuggestionPreDraw();
        assertEquals(0, BrainPerformanceMetrics.snapshot(
                BrainPerformanceMetrics.Stage.KEY_TO_SUGGESTION_PREDRAW).totalRequests);
    }
}
