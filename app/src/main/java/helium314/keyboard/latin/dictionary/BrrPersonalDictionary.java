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
import java.util.Set;

/**
 * Fixed offline brrPersonal supplement. The existing native decoder creates and caches
 * its binary representation from the compact, versioned asset on first use.
 */
public final class BrrPersonalDictionary extends ExpandableBinaryDictionary {
    private static final String TAG = "brrPersonal";
    // Rebuild the on-device expandable cache after adding the reviewed social lexicon.
    // Keep the old v1 cache untouched for rollback and backup compatibility.
    // Separate cache for the extended static validity lexicon; preserve v2/learned data.
    private static final String NAME = "brrPersonal_v4";
    private static final String DATA_ASSET = "brrPersonal.tsv";
    private static final String RULES_ASSET = "brrPersonal_rules.tsv";
    private static final String ENGLISH_VALID_ONLY_ASSET = "brrPersonal_english_valid_only.tsv";
    private static final int SHORTCUT_FREQUENCY = 14;

    private static volatile Set<String> noSuggestionWords = Collections.emptySet();
    private static volatile boolean noSuggestionWordsLoaded;

    private BrrPersonalDictionary(final Context context, final Locale locale,
            final File dictFile, final String dictNamePrefix) {
        super(context, getDictName(dictNamePrefix + ("en".equals(locale.getLanguage())
                        ? NAME : "brrPersonal_v3"), locale, dictFile),
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
            noSuggestionWords = Collections.unmodifiableSet(words);
            noSuggestionWordsLoaded = true;
        }
    }

    @Override
    protected void loadInitialContentsLocked() {
        int loadedCount = 0;
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
        Log.i(TAG, "Loaded " + loadedCount + " brrPersonal entries");
    }
}
