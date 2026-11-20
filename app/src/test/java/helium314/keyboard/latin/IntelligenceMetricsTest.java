package helium314.keyboard.latin;

import org.junit.Test;
import static org.junit.Assert.assertArrayEquals;

public class IntelligenceMetricsTest {
    @Test public void syntheticTimingBucketsHaveNoContentFields() {
        final long[] before = IntelligenceMetrics.counts(IntelligenceMetrics.Path.NEXT_WORD);
        IntelligenceMetrics.record(IntelligenceMetrics.Path.NEXT_WORD, 0);
        IntelligenceMetrics.record(IntelligenceMetrics.Path.NEXT_WORD, 1_000_000);
        IntelligenceMetrics.record(IntelligenceMetrics.Path.NEXT_WORD, 5_000_000);
        IntelligenceMetrics.record(IntelligenceMetrics.Path.NEXT_WORD, 20_000_000);
        final long[] after = IntelligenceMetrics.counts(IntelligenceMetrics.Path.NEXT_WORD);
        assertArrayEquals(new long[]{before[0] + 1, before[1] + 1,
                before[2] + 1, before[3] + 1}, after);
    }
}
