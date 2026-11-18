// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji;

import java.text.Normalizer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** Bounded, locale-independent normalization for explicitly requested Emoji Search. */
final class EmojiDescribeNormalizer {
    static final int MAX_QUERY_CODE_POINTS = 80;
    static final int MAX_TOKENS = 8;
    private static final Set<String> FILLER = Set.of(
            "a", "an", "the", "of", "with", "for", "to", "in", "on", "at",
            "me", "show", "find", "give", "please", "emoji", "emojis", "icon", "icons",
            "that", "is", "looks", "like");
    // Conversational words are ignorable only in a multiword *query*.
    // Never delete them from the indexed CLDR names/keywords or from a
    // one-word search: this is a description parser, not a language model.
    private static final Set<String> CONVERSATIONAL = Set.of(
            "i", "im", "am", "my", "you", "your", "someone", "somebody", "who",
            "was", "were", "are", "and", "or", "but", "because", "today",
            "really", "so", "very", "quite", "this", "about", "have", "has",
            "had", "need", "want", "feel", "feeling", "felt", "could", "would",
            "should", "there", "here", "it", "its", "just", "some", "something",
            "hard", "made", "use", "take", "keeping");

    private EmojiDescribeNormalizer() { }

    static String normalizePhrase(final String input) {
        if (input == null || input.isEmpty()) return "";
        final String normalized = Normalizer.normalize(input, Normalizer.Form.NFKC)
                .toLowerCase(Locale.ROOT);
        final StringBuilder out = new StringBuilder(normalized.length());
        boolean separator = true;
        for (int offset = 0; offset < normalized.length();) {
            final int cp = normalized.codePointAt(offset);
            offset += Character.charCount(cp);
            final int kind = Character.getType(cp);
            final boolean part = Character.isLetterOrDigit(cp)
                    || kind == Character.NON_SPACING_MARK || kind == Character.COMBINING_SPACING_MARK;
            if (part) {
                out.appendCodePoint(cp);
                separator = false;
            } else if (!separator) {
                out.append(' ');
                separator = true;
            }
        }
        if (separator && out.length() > 0) out.setLength(out.length() - 1);
        return out.toString();
    }

    static String clampQuery(final String input) {
        if (input == null || input.isEmpty()) return "";
        final int count = input.codePointCount(0, input.length());
        return count <= MAX_QUERY_CODE_POINTS ? input
                : input.substring(0, input.offsetByCodePoints(0, MAX_QUERY_CODE_POINTS));
    }

    static List<String> tokens(final String text, final boolean query) {
        final String normalized = normalizePhrase(query ? clampQuery(text) : text);
        if (normalized.isEmpty()) return List.of();
        final String[] pieces = normalized.split(" ");
        final ArrayList<String> tokens = new ArrayList<>(Math.min(pieces.length, MAX_TOKENS));
        final Set<String> seen = new HashSet<>();
        for (final String piece : pieces) {
            if (piece.isEmpty() || piece.length() > 32 || FILLER.contains(piece)
                    || (query && pieces.length > 1 && CONVERSATIONAL.contains(piece))) continue;
            final String token = singular(piece);
            if (seen.add(token)) tokens.add(token);
            if (query && tokens.size() == MAX_TOKENS) break;
        }
        return Collections.unmodifiableList(tokens);
    }

    /** Editor context is a suffix: retain the LAST useful words, unlike a query. */
    static List<String> recentContextTokens(final String input) {
        if (input == null || input.isEmpty()) return List.of();
        final int count = input.codePointCount(0, input.length());
        final String suffix = count <= MAX_QUERY_CODE_POINTS ? input
                : input.substring(input.offsetByCodePoints(0, count - MAX_QUERY_CODE_POINTS));
        final String normalized = normalizePhrase(suffix);
        if (normalized.isEmpty()) return List.of();
        final String[] pieces = normalized.split(" ");
        final ArrayList<String> mostRecentFirst = new ArrayList<>(MAX_TOKENS);
        final Set<String> seen = new HashSet<>();
        for (int i = pieces.length - 1; i >= 0 && mostRecentFirst.size() < MAX_TOKENS; i--) {
            final String piece = pieces[i];
            if (piece.isEmpty() || piece.length() > 32 || FILLER.contains(piece)
                    || (pieces.length > 1 && CONVERSATIONAL.contains(piece))) continue;
            final String token = singular(piece);
            if (seen.add(token)) mostRecentFirst.add(token);
        }
        Collections.reverse(mostRecentFirst);
        return Collections.unmodifiableList(mostRecentFirst);
    }

    private static String singular(final String word) {
        if (word.length() > 4 && word.endsWith("ies"))
            return word.substring(0, word.length() - 3) + "y";
        if (word.length() > 4 && word.endsWith("sses"))
            return word.substring(0, word.length() - 2);
        if (word.length() > 3 && word.endsWith("s") && !word.endsWith("ss")
                && !word.endsWith("us"))
            return word.substring(0, word.length() - 1);
        return word;
    }
}
