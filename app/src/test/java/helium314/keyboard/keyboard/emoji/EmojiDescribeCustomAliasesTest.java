// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertThrows;
import static org.junit.Assert.assertTrue;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.List;
import java.util.Set;
import org.junit.Test;

/** Pure parser tests: imported files cannot mutate the built-in Unicode corpus. */
public class EmojiDescribeCustomAliasesTest {
    private static final Set<String> CORPUS = Set.of("😂", "🎂", "💖", "🐶");

    private static Map<String, String> parse(final String lines) throws IOException {
        return EmojiDescribeCustomAliases.parse(
                new ByteArrayInputStream(lines.getBytes(StandardCharsets.UTF_8)),
                CORPUS::contains);
    }

    @Test public void acceptsOnlyNormalizedDescriptionsMappedToKnownEmoji() throws Exception {
        final Map<String, String> rules = parse("# Emoji shortcuts\n"
                + "Sparkly   heart\t💖\r\n"
                + "FESTIVE cake\t🎂\n"
                + "\uFEFFmy happy tears\t😂\n");
        assertEquals(3, rules.size());
        assertEquals("💖", rules.get("sparkly heart"));
        assertEquals("🎂", rules.get("festive cake"));
        assertEquals("😂", rules.get("my happy tears"));
        assertFalse(rules.containsKey("FESTIVE cake"));
        assertThrows(UnsupportedOperationException.class, () -> rules.put("new", "🐶"));
    }

    @Test public void rejectsMalformedOversizedUnsupportedAndDuplicateFiles() {
        assertThrows(IOException.class, () -> parse("face\t🤯\n"));
        assertThrows(IOException.class, () -> parse("\t🎂\n"));
        assertThrows(IOException.class, () -> parse("cake 🎂\n"));
        assertThrows(IOException.class, () -> parse("fast\t🎂\textra\n"));
        assertThrows(IOException.class, () -> parse("birthday cake\t🎂\nBIRTHDAY  CAKE\t💖\n"));
        assertThrows(IOException.class, () -> parse("# only a comment\n"));
        assertThrows(IOException.class, () -> parse("x".repeat(33_000) + "\t🎂"));
        final StringBuilder tooMany = new StringBuilder();
        for (int i = 0; i < 129; i++) tooMany.append("shortcut").append(i).append("\t🎂\n");
        assertThrows(IOException.class, () -> parse(tooMany.toString()));
        assertThrows(IOException.class, () -> parse("a\t🐶\n")); // only filler word
    }

    @Test public void strictUtf8NeverSilentlyRepairsInvalidInput() {
        final byte[] invalid = {(byte) 0xC3, (byte) 0x28, 0x09, 0x41};
        assertThrows(IOException.class, () -> EmojiDescribeCustomAliases.parse(
                new ByteArrayInputStream(invalid), CORPUS::contains));
        assertTrue(CORPUS.contains("🐶"));
    }

    @Test public void exactUserChoiceIsFirstDeduplicatedAndResultLimited() {
        assertEquals(List.of("💖", "😂"), EmojiDescribeCustomAliases.promote(
                "💖", List.of("😂", "💖", "🎂"), 2));
        assertEquals(List.of("😂"), EmojiDescribeCustomAliases.promote(
                "😂", List.of("😂", "🎂"), 1));
        assertEquals(List.of("🐶"), EmojiDescribeCustomAliases.promote(
                "🐶", List.of("😂"), 1));
        assertEquals(List.of("🎂"), EmojiDescribeCustomAliases.promote(
                null, List.of("🎂"), 2));
    }
}
