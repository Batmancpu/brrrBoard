// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.Test;

/** Pure-JVM relevance, privacy-boundary and typo regression fixtures. */
public class EmojiDescribeIndexTest {
    private static EmojiDescribeIndex index() throws Exception {
        final String rows = "😂\tface with tears of joy\tcrying | laugh | tear | funny | joy\n"
                + "🤣\trolling on the floor laughing\tcry | laugh | tear | funny\n"
                + "😢\tcrying face\tcry | sad | tear\n"
                + "❤️\tred heart\tred | heart | love\n"
                + "💘\theart with arrow\tarrow | heart | love\n"
                + "❤️‍🔥\theart on fire\tfire | heart | burn\n"
                + "🐶\tdog face\tdog | pet | puppy\n"
                + "🎂\tbirthday cake\tbirthday | cake | celebrate\n"
                + "🚽\ttoilet\tbathroom | restroom | wc\n"
                + "🎓\tgraduation cap\teducation | college | school\n";
        return EmojiDescribeIndex.from(EmojiContextIndex.read(
                new ByteArrayInputStream(rows.getBytes(StandardCharsets.UTF_8))));
    }

    @Test public void exactNamesAndFullWidthNfkcWin() throws Exception {
        final EmojiDescribeIndex search = index();
        assertEquals("❤️", search.search("red heart", 24).get(0));
        assertEquals("❤️", search.search("ＲＥＤ　ＨＥＡＲＴ", 24).get(0));
        assertEquals("🐶", search.search("dog face", 24).get(0));
        assertEquals("🎂", search.search("birthday cake", 24).get(0));
    }

    @Test public void naturalDescriptionsUseReviewedMeaningWithoutInventingEmoji() throws Exception {
        final EmojiDescribeIndex search = index();
        assertEquals("😂", search.search("laughing with tears", 24).get(0));
        assertEquals("😂", search.search("crying laughing", 24).get(0));
        assertEquals("💘", search.search("red heart with arrow", 24).get(0));
        assertEquals("❤️‍🔥", search.search("burning heart", 24).get(0));
        assertEquals("🎂", search.search("bday cake", 24).get(0));
        assertEquals("🚽", search.search("loo", 24).get(0));
    }

    @Test public void typosAndAdjacentTranspositionsAreBounded() throws Exception {
        final EmojiDescribeIndex search = index();
        assertEquals("🐶", search.search("doog face", 24).get(0));
        assertEquals("💘", search.search("hearrt arwow", 24).get(0));
        assertEquals(1, EmojiDescribeIndex.damerauDistance("haert", "heart", 1));
        assertTrue(EmojiDescribeIndex.damerauDistance("cat", "heart", 1) > 1);
    }

    @Test public void noSubstringTrapsHallucinationOrBroadQueryLeakage() throws Exception {
        final EmojiDescribeIndex search = index();
        assertTrue(search.search("cat", 24).isEmpty()); // education does NOT contain a cat token
        assertTrue(search.search("unknownword dog", 24).isEmpty());
        assertTrue(search.search("flying dog", 24).isEmpty());
        assertTrue(search.search("zzzx", 24).isEmpty());
        assertTrue(search.search("the with a", 24).isEmpty());
        assertTrue(search.search("", 24).isEmpty());
        assertTrue(search.search(null, 24).isEmpty());
        assertEquals(0, search.search("dog", 0).size());
        assertEquals(1, search.search("heart", 1).size());
        assertFalse(search.search("dog", 24).isEmpty());
    }

    @Test public void indexAndInputAreStrictlyBoundedAndImmutable() throws Exception {
        final EmojiDescribeIndex search = index();
        assertEquals(10, search.emojiCount());
        assertTrue(search.termCount() > 10);
        assertEquals(80, EmojiDescribeNormalizer.clampQuery("x".repeat(100)).length());
        assertEquals(8, EmojiDescribeNormalizer.tokens(
                "one two three four five six seven eight nine ten", true).size());
        final List<String> result = search.search("dog", 24);
        boolean immutable = false;
        try { result.add("🦄"); } catch (UnsupportedOperationException expected) {
            immutable = true;
        }
        assertTrue(immutable);
        for (final String candidate : result) {
            assertTrue(List.of("😂", "🤣", "😢", "❤️", "💘", "❤️‍🔥", "🐶", "🎂", "🚽", "🎓")
                    .contains(candidate));
        }
    }
}
