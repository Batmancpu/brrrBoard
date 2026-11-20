// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.brain;

import java.util.Arrays;

/**
 * Per-process, content-free diagnostics for BR Brain. Nothing is written to disk,
 * uploaded, or correlated with a field, app, word, language or timestamp.
 *
 * Timings are computation time per suggestion request, NOT key-to-screen latency,
 * end-to-end accuracy, or neural-model inference time.
 */
public final class BrainPerformanceMetrics {
    public enum Stage { TYPING, NEXT_WORD, SWIPE, SMART_EMOJI }

    private static final int WINDOW = 128;
    private static final long MAX_DURATION_NS = 10_000_000_000L;
    private static final Object LOCK = new Object();
    private static final Window[] WINDOWS = new Window[Stage.values().length];
    private static long suggestionBatches;
    private static long candidatesReturned;
    private static long textSuggestionTaps;
    private static long emojiSuggestionTaps;

    static {
        for (int i = 0; i < WINDOWS.length; ++i) WINDOWS[i] = new Window();
    }

    private BrainPerformanceMetrics() { }

    private static final class Window {
        final long[] samples = new long[WINDOW];
        int next;
        int size;
        long totalCount;
        long lastNs;

        void add(final long ns) {
            samples[next] = ns;
            next = (next + 1) % WINDOW;
            size = Math.min(WINDOW, size + 1);
            totalCount++;
            lastNs = ns;
        }

        Snapshot snapshot() {
            if (size == 0) return new Snapshot(totalCount, 0, 0, 0, 0, 0);
            final long[] ordered = Arrays.copyOf(samples, size);
            long sum = 0;
            for (final long ns : ordered) sum += ns;
            Arrays.sort(ordered);
            final int p95Index = Math.max(0, (int) Math.ceil(size * 0.95) - 1);
            return new Snapshot(totalCount, size, lastNs / 1_000_000.0,
                    sum / (size * 1_000_000.0), ordered[p95Index] / 1_000_000.0,
                    ordered[size - 1] / 1_000_000.0);
        }

        void clear() {
            Arrays.fill(samples, 0);
            next = 0;
            size = 0;
            totalCount = 0;
            lastNs = 0;
        }
    }

    public static final class Snapshot {
        public final long totalRequests;
        public final int windowRequests;
        public final double lastMs;
        public final double averageMs;
        public final double p95Ms;
        public final double worstMs;

        Snapshot(final long totalRequests, final int windowRequests, final double lastMs,
                 final double averageMs, final double p95Ms, final double worstMs) {
            this.totalRequests = totalRequests;
            this.windowRequests = windowRequests;
            this.lastMs = lastMs;
            this.averageMs = averageMs;
            this.p95Ms = p95Ms;
            this.worstMs = worstMs;
        }
    }

    public static final class Activity {
        public final long suggestionBatches;
        public final long candidatesReturned;
        public final long textSuggestionTaps;
        public final long emojiSuggestionTaps;

        Activity(final long batches, final long candidates, final long textTaps, final long emojiTaps) {
            suggestionBatches = batches;
            candidatesReturned = candidates;
            textSuggestionTaps = textTaps;
            emojiSuggestionTaps = emojiTaps;
        }
    }

    public static long begin() { return System.nanoTime(); }

    public static void end(final Stage stage, final long startedNs) {
        if (startedNs <= 0) return;
        final long elapsed = System.nanoTime() - startedNs;
        record(stage, elapsed);
    }

    static void record(final Stage stage, final long durationNs) {
        if (stage == null || durationNs < 0 || durationNs > MAX_DURATION_NS) return;
        synchronized (LOCK) { WINDOWS[stage.ordinal()].add(durationNs); }
    }

    /** Candidate counts only: no candidate text and no claim they were all visible. */
    public static void recordSuggestionBatch(final int candidates) {
        synchronized (LOCK) {
            suggestionBatches++;
            candidatesReturned += Math.max(0, Math.min(18, candidates));
        }
    }

    /** A manually tapped candidate is not evidence of accurate prediction. */
    public static void recordManualTap(final boolean emoji) {
        synchronized (LOCK) {
            if (emoji) emojiSuggestionTaps++; else textSuggestionTaps++;
        }
    }

    public static Snapshot snapshot(final Stage stage) {
        synchronized (LOCK) { return WINDOWS[stage.ordinal()].snapshot(); }
    }

    public static Activity activity() {
        synchronized (LOCK) {
            return new Activity(suggestionBatches, candidatesReturned,
                    textSuggestionTaps, emojiSuggestionTaps);
        }
    }

    /** Clears diagnostics only; never touches learned dictionaries or Emoji history. */
    public static void clearSession() {
        synchronized (LOCK) {
            for (final Window window : WINDOWS) window.clear();
            suggestionBatches = 0;
            candidatesReturned = 0;
            textSuggestionTaps = 0;
            emojiSuggestionTaps = 0;
        }
    }
}
