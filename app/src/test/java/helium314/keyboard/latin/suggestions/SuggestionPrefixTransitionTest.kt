// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.suggestions

import org.junit.Assert.assertEquals
import org.junit.Test

class SuggestionPrefixTransitionTest {
    @Test fun quickTypeReplacementKeepsTypedPrefixAsAnchor() {
        assertEquals(1, SuggestionPrefixTransition.stablePrefixEnd("tree", "they"))
    }

    @Test fun fullySharedPrefixKeepsExistingWordStationary() {
        assertEquals(3, SuggestionPrefixTransition.stablePrefixEnd("play", "player"))
    }

    @Test fun unrelatedWordsUseWholeWordFallback() {
        assertEquals(0, SuggestionPrefixTransition.stablePrefixEnd("cat", "sun"))
    }

    @Test fun shapingSensitiveLabelsUseNativeFallback() {
        assertEquals(0, SuggestionPrefixTransition.stablePrefixEnd("café", "cafés"))
        assertEquals(0, SuggestionPrefixTransition.stablePrefixEnd("नमस्ते", "नमस्ते!"))
    }
}
