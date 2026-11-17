// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertThrows;

import org.junit.Test;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;

public class EmojiContextIndexTest {
    private static ByteArrayInputStream utf8(final String value) {
        return new ByteArrayInputStream(value.getBytes(StandardCharsets.UTF_8));
    }

    @Test public void indexesFullCodePointSequencesWithoutCollapsingVariants() throws IOException {
        final EmojiContextIndex index = EmojiContextIndex.read(utf8(
                "# Unicode CLDR 48.2\n"
                + "🎂\tbirthday cake\tbirthday | cake | dessert\n"
                + "👨‍👩‍👧\tfamily\tfamily | parents\n"
                + "👍🏻\tthumbs up: light skin tone\tagree | thumbs up\n"
                + "#️⃣\tkeycap: #\thash | keycap\n"));
        assertEquals(4, index.size());
        assertEquals("birthday cake", index.get("🎂").name);
        assertEquals("family | parents", index.get("👨‍👩‍👧").keywords);
        assertNull(index.get("👍"));
        assertEquals("thumbs up: light skin tone", index.get("👍🏻").name);
        assertEquals("keycap: #", index.get("#️⃣").name);
    }

    @Test public void invalidAndDuplicateRowsFailRatherThanSilentlyDrift() {
        assertThrows(IOException.class, () -> EmojiContextIndex.read(utf8("🎂\tname\n")));
        assertThrows(IOException.class, () -> EmojiContextIndex.read(utf8(
                "🎂\tcake\tbirthday\n🎂\tduplicate\tbirthday\n")));
    }
}
