// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.suggestions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertSame;

import helium314.keyboard.latin.SuggestedWords;
import helium314.keyboard.latin.SuggestedWords.SuggestedWordInfo;
import helium314.keyboard.latin.dictionary.Dictionary;
import java.util.ArrayList;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class SuggestionPrefixPreviewTest {
    private SuggestedWordInfo candidate(final String word, final int kind) {
        return new SuggestedWordInfo(word, "", 400, kind,
                Dictionary.DICTIONARY_HARDCODED, SuggestedWordInfo.NOT_AN_INDEX,
                SuggestedWordInfo.NOT_A_CONFIDENCE);
    }

    private SuggestedWords suggestions(final int style, final String... words) {
        final ArrayList<SuggestedWordInfo> infos = new ArrayList<>();
        for (final String word : words) {
            infos.add(candidate(word, SuggestedWordInfo.KIND_PREDICTION));
        }
        return new SuggestedWords(infos, null, null, false, false, false,
                style, SuggestedWords.NOT_A_SEQUENCE_NUMBER);
    }

    @Test public void firstLetterImmediatelyReusesOnlyMatchingNativePredictions() {
        final SuggestedWords previous = suggestions(SuggestedWords.INPUT_STYLE_PREDICTION,
                "tree", "there", "they", "world");
        final SuggestedWords preview = SuggestionPrefixPreview.forTypedPrefix(previous, "t", false);
        assertEquals(4, preview.size());
        assertEquals("t", preview.getWord(0));
        assertEquals("tree", preview.getWord(1));
        assertEquals("there", preview.getWord(2));
        assertEquals("they", preview.getWord(3));
        assertSame(previous.getInfo(1), preview.getInfo(2));
        assertFalse(preview.mWillAutoCorrect);
    }

    @Test public void moreTypingReplacesIncompatibleCandidatesWithoutStaleTaps() {
        final SuggestedWords previous = suggestions(SuggestedWords.INPUT_STYLE_TYPING,
                "tree", "there", "they", "world");
        final SuggestedWords preview = SuggestionPrefixPreview.forTypedPrefix(previous, "th", false);
        assertEquals(3, preview.size());
        assertEquals("there", preview.getWord(1));
        assertEquals("they", preview.getWord(2));
        assertEquals("th", preview.mTypedWordInfo.mWord);
    }

    @Test public void noMatchingPredictionsPreservesUntappableTypedWord() {
        final SuggestedWords preview = SuggestionPrefixPreview.forTypedPrefix(
                suggestions(SuggestedWords.INPUT_STYLE_PREDICTION, "dog", "cat"), "t", false);
        assertEquals(1, preview.size());
        assertEquals("t", preview.getWord(0));
        assertFalse(preview.mWillAutoCorrect);
        assertEquals(true, SuggestionStripLayoutHelper.shouldShowTypedFallback(preview));
    }

    @Test public void protectedTextAndApplicationCompletionsNeverLeakIntoPrefixPreview() {
        final SuggestedWords prior = suggestions(SuggestedWords.INPUT_STYLE_APPLICATION_SPECIFIED,
                "tokenSecret", "tokenAnother");
        assertEquals(1, SuggestionPrefixPreview.forTypedPrefix(prior, "tok", false).size());
        final SuggestedWords words = suggestions(SuggestedWords.INPUT_STYLE_TYPING, "tokenSecret");
        assertEquals(1, SuggestionPrefixPreview.forTypedPrefix(words, "tok", true).size());
    }

    @Test public void previewCannotCarryExpiredRecallOrProviderCandidates() {
        final ArrayList<SuggestedWordInfo> infos = new ArrayList<>();
        infos.add(candidate("test", SuggestedWordInfo.KIND_TYPED));
        infos.add(candidate("testing", SuggestedWordInfo.KIND_SESSION_RECALL));
        infos.add(candidate("tester", SuggestedWordInfo.KIND_APP_DEFINED));
        infos.add(candidate("tested", SuggestedWordInfo.KIND_COMPLETION));
        final SuggestedWords prior = new SuggestedWords(infos, null, infos.get(0),
                false, false, false, SuggestedWords.INPUT_STYLE_TYPING, SuggestedWords.NOT_A_SEQUENCE_NUMBER);
        final SuggestedWords result = SuggestionPrefixPreview.forTypedPrefix(prior, "test", false);
        assertEquals(2, result.size());
        assertEquals("tested", result.getWord(1));
        assertFalse(result.mWillAutoCorrect);
    }

    @Test public void filterIsCaseInsensitiveAndDoesNotRepeatTypedWord() {
        final SuggestedWords words = suggestions(SuggestedWords.INPUT_STYLE_PREDICTION,
                "There", "THERE", "the", "Theory");
        final SuggestedWords preview = SuggestionPrefixPreview.forTypedPrefix(words, "Th", false);
        assertEquals(4, preview.size());
        assertEquals("There", preview.getWord(1));
        assertEquals("the", preview.getWord(2));
        assertEquals("Theory", preview.getWord(3));
    }
}
