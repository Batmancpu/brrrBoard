/*
 * Copyright (C) 2026 brrBoard contributors
 * SPDX-License-Identifier: GPL-3.0-only
 */
package helium314.keyboard.latin.dictionary;

import android.content.Context;
import com.android.inputmethod.latin.BinaryDictionary;
import helium314.keyboard.latin.utils.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashSet;
import java.util.Locale;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/**
 * Fixed offline brrPersonal supplement. The existing native decoder creates and caches
 * its binary representation from the compact, versioned asset on first use.
 */
public final class BrrPersonalDictionary extends ExpandableBinaryDictionary {
    private static final String TAG = "brrPersonal";
    // Only this public, derived cache gets a new key. Personal stores keep their filenames.
    private static final String NAME = "brrPersonal_v5";
    private static final String MODERN_ASSET = "brrPersonal_modern.tsv";
    private static final String DATA_ASSET = "brrPersonal.tsv";
    private static final String RULES_ASSET = "brrPersonal_rules.tsv";
    private static final String ENGLISH_VALID_ONLY_ASSET = "brrPersonal_english_valid_only.tsv";
    private static final int SHORTCUT_FREQUENCY = 14;

    private static volatile Set<String> noSuggestionWords = Collections.emptySet();
    private static volatile boolean noSuggestionWordsLoaded;
    private static volatile Map<String, ModernEntry> modernWords = Collections.emptyMap();

    private static final class ModernEntry {
        final int frequency;
        final boolean validOnly;

        ModernEntry(int frequency, boolean validOnly) {
            this.frequency = frequency;
            this.validOnly = validOnly;
        }
    }

    private BrrPersonalDictionary(final Context context, final Locale locale,
            final File dictFile, final String dictNamePrefix) {
        super(context, getDictName(dictNamePrefix + NAME, locale, dictFile),
                locale, Dictionary.TYPE_BRR_PERSONAL, dictFile);
        loadNoSuggestionWords(context);
        reloadDictionaryIfRequired();
    }

    public static BrrPersonalDictionary getDictionary(final Context context,
            final Locale locale, final File dictFile, final String dictNamePrefix) {
        return new BrrPersonalDictionary(context, locale, dictFile, dictNamePrefix);
    }

    /** A slang/profanity entry is spelling-valid but hidden from proactive text suggestions. */
    public static boolean isNoSuggestionWord(final String word) {
        return word != null && noSuggestionWords.contains(word.toLowerCase(Locale.ROOT));
    }

    public static boolean requiresPrefix(final String word) {
        final ModernEntry entry = modernWords.get(word.toLowerCase(Locale.ROOT));
        return entry != null && !entry.validOnly;
    }

    private static Map<String, ModernEntry> readModernWords(final Context context) {
        final Map<String, ModernEntry> entries = new LinkedHashMap<>();
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                context.getAssets().open(MODERN_ASSET), StandardCharsets.UTF_8))) {
            for (String line; (line = reader.readLine()) != null;) {
                if (line.isEmpty() || line.startsWith("#")) continue;
                final String[] fields = line.split("\\t", -1);
                if (fields.length != 4 || !fields[1].matches("[a-z]{2,24}(-[a-z]{1,12})?")) {
                    throw new IOException("Malformed lexicon entry");
                }
                final int frequency = Integer.parseInt(fields[2]);
                if ((!fields[0].equals("en") && !fields[0].equals("mix"))
                        || (!fields[3].equals("prefix") && !fields[3].equals("valid"))
                        || frequency < 1 || frequency > 105 || entries.size() >= 192
                        || entries.containsKey(fields[1])) {
                    throw new IOException("Invalid lexicon attributes");
                }
                entries.put(fields[1], new ModernEntry(frequency, fields[3].equals("valid")));
            }
            return Collections.unmodifiableMap(entries);
        } catch (IOException | NumberFormatException e) {
            Log.w(TAG, "Modern lexicon unavailable");
            return Collections.emptyMap();
        }
    }

    private static void loadNoSuggestionWords(final Context context) {
        if (noSuggestionWordsLoaded) return;
        synchronized (BrrPersonalDictionary.class) {
            if (noSuggestionWordsLoaded) return;
            final Set<String> words = new HashSet<>();
            for (final String asset : new String[]{RULES_ASSET, ENGLISH_VALID_ONLY_ASSET}) {
                try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                        context.getAssets().open(asset), StandardCharsets.UTF_8))) {
                    String line;
                    while ((line = reader.readLine()) != null) {
                        if (line.isEmpty() || line.startsWith("#")) continue;
                        final String[] fields = line.split("\\t", 2);
                        final String word = fields[0].trim().toLowerCase(Locale.ROOT);
                        if (word.matches("[a-z]{2,24}")) words.add(word);
                    }
                } catch (IOException e) {
                    Log.e(TAG, "Could not load brrPersonal no-suggest rules", e);
                }
            }
            modernWords = readModernWords(context);
            for (Map.Entry<String, ModernEntry> entry : modernWords.entrySet()) {
                if (entry.getValue().validOnly) words.add(entry.getKey());
            }
            noSuggestionWords = Collections.unmodifiableSet(words);
            noSuggestionWordsLoaded = true;
        }
    }

    @Override
    protected void loadInitialContentsLocked() {
        int loadedCount = 0;
        final Set<String> seedWords = new HashSet<>();
        final EnglishApostropheLexicon.Audit apostropheAudit =
                EnglishApostropheLexicon.audit(mContext, mLocale);
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                mContext.getAssets().open(DATA_ASSET), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.isEmpty() || line.startsWith("#")) continue;
                final String[] fields = line.split("\\t", -1);
                if (fields.length < 3) continue;
                final String word = fields[0].trim().toLowerCase(Locale.ROOT);
                if (word.length() > MAX_WORD_LENGTH || !word.matches("[a-z]{2,24}")) continue;
                final int frequency;
                try {
                    frequency = Math.max(1, Math.min(255, Integer.parseInt(fields[1])));
                } catch (NumberFormatException e) {
                    continue;
                }
                final String shortcut = fields[2].isEmpty() ? null : fields[2];
                // Only suppress specifically reviewed malformed corpus spellings when the
                // bundled main does not contain them. Preserve all shortcuts and valid-only rules.
                if (shortcut == null && !isNoSuggestionWord(word)
                        && apostropheAudit.getMalformedAliases().contains(word)) continue;
                addUnigramLocked(word, frequency, shortcut,
                        shortcut == null ? 0 : SHORTCUT_FREQUENCY,
                        false /* isNotAWord */, false /* isPossiblyOffensive */,
                        BinaryDictionary.NOT_A_VALID_TIMESTAMP);
                seedWords.add(word);
                ++loadedCount;
            }
        } catch (IOException e) {
            Log.e(TAG, "Could not load brrPersonal asset", e);
        }
        for (final EnglishApostropheLexicon.Entry entry : apostropheAudit.getMissing()) {
            addUnigramLocked(entry.getWord(), entry.getFrequency(), null, 0,
                    false, false, BinaryDictionary.NOT_A_VALID_TIMESTAMP);
            ++loadedCount;
        }
        final boolean english = "en".equals(mLocale.getLanguage());
        final boolean romanHindi = "hi".equals(mLocale.getLanguage()) && "Latn".equals(mLocale.getScript());
        for (Map.Entry<String, ModernEntry> entry : modernWords.entrySet()) {
            final ModernEntry value = entry.getValue();
            if ((!english && !romanHindi) || seedWords.contains(entry.getKey())) continue;
            addUnigramLocked(entry.getKey(), value.frequency, null, 0,
                    false, false, BinaryDictionary.NOT_A_VALID_TIMESTAMP);
            ++loadedCount;
        }
        Log.i(TAG, "Loaded " + loadedCount + " brrPersonal entries");
    }
}
