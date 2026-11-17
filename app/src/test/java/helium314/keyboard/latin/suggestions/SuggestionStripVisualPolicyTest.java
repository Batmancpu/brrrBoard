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

@RunWith(RobolectricTestRunner.class)
public class SuggestionStripVisualPolicyTest {
    @Test public void highlightedOnlyForDecoderApprovedCenterCorrection() {
        assertTrue(SuggestionStripLayoutHelper.isCenterConfidenceActive(true,
                SuggestedWords.INDEX_OF_AUTO_CORRECTION));
        assertFalse(SuggestionStripLayoutHelper.isCenterConfidenceActive(false,
                SuggestedWords.INDEX_OF_AUTO_CORRECTION));
        assertFalse(SuggestionStripLayoutHelper.isCenterConfidenceActive(true,
                SuggestedWords.INDEX_OF_TYPED_WORD));
        assertFalse(SuggestionStripLayoutHelper.isCenterConfidenceActive(true, null));
    }

    @Test public void pillIsDistinctAndLegibleInLightAndDarkThemes() {
        final int darkPill = SuggestionStripLayoutHelper.confidencePillColor(Color.WHITE);
        final int lightPill = SuggestionStripLayoutHelper.confidencePillColor(Color.BLACK);
        assertTrue(ColorUtils.calculateContrast(Color.WHITE, darkPill) >= 4.5);
        assertTrue(ColorUtils.calculateContrast(Color.BLACK, lightPill) >= 4.5);
        assertEquals(255, Color.alpha(darkPill));
        assertEquals(255, Color.alpha(lightPill));
        assertTrue(darkPill != lightPill);
    }
}
