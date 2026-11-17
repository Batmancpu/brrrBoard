// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import org.junit.Test;

public class EmojiContextSearchTest {
    private static EmojiContextIndex sample() throws Exception {
        final String rows = "😀\tgrinning face\tface | happy | grin | smile\n"
                + "❤️\tred heart\theart | hearts | love | red\n"
                + "☕\thot beverage\tcoffee | drink | hot\n";
        return EmojiContextIndex.read(new ByteArrayInputStream(
                rows.getBytes(StandardCharsets.UTF_8)));
    }

    @Test public void searchNamesAndKeywordsWithoutBinaryDictionary() throws Exception {
        assertEquals("❤️", sample().search("red heart", 8).get(0));
        assertEquals("😀", sample().search("smile", 8).get(0));
        assertEquals("☕", sample().search("coffee", 8).get(0));
    }

    @Test public void searchIsCaseInsensitiveAndBounded() throws Exception {
        assertEquals("☕", sample().search("  COFFEE ", 8).get(0));
        assertEquals(1, sample().search("heart", 1).size());
        assertFalse(sample().search("face", 8).isEmpty());
        assertTrue(sample().search("nonsense-xx", 8).isEmpty());
        assertTrue(sample().search("", 8).isEmpty());
    }
}
