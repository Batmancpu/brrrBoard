// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;

/** A nonempty, duplicate-free Emoji row before a Search query is entered. */
final class EmojiSearchIdleResults {
    private static final List<String> DEFAULTS = List.of(
            "😀", "😂", "❤️", "👍", "🎉", "🙏", "🥰", "✨", "😊", "🔥");

    private EmojiSearchIdleResults() { }

    static List<String> merge(final List<String> reviewed, final List<String> semantic,
            final List<String> recent, final int limit) {
        if (limit <= 0) return List.of();
        final LinkedHashSet<String> results = new LinkedHashSet<>();
        append(results, reviewed, limit);
        append(results, semantic, limit);
        append(results, recent, limit);
        append(results, DEFAULTS, limit);
        return List.copyOf(new ArrayList<>(results));
    }

    private static void append(final LinkedHashSet<String> results,
            final List<String> candidates, final int limit) {
        for (final String candidate : candidates) {
            if (results.size() >= limit) return;
            if (candidate != null && !candidate.isEmpty()) results.add(candidate);
        }
    }
}
