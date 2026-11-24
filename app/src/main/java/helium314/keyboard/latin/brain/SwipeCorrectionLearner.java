// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.brain;

import android.content.Context;
import android.util.AtomicFile;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.Nullable;

import helium314.keyboard.keyboard.emoji.EmojiContextEligibility;
import helium314.keyboard.latin.settings.Settings;
import helium314.keyboard.latin.IntelligenceMetrics;
import helium314.keyboard.latin.utils.KtxKt;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.IOException;
import java.util.Arrays;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Bounded, local swipe-correction memory: geometric key skeleton -> confirmed word.
 * No gesture coordinates, messages or application identifiers ever reach disk.
 * It is an alternative-candidate prior, never an automatic top-word replacement.
 */
public final class SwipeCorrectionLearner {
    private static final String FILENAME = "brr_swipe_corrections_v2.aesgcm";
    private static final String LEGACY_FILENAME = "brr_swipe_corrections_v1";
    private static final int MAX_PAIRS = 64;
    private static final int MAX_FILE_BYTES = 8192;
    private static final ExecutorService DISK = Executors.newSingleThreadExecutor(r -> {
        final Thread thread = new Thread(r, "brr-swipe-learning");
        thread.setDaemon(true);
        return thread;
    });
    private static SwipeCorrectionLearner instance;

    private final AtomicFile file;
    private final AtomicFile legacyFile;
    private volatile boolean vaultReadOnly;
    private volatile boolean encryptedPersisted;
    private volatile boolean legacyPending;
    private final Map<String, Entry> learned = new HashMap<>();
    private long revision;
    private final AtomicLong sessionSwipes = new AtomicLong();
    private final AtomicLong sessionCorrections = new AtomicLong();
    private final AtomicLong sessionRecallOffers = new AtomicLong();

    private static final class Entry {
        final String seed;
        final String word;
        final int count;
        final long lastUsed;

        Entry(final String seed, final String word, final int count, final long lastUsed) {
            this.seed = seed;
            this.word = word;
            this.count = count;
            this.lastUsed = lastUsed;
        }
    }

    private SwipeCorrectionLearner(final Context context) {
        final File dir = context.getNoBackupFilesDir();
        file = new AtomicFile(new File(dir, FILENAME));
        legacyFile = new AtomicFile(new File(dir, LEGACY_FILENAME));
        final boolean hasEncrypted = file.getBaseFile().exists();
        final boolean hasLegacy = legacyFile.getBaseFile().exists();
        if (!hasEncrypted && !hasLegacy) return;

        byte[] plaintext = null;
        try {
            if (hasEncrypted) {
                // NEVER overwrite a sealed store that we cannot authenticate.
                plaintext = BrainLearningVault.open(readBounded(file, MAX_FILE_BYTES + 1024));
            } else {
                // Old plaintext is read only for a one-time, lossless migration.
                plaintext = readBounded(legacyFile, MAX_FILE_BYTES);
                legacyPending = true;
            }
            loadEntries(plaintext);
            encryptedPersisted = hasEncrypted;
            if (hasEncrypted && hasLegacy) {
                // A valid authenticated v2 store supersedes the old plaintext.
                DISK.execute(legacyFile::delete);
            } else if (legacyPending) {
                final List<Entry> initial = new ArrayList<>(learned.values());
                final long initialVersion = revision;
                DISK.execute(() -> persistEncrypted(initial, initialVersion));
            }
        } catch (Exception ignored) {
            // Device locked, Keystore invalidated, or corrupted data.
            // Work in memory, leave old/ciphertext bytes intact for recovery.
            vaultReadOnly = true;
            encryptedPersisted = false;
            learned.clear();
        } finally {
            if (plaintext != null) Arrays.fill(plaintext, (byte) 0);
        }
    }

    private static byte[] readBounded(final AtomicFile source, final int max)
            throws IOException {
        try (InputStream in = source.openRead();
             ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            final byte[] buffer = new byte[1024];
            int count;
            while ((count = in.read(buffer)) != -1) {
                if (out.size() + count > max) throw new IOException("Learning file exceeds bound");
                out.write(buffer, 0, count);
            }
            return out.toByteArray();
        }
    }

    private void loadEntries(final byte[] plaintext) throws IOException {
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                new ByteArrayInputStream(plaintext), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null && learned.size() < MAX_PAIRS) {
                final String[] tokens = line.split("\\t", -1);
                if (tokens.length != 4 || !valid(tokens[0]) || !valid(tokens[1])) continue;
                try {
                    final int count = Integer.parseInt(tokens[2]);
                    final long last = Long.parseLong(tokens[3]);
                    if (count < 1 || count > 20 || last < 0) continue;
                    learned.put(key(tokens[0], tokens[1]),
                            new Entry(tokens[0], tokens[1], count, last));
                } catch (NumberFormatException ignored) {
                    // Skip a bad legacy row instead of destroying valid rows.
                }
            }
        }
    }

    private void persistEncrypted(final List<Entry> snapshot, final long version) {
        if (vaultReadOnly) return;
        final ByteArrayOutputStream plain = new ByteArrayOutputStream(MAX_FILE_BYTES);
        try {
            for (final Entry entry : snapshot) {
                final String line = entry.seed + "\t" + entry.word + "\t"
                        + entry.count + "\t" + entry.lastUsed + "\n";
                plain.write(line.getBytes(StandardCharsets.UTF_8));
            }
            final byte[] raw = plain.toByteArray();
            byte[] sealed = null;
            try {
                sealed = BrainLearningVault.seal(raw);
                synchronized (this) {
                    if (revision != version || vaultReadOnly) return;
                    FileOutputStream out = null;
                    try {
                        out = file.startWrite();
                        out.write(sealed);
                        file.finishWrite(out);
                        out = null; // finishWrite closed it; never failWrite afterwards.
                        byte[] recovered = null;
                        boolean verified = false;
                        try {
                            recovered = BrainLearningVault.open(readBounded(file, MAX_FILE_BYTES + 1024));
                            verified = Arrays.equals(raw, recovered);
                        } finally {
                            if (recovered != null) Arrays.fill(recovered, (byte) 0);
                        }
                        if (!verified) throw new IOException("Encrypted learning verification failed");
                        encryptedPersisted = true;
                        if (SwipeLearningMigrationGate.mayDeleteLegacy(true, true, verified)) {
                            legacyPending = false;
                            // Never delete the old store until the new one was
                            // written, authenticated, and checked byte-for-byte.
                            legacyFile.delete();
                        }
                    } catch (Exception ignored) {
                        if (out != null) file.failWrite(out);
                        // Preserve both the legacy and unreadable sealed bytes.
                        // Never fall back to disk plaintext or overwrite v2.
                        vaultReadOnly = true;
                    }
                }
            } finally {
                Arrays.fill(raw, (byte) 0);
                if (sealed != null) Arrays.fill(sealed, (byte) 0);
            }
        } catch (Exception ignored) {
            // If the Keystore is unavailable, new learning remains in RAM only.
            vaultReadOnly = true;
        }
    }

    public static synchronized SwipeCorrectionLearner get(final Context context) {
        if (instance == null) instance = new SwipeCorrectionLearner(context.getApplicationContext());
        return instance;
    }

    /** Read-only live diagnostics. Counts are sourced from actual learner events. */
    public static final class Snapshot {
        public final int storedPairs;
        public final int readyPairs;
        public final int totalConfirmationsInStore;
        public final long swipesThisSession;
        public final long confirmedCorrectionsThisSession;
        public final long recallOffersThisSession;
        public final List<String> recentAssociations;
        public final String storageStatus;

        Snapshot(int stored, int ready, int total, long swipes, long corrections,
                long recallOffers, List<String> associations, String status) {
            storedPairs = stored;
            readyPairs = ready;
            totalConfirmationsInStore = total;
            swipesThisSession = swipes;
            confirmedCorrectionsThisSession = corrections;
            recallOffersThisSession = recallOffers;
            recentAssociations = java.util.Collections.unmodifiableList(associations);
            storageStatus = status;
        }
    }

    /** Do not log the swipe geometry or editor text. Only count eligible gestures. */
    public void recordSwipeAttempt() { sessionSwipes.incrementAndGet(); }

    public synchronized Snapshot snapshot() {
        int ready = 0;
        int confirmations = 0;
        final List<Entry> sorted = new ArrayList<>(learned.values());
        sorted.sort(Comparator.comparingInt((Entry e) -> e.count).reversed()
                .thenComparingLong(e -> -e.lastUsed));
        final List<String> examples = new ArrayList<>();
        for (final Entry e : sorted) {
            confirmations += e.count;
            if (e.count >= 2) ready++;
            if (examples.size() < 8) {
                examples.add(e.seed + "  →  " + e.word
                        + (e.count >= 2 ? "  · ready" : "  · learning"));
            }
        }
        return new Snapshot(learned.size(), ready, confirmations,
                sessionSwipes.get(), sessionCorrections.get(),
                sessionRecallOffers.get(), examples,
                vaultReadOnly ? "Vault unavailable · in-memory only (no plaintext fallback)"
                : encryptedPersisted ? "Encrypted on device · Android Keystore AES-256-GCM"
                : legacyPending ? "Migrating existing corrections into encrypted vault"
                : "Ready · no encrypted corrections saved yet");
    }

    /** Existing encrypted associations only; no new data is collected for inspection. */
    public static final class Association {
        public final String seed, word;
        public final int count;
        Association(Entry e) { seed=e.seed; word=e.word; count=e.count; }
    }
    public synchronized List<Association> associations() {
        final List<Entry> rows=new ArrayList<>(learned.values());
        rows.sort(Comparator.comparing((Entry e)->e.seed).thenComparing(e->e.word));
        final List<Association> result=new ArrayList<>(rows.size());
        for(Entry e:rows) result.add(new Association(e));
        return java.util.Collections.unmodifiableList(result);
    }
    /** Remove exactly one existing pair; never overwrite an unavailable encrypted vault. */
    public boolean forget(String seed,String word) {
        final List<Entry> snapshot;
        final long version;
        synchronized(this) {
            if(vaultReadOnly || learned.remove(key(seed,word))==null) return false;
            snapshot=new ArrayList<>(learned.values());
            version=++revision;
        }
        DISK.execute(()->persistEncrypted(snapshot,version));
        return true;
    }

    public static boolean isAllowed(final Context context, @Nullable final EditorInfo editor,
            final boolean incognito, final boolean personalizedSuggestionsEnabled) {
        return personalizedSuggestionsEnabled && EmojiContextEligibility.isEligible(editor, incognito)
                && KtxKt.prefs(context).getBoolean(Settings.PREF_KEY_USE_PERSONALIZED_DICTS, true)
                && KtxKt.prefs(context).getBoolean(Settings.PREF_LEARN_SWIPE_CORRECTIONS, true);
    }

    private static String key(final String seed, final String word) { return seed + "/" + word; }

    private static boolean valid(@Nullable final String word) {
        if (word == null || word.length() < 2 || word.length() > 20) return false;
        for (int i = 0; i < word.length(); i++) {
            if (word.charAt(i) < 'a' || word.charAt(i) > 'z') return false;
        }
        return true;
    }

    /** Require two separate explicit corrections before a word can be recalled. */
    public synchronized @Nullable String suggest(final String rawSeed) {
        final long started = IntelligenceMetrics.begin();
        try {
        final String seed = rawSeed.toLowerCase(Locale.ROOT);
        if (!valid(seed)) return null;
        Entry best = null;
        Entry runnerUp = null;
        for (final Entry candidate : learned.values()) {
            if (candidate.count < 2 || !candidate.seed.equals(seed)
                    || !SwipeGeometryRescue.INSTANCE.couldMatchSeed(seed, candidate.word)) continue;
            if (best == null || candidate.count > best.count
                    || (candidate.count == best.count && candidate.lastUsed > best.lastUsed)) {
                runnerUp = best;
                best = candidate;
            } else if (runnerUp == null || candidate.count > runnerUp.count) {
                runnerUp = candidate;
            }
        }
        // Two equally supported corrections for the same thumb path mean the
        // learned evidence is ambiguous. Do not inject an arbitrary alternative
        // merely because one happened a few milliseconds more recently.
        if (best != null && runnerUp != null && best.count == runnerUp.count) return null;
        if (best != null) sessionRecallOffers.incrementAndGet();
        return best == null ? null : best.word;
        } finally {
            IntelligenceMetrics.end(IntelligenceMetrics.Path.SWIPE_RECALL, started);
        }
    }

    public void confirm(final String rawSeed, final String rawWord) {
        final String seed = rawSeed.toLowerCase(Locale.ROOT);
        final String word = rawWord.toLowerCase(Locale.ROOT);
        if (!valid(seed) || !valid(word) || seed.equals(word)
                || !SwipeGeometryRescue.INSTANCE.couldMatchSeed(seed, word)) return;
        final List<Entry> snapshot;
        final long version;
        synchronized (this) {
            final String key = key(seed, word);
            final Entry previous = learned.get(key);
            learned.put(key, new Entry(seed, word,
                    previous == null ? 1 : Math.min(previous.count + 1, 20), System.currentTimeMillis()));
            if (learned.size() > MAX_PAIRS) {
                final Entry oldest = learned.values().stream().min(Comparator.comparingLong(e -> e.lastUsed)).orElse(null);
                if (oldest != null) learned.remove(key(oldest.seed, oldest.word));
            }
            sessionCorrections.incrementAndGet();
            snapshot = new ArrayList<>(learned.values());
            version = ++revision;
        }
        DISK.execute(() -> persistEncrypted(snapshot, version));
    }

    public void clear() {
        synchronized (this) {
            learned.clear();
            ++revision;
            vaultReadOnly = false;
            encryptedPersisted = false;
            legacyPending = false;
        }
        DISK.execute(() -> {
            synchronized (SwipeCorrectionLearner.this) {
                file.delete();
                legacyFile.delete();
            }
        });
    }
}
