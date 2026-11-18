// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji;

import android.content.Context;
import android.util.AtomicFile;
import android.util.Log;

import helium314.keyboard.latin.utils.DeviceProtectedUtils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.nio.charset.CharacterCodingException;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.function.Predicate;

/**
 * Opt-in, offline user-defined SEARCH shortcuts, not arbitrary Emoji graphics.
 * Each tab-separated line is "description<TAB>existing Unicode emoji".
 * Validate the whole document before atomically replacing an installed file.
 */
public final class EmojiDescribeCustomAliases {
    private static final String FILE_NAME = "emoji_describe_custom.tsv";
    private static final int MAX_BYTES = 32 * 1024;
    private static final int MAX_RULES = 128;

    private EmojiDescribeCustomAliases() { }

    public static File getFile(final Context context) {
        return new File(DeviceProtectedUtils.getFilesDir(context), FILE_NAME);
    }

    /** Called only from Settings' background import coroutine. */
    public static int importFrom(final Context context, final InputStream document)
            throws IOException {
        final byte[] bytes = readBounded(document);
        final EmojiContextIndex corpus;
        try (InputStream asset = context.getAssets().open(EmojiContextIndex.ASSET_NAME)) {
            corpus = EmojiContextIndex.read(asset);
        }
        final Map<String, String> validated = parse(new ByteArrayInputStream(bytes),
                candidate -> corpus.get(candidate) != null);
        // Re-encode only the reviewed, normalized columns. Extra input metadata
        // and unwanted whitespace must never persist into the private file.
        final StringBuilder contents = new StringBuilder();
        for (final Map.Entry<String, String> rule : validated.entrySet())
            contents.append(rule.getKey()).append('\t').append(rule.getValue()).append('\n');
        final AtomicFile target = new AtomicFile(getFile(context));
        java.io.FileOutputStream stream = null;
        try {
            stream = target.startWrite();
            stream.write(contents.toString().getBytes(StandardCharsets.UTF_8));
            target.finishWrite(stream);
        } catch (final IOException | RuntimeException failure) {
            if (stream != null) target.failWrite(stream);
            throw failure;
        }
        EmojiDescribeEngine.get().customMappingsChanged();
        return validated.size();
    }

    public static void clear(final Context context) {
        new AtomicFile(getFile(context)).delete();
        EmojiDescribeEngine.get().customMappingsChanged();
    }

    /** On the Describe worker only; invalid restored/older files fail closed. */
    static Map<String, String> load(final Context context, final EmojiDescribeIndex index) {
        final File file = getFile(context);
        if (!file.exists()) return Map.of();
        try (InputStream source = new FileInputStream(file)) {
            return parse(source, index::containsEmoji);
        } catch (final IOException | RuntimeException invalid) {
            Log.w("EmojiDescribe", "Custom Emoji shortcuts invalid; ignoring local file");
            return Map.of();
        }
    }

    static List<String> promote(final String userChoice, final List<String> builtIn,
            final int limit) {
        if (userChoice == null || limit <= 0) return builtIn;
        final LinkedHashSet<String> ordered = new LinkedHashSet<>();
        ordered.add(userChoice);
        ordered.addAll(builtIn);
        final java.util.ArrayList<String> all = new java.util.ArrayList<>(ordered);
        return List.copyOf(all.subList(0, Math.min(limit, all.size())));
    }

    /** Pure-JVM validation for unit tests. No escaping or alternate formats. */
    static Map<String, String> parse(final InputStream source,
            final Predicate<String> validEmoji) throws IOException {
        final String decoded;
        try {
            decoded = StandardCharsets.UTF_8.newDecoder()
                    .onMalformedInput(CodingErrorAction.REPORT)
                    .onUnmappableCharacter(CodingErrorAction.REPORT)
                    .decode(ByteBuffer.wrap(readBounded(source))).toString();
        } catch (final CharacterCodingException malformed) {
            throw new IOException("Expected UTF-8 text", malformed);
        }
        final LinkedHashMap<String, String> found = new LinkedHashMap<>();
        for (String line : decoded.replace("\r\n", "\n").split("\n", -1)) {
            if (line.startsWith("\uFEFF")) line = line.substring(1);
            if (line.isBlank() || line.startsWith("# ")) continue;
            final String[] columns = line.split("\t", -1);
            if (columns.length != 2) throw new IOException("Expected two tab-separated columns");
            final String description = columns[0].trim();
            final String emoji = columns[1].trim();
            if (description.isEmpty() || description.codePointCount(0, description.length()) > 64
                    || description.chars().anyMatch(Character::isISOControl)
                    || EmojiDescribeNormalizer.tokens(description, true).isEmpty()
                    || !validEmoji.test(emoji)) {
                throw new IOException("Invalid shortcut description or unsupported Emoji");
            }
            final String normalized = EmojiDescribeNormalizer.normalizePhrase(description);
            if (found.putIfAbsent(normalized, emoji) != null)
                throw new IOException("Duplicate shortcut description");
            if (found.size() > MAX_RULES) throw new IOException("Too many Emoji shortcuts");
        }
        if (found.isEmpty()) throw new IOException("No valid Emoji shortcuts");
        return Collections.unmodifiableMap(found);
    }

    private static byte[] readBounded(final InputStream stream) throws IOException {
        final ByteArrayOutputStream data = new ByteArrayOutputStream();
        final byte[] buffer = new byte[2048];
        int count;
        while ((count = stream.read(buffer)) != -1) {
            if (data.size() + count > MAX_BYTES) throw new IOException("Emoji file exceeds 32 KiB");
            data.write(buffer, 0, count);
        }
        return data.toByteArray();
    }
}
