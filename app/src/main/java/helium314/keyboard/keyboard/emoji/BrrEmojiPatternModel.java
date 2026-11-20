// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Bounded, local Emoji-to-context associations. No sentence or app name is stored.
 * The caller encrypts serialized records; fingerprints are not anonymous by themselves.
 */
final class BrrEmojiPatternModel {
    static final int MAX_RECORDS = 144;
    static final long MAX_AGE_MS = 45L * 24L * 60L * 60L * 1000L;
    private static final Pattern WORDS = Pattern.compile("[\\p{L}][\\p{L}\\p{M}'’]*");
    private static final int MAX_INPUT_CHARS = 160;
    private final LinkedHashMap<String, Record> records = new LinkedHashMap<>();

    static final class Prediction {
        final String emoji;
        final int confidence;
        Prediction(final String emoji, final int confidence) {
            this.emoji = emoji;
            this.confidence = confidence;
        }
    }

    private static final class Record {
        String emoji;
        int count;
        long lastUsed;
        Record(final String emoji, final int count, final long lastUsed) {
            this.emoji = emoji;
            this.count = count;
            this.lastUsed = lastUsed;
        }
    }

    private static List<String> suffixes(final CharSequence context) {
        if (context == null || context.length() == 0) return List.of();
        final String text = context.subSequence(Math.max(0, context.length() - MAX_INPUT_CHARS),
                context.length()).toString().toLowerCase(Locale.ROOT);
        if (text.indexOf('@') >= 0 || text.indexOf('/') >= 0
                || text.indexOf('\\') >= 0 || text.indexOf('=') >= 0
                || text.indexOf(':') >= 0) return List.of();
        final Matcher matcher = WORDS.matcher(text);
        final ArrayList<String> tokens = new ArrayList<>();
        while (matcher.find()) {
            final String word = matcher.group();
            if (word.length() > 30 || word.length() < 2) continue;
            tokens.add(word);
            if (tokens.size() > 18) tokens.remove(0);
        }
        final ArrayList<String> suffixes = new ArrayList<>();
        final int size = tokens.size();
        for (int n = 1; n <= 3 && n <= size; ++n) {
            suffixes.add(String.join(" ", tokens.subList(size - n, size)));
        }
        return suffixes;
    }

    private static String fingerprint(final String phrase) {
        try {
            final byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(phrase.getBytes(StandardCharsets.UTF_8));
            final StringBuilder out = new StringBuilder(20);
            for (int i = 0; i < 10; ++i) {
                out.append(Character.forDigit((digest[i] >>> 4) & 0xf, 16));
                out.append(Character.forDigit(digest[i] & 0xf, 16));
            }
            return out.toString();
        } catch (NoSuchAlgorithmException impossible) {
            throw new IllegalStateException(impossible);
        }
    }

    static String appScope(final String packageName, final int inputVariation) {
        if (packageName == null || packageName.isEmpty()) return "";
        return "a" + fingerprint(packageName) + "f" + inputVariation;
    }

    void learn(final String appScope, final CharSequence context, final String emoji,
            final long now) {
        if (emoji == null || emoji.isEmpty() || emoji.length() > 32 || now <= 0
                || emoji.indexOf('\t') >= 0 || emoji.indexOf('\n') >= 0) return;
        final List<String> suffixes = suffixes(context);
        if (suffixes.isEmpty()) return;
        prune(now);
        for (int n = 0; n < suffixes.size(); ++n) {
            final String digest = fingerprint(suffixes.get(n));
            increment("g", digest, emoji, now);
            if (appScope != null && !appScope.isEmpty()) increment(appScope, digest, emoji, now);
        }
        trim();
    }

    private void increment(final String scope, final String digest, final String emoji, final long now) {
        final String key = scope + "." + digest + "." + emoji;
        final Record previous = records.remove(key);
        final int count = previous == null ? 1 : Math.min(32, previous.count + 1);
        records.put(key, new Record(emoji, count, now));
    }

    List<Prediction> predict(final String appScope, final CharSequence context, final long now) {
        final List<String> suffixes = suffixes(context);
        if (suffixes.isEmpty()) return List.of();
        final HashMap<String, Integer> scores = new HashMap<>();
        for (int n = 0; n < suffixes.size(); ++n) {
            final int wordCount = n + 1;
            final String digest = fingerprint(suffixes.get(n));
            addScores(scores, "g." + digest + ".", wordCount, false, now);
            if (appScope != null && !appScope.isEmpty()) {
                addScores(scores, appScope + "." + digest + ".", wordCount, true, now);
            }
        }
        final ArrayList<Prediction> ranked = new ArrayList<>();
        for (final Map.Entry<String, Integer> entry : scores.entrySet()) {
            ranked.add(new Prediction(entry.getKey(), entry.getValue()));
        }
        ranked.sort(Comparator.comparingInt((Prediction p) -> p.confidence)
                .reversed().thenComparing(p -> p.emoji));
        return ranked;
    }

    private void addScores(final Map<String, Integer> output, final String prefix,
            final int words, final boolean scoped, final long now) {
        for (final Map.Entry<String, Record> entry : records.entrySet()) {
            if (!entry.getKey().startsWith(prefix)) continue;
            final Record record = entry.getValue();
            final long age = now - record.lastUsed;
            if (age < 0 || age > MAX_AGE_MS) continue;
            final int effectiveCount = Math.max(0, record.count - (int) (age / (14L * 86400000L)));
            if (effectiveCount == 0) continue;
            final int confidence = 165 + 85 * words + 36 * Math.min(8, effectiveCount)
                    + (scoped ? 45 : 0);
            output.merge(record.emoji, confidence, Math::max);
        }
    }

    String serialize(final long now) {
        prune(now);
        final StringBuilder result = new StringBuilder("v1\n");
        for (final Map.Entry<String, Record> entry : records.entrySet()) {
            final Record record = entry.getValue();
            result.append(entry.getKey()).append('\t').append(record.count)
                    .append('\t').append(record.lastUsed).append('\n');
        }
        return result.toString();
    }

    void deserialize(final String serialized, final long now) {
        records.clear();
        if (serialized == null || !serialized.startsWith("v1\n") || serialized.length() > 15200) return;
        final String[] lines = serialized.split("\n");
        for (int i = 1; i < lines.length && i <= MAX_RECORDS; ++i) {
            final String[] pieces = lines[i].split("\\t", -1);
            if (pieces.length != 3) continue;
            final String key = pieces[0];
            final String[] split = key.split("\\.", 3);
            if (split.length != 3 || !split[1].matches("[0-9a-f]{20}")
                    || !(split[0].equals("g") || split[0].matches("a[0-9a-f]{20}f[0-9]+"))) continue;
            try {
                final String emoji = split[2];
                final int count = Integer.parseInt(pieces[1]);
                final long lastUsed = Long.parseLong(pieces[2]);
                if (emoji.isEmpty() || emoji.length() > 32 || count < 1 || count > 32
                        || lastUsed > now || now - lastUsed > MAX_AGE_MS) continue;
                records.put(key, new Record(emoji, count, lastUsed));
            } catch (IllegalArgumentException ignored) { // Corrupt individual record is skipped.
            }
        }
        trim();
    }

    int size() { return records.size(); }

    private void prune(final long now) {
        final Iterator<Map.Entry<String, Record>> iterator = records.entrySet().iterator();
        while (iterator.hasNext()) {
            final Record record = iterator.next().getValue();
            if (record.lastUsed > now || now - record.lastUsed > MAX_AGE_MS) iterator.remove();
        }
    }

    private void trim() {
        while (records.size() > MAX_RECORDS) records.remove(records.keySet().iterator().next());
    }
}
