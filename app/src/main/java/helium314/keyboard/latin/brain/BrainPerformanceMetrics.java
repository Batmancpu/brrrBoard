// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.brain;

import java.util.Arrays;

/**
 * Per-process, content-free diagnostics for BR Brain. Nothing is written to disk,
 * uploaded, or correlated with a field, app, word, language or timestamp.
 *
 * Model-stage timings are computation time per suggestion request. The separate
 * key-to-pre-draw timing measures the first strip draw after a letter key event;
 * it is not a guarantee that a frame reached the display or evidence of accuracy.
 */
public final class BrainPerformanceMetrics {
    public enum Stage {
        TYPING, NEXT_WORD, SWIPE, SMART_EMOJI, KEY_TO_INPUT_TRANSACTION,
        STRIP_UPDATE, KEY_TO_SUGGESTION_PREDRAW
    }

    private static final int WINDOW = 128;
    private static final long MAX_DURATION_NS = 10_000_000_000L;
    private static final long MAX_KEY_TO_STRIP_NS = 2_000_000_000L;
    private static final Object LOCK = new Object();
    private static final Window[] WINDOWS = new Window[Stage.values().length];
    private static long suggestionBatches;
    private static long candidatesReturned;
    private static long textSuggestionTaps;
    private static long emojiSuggestionTaps;
    private static long pendingKeyNs;

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
            if (size == 0) return new Snapshot(totalCount, 0, 0, 0, 0, 0, 0, 0);
            final long[] ordered = Arrays.copyOf(samples, size);
            long sum = 0;
            for (final long ns : ordered) sum += ns;
            Arrays.sort(ordered);
            final int p50Index = Math.max(0, (int) Math.ceil(size * 0.50) - 1);
            final int p95Index = Math.max(0, (int) Math.ceil(size * 0.95) - 1);
            final int p99Index = Math.max(0, (int) Math.ceil(size * 0.99) - 1);
            return new Snapshot(totalCount, size, lastNs / 1_000_000.0,
                    sum / (size * 1_000_000.0), ordered[p50Index] / 1_000_000.0,
                    ordered[p95Index] / 1_000_000.0, ordered[p99Index] / 1_000_000.0,
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
        public final double p50Ms;
        public final double p95Ms;
        public final double p99Ms;
        public final double worstMs;

        Snapshot(final long totalRequests, final int windowRequests, final double lastMs,
                 final double averageMs, final double p50Ms, final double p95Ms,
                 final double p99Ms, final double worstMs) {
            this.totalRequests = totalRequests;
            this.windowRequests = windowRequests;
            this.lastMs = lastMs;
            this.averageMs = averageMs;
            this.p50Ms = p50Ms;
            this.p95Ms = p95Ms;
            this.p99Ms = p99Ms;
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

    /** Starts one content-free sample; the next strip pre-draw consumes it. */
    public static void beginKeyToSuggestionPreDraw() {
        synchronized (LOCK) { pendingKeyNs = System.nanoTime(); }
    }

    /** Records at most one pending sample and rejects stale or implausible timings. */
    public static void recordKeyToSuggestionPreDraw() {
        final long started;
        synchronized (LOCK) {
            started = pendingKeyNs;
            pendingKeyNs = 0;
        }
        if (started > 0) {
            final long elapsed = System.nanoTime() - started;
            if (elapsed <= MAX_KEY_TO_STRIP_NS) record(Stage.KEY_TO_SUGGESTION_PREDRAW, elapsed);
        }
    }

    public static void cancelPendingKeyToSuggestionPreDraw() {
        synchronized (LOCK) { pendingKeyNs = 0; }
    }

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
            pendingKeyNs = 0;
        }
    }
}
