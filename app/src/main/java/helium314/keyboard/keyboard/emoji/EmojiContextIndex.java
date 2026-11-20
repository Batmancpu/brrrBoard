// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji;

import helium314.keyboard.latin.IntelligenceMetrics;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Immutable, lazily parsed CLDR 48.2 English Emoji metadata (search, not typing triggers). */
final class EmojiContextIndex {
    static final String ASSET_NAME = "emoji_cldr48_en.tsv";

    static final class Annotation {
        final String name;
        final String keywords;

        Annotation(final String name, final String keywords) {
            this.name = name;
            this.keywords = keywords;
        }
    }

    private final Map<String, Annotation> mByEmoji;

    private EmojiContextIndex(final Map<String, Annotation> byEmoji) {
        mByEmoji = Collections.unmodifiableMap(byEmoji);
    }

    static EmojiContextIndex read(final InputStream input) throws IOException {
        final Map<String, Annotation> annotations = new LinkedHashMap<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // #️⃣ is a legitimate Emoji sequence; only '# ' starts a
                // provenance/comment line in this generated TSV format.
                if (line.isEmpty() || line.startsWith("# ")) {
                    continue;
                }
                final String[] columns = line.split("\\t", 3);
                if (columns.length != 3 || columns[0].isEmpty()
                        || columns[0].codePointCount(0, columns[0].length()) > 32) {
                    throw new IOException("Invalid Emoji annotation row");
                }
                if (annotations.putIfAbsent(columns[0],
                        new Annotation(columns[1], columns[2])) != null) {
                    throw new IOException("Duplicate Emoji annotation sequence");
                }
            }
        }
        return new EmojiContextIndex(annotations);
    }

    /**
     * Deterministic, offline search across bundled CLDR names and keywords.
     * Search is deliberately separate from context-triggered typing suggestions.
     */
    List<String> search(final String input, final int maxResults) {
        final long started = IntelligenceMetrics.begin();
        try {
            return searchInternal(input, maxResults);
        } finally {
            IntelligenceMetrics.end(IntelligenceMetrics.Path.EMOJI_SEARCH, started);
        }
    }

    private List<String> searchInternal(final String input, final int maxResults) {
        if (input == null || maxResults <= 0) return List.of();
        final String query = input.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        if (query.isEmpty()) return List.of();
        final String[] terms = query.split(" ");
        final List<SearchHit> hits = new ArrayList<>();
        for (final Map.Entry<String, Annotation> entry : mByEmoji.entrySet()) {
            final Annotation annotation = entry.getValue();
            final String name = annotation.name.toLowerCase(Locale.ROOT);
            final String keywords = annotation.keywords.toLowerCase(Locale.ROOT);
            int score = name.equals(query) ? 1000
                    : name.startsWith(query) ? 900
                    : name.contains(query) ? 780 : 0;
            for (final String rawKeyword : keywords.split("\\|")) {
                final String keyword = rawKeyword.trim();
                if (keyword.equals(query)) score = Math.max(score, 730);
                else if (keyword.startsWith(query)) score = Math.max(score, 600);
            }
            if (score == 0) {
                final String searchable = name + " | " + keywords;
                boolean allTermsMatch = true;
                for (final String term : terms) {
                    if (!searchable.contains(term)) {
                        allTermsMatch = false;
                        break;
                    }
                }
                if (allTermsMatch) score = 350;
            }
            if (score > 0) hits.add(new SearchHit(entry.getKey(), score));
        }
        // Java's stable list sort preserves CLDR's original order on ties.
        hits.sort(Comparator.comparingInt((SearchHit hit) -> hit.score).reversed());
        final List<String> results = new ArrayList<>(Math.min(maxResults, hits.size()));
        for (int i = 0; i < Math.min(maxResults, hits.size()); i++)
            results.add(hits.get(i).emoji);
        return Collections.unmodifiableList(results);
    }

    private static final class SearchHit {
        final String emoji;
        final int score;
        SearchHit(final String emoji, final int score) {
            this.emoji = emoji;
            this.score = score;
        }
    }

    Annotation get(final String exactEmoji) {
        return mByEmoji.get(exactEmoji);
    }

    /** Read-only CLDR-order source for the lazy Describe index; never expose mutations. */
    Map<String, Annotation> entries() {
        return mByEmoji;
    }

    int size() {
        return mByEmoji.size();
    }
}
