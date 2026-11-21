// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.suggestions;

import helium314.keyboard.latin.SuggestedWords;
import helium314.keyboard.latin.SuggestedWords.SuggestedWordInfo;
import helium314.keyboard.latin.dictionary.Dictionary;

import java.util.ArrayList;
import java.util.Locale;

/**
 * Synchronous, zero-network, English-only predictions for the keyboard's first frame.
 * These are neutral starting words, never trained/user-specific or autocorrections.
 * The decoder replaces them as soon as its actual context predictions arrive.
 */
public final class StartupSuggestionPolicy {
    private StartupSuggestionPolicy() { }

    public static boolean eligible(final Locale locale, final boolean isProse,
            final boolean lookupsEnabled, final boolean predictionEnabled,
            final boolean suggestionsEnabled, final boolean secureOrIncognito,
            final boolean applicationCompletions, final boolean inlineAutofill,
            final boolean hardwareKeyboard, final boolean delayedKeyboard,
            final int initialSelectionStart, final int initialSelectionEnd) {
        return locale != null && "en".equals(locale.getLanguage())
                && isProse && lookupsEnabled && predictionEnabled && suggestionsEnabled
                && !secureOrIncognito && !applicationCompletions && !inlineAutofill
                && !hardwareKeyboard && !delayedKeyboard
                && initialSelectionStart == 0 && initialSelectionEnd == 0;
    }

    public static SuggestedWords initialEnglishWords() {
        final ArrayList<SuggestedWordInfo> words = new ArrayList<>(3);
        for (final String word : new String[] { "I", "The", "You" }) {
            words.add(new SuggestedWordInfo(word, "", 0, SuggestedWordInfo.KIND_PREDICTION,
                    Dictionary.DICTIONARY_HARDCODED, SuggestedWordInfo.NOT_AN_INDEX,
                    SuggestedWordInfo.NOT_A_CONFIDENCE));
        }
        return new SuggestedWords(words, null, null, false, false, false,
                SuggestedWords.INPUT_STYLE_BEGINNING_OF_SENTENCE_PREDICTION,
                SuggestedWords.NOT_A_SEQUENCE_NUMBER);
    }
}
