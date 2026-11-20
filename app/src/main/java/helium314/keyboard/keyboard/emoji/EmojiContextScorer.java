// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji;

import helium314.keyboard.latin.IntelligenceMetrics;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Offline contextual Emoji ranking. Only reviewed phrases may trigger a typing
 * suggestion; broad CLDR search keywords are intentionally not trigger rules.
 * No editor text is retained, logged or modified by this scorer.
 */
public final class EmojiContextScorer {
    private static final int MIN_SCORE = 450;
    private static final int MAX_CONTEXT_CHARS = 160;
    private static final int MAX_RESULTS = 4;
    private static final int STRIP_MIN_SCORE = 520;

    static final class Candidate {
        final String emoji;
        final int confidence;
        final String matchedTrigger;

        Candidate(final String emoji, final int confidence, final String matchedTrigger) {
            this.emoji = emoji;
            this.confidence = confidence;
            this.matchedTrigger = matchedTrigger;
        }
    }

    private static final class Rule {
        final String emoji;
        final int confidence;

        Rule(final String emoji, final int confidence) {
            this.emoji = emoji;
            this.confidence = confidence;
        }
    }

    private static final Map<String, List<Rule>> REVIEWED_TRIGGERS = buildRules();

    private EmojiContextScorer() {}

    private static Map<String, List<Rule>> buildRules() {
        final Map<String, List<Rule>> rules = new LinkedHashMap<>();
        // Owner-independent, manually reviewed English and Romanized Hinglish
        // triggers. These are NOT copied from CLDR annotation keywords.
        add(rules, "happy birthday", "🎂", 650); add(rules, "happy birthday", "🎉", 620);
        add(rules, "birthday party", "🎉", 610); add(rules, "birthday cake", "🎂", 610);
        add(rules, "birthday", "🎂", 575); add(rules, "happy", "😊", 540);
        add(rules, "congratulations", "🎉", 550); add(rules, "congrats", "🎉", 540);
        add(rules, "well done", "👏", 565); add(rules, "great job", "👏", 570);
        add(rules, "good luck", "🍀", 585); add(rules, "best wishes", "💐", 550);
        add(rules, "good morning", "🌅", 590); add(rules, "good night", "🌙", 605);
        add(rules, "sweet dreams", "😴", 580); add(rules, "sleep well", "😴", 570);
        add(rules, "love you", "❤️", 640); add(rules, "i love you", "❤️", 670);
        add(rules, "miss you", "🥺", 580); add(rules, "big hug", "🤗", 565);
        add(rules, "sending hugs", "🤗", 585); add(rules, "thank you", "🙏", 590);
        add(rules, "thanks a lot", "🙏", 600); add(rules, "much appreciated", "🙏", 560);
        add(rules, "so funny", "😂", 570); add(rules, "laughing so hard", "🤣", 600);
        add(rules, "rofl", "🤣", 520); add(rules, "lol", "😂", 455);
        add(rules, "heartbroken", "💔", 570); add(rules, "so sad", "😢", 550);
        add(rules, "crying", "😭", 465); add(rules, "angry", "😡", 475);
        add(rules, "feeling sick", "🤒", 570); add(rules, "get well soon", "❤️", 560);
        add(rules, "pizza", "🍕", 475); add(rules, "burger", "🍔", 475);
        add(rules, "coffee", "☕", 485); add(rules, "ice cream", "🍦", 520);
        add(rules, "movie night", "🍿", 570); add(rules, "popcorn", "🍿", 490);
        add(rules, "football match", "⚽", 560); add(rules, "cricket match", "🏏", 560);
        add(rules, "match day", "🏟️", 510); add(rules, "on fire", "🔥", 570);
        add(rules, "beautiful sunset", "🌅", 585); add(rules, "heavy rain", "🌧️", 540);
        add(rules, "snowing", "❄️", 475); add(rules, "rainbow", "🌈", 485);
        add(rules, "diwali", "🪔", 560); add(rules, "happy diwali", "🪔", 635);
        add(rules, "holi", "🎨", 560); add(rules, "happy holi", "🎨", 610);
        add(rules, "eid mubarak", "🌙", 620); add(rules, "raksha bandhan", "🧵", 565);
        add(rules, "namaste", "🙏", 545); add(rules, "shukriya", "🙏", 540);
        add(rules, "bahut badhiya", "👌", 550); add(rules, "yaar mast", "😎", 585);
        add(rules, "mast", "🔥", 530); add(rules, "bhai", "🤝", 525);
        add(rules, "bohot badhiya", "👏", 560); add(rules, "kya scene", "👀", 530);
        add(rules, "sab badhiya", "👌", 540);
        add(rules, "yaar mast", "🔥", 540); add(rules, "kya baat", "👏", 550);
        add(rules, "dost", "🤝", 470); add(rules, "dosti", "🤝", 490);
        add(rules, "goodbye", "👋", 470); add(rules, "see you soon", "👋", 555);
        add(rules, "new year", "🎆", 575); add(rules, "happy new year", "🎆", 650);
        add(rules, "wedding day", "💍", 585); add(rules, "engaged", "💍", 505);
        add(rules, "new baby", "👶", 550); add(rules, "graduation", "🎓", 540);
        add(rules, "road trip", "🚗", 560); add(rules, "safe travels", "✈️", 580);
        add(rules, "beach day", "🏖️", 550); add(rules, "mountain hike", "🥾", 545);
        final Map<String, List<Rule>> immutable = new LinkedHashMap<>();
        for (final Map.Entry<String, List<Rule>> entry : rules.entrySet()) {
            immutable.put(entry.getKey(), Collections.unmodifiableList(entry.getValue()));
        }
        return Collections.unmodifiableMap(immutable);
    }

    private static void add(final Map<String, List<Rule>> map, final String phrase,
            final String emoji, final int confidence) {
        map.computeIfAbsent(phrase, ignored -> new ArrayList<>()).add(new Rule(emoji, confidence));
    }

    /**
     * @param sensitiveField caller-computed password/incognito/no-learning guard.
     * @return immutable highest-confidence results, empty for untrusted contexts.
     */
    static List<Candidate> rank(final String context, final boolean sensitiveField) {
        final long started = IntelligenceMetrics.begin();
        try {
            return rankInternal(context, sensitiveField);
        } finally {
            IntelligenceMetrics.end(IntelligenceMetrics.Path.EMOJI_RANK, started);
        }
    }

    private static List<Candidate> rankInternal(final String context, final boolean sensitiveField) {
        if (sensitiveField || context == null || context.isEmpty() || context.length() > 4096) {
            return List.of();
        }
        // These input classes should not produce context-derived Emoji. Do the
        // cheap safety check before any normalization or dictionary access.
        if (context.indexOf('@') >= 0 || context.indexOf('/') >= 0
                || context.indexOf('\\') >= 0 || context.indexOf('=') >= 0
                || context.indexOf(':') >= 0) {
            return List.of();
        }
        final String normalized = normalizeSuffix(context);
        if (normalized.isEmpty()) {
            return List.of();
        }
        final Map<String, Candidate> byEmoji = new HashMap<>();
        for (final Map.Entry<String, List<Rule>> entry : REVIEWED_TRIGGERS.entrySet()) {
            final String trigger = entry.getKey();
            if (!normalized.equals(trigger) && !normalized.endsWith(" " + trigger)) {
                continue;
            }
            for (final Rule rule : entry.getValue()) {
                if (rule.confidence < MIN_SCORE) {
                    continue;
                }
                final Candidate existing = byEmoji.get(rule.emoji);
                if (existing == null || existing.confidence < rule.confidence) {
                    byEmoji.put(rule.emoji, new Candidate(rule.emoji, rule.confidence, trigger));
                }
            }
        }
        if (byEmoji.isEmpty()) {
            return List.of();
        }
        final List<Candidate> sorted = new ArrayList<>(byEmoji.values());
        sorted.sort(Comparator.comparingInt((Candidate c) -> c.confidence).reversed()
                .thenComparing(c -> c.emoji));
        return Collections.unmodifiableList(sorted.subList(0, Math.min(MAX_RESULTS, sorted.size())));
    }

    /**
     * Compact, strictly high-confidence ranking for existing decoder-provided
     * Emoji only. Callers must never synthesize or replace a text candidate.
     */
    public static List<String> rankEmojiSideCandidates(final String typedWord,
            final boolean sensitiveField) {
        final List<String> outputs = new ArrayList<>();
        for (final Candidate candidate : rank(typedWord, sensitiveField)) {
            if (candidate.confidence >= STRIP_MIN_SCORE) outputs.add(candidate.emoji);
        }
        return Collections.unmodifiableList(outputs);
    }

    /**
     * Search and the suggestion strip use this SAME reviewed context rule
     * table. Search may show lower-confidence, explicitly requested subjects;
     * typing still requires STRIP_MIN_SCORE and an existing decoder Emoji.
     * The caller must check that each sequence belongs to its bundled index.
     */
    static List<String> rankSearchCandidates(final String description, final int limit) {
        if (limit <= 0) return List.of();
        // An incidental trailing trigger like "crying" must not outrank
        // the semantic CLDR match for "laughing so hard I am crying".
        // Promote reviewed rules only for an exact explicit query; the
        // suggestion strip still uses suffix matches for live context.
        final String requestedPhrase = description == null ? "" : normalizeSuffix(description);
        final List<String> outputs = new ArrayList<>();
        for (final Candidate candidate : rank(description, false)) {
            if (!candidate.matchedTrigger.equals(requestedPhrase)) continue;
            outputs.add(candidate.emoji);
            if (outputs.size() == limit) break;
        }
        return Collections.unmodifiableList(outputs);
    }

    private static String normalizeSuffix(final String input) {
        final int start = Math.max(0, input.length() - MAX_CONTEXT_CHARS);
        final StringBuilder result = new StringBuilder(MAX_CONTEXT_CHARS);
        boolean space = true;
        for (int index = start; index < input.length();) {
            final int cp = input.codePointAt(index);
            index += Character.charCount(cp);
            if (Character.isLetter(cp)) {
                result.appendCodePoint(Character.toLowerCase(cp));
                space = false;
            } else if (Character.isDigit(cp)) {
                // Dates, identifiers and codes are not semantic Emoji triggers.
                return "";
            } else if (!space) {
                result.append(' ');
                space = true;
            }
        }
        return result.toString().trim();
    }
}
