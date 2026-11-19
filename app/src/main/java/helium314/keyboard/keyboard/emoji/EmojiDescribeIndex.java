// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/**
 * Immutable inverted index of the APK's pinned English CLDR annotations.
 * Built once off the IME thread; queries never scan all 3,944 Emoji strings.
 * Only Unicode sequences already in that pinned corpus can ever be returned.
 */
final class EmojiDescribeIndex {
    private static final int MAX_PREFIX_TERMS = 48;
    private static final int MAX_FUZZY_CANDIDATES = 96;
    private static final Set<String> WEAK_CONTEXT_TERMS = Set.of(
            "let", "lets", "get", "we", "plan", "planning", "watch", "going",
            "today", "tonight", "tomorrow", "night", "holiday", "maybe",
            "order", "most", "all", "few", "thing", "buy", "please");

    private final String[] emoji;
    private final String[] normalizedNames;
    private final TreeMap<String, Posting> tokens;
    private final Map<String, String[]> trigramTerms;
    private final Map<String, Integer> emojiId;
    private final Map<String, List<String>> toneFamilies;

    private EmojiDescribeIndex(final String[] emoji, final String[] normalizedNames,
            final TreeMap<String, Posting> tokens, final Map<String, String[]> trigramTerms,
            final Map<String, Integer> emojiId,
            final Map<String, List<String>> toneFamilies) {
        this.emoji = emoji;
        this.normalizedNames = normalizedNames;
        this.tokens = tokens;
        this.trigramTerms = trigramTerms;
        this.emojiId = emojiId;
        this.toneFamilies = toneFamilies;
    }

    static EmojiDescribeIndex from(final EmojiContextIndex source) {
        final int size = source.size();
        final String[] emoji = new String[size];
        final String[] names = new String[size];
        final Map<String, MutablePosting> building = new HashMap<>();
        final Map<String, Integer> bySequence = new HashMap<>(size * 2);
        int id = 0;
        for (final Map.Entry<String, EmojiContextIndex.Annotation> entry
                : source.entries().entrySet()) {
            emoji[id] = entry.getKey();
            bySequence.put(entry.getKey(), id);
            final EmojiContextIndex.Annotation annotation = entry.getValue();
            names[id] = EmojiDescribeNormalizer.normalizePhrase(annotation.name);
            final Set<String> nameTokens = new HashSet<>(
                    EmojiDescribeNormalizer.tokens(annotation.name, false));
            for (final String token : nameTokens) {
                building.computeIfAbsent(token, ignored -> new MutablePosting()).name.add(id);
            }
            final Set<String> keywordTokens = new HashSet<>();
            for (final String phrase : annotation.keywords.split("\\|")) {
                keywordTokens.addAll(EmojiDescribeNormalizer.tokens(phrase, false));
            }
            for (final String token : keywordTokens) {
                if (!nameTokens.contains(token))
                    building.computeIfAbsent(token, ignored -> new MutablePosting()).keyword.add(id);
            }
            id++;
        }
        final TreeMap<String, Posting> postings = new TreeMap<>();
        final Map<String, ArrayList<String>> gramBuilding = new HashMap<>();
        for (final Map.Entry<String, MutablePosting> entry : building.entrySet()) {
            final String term = entry.getKey();
            postings.put(term, new Posting(toInts(entry.getValue().name),
                    toInts(entry.getValue().keyword)));
            for (final String gram : trigrams(term)) {
                gramBuilding.computeIfAbsent(gram, ignored -> new ArrayList<>()).add(term);
            }
        }
        final Map<String, String[]> gramIndex = new HashMap<>(gramBuilding.size() * 2);
        for (final Map.Entry<String, ArrayList<String>> entry : gramBuilding.entrySet()) {
            final ArrayList<String> list = entry.getValue();
            Collections.sort(list);
            gramIndex.put(entry.getKey(), list.toArray(new String[0]));
        }
        return new EmojiDescribeIndex(emoji, names, postings,
                Collections.unmodifiableMap(gramIndex), Collections.unmodifiableMap(bySequence),
                EmojiSkinToneSearchPolicy.families(bySequence.keySet()));
    }

    int emojiCount() { return emoji.length; }
    int termCount() { return tokens.size(); }
    boolean containsEmoji(final String sequence) { return emojiId.containsKey(sequence); }

    /**
     * Search-entry suggestions from the last few ordinary editor words.
     * Unlike a description query, unrelated context words are NOT required
     * to coexist on one Emoji. Prefer recent specific terms, not ubiquitous
     * words like "face" or "person". This stays off the IME main thread.
     */
    List<String> suggestFromContext(final String input, final int maxResults) {
        if (input == null || maxResults <= 0) return List.of();
        final List<String> terms = EmojiDescribeNormalizer.recentContextTokens(input);
        final LinkedHashSet<String> suggestions = new LinkedHashSet<>();
        final int from = Math.max(0, terms.size() - 5);
        final ArrayList<String> specific = new ArrayList<>();
        for (int i = from; i < terms.size(); i++) {
            final String term = terms.get(i);
            if (term.length() < 3 || WEAK_CONTEXT_TERMS.contains(term)) continue;
            final Posting posting = tokens.get(term);
            if (posting != null && posting.name.length + posting.keyword.length <= 160)
                specific.add(term);
        }
        // An uncommon subject such as "pizza" outranks incidental "night"
        // or "food". Stable tie order prefers the latest word.
        specific.sort(Comparator.comparingInt((String term) -> {
            final Posting posting = tokens.get(term);
            return posting.name.length + posting.keyword.length;
        }).thenComparing((String term) -> -terms.lastIndexOf(term)));
        for (final String term : specific) {
            for (final String result : search(term, 3)) {
                suggestions.add(result);
                if (suggestions.size() == maxResults) break;
            }
            if (suggestions.size() == maxResults) break;
        }
        return Collections.unmodifiableList(new ArrayList<>(suggestions));
    }

    List<String> search(final String input, final int maxResults) {
        if (input == null || maxResults <= 0) return List.of();
        final EmojiSkinToneSearchPolicy.Request toneRequest = EmojiSkinToneSearchPolicy.parse(input);
        // CLDR contains up to six copies of a tone-bearing subject. Rank a
        // bounded larger pool BEFORE selecting tones, or those copies consume
        // nearly every visible Search slot and hide unrelated useful Emoji.
        final int poolSize = (int)Math.min(240L, Math.max(48L, (long)maxResults * 8L));
        final String literalName = EmojiDescribeNormalizer.normalizePhrase(input);
        if (toneRequest.explicitlyRequestedTone() && !literalName.contains("skin")
                && !literalName.contains("tone")) {
            // Color words can be the real, exact CLDR name of a non-human Emoji.
            // In particular "black heart" must NEVER turn into dark heart hands.
            for (final String name : normalizedNames) {
                if (name.equals(literalName)) {
                    return EmojiSkinToneSearchPolicy.select(
                            new EmojiSkinToneSearchPolicy.Request(input,
                                    EmojiSkinToneSearchPolicy.Mode.DEFAULT, 0),
                            searchRanked(input, poolSize), emojiId.keySet(),
                            toneFamilies, maxResults);
                }
            }
        }
        final List<String> selected = EmojiSkinToneSearchPolicy.select(toneRequest,
                searchRanked(toneRequest.lookup, poolSize), emojiId.keySet(),
                toneFamilies, maxResults);
        if (!selected.isEmpty() || !toneRequest.explicitlyRequestedTone()) return selected;
        // "black heart", "brown square" and "light bulb" denote colors or
        // objects, not complexions. Revert to the literal query when no
        // eligible skin-tone family answered the qualified interpretation.
        return EmojiSkinToneSearchPolicy.select(
                new EmojiSkinToneSearchPolicy.Request(input,
                        EmojiSkinToneSearchPolicy.Mode.DEFAULT, 0),
                searchRanked(input, poolSize), emojiId.keySet(), toneFamilies, maxResults);
    }

    private List<String> searchRanked(final String input, final int maxResults) {
        // A single offline knowledge base: CLDR names/keywords for broad
        // discovery plus the SAME reviewed phrase rules used by Smart Emoji
        // and the native suggestion strip. No second corpus or duplicated
        // trigger list. Explicit user aliases are still promoted by the
        // surrounding EmojiDescribeEngine after this lookup.
        final List<String> reviewed = EmojiContextScorer.rankSearchCandidates(
                EmojiDescribeNormalizer.clampQuery(input), maxResults);
        final List<String> indexed = searchIndexed(input, maxResults);
        if (reviewed.isEmpty()) return indexed;
        final LinkedHashSet<String> merged = new LinkedHashSet<>();
        for (final String candidate : reviewed) {
            if (emojiId.containsKey(candidate)) merged.add(candidate);
            if (merged.size() == maxResults) break;
        }
        for (final String candidate : indexed) {
            if (merged.size() == maxResults) break;
            merged.add(candidate);
        }
        return Collections.unmodifiableList(new ArrayList<>(merged));
    }

    private List<String> searchIndexed(final String input, final int maxResults) {
        if (maxResults <= 0) return List.of();
        final String phrase = EmojiDescribeNormalizer.normalizePhrase(
                EmojiDescribeNormalizer.clampQuery(input));
        final List<String> query = EmojiDescribeNormalizer.tokens(
                EmojiDescribeAliases.rewriteDescription(phrase), true);
        if (query.isEmpty()) return List.of();

        final int[] score = new int[emoji.length];
        final int[] matched = new int[emoji.length];
        boolean allTermsResolvable = true;
        for (final String term : query) {
            final int[] perTerm = new int[emoji.length];
            boolean resolved = false;
            final Posting exact = tokens.get(term);
            if (exact != null) {
                apply(perTerm, exact, 115, 72);
                resolved = true;
            }

            final String[] aliases = EmojiDescribeAliases.expand(term);
            if (aliases != null) for (final String alias : aliases) {
                final Posting synonyms = tokens.get(alias);
                if (synonyms == null) continue;
                apply(perTerm, synonyms, 58, 39);
                resolved = true;
            }

            // Prefix matching is useful while typing but cannot make "cat"
            // match inside "education". Exact terms remain stronger.
            if (!resolved && term.length() >= 2) {
                int checked = 0;
                for (final Map.Entry<String, Posting> candidate
                        : tokens.tailMap(term, true).entrySet()) {
                    if (!candidate.getKey().startsWith(term) || checked++ == MAX_PREFIX_TERMS)
                        break;
                    apply(perTerm, candidate.getValue(), 52, 30);
                    resolved = true;
                }
            }

            // Only unknown Latin words get typo recovery. All candidates come
            // from the tiny n-gram lexicon and comparisons are strictly capped.
            if (!resolved && term.length() >= 4 && asciiLetters(term)) {
                final int distance = term.length() <= 5 ? 1 : 2;
                for (final String candidate : nearbyTerms(term)) {
                    if (Math.abs(candidate.length() - term.length()) > distance ||
                            damerauDistance(term, candidate, distance) > distance) continue;
                    final Posting fuzzy = tokens.get(candidate);
                    if (fuzzy != null) {
                        apply(perTerm, fuzzy, 24, 15);
                        resolved = true;
                    }
                }
            }
            if (!resolved) allTermsResolvable = false;
            for (int i = 0; i < score.length; i++) {
                if (perTerm[i] > 0) {
                    score[i] += perTerm[i];
                    matched[i]++;
                }
            }
        }

        final String preferred = EmojiDescribeAliases.preferredEmoji(phrase);
        final Integer preferredId = emojiId.get(preferred);
        // Unknown multi-word phrases must not hallucinate a result because
        // one unrelated word happens to occur in some Emoji annotation.
        if (!allTermsResolvable && preferredId == null) return List.of();

        boolean hasComplete = false;
        for (final int count : matched) if (count == query.size()) { hasComplete = true; break; }
        // A multi-part description without a single matching Emoji is not
        // permission to combine unrelated one-word results. Reviewed phrase
        // overrides are the only deliberate exception.
        if (query.size() > 1 && !hasComplete && preferredId == null) return List.of();
        final ArrayList<Hit> hits = new ArrayList<>();
        for (int i = 0; i < emoji.length; i++) {
            if (score[i] == 0 && (preferredId == null || preferredId != i)) continue;
            if (hasComplete && matched[i] < query.size()
                    && (preferredId == null || preferredId != i)) continue;
            int rank = score[i] + 115 * matched[i] - 145 * (query.size() - matched[i]);
            if (matched[i] == query.size()) rank += 290;
            final String name = normalizedNames[i];
            if (name.equals(phrase)) rank += 1200;
            else if (name.startsWith(phrase + " ")) rank += 720;
            else if (phrase.length() >= 4 && name.contains(phrase)) rank += 510;
            if (preferredId != null && preferredId == i) rank += 1800;
            if (rank > 0) hits.add(new Hit(i, rank));
        }
        hits.sort(Comparator.comparingInt((Hit hit) -> hit.score).reversed()
                .thenComparingInt(hit -> hit.id));
        final ArrayList<String> results = new ArrayList<>(Math.min(hits.size(), maxResults));
        for (int i = 0; i < Math.min(hits.size(), maxResults); i++)
            results.add(emoji[hits.get(i).id]);
        return Collections.unmodifiableList(results);
    }

    private static void apply(final int[] best, final Posting posting, final int nameScore,
            final int keywordScore) {
        for (final int id : posting.name) best[id] = Math.max(best[id], nameScore);
        for (final int id : posting.keyword) best[id] = Math.max(best[id], keywordScore);
    }

    private List<String> nearbyTerms(final String word) {
        final Map<String, Integer> overlap = new HashMap<>();
        for (final String gram : trigrams(word)) {
            final String[] candidateTerms = trigramTerms.get(gram);
            if (candidateTerms == null) continue;
            for (final String candidate : candidateTerms)
                overlap.merge(candidate, 1, Integer::sum);
        }
        final ArrayList<String> candidates = new ArrayList<>(overlap.keySet());
        candidates.sort(Comparator.<String>comparingInt(overlap::get).reversed()
                .thenComparingInt(candidate -> Math.abs(candidate.length() - word.length()))
                .thenComparing(Comparator.naturalOrder()));
        return candidates.subList(0, Math.min(MAX_FUZZY_CANDIDATES, candidates.size()));
    }

    private static Set<String> trigrams(final String word) {
        final String padded = "^" + word + "$";
        final HashSet<String> grams = new HashSet<>();
        for (int i = 0; i + 3 <= padded.length(); i++)
            grams.add(padded.substring(i, i + 3));
        return grams;
    }

    private static boolean asciiLetters(final String word) {
        for (int i = 0; i < word.length(); i++)
            if (word.charAt(i) < 'a' || word.charAt(i) > 'z') return false;
        return true;
    }

    /** Optimal-string-alignment (bounded Damerau-Levenshtein) distance. */
    static int damerauDistance(final String left, final String right, final int bound) {
        if (Math.abs(left.length() - right.length()) > bound) return bound + 1;
        final int[][] cost = new int[left.length() + 1][right.length() + 1];
        for (int i = 0; i <= left.length(); i++) cost[i][0] = i;
        for (int j = 0; j <= right.length(); j++) cost[0][j] = j;
        for (int i = 1; i <= left.length(); i++) {
            int rowMinimum = Integer.MAX_VALUE;
            for (int j = 1; j <= right.length(); j++) {
                int value = Math.min(cost[i - 1][j] + 1, cost[i][j - 1] + 1);
                value = Math.min(value, cost[i - 1][j - 1] +
                        (left.charAt(i - 1) == right.charAt(j - 1) ? 0 : 1));
                if (i > 1 && j > 1 && left.charAt(i - 1) == right.charAt(j - 2)
                        && left.charAt(i - 2) == right.charAt(j - 1))
                    value = Math.min(value, cost[i - 2][j - 2] + 1);
                cost[i][j] = value;
                rowMinimum = Math.min(rowMinimum, value);
            }
            if (rowMinimum > bound) return bound + 1;
        }
        return cost[left.length()][right.length()];
    }

    private static int[] toInts(final List<Integer> input) {
        final int[] result = new int[input.size()];
        for (int i = 0; i < input.size(); i++) result[i] = input.get(i);
        return result;
    }

    private static final class MutablePosting {
        final ArrayList<Integer> name = new ArrayList<>();
        final ArrayList<Integer> keyword = new ArrayList<>();
    }

    private static final class Posting {
        final int[] name;
        final int[] keyword;
        Posting(final int[] name, final int[] keyword) {
            this.name = name;
            this.keyword = keyword;
        }
    }

    private static final class Hit {
        final int id;
        final int score;
        Hit(final int id, final int score) {
            this.id = id;
            this.score = score;
        }
    }
}
