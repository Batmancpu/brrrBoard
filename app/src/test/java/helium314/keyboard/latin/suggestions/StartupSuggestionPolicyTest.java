// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.suggestions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import helium314.keyboard.latin.SuggestedWords;

import org.junit.Test;

import java.util.Locale;

public class StartupSuggestionPolicyTest {
    private boolean eligible(final Locale locale, final boolean prose, final boolean privacy,
            final boolean appCompletion, final boolean autofill, final int selection) {
        return StartupSuggestionPolicy.eligible(locale, prose, true, true, true, privacy,
                appCompletion, autofill, false, false, selection, selection);
    }

    @Test public void englishProseAtEmptyBeginningHasAnImmediateRow() {
        assertTrue(eligible(Locale.ENGLISH, true, false, false, false, 0));
        final SuggestedWords words = StartupSuggestionPolicy.initialEnglishWords();
        assertEquals(3, words.getWordCountToShow());
        assertTrue(words.isPrediction());
        assertFalse(words.mWillAutoCorrect);
        assertFalse(words.mTypedWordValid);
        assertEquals("I", words.getWord(0));
        assertEquals("The", words.getWord(1));
        assertEquals("You", words.getWord(2));
        for (int i = 0; i < words.size(); i++) {
            assertEquals(SuggestedWords.SuggestedWordInfo.KIND_PREDICTION,
                    words.getInfo(i).getKind());
        }
    }

    @Test public void nonProsePrivateOrExternallyOwnedFieldsDoNotSeed() {
        assertFalse(eligible(Locale.FRENCH, true, false, false, false, 0));
        assertFalse(eligible(Locale.ENGLISH, false, false, false, false, 0));
        assertFalse(eligible(Locale.ENGLISH, true, true, false, false, 0));
        assertFalse(eligible(Locale.ENGLISH, true, false, true, false, 0));
        assertFalse(eligible(Locale.ENGLISH, true, false, false, true, 0));
        assertFalse(eligible(Locale.ENGLISH, true, false, false, false, 3));
    }

    @Test public void disabledPredictionsHardwareAndPendingKeyboardDoNotSeed() {
        assertFalse(StartupSuggestionPolicy.eligible(Locale.ENGLISH,
                true, true, false, true, false, false, false, false, false, 0, 0));
        assertFalse(StartupSuggestionPolicy.eligible(Locale.ENGLISH,
                true, true, true, false, false, false, false, false, false, 0, 0));
        assertFalse(StartupSuggestionPolicy.eligible(Locale.ENGLISH,
                true, true, true, true, false, false, false, true, false, 0, 0));
        assertFalse(StartupSuggestionPolicy.eligible(Locale.ENGLISH,
                true, true, true, true, false, false, false, false, true, 0, 0));
    }
}
