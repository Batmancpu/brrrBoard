// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Test-only normalized-metadata candidate: stable score/order parity, no runtime swap. */
public class EmojiAllocationParityTest {
    private static final class Prepared {
        final String emoji, name, keywords;
        final String[] phrases;
        Prepared(String emoji, EmojiContextIndex.Annotation annotation) {
            this.emoji = emoji;
            name = annotation.name.toLowerCase(Locale.ROOT);
            keywords = annotation.keywords.toLowerCase(Locale.ROOT);
            phrases = keywords.split("\\|");
        }
    }

    private static List<String> candidate(final List<Prepared> source,
            final String raw, final int limit) {
        if (raw == null || limit <= 0) return List.of();
        final String query = raw.trim().toLowerCase(Locale.ROOT).replaceAll("\\s+", " ");
        if (query.isEmpty()) return List.of();
        final String[] terms = query.split(" ");
        final class Hit {
            final String emoji; final int score;
            Hit(String emoji, int score) { this.emoji = emoji; this.score = score; }
        }
        final List<Hit> hits = new ArrayList<>();
        for (final Prepared item : source) {
            int score = item.name.equals(query) ? 1000
                    : item.name.startsWith(query) ? 900 : item.name.contains(query) ? 780 : 0;
            for (final String phrase : item.phrases) {
                final String keyword = phrase.trim();
                if (keyword.equals(query)) score = Math.max(score, 730);
                else if (keyword.startsWith(query)) score = Math.max(score, 600);
            }
            if (score == 0) {
                final String searchable = item.name + " | " + item.keywords;
                boolean all = true;
                for (final String term : terms) if (!searchable.contains(term)) { all = false; break; }
                if (all) score = 350;
            }
            if (score > 0) hits.add(new Hit(item.emoji, score));
        }
        hits.sort(Comparator.comparingInt((Hit hit) -> hit.score).reversed());
        final List<String> result = new ArrayList<>();
        for (int i = 0; i < Math.min(limit, hits.size()); i++) result.add(hits.get(i).emoji);
        return result;
    }

    @Test public void syntheticEmojiCLDRSearchReturnsSameSequencesAndStableTies() throws Exception {
        final String data = "🎂\tbirthday cake\tbirthday | cake | dessert\n"
                + "🎉\tparty popper\tparty | celebration | birthday\n"
                + "👍🏻\tthumbs up: light skin tone\tagree | thumbs up\n"
                + "#️⃣\tkeycap: #\thash | keycap\n"
                + "👨‍👩‍👧\tfamily\tfamily | parents\n"
                + "🪔\tdiya lamp\tdiwali | light\n";
        final EmojiContextIndex index = EmojiContextIndex.read(new ByteArrayInputStream(
                data.getBytes(StandardCharsets.UTF_8)));
        final List<Prepared> prepared = new ArrayList<>();
        for (final Map.Entry<String, EmojiContextIndex.Annotation> row : index.entries().entrySet())
            prepared.add(new Prepared(row.getKey(), row.getValue()));
        for (final String query : new String[]{"birthday", "cake", "PARTY", "thumbs up",
                "light skin tone", "keycap", "hash", "family", "diwali", "light",
                "no results", "  BIRTHDAY    cake  ", ""}) {
            for (int size : new int[]{1, 2, 4, 8})
                assertEquals("synthetic query " + query, index.search(query, size),
                        candidate(prepared, query, size));
        }
    }
}
