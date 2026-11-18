// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EmojiSearchContextPolicyTest {
    @Test public void onlyShortOrdinaryProseMayBeConsidered() {
        assertEquals("", EmojiSearchContextPolicy.safeSuffix(null));
        assertEquals("", EmojiSearchContextPolicy.safeSuffix(""));
        assertEquals("", EmojiSearchContextPolicy.safeSuffix("card 1234"));
        assertEquals("", EmojiSearchContextPolicy.safeSuffix("user@example.com"));
        assertEquals("", EmojiSearchContextPolicy.safeSuffix("https://example.com"));
        assertEquals("", EmojiSearchContextPolicy.safeSuffix("a=b"));
        assertEquals("Let's get pizza tonight",
                EmojiSearchContextPolicy.safeSuffix("Let's get pizza tonight"));
    }

    @Test public void boundedContextRetainsTheMostRecentWords() {
        final String prefix = "older words ".repeat(20);
        final String suffix = EmojiSearchContextPolicy.safeSuffix(prefix + "pizza tonight");
        assertTrue(suffix.endsWith("pizza tonight"));
        assertTrue(suffix.codePointCount(0, suffix.length()) <= 80);
    }
}
