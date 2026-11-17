// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/** Immutable, lazily parsed CLDR 48.2 English Emoji metadata (search, not typing triggers). */
final class EmojiContextIndex {
    static final String ASSET_NAME = "emoji_cldr48_en.tsv";

    static final class Annotation {
        final String name;
        final String keywords;

        Annotation(final String name, final String keywords) {
            this.name = name;
            this.keywords = keywords;
        }
    }

    private final Map<String, Annotation> mByEmoji;

    private EmojiContextIndex(final Map<String, Annotation> byEmoji) {
        mByEmoji = Collections.unmodifiableMap(byEmoji);
    }

    static EmojiContextIndex read(final InputStream input) throws IOException {
        final Map<String, Annotation> annotations = new LinkedHashMap<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(input, StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // #️⃣ is a legitimate Emoji sequence; only '# ' starts a
                // provenance/comment line in this generated TSV format.
                if (line.isEmpty() || line.startsWith("# ")) {
                    continue;
                }
                final String[] columns = line.split("\\t", 3);
                if (columns.length != 3 || columns[0].isEmpty()
                        || columns[0].codePointCount(0, columns[0].length()) > 32) {
                    throw new IOException("Invalid Emoji annotation row");
                }
                if (annotations.putIfAbsent(columns[0],
                        new Annotation(columns[1], columns[2])) != null) {
                    throw new IOException("Duplicate Emoji annotation sequence");
                }
            }
        }
        return new EmojiContextIndex(annotations);
    }

    Annotation get(final String exactEmoji) {
        return mByEmoji.get(exactEmoji);
    }

    int size() {
        return mByEmoji.size();
    }
}
