// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.brain;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import helium314.keyboard.latin.LatinIME;
import helium314.keyboard.latin.NgramContext;
import helium314.keyboard.latin.SuggestedWords.SuggestedWordInfo;
import helium314.keyboard.latin.dictionary.Dictionary;
import helium314.keyboard.latin.utils.KtxKt;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

/** Explicit, local-only ContextLM experiment. It never controls automatic correction. */
public final class BrContextLmExperiment {
    public static final String PREF_ENABLED = "brr_context_lm_experiment_enabled";
    private static final String ASSET_ROOT = "contextlm/experimental/";
    private static final int WINDOW = 128;
    private static final ExecutorService LOADER = Executors.newSingleThreadExecutor(task -> {
        final Thread thread = new Thread(task, "brr-contextlm-load");
        thread.setDaemon(true);
        return thread;
    });
    private static final AtomicLong LOAD_GENERATION = new AtomicLong();
    private static final Object METRICS_LOCK = new Object();
    private static final long[] LATENCIES_NS = new long[WINDOW];
    private static int latencySize;
    private static int latencyNext;
    private static long requestsScored;
    private static long batchesReranked;
    private static long loadFailures;
    private static volatile BrContextLmInt8 model;
    private static volatile String loadState = "Not loaded";
    private static final Object CONTEXT_LOCK = new Object();
    private static EditorCache editorCache;
    private static final long MAX_RERANK_NS = 8_000_000L;

    private static final class EditorCache {
        final long generation;
        final BrContextLmInt8 owner;
        final BrContextLmInt8.ContextCache context = new BrContextLmInt8.ContextCache();
        EditorCache(long generation, BrContextLmInt8 owner) {
            this.generation = generation;
            this.owner = owner;
        }
    }

    /** Detach the memo at editor/privacy boundaries; in-flight work cannot republish it. */
    public static void clearContextCache() {
        synchronized (CONTEXT_LOCK) { editorCache = null; }
    }

    private static EditorCache cacheFor(LatinIME ime, long generation, BrContextLmInt8 owner) {
        synchronized (CONTEXT_LOCK) {
            if (ime.editorGeneration() != generation || model != owner) return null;
            if (editorCache == null || editorCache.generation != generation || editorCache.owner != owner)
                editorCache = new EditorCache(generation, owner);
            return editorCache;
        }
    }

    private static boolean cacheIsCurrent(EditorCache cache) {
        synchronized (CONTEXT_LOCK) { return editorCache == cache; }
    }

    private BrContextLmExperiment() { }

    public static boolean isEnabled(@NonNull final Context context) {
        return preferences(context).getBoolean(PREF_ENABLED, false);
    }

    @NonNull public static String status(@NonNull final Context context) {
        if (!isEnabled(context)) return "Off";
        return loadState;
    }

    public static void setEnabled(@NonNull final Context context, final boolean enabled) {
        final SharedPreferences preferences = preferences(context);
        preferences.edit().putBoolean(PREF_ENABLED, enabled).apply();
        final long generation = LOAD_GENERATION.incrementAndGet();
        if (!enabled) {
            model = null;
            clearContextCache();
            loadState = "Not loaded";
            return;
        }
        if (model != null) {
            loadState = "Ready · manual suggestions only";
            return;
        }
        loadState = "Loading on device…";
        final Context app = context.getApplicationContext();
        LOADER.execute(() -> loadExperiment(app, generation));
    }

    private static SharedPreferences preferences(final Context context) {
        return KtxKt.prefs(context.getApplicationContext());
    }

    private static void loadExperiment(final Context context, final long generation) {
        BrContextLmInt8 loaded = null;
        try (InputStream manifest = context.getAssets().open(ASSET_ROOT + "manifest.json");
             InputStream vocab = context.getAssets().open(ASSET_ROOT + "vocab.json");
             InputStream weights = context.getAssets().open(ASSET_ROOT + "weights.int8")) {
            loaded = BrContextLmInt8.loadExperimentalForTesting(manifest, vocab, weights);
        } catch (IOException | RuntimeException ignored) {
            // Keep this generic: diagnostics never retain a word, model input or filesystem path.
        }
        if (generation != LOAD_GENERATION.get() || !isEnabled(context)) return;
        if (loaded == null) {
            synchronized (METRICS_LOCK) { loadFailures++; }
            loadState = "Could not load test model";
            return;
        }
        model = loaded;
        loadState = "Ready · manual suggestions only";
    }

    /**
     * Reranks a bounded set of existing native word candidates in the decoder worker.
     * Caller must enforce normal-prose/no-learning/incognito eligibility and disable this when
     * native auto-correction is active. We repeat the editor-generation check before and after.
     */
    @NonNull public static ArrayList<SuggestedWordInfo> rerankManualSuggestions(
            @NonNull final Context context,
            @NonNull final NgramContext ngramContext,
            @NonNull final ArrayList<SuggestedWordInfo> original,
            final long requestEditorGeneration,
            final boolean eligible) {
        return rerankManualSuggestions(context, ngramContext, original, requestEditorGeneration,
                eligible, -1L, null);
    }

    @NonNull public static ArrayList<SuggestedWordInfo> rerankManualSuggestions(
            @NonNull final Context context,
            @NonNull final NgramContext ngramContext,
            @NonNull final ArrayList<SuggestedWordInfo> original,
            final long requestEditorGeneration,
            final boolean eligible,
            final long requestSerial,
            @Nullable final LongSupplier requestSerialSupplier) {
        final BrContextLmInt8 activeModel = model;
        final LatinIME ime = LatinIME.getInstance();
        if (!eligible || !isEnabled(context) || activeModel == null || ime == null
                || requestEditorGeneration < 0
                || ime.editorGeneration() != requestEditorGeneration
                || requestIsStale(requestSerial, requestSerialSupplier)) return original;
        final ArrayList<Integer> slots = new ArrayList<>();
        final ArrayList<SuggestedWordInfo> candidates = new ArrayList<>();
        final Set<String> seen = new HashSet<>();
        for (int i = 0; i < original.size() && candidates.size() < BrPersonalContextFeatures.MAX_CANDIDATES; i++) {
            if (requestIsStale(requestSerial, requestSerialSupplier)) return original;
            final SuggestedWordInfo info = original.get(i);
            if (info == null || info.mSourceDict == null || info.isEmoji()
                    || info.isKindOf(SuggestedWordInfo.KIND_TYPED)
                    || info.isKindOf(SuggestedWordInfo.KIND_BLACKLIST)
                    || info.isPossiblyOffensive() || info.isExactMatch()
                    || info.isExactMatchWithIntentionalOmission()
                    || info.mScore <= 0 || info.mScore == SuggestedWordInfo.MAX_SCORE
                    || !(info.isKindOf(SuggestedWordInfo.KIND_PREDICTION)
                         || info.isKindOf(SuggestedWordInfo.KIND_COMPLETION)
                         || info.isKindOf(SuggestedWordInfo.KIND_CORRECTION))) continue;
            final String type = info.mSourceDict.mDictType;
            // Public-corpus-trained weights do not contain personal-history labels. Keep those
            // candidates native-ranked until a model has consented, measured personal features.
            if (!Dictionary.TYPE_MAIN.equals(type) && !Dictionary.TYPE_BRR_PERSONAL.equals(type)) continue;
            if (!BrPersonalContextFeatures.isSafeRomanWord(info.mWord)
                    || !activeModel.knowsWord(info.mWord)
                    || !seen.add(info.mWord.toLowerCase(Locale.ROOT))) continue;
            candidates.add(info);
            slots.add(i);
        }
        if (candidates.size() < 2 || requestIsStale(requestSerial, requestSerialSupplier)) return original;

        final int[] nativeScores = new int[candidates.size()];
        final int[] nativeSlots = new int[candidates.size()];
        for (int i = 0; i < candidates.size(); i++) {
            nativeScores[i] = candidates.get(i).mScore;
            nativeSlots[i] = slots.get(i);
        }
        // Neural work cannot change the native list when no eligible visible near-tie exists.
        if (!BrHybridRankingPolicy.hasPotentialAdjacentSwap(nativeScores, nativeSlots)
                || requestIsStale(requestSerial, requestSerialSupplier)) return original;

        final EditorCache cache = cacheFor(ime, requestEditorGeneration, activeModel);
        if (cache == null || requestIsStale(requestSerial, requestSerialSupplier)) return original;

        final BrPersonalContextFeatures.Snapshot featureSnapshot =
                BrPersonalContextFeatures.fromNative(ngramContext, candidates, true, false);
        if (featureSnapshot.candidates.size() < 2
                || requestIsStale(requestSerial, requestSerialSupplier)) return original;
        final long started = System.nanoTime();
        final float[] scores;
        try {
            scores = activeModel.score(featureSnapshot, cache.context,
                    requestSerialSupplier, requestSerial);
        } catch (RuntimeException ignored) {
            return original;
        } finally {
            recordLatency(System.nanoTime() - started);
        }
        if (scores == null) return original;
        synchronized (METRICS_LOCK) { requestsScored++; }
        if (requestIsStale(requestSerial, requestSerialSupplier)
                || ime.editorGeneration() != requestEditorGeneration || !isEnabled(context)
                || model != activeModel || !cacheIsCurrent(cache)
                || System.nanoTime() - started > MAX_RERANK_NS
                || scores.length != slots.size()) return original;

        final int swap = BrHybridRankingPolicy.adjacentSwap(nativeScores, nativeSlots, scores);
        if (swap < 0) return original;
        final ArrayList<SuggestedWordInfo> result = new ArrayList<>(original);
        result.set(slots.get(swap), candidates.get(swap + 1));
        result.set(slots.get(swap + 1), candidates.get(swap));
        synchronized (METRICS_LOCK) { batchesReranked++; }
        return result;
    }

    private static boolean requestIsStale(final long requestSerial,
            @Nullable final LongSupplier requestSerialSupplier) {
        return requestSerialSupplier != null && requestSerialSupplier.getAsLong() != requestSerial;
    }

    private static void recordLatency(final long elapsedNs) {
        synchronized (METRICS_LOCK) {
            LATENCIES_NS[latencyNext] = Math.max(0L, elapsedNs);
            latencyNext = (latencyNext + 1) % WINDOW;
            latencySize = Math.min(WINDOW, latencySize + 1);
        }
    }

    public static Snapshot snapshot() {
        synchronized (METRICS_LOCK) {
            final long[] sorted = Arrays.copyOf(LATENCIES_NS, latencySize);
            Arrays.sort(sorted);
            final double p50 = percentile(sorted, .50);
            final double p95 = percentile(sorted, .95);
            return new Snapshot(requestsScored, batchesReranked, loadFailures,
                    latencySize, p50, p95);
        }
    }

    private static double percentile(final long[] sorted, final double fraction) {
        if (sorted.length == 0) return 0.0;
        final int index = Math.max(0, (int) Math.ceil(sorted.length * fraction) - 1);
        return sorted[index] / 1_000_000.0;
    }

    public static void clearStats() {
        synchronized (METRICS_LOCK) {
            Arrays.fill(LATENCIES_NS, 0L);
            latencySize = 0;
            latencyNext = 0;
            requestsScored = 0;
            batchesReranked = 0;
            loadFailures = 0;
        }
    }

    public static final class Snapshot {
        public final long requestsScored;
        public final long batchesReranked;
        public final long loadFailures;
        public final int latencySamples;
        public final double p50Ms;
        public final double p95Ms;

        private Snapshot(final long requestsScored, final long batchesReranked,
                         final long loadFailures, final int latencySamples,
                         final double p50Ms, final double p95Ms) {
            this.requestsScored = requestsScored;
            this.batchesReranked = batchesReranked;
            this.loadFailures = loadFailures;
            this.latencySamples = latencySamples;
            this.p50Ms = p50Ms;
            this.p95Ms = p95Ms;
        }
    }
}
