// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.brain;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import helium314.keyboard.latin.NgramContext;
import helium314.keyboard.latin.SuggestedWords.SuggestedWordInfo;
import helium314.keyboard.latin.brain.BrPersonalContextFeatures.Source;
import helium314.keyboard.latin.dictionary.Dictionary;
import helium314.keyboard.latin.common.ComposedData;
import helium314.keyboard.latin.settings.SettingsValuesForSuggestion;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/** Personal-history features are inputs, not replacement learning storage. */
@RunWith(RobolectricTestRunner.class)
public class BrPersonalContextFeaturesTest {
    private static final class FakeDictionary extends Dictionary {
        FakeDictionary(final String type) { super(type, null); }

        @Override public ArrayList<SuggestedWordInfo> getSuggestions(
                final ComposedData data, final NgramContext context, final long layout,
                final SettingsValuesForSuggestion settings, final int session, final float weight,
                final float[] override) {
            return null;
        }

        @Override public boolean isInDictionary(final String word) { return false; }
    }

    private static SuggestedWordInfo info(final String word, final int score, final String source) {
        return new SuggestedWordInfo(word, "", score, SuggestedWordInfo.KIND_PREDICTION,
                new FakeDictionary(source), SuggestedWordInfo.NOT_AN_INDEX,
                SuggestedWordInfo.NOT_A_CONFIDENCE);
    }

    @Test public void previouslyLearnedHistoryIsAFirstClassModelFeature() {
        assertEquals(Source.LEARNED_HISTORY, BrPersonalContextFeatures.classifySource(
                Dictionary.TYPE_USER_HISTORY, true));
        assertEquals(Source.PERSONAL_DICTIONARY, BrPersonalContextFeatures.classifySource(
                Dictionary.TYPE_USER, true));
        assertEquals(Source.PERSONAL_DICTIONARY, BrPersonalContextFeatures.classifySource(
                Dictionary.TYPE_USER_SHORTCUT, true));
        assertEquals(Source.NATIVE, BrPersonalContextFeatures.classifySource(
                Dictionary.TYPE_MAIN, true));
    }

    @Test public void togglingPersonalizationOffPreventsReadingOldPersonalCandidates() {
        assertNull(BrPersonalContextFeatures.classifySource(Dictionary.TYPE_USER_HISTORY, false));
        assertNull(BrPersonalContextFeatures.classifySource(Dictionary.TYPE_USER, false));
        assertNull(BrPersonalContextFeatures.classifySource(Dictionary.TYPE_USER_SHORTCUT, false));
        assertEquals(Source.NATIVE, BrPersonalContextFeatures.classifySource(
                Dictionary.TYPE_BRR_PERSONAL, false)); // Reviewed static seed, not learned history.
    }

    @Test public void contactsAppsAndHostCompletionsNeverBecomeModelInput() {
        assertNull(BrPersonalContextFeatures.classifySource(Dictionary.TYPE_CONTACTS, true));
        assertNull(BrPersonalContextFeatures.classifySource(Dictionary.TYPE_APPS, true));
        assertNull(BrPersonalContextFeatures.classifySource(Dictionary.TYPE_APPLICATION_DEFINED, true));
        assertNull(BrPersonalContextFeatures.classifySource(Dictionary.TYPE_USER_TYPED, true));
        assertNull(BrPersonalContextFeatures.classifySource(Dictionary.TYPE_HARDCODED, true));
        assertNull(BrPersonalContextFeatures.classifySource(Dictionary.TYPE_EMOJI, true));
        assertNull(BrPersonalContextFeatures.classifySource("external_unknown_dictionary", true));
    }

    @Test public void candidateSanitizationExcludesEditorAndPrivateShapes() {
        assertTrue(BrPersonalContextFeatures.isSafeRomanWord("meeting"));
        assertTrue(BrPersonalContextFeatures.isSafeRomanWord("kal"));
        assertTrue(BrPersonalContextFeatures.isSafeRomanWord("don't"));
        assertFalse(BrPersonalContextFeatures.isSafeRomanWord("hi@example.com"));
        assertFalse(BrPersonalContextFeatures.isSafeRomanWord("https://example.com"));
        assertFalse(BrPersonalContextFeatures.isSafeRomanWord("hello world"));
        assertFalse(BrPersonalContextFeatures.isSafeRomanWord("123456"));
        assertFalse(BrPersonalContextFeatures.isSafeRomanWord("🎉"));
        assertFalse(BrPersonalContextFeatures.isSafeRomanWord(""));
    }

    @Test public void upgradeUsesPreviouslyLearnedNativeWordAsActualModelInput() {
        final NgramContext context = new NgramContext(new NgramContext.WordInfo("kal"));
        final List<SuggestedWordInfo> nativeWords = Arrays.asList(
                info("meeting", 216, Dictionary.TYPE_USER_HISTORY),
                info("office", 180, Dictionary.TYPE_MAIN),
                info("contactName", 255, Dictionary.TYPE_CONTACTS),
                info("meeting", 120, Dictionary.TYPE_MAIN));
        final BrPersonalContextFeatures.Snapshot personalized =
                BrPersonalContextFeatures.fromNative(context, nativeWords, true, true);
        assertTrue(personalized.hasInheritedPersonalSignals);
        assertEquals(Arrays.asList("kal"), personalized.previousWords);
        assertEquals(2, personalized.candidates.size());
        final BrPersonalContextFeatures.Candidate learned = personalized.candidates.get(0);
        assertEquals("meeting", learned.word);
        assertEquals(Source.LEARNED_HISTORY, learned.source);
        assertEquals(1f, learned.numericModelInputs()[2], 0f);
        assertEquals(216f / 255f, learned.numericModelInputs()[0], 0.00001f);
        assertEquals(1f, learned.numericModelInputs()[1], 0f);

        final BrPersonalContextFeatures.Snapshot turnedOff =
                BrPersonalContextFeatures.fromNative(context, nativeWords, true, false);
        assertFalse(turnedOff.hasInheritedPersonalSignals);
        // Personal-history entries disappear, but an independently offered main-dictionary
        // "meeting" remains legal and must not be erased by an OFF setting.
        assertEquals(2, turnedOff.candidates.size());
        assertEquals("office", turnedOff.candidates.get(0).word);
        assertEquals("meeting", turnedOff.candidates.get(1).word);
        assertEquals(Source.NATIVE, turnedOff.candidates.get(1).source);

        final BrPersonalContextFeatures.Snapshot protectedEditor =
                BrPersonalContextFeatures.fromNative(context, nativeWords, false, true);
        assertEquals(0, protectedEditor.candidates.size());
        assertEquals(0, protectedEditor.previousWords.size());
    }
}
