// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji;

import java.util.Map;

/**
 * Small, reviewed English phrase/word aliases. They are not learned from the
 * user, do not rewrite text and never generate new Unicode Emoji sequences.
 * Keep this table deliberately conservative: CLDR remains the main authority.
 */
final class EmojiDescribeAliases {
    private static final Map<String, String[]> WORDS = Map.ofEntries(
            Map.entry("bday", new String[]{"birthday"}),
            Map.entry("hbd", new String[]{"birthday"}),
            Map.entry("joyful", new String[]{"joy", "happy"}),
            Map.entry("weeping", new String[]{"cry", "crying"}),
            Map.entry("sobbing", new String[]{"sob", "cry"}),
            Map.entry("giggling", new String[]{"giggle", "laugh"}),
            Map.entry("laughing", new String[]{"laugh"}),
            Map.entry("teary", new String[]{"tear"}),
            Map.entry("doggo", new String[]{"dog"}),
            Map.entry("pooch", new String[]{"dog"}),
            Map.entry("kitty", new String[]{"cat"}),
            Map.entry("feline", new String[]{"cat"}),
            Map.entry("indian", new String[]{"india"}),
            Map.entry("bharat", new String[]{"india"}),
            Map.entry("chai", new String[]{"tea"}),
            Map.entry("loo", new String[]{"toilet"}),
            Map.entry("restroom", new String[]{"toilet"}),
            Map.entry("bathroom", new String[]{"toilet"}),
            Map.entry("puking", new String[]{"vomit"}),
            Map.entry("furious", new String[]{"angry"}),
            Map.entry("smiley", new String[]{"smile", "smiling", "face"}),
            Map.entry("smily", new String[]{"smile", "face"}),
            Map.entry("man", new String[]{"person"}),
            Map.entry("lady", new String[]{"woman", "person"}),
            Map.entry("love", new String[]{"heart"}),
            Map.entry("silly", new String[]{"funny", "awkward"}),
            Map.entry("throwing", new String[]{"throw", "vomit"}),
            Map.entry("sickly", new String[]{"sick", "vomit"}),
            Map.entry("hurt", new String[]{"broken", "sad"}),
            Map.entry("shades", new String[]{"sunglasses"}),
            Map.entry("sunny", new String[]{"sun", "beach"}),
            Map.entry("holiday", new String[]{"vacation", "beach"}),
            Map.entry("stressed", new String[]{"stress", "nervous"})
    );

    private static final Map<String, String> PHRASES = Map.ofEntries(
            Map.entry("laughing with tears", "😂"),
            Map.entry("laughing tears", "😂"),
            Map.entry("crying laughing", "😂"),
            Map.entry("laughing and crying", "😂"),
            Map.entry("tears of joy", "😂"),
            Map.entry("red heart with arrow", "💘"),
            Map.entry("heart shot by arrow", "💘"),
            Map.entry("burning heart", "❤️‍🔥"),
            Map.entry("heart on fire", "❤️‍🔥"),
            Map.entry("birthday cake", "🎂")
    );

    private EmojiDescribeAliases() { }

    static String[] expand(final String word) {
        return WORDS.get(word);
    }

    static String preferredEmoji(final String normalizedPhrase) {
        return PHRASES.get(normalizedPhrase);
    }

    /**
     * Recognize a few ordinary English idioms before CLDR token lookup.
     * Only verified meanings are rewritten; arbitrary unrelated adjectives
     * still require one real emoji to match the whole description.
     */
    static String rewriteDescription(final String normalizedPhrase) {
        String phrase = " " + normalizedPhrase + " ";
        phrase = phrase.replace(" can t stop laughing ", " laughing ")
                .replace(" can t stop ", " ")
                .replace(" cannot stop ", " ")
                .replace(" cannot see ", " blind ")
                .replace(" can t see ", " blind ")
                .replace(" no energy ", " tired ")
                .replace(" born today ", " birthday ")
                .replace(" take a break ", " ")
                .replace(" silly mistake ", " grimacing ")
                .replace(" keeping a secret ", " shushing ");
        // CLDR includes the real guide-dog character, but no arbitrary
        // "man and pet dog" combination. Keep that distinction.
        if (phrase.contains(" dog ") && phrase.contains(" blind ")
                && (phrase.contains(" helping ") || phrase.contains(" guiding ")))
            return "guide dog";
        if (phrase.contains(" beach ") && (phrase.contains(" holiday ")
                || phrase.contains(" vacation ")))
            return "beach umbrella";
        if (phrase.contains(" cane ")) phrase = phrase.replace(" walks ", " ");
        return EmojiDescribeNormalizer.normalizePhrase(phrase);
    }
}
