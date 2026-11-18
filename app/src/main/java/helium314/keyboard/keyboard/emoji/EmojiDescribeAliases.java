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
            Map.entry("furious", new String[]{"angry"})
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
}
