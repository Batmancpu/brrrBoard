// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji;

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
final class EmojiContextScorer {
    private static final int MIN_SCORE = 450;
    private static final int MAX_CONTEXT_CHARS = 160;
    private static final int MAX_RESULTS = 4;

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
        add(rules, "namaste", "🙏", 545); add(rules, "shukriya", "🙏", 540);
        add(rules, "bahut badhiya", "👌", 550); add(rules, "yaar mast", "😎", 585);
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
