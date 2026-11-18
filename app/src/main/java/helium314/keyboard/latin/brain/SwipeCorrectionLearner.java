// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.brain;

import android.content.Context;
import android.util.AtomicFile;
import android.view.inputmethod.EditorInfo;

import androidx.annotation.Nullable;

import helium314.keyboard.keyboard.emoji.EmojiContextEligibility;
import helium314.keyboard.latin.settings.Settings;
import helium314.keyboard.latin.utils.KtxKt;

import java.io.BufferedReader;
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

/**
 * Bounded, local swipe-correction memory: geometric key skeleton -> confirmed word.
 * No gesture coordinates, messages or application identifiers ever reach disk.
 * It is an alternative-candidate prior, never an automatic top-word replacement.
 */
public final class SwipeCorrectionLearner {
    private static final String FILENAME = "brr_swipe_corrections_v1";
    private static final int MAX_PAIRS = 64;
    private static final int MAX_FILE_BYTES = 8192;
    private static final ExecutorService DISK = Executors.newSingleThreadExecutor(r -> {
        final Thread thread = new Thread(r, "brr-swipe-learning");
        thread.setDaemon(true);
        return thread;
    });
    private static SwipeCorrectionLearner instance;

    private final AtomicFile file;
    private final Map<String, Entry> learned = new HashMap<>();
    private long revision;

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
        file = new AtomicFile(new File(context.getNoBackupFilesDir(), FILENAME));
        try {
            final File base = file.getBaseFile();
            if (!base.exists() || base.length() > MAX_FILE_BYTES) return;
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                    file.openRead(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null && learned.size() < MAX_PAIRS) {
                    final String[] tokens = line.split("\\t", -1);
                    if (tokens.length != 4 || !valid(tokens[0]) || !valid(tokens[1])) continue;
                    final int count = Integer.parseInt(tokens[2]);
                    final long last = Long.parseLong(tokens[3]);
                    if (count < 1 || count > 20 || last < 0) continue;
                    learned.put(key(tokens[0], tokens[1]), new Entry(tokens[0], tokens[1], count, last));
                }
            }
        } catch (Exception ignored) {
            learned.clear(); // malformed or incomplete data must fail closed
        }
    }

    public static synchronized SwipeCorrectionLearner get(final Context context) {
        if (instance == null) instance = new SwipeCorrectionLearner(context.getApplicationContext());
        return instance;
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
        final String seed = rawSeed.toLowerCase(Locale.ROOT);
        if (!valid(seed)) return null;
        Entry best = null;
        for (final Entry candidate : learned.values()) {
            if (candidate.count < 2 || !candidate.seed.equals(seed)
                    || !SwipeGeometryRescue.INSTANCE.couldMatchSeed(seed, candidate.word)) continue;
            if (best == null || candidate.count > best.count
                    || (candidate.count == best.count && candidate.lastUsed > best.lastUsed)) best = candidate;
        }
        return best == null ? null : best.word;
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
            snapshot = new ArrayList<>(learned.values());
            version = ++revision;
        }
        DISK.execute(() -> {
            synchronized (SwipeCorrectionLearner.this) {
                // A later write/reset supersedes this snapshot.
                if (revision != version) return;
                FileOutputStream out = null;
                try {
                    out = file.startWrite();
                    for (final Entry entry : snapshot) {
                        final String line = entry.seed + "\t" + entry.word + "\t"
                                + entry.count + "\t" + entry.lastUsed + "\n";
                        out.write(line.getBytes(StandardCharsets.UTF_8));
                    }
                    file.finishWrite(out);
                } catch (Exception ignored) {
                    if (out != null) file.failWrite(out);
                }
            }
        });
    }

    public void clear() {
        synchronized (this) {
            learned.clear();
            ++revision;
        }
        DISK.execute(() -> {
            synchronized (SwipeCorrectionLearner.this) {
                file.delete();
            }
        });
    }
}
