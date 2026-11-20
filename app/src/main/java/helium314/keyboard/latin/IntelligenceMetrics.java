// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin;

import java.util.concurrent.atomic.AtomicLongArray;

/**
 * Debug-only, RAM-only aggregate timings. No strings, editor IDs, gesture points,
 * transcripts or clipboard values can be passed to or retained by this probe.
 * The four buckets are counts, not latency claims or persisted telemetry.
 */
public final class IntelligenceMetrics {
    public enum Path { NEXT_WORD, EMOJI_RANK, EMOJI_SEARCH, SWIPE_RECALL }

    private static final int BUCKETS = 4; // <1 ms, <5 ms, <20 ms, >=20 ms
    private static final AtomicLongArray COUNTS =
            new AtomicLongArray(Path.values().length * BUCKETS);

    private IntelligenceMetrics() {}

    public static long begin() {
        return BuildConfig.DEBUG ? System.nanoTime() : 0L;
    }

    public static void end(final Path path, final long startNanos) {
        if (!BuildConfig.DEBUG || startNanos == 0L) return;
        record(path, Math.max(0L, System.nanoTime() - startNanos));
    }

    static void record(final Path path, final long nanos) {
        final int bucket = nanos < 1_000_000L ? 0
                : nanos < 5_000_000L ? 1 : nanos < 20_000_000L ? 2 : 3;
        COUNTS.incrementAndGet(path.ordinal() * BUCKETS + bucket);
    }

    /** Only aggregate counts, never inputs or measured private content. */
    static long[] counts(final Path path) {
        final long[] counts = new long[BUCKETS];
        for (int i = 0; i < BUCKETS; ++i)
            counts[i] = COUNTS.get(path.ordinal() * BUCKETS + i);
        return counts;
    }
}
