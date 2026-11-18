// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.Test;

/** Verifies the real Unicode-17/CLDR-48.2 asset, not only hand-picked fixtures. */
public class EmojiDescribeBundledCorpusTest {
    @Test public void realCorpusSupportsDescribeSearchWithoutDuplicateDataFiles() throws Exception {
        Path asset = Path.of("src/main/assets/emoji_cldr48_en.tsv");
        if (!Files.exists(asset)) asset = Path.of("app/src/main/assets/emoji_cldr48_en.tsv");
        assertTrue("Pinned CLDR asset must be present", Files.exists(asset));
        final EmojiContextIndex corpus;
        try (InputStream input = Files.newInputStream(asset)) {
            corpus = EmojiContextIndex.read(input);
        }
        assertEquals(3944, corpus.size());
        final EmojiDescribeIndex engine = EmojiDescribeIndex.from(corpus);
        assertEquals(corpus.size(), engine.emojiCount());
        assertTrue(engine.termCount() > 2000);
        assertEquals("😂", engine.search("laughing with tears", 24).get(0));
        assertEquals("❤️", engine.search("red heart", 24).get(0));
        assertEquals("🐶", engine.search("dog face", 24).get(0));
        assertEquals("🎂", engine.search("bday cake", 24).get(0));
        assertTrue(engine.search("definitely-not-a-real-emoji-phrase-blorple", 24).isEmpty());
        for (final String query : List.of("happy", "dog", "heart", "crying", "haert",
                "birthday cake", "flying car", "red heart with arrow", "fire", "flower")) {
            final List<String> results = engine.search(query, 24);
            assertTrue(results.size() <= 24);
            for (final String emoji : results) assertFalse(emoji.isEmpty());
        }
    }
}
