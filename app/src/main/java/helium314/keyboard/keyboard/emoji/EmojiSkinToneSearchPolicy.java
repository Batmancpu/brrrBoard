// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Unicode-scalar, corpus-only skin-tone intent for explicit Emoji Search.
 * A plain description has ONE result per tone family; a requested complexion
 * selects that modifier, and an explicit request for shades reveals the family.
 * No modifier sequence is ever synthesized or sent to a network service.
 */
final class EmojiSkinToneSearchPolicy {
    private static final int LIGHT = 0x1F3FB;
    private static final int MEDIUM_LIGHT = 0x1F3FC;
    private static final int MEDIUM = 0x1F3FD;
    private static final int MEDIUM_DARK = 0x1F3FE;
    private static final int DARK = 0x1F3FF;

    enum Mode { DEFAULT, EXACT, ALL }

    static final class Request {
        final String lookup;
        final Mode mode;
        final int modifier;

        Request(final String lookup, final Mode mode, final int modifier) {
            this.lookup = lookup;
            this.mode = mode;
            this.modifier = modifier;
        }

        boolean explicitlyRequestedTone() { return mode != Mode.DEFAULT; }
    }

    private EmojiSkinToneSearchPolicy() { }

    static Request parse(final String input) {
        final String normalized = EmojiDescribeNormalizer.normalizePhrase(
                EmojiDescribeNormalizer.clampQuery(input));
        if (normalized.isEmpty()) return new Request(normalized, Mode.DEFAULT, 0);
        final String[] words = normalized.split(" ");
        final Set<String> terms = new HashSet<>(Arrays.asList(words));
        final boolean medium = terms.contains("medium");
        final boolean light = terms.contains("light") || terms.contains("lighter");
        final boolean dark = terms.contains("dark") || terms.contains("darker");
        final int modifier;
        if (medium && dark) modifier = MEDIUM_DARK;
        else if (medium && light) modifier = MEDIUM_LIGHT;
        else if (terms.contains("brown") || terms.contains("bronze")) modifier = MEDIUM_DARK;
        else if (terms.contains("black") || terms.contains("deep") || dark) modifier = DARK;
        else if (terms.contains("tan") || terms.contains("tanned") || medium) modifier = MEDIUM;
        else if (terms.contains("white") || terms.contains("pale")
                || terms.contains("fair") || light) modifier = LIGHT;
        else modifier = 0;
        final boolean neutral = terms.contains("neutral") || terms.contains("yellow");
        final boolean toneWords = terms.contains("skin") || terms.contains("skinned")
                || terms.contains("tone") || terms.contains("tones")
                || terms.contains("complexion") || terms.contains("shade")
                || terms.contains("shades") || terms.contains("color")
                || terms.contains("colors") || terms.contains("colour")
                || terms.contains("colours");
        final Mode mode = modifier != 0 || neutral ? Mode.EXACT
                : toneWords ? Mode.ALL : Mode.DEFAULT;
        if (mode == Mode.DEFAULT) return new Request(normalized, mode, 0);

        final StringBuilder base = new StringBuilder();
        for (final String word : words) {
            if (isToneWord(word)) continue;
            if (base.length() != 0) base.append(' ');
            base.append(word);
        }
        // "black" by itself should still search black hearts and squares;
        // an explicit tone needs an actual Emoji subject to disambiguate it.
        if (base.length() == 0) return new Request(normalized, Mode.DEFAULT, 0);
        return new Request(base.toString(), mode, neutral ? 0 : modifier);
    }

    private static boolean isToneWord(final String word) {
        return switch (word) {
            case "medium", "light", "lighter", "dark", "darker", "brown", "bronze",
                    "black", "deep", "tan", "tanned", "white", "pale", "fair",
                    "neutral", "yellow", "skin", "skinned", "tone", "tones",
                    "complexion", "shade", "shades", "color", "colors", "colour",
                    "colours", "all", "every", "different", "variant", "variants" -> true;
            default -> false;
        };
    }

    static String family(final String emoji) {
        final StringBuilder base = new StringBuilder(emoji.length());
        for (int offset = 0; offset < emoji.length();) {
            final int cp = emoji.codePointAt(offset);
            offset += Character.charCount(cp);
            if (cp < LIGHT || cp > DARK) base.appendCodePoint(cp);
        }
        return base.toString();
    }

    /** 0 = neutral, -1 = mixed skin tones, otherwise the uniform Unicode modifier. */
    private static int toneOf(final String emoji) {
        int tone = 0;
        for (int offset = 0; offset < emoji.length();) {
            final int cp = emoji.codePointAt(offset);
            offset += Character.charCount(cp);
            if (cp < LIGHT || cp > DARK) continue;
            if (tone != 0 && tone != cp) return -1;
            tone = cp;
        }
        return tone;
    }

    static Map<String, List<String>> families(final Set<String> corpus) {
        final Map<String, List<String>> building = new HashMap<>();
        for (final String emoji : corpus) {
            if (toneOf(emoji) == 0) continue;
            building.computeIfAbsent(family(emoji), unused -> new ArrayList<>()).add(emoji);
        }
        final Map<String, List<String>> result = new HashMap<>();
        for (final Map.Entry<String, List<String>> entry : building.entrySet()) {
            entry.getValue().sort(Comparator.comparingInt((String sequence) -> {
                        final int tone = toneOf(sequence);
                        return tone < 0 ? Integer.MAX_VALUE : tone;
                    })
                    .thenComparing(Comparator.naturalOrder()));
            result.put(entry.getKey(), List.copyOf(entry.getValue()));
        }
        return Collections.unmodifiableMap(result);
    }

    static List<String> select(final Request request, final List<String> ranked,
            final Set<String> corpus, final Map<String, List<String>> families,
            final int maxResults) {
        if (maxResults <= 0 || ranked.isEmpty()) return List.of();
        final LinkedHashSet<String> selected = new LinkedHashSet<>();
        final Set<String> visitedFamilies = new HashSet<>();
        for (final String candidate : ranked) {
            final String base = family(candidate);
            if (!visitedFamilies.add(base)) continue;
            final List<String> variants = families.get(base);
            if (request.mode == Mode.DEFAULT || variants == null) {
                if (request.mode == Mode.DEFAULT)
                    selected.add(corpus.contains(base) ? base : candidate);
            } else if (request.mode == Mode.ALL) {
                if (corpus.contains(base)) selected.add(base);
                selected.addAll(variants);
            } else if (request.modifier == 0) {
                if (corpus.contains(base)) selected.add(base);
            } else {
                for (final String variant : variants) {
                    if (toneOf(variant) == request.modifier) selected.add(variant);
                }
            }
            if (selected.size() >= maxResults) break;
        }
        final ArrayList<String> result = new ArrayList<>(Math.min(maxResults, selected.size()));
        for (final String emoji : selected) {
            if (result.size() == maxResults) break;
            result.add(emoji);
        }
        return Collections.unmodifiableList(result);
    }
}
