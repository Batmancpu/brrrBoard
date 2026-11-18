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

    @Test public void realLifeDescriptionsUseMeaningRatherThanOnlyLiteralEmojiNames()
            throws Exception {
        Path asset = Path.of("src/main/assets/emoji_cldr48_en.tsv");
        if (!Files.exists(asset)) asset = Path.of("app/src/main/assets/emoji_cldr48_en.tsv");
        final EmojiDescribeIndex engine;
        try (InputStream input = Files.newInputStream(asset)) {
            engine = EmojiDescribeIndex.from(EmojiContextIndex.read(input));
        }
        // These are natural descriptions, not CLDR short names. Several
        // reproduced zero matches in the owner's v113 recording.
        assertEquals("😎", engine.search("smiley with sunglasses", 8).get(0));
        assertEquals("🥸", engine.search("smily glasses", 8).get(0));
        assertEquals("🥸", engine.search("man with glasses", 8).get(0));
        assertEquals("🤣", engine.search("laughing so hard I am crying", 8).get(0));
        assertEquals("🤣", engine.search("I can't stop laughing", 8).get(0));
        assertEquals("🤮", engine.search("I am sick and throwing up", 8).get(0));
        assertEquals("🧑‍🦯", engine.search(
                "a person who cannot see and walks with a cane", 8).get(0));
        assertEquals("🦮", engine.search("a dog helping a blind person", 8).get(0));
        assertTrue(engine.search("party because someone was born today", 8).contains("🥳"));
        assertEquals("💔", engine.search("love that hurts", 8).get(0));
        assertTrue(engine.search("I have no energy and need sleep", 8).contains("😴"));
        assertEquals("😬", engine.search(
                "I made a silly mistake and feel awkward", 8).get(0));
        assertEquals("😅", engine.search("I am really nervous about this", 8).get(0));
        assertEquals("🤫", engine.search("someone keeping a secret", 8).get(0));
        assertEquals("🍵", engine.search("take a break and have a cup of tea", 8).get(0));
        assertEquals("🚽", engine.search("need to use the bathroom", 8).get(0));
        assertEquals("🏖️", engine.search("sunny and happy beach holiday", 8).get(0));

        // There is no Unicode composite for a man with an ordinary pet dog;
        // weak one-word overlap must not hallucinate a combined character.
        assertTrue(engine.search("man with a dog", 8).isEmpty());
        assertTrue(engine.search("flying dog", 8).isEmpty());
    }

    @Test public void blankSearchCanRankContextTopicsWithoutScanningEditorOnUiThread()
            throws Exception {
        Path asset = Path.of("src/main/assets/emoji_cldr48_en.tsv");
        if (!Files.exists(asset)) asset = Path.of("app/src/main/assets/emoji_cldr48_en.tsv");
        final EmojiDescribeIndex engine;
        try (InputStream input = Files.newInputStream(asset)) {
            engine = EmojiDescribeIndex.from(EmojiContextIndex.read(input));
        }
        assertEquals("🍕", engine.suggestFromContext("Let's get pizza tonight", 8).get(0));
        assertEquals("☕", engine.suggestFromContext("We need coffee", 8).get(0));
        assertEquals("🏖️", engine.suggestFromContext("Planning a beach holiday", 8).get(0));
        assertEquals("🎥", engine.suggestFromContext("Let's watch a movie tonight", 8).get(0));
        assertEquals("🐕", engine.suggestFromContext("We need dog food", 8).get(0));
        // Context is read from the end; a long preceding sentence must never
        // cause the latest subject to be silently dropped by query token caps.
        assertEquals("🍕", engine.suggestFromContext(
                "I wanted to explain a few things but most of all let's order pizza", 8).get(0));
        assertTrue(engine.suggestFromContext("", 8).isEmpty());
        assertTrue(engine.suggestFromContext("pizza", 0).isEmpty());
    }
}
