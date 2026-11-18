// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import org.junit.Test;

public class EmojiSearchIdleResultsTest {
    @Test public void emptyHistoryStillShowsTenTappableEmojis() {
        final List<String> results = EmojiSearchIdleResults.merge(
                List.of(), List.of(), List.of(), 10);
        assertEquals(10, results.size());
        assertEquals("😀", results.get(0));
        assertTrue(results.contains("❤️"));
    }

    @Test public void specificRecentContextOutranksPastUsageAndDefaults() {
        final List<String> results = EmojiSearchIdleResults.merge(
                List.of("🎂", "🎉"), List.of("🍕", "🎂"),
                List.of("👍", "🍕", "🔥"), 10);
        assertEquals(List.of("🎂", "🎉", "🍕", "👍", "🔥"), results.subList(0, 5));
        assertEquals(10, results.size());
    }

    @Test public void respectsSmallLimitsAndNeverSynthesizesFromNull() {
        assertTrue(EmojiSearchIdleResults.merge(
                List.of(), List.of(), List.of(), 0).isEmpty());
        assertEquals(List.of("🍕"), EmojiSearchIdleResults.merge(
                List.of(), List.of("🍕"), List.of(), 1));
        assertFalse(EmojiSearchIdleResults.merge(
                List.of(), List.of(), List.of(), 3).isEmpty());
    }
}
