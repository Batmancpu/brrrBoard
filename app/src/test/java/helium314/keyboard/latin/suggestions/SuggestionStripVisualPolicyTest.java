// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.suggestions;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.assertEquals;

import android.graphics.Color;

import androidx.core.graphics.ColorUtils;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

import helium314.keyboard.latin.SuggestedWords;
import helium314.keyboard.latin.SuggestedWords.SuggestedWordInfo;
import helium314.keyboard.latin.dictionary.Dictionary;
import java.util.ArrayList;

@RunWith(RobolectricTestRunner.class)
public class SuggestionStripVisualPolicyTest {
    private static SuggestedWords oneTypedWord(final String value) {
        final SuggestedWordInfo typed = new SuggestedWordInfo(value, "", SuggestedWordInfo.MAX_SCORE,
                SuggestedWordInfo.KIND_TYPED, Dictionary.DICTIONARY_USER_TYPED,
                SuggestedWordInfo.NOT_AN_INDEX, SuggestedWordInfo.NOT_A_CONFIDENCE);
        final ArrayList<SuggestedWordInfo> infos = new ArrayList<>();
        infos.add(typed);
        return new SuggestedWords(infos, null, typed, false, false, false,
                SuggestedWords.INPUT_STYLE_TYPING, SuggestedWords.NOT_A_SEQUENCE_NUMBER);
    }

    @Test public void initialTypedPrefixDoesNotDisappearWhenNoDecoderCandidatesExist() {
        assertTrue(SuggestionStripLayoutHelper.shouldShowTypedFallback(oneTypedWord("t")));
        assertTrue(SuggestionStripLayoutHelper.shouldShowTypedFallback(oneTypedWord("th")));
        assertFalse(SuggestionStripLayoutHelper.shouldShowTypedFallback(oneTypedWord("")));
        assertFalse(SuggestionStripLayoutHelper.shouldShowTypedFallback(SuggestedWords.getEmptyInstance()));
        assertEquals(1, SuggestionStripLayoutHelper.getPositionInSuggestionStrip(
                0, false, false, 1, 0));
    }

    @Test public void highlightedOnlyForDecoderApprovedCenterCorrection() {
        assertTrue(SuggestionStripLayoutHelper.isCenterConfidenceActive(true,
                SuggestedWords.INDEX_OF_AUTO_CORRECTION));
        assertFalse(SuggestionStripLayoutHelper.isCenterConfidenceActive(false,
                SuggestedWords.INDEX_OF_AUTO_CORRECTION));
        assertFalse(SuggestionStripLayoutHelper.isCenterConfidenceActive(true,
                SuggestedWords.INDEX_OF_TYPED_WORD));
        assertFalse(SuggestionStripLayoutHelper.isCenterConfidenceActive(true, null));
    }

    @Test public void liveGestureChromeIsPresentationOnlyAndRequiresVisibleText() {
        assertTrue(SuggestionStripLayoutHelper.shouldShowLiveGestureChrome(true, true, true));
        assertFalse(SuggestionStripLayoutHelper.shouldShowLiveGestureChrome(false, true, true));
        assertFalse(SuggestionStripLayoutHelper.shouldShowLiveGestureChrome(true, false, true));
        assertFalse(SuggestionStripLayoutHelper.shouldShowLiveGestureChrome(true, true, false));
        assertFalse(SuggestionStripLayoutHelper.isCenterConfidenceActive(false,
                SuggestedWords.INDEX_OF_AUTO_CORRECTION));
    }

    @Test public void quotedOriginalOnlyForActualIosTypingCorrection() {
        assertTrue(SuggestionStripLayoutHelper.shouldQuoteOriginal(true, true,
                SuggestedWords.INPUT_STYLE_TYPING, "mot", "not"));
        assertFalse(SuggestionStripLayoutHelper.shouldQuoteOriginal(false, true,
                SuggestedWords.INPUT_STYLE_TYPING, "mot", "not"));
        assertFalse(SuggestionStripLayoutHelper.shouldQuoteOriginal(true, false,
                SuggestedWords.INPUT_STYLE_TYPING, "mot", "not"));
        assertFalse(SuggestionStripLayoutHelper.shouldQuoteOriginal(true, true,
                SuggestedWords.INPUT_STYLE_UPDATE_BATCH, "mot", "not"));
        assertFalse(SuggestionStripLayoutHelper.shouldQuoteOriginal(true, true,
                SuggestedWords.INPUT_STYLE_TYPING, "not", "not"));
        assertFalse(SuggestionStripLayoutHelper.shouldQuoteOriginal(true, true,
                SuggestedWords.INPUT_STYLE_TYPING, "", "not"));
    }

    @Test public void pillIsDistinctAndLegibleInLightAndDarkThemes() {
        final int darkPill = SuggestionStripLayoutHelper.confidencePillColor(Color.WHITE);
        final int lightPill = SuggestionStripLayoutHelper.confidencePillColor(Color.BLACK);
        assertTrue(ColorUtils.calculateContrast(Color.WHITE, darkPill) >= 4.5);
        assertTrue(ColorUtils.calculateContrast(Color.BLACK, lightPill) >= 4.5);
        assertEquals(255, Color.alpha(darkPill));
        assertEquals(255, Color.alpha(lightPill));
        assertTrue(darkPill != lightPill);
        assertEquals(Color.red(darkPill), Color.green(darkPill));
        assertEquals(Color.green(darkPill), Color.blue(darkPill));
        assertEquals(Color.red(lightPill), Color.green(lightPill));
        assertEquals(Color.green(lightPill), Color.blue(lightPill));
    }

    @Test public void themeTintIsOptInAndRetainsReadableAccentHue() {
        final int lavender = Color.rgb(218, 159, 237);
        final int neutral = SuggestionStripLayoutHelper.confidencePillColor(Color.WHITE, false, lavender);
        final int tinted = SuggestionStripLayoutHelper.confidencePillColor(Color.WHITE, true, lavender);
        assertEquals(SuggestionStripLayoutHelper.confidencePillColor(Color.WHITE), neutral);
        assertTrue("tint must be distinguishable from gray", Color.red(tinted) != Color.green(tinted));
        assertTrue("contrast must remain accessible", ColorUtils.calculateContrast(Color.WHITE, tinted) >= 4.5);
        assertTrue(ColorUtils.calculateContrast(Color.BLACK,
                SuggestionStripLayoutHelper.confidencePillColor(Color.BLACK, true, lavender)) >= 4.5);
    }
}
