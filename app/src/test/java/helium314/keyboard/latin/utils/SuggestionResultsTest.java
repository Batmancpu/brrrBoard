// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.utils;

import static org.junit.Assert.*;

import helium314.keyboard.latin.SuggestedWords.SuggestedWordInfo;
import helium314.keyboard.latin.dictionary.Dictionary;
import java.util.Arrays;
import org.junit.Test;

public class SuggestionResultsTest {
    private static SuggestedWordInfo word(String value, int score, int source) {
        return new SuggestedWordInfo(value, "", score, SuggestedWordInfo.KIND_PREDICTION,
                Dictionary.DICTIONARY_HARDCODED, source, SuggestedWordInfo.NOT_A_CONFIDENCE);
    }

    @Test public void duplicateAtCapacityDoesNotEvictTheWorstCandidate() {
        SuggestionResults results = new SuggestionResults(2, false, false);
        SuggestedWordInfo first = word("alpha", 300, SuggestedWordInfo.NOT_AN_INDEX);
        SuggestedWordInfo second = word("beta", 200, SuggestedWordInfo.NOT_AN_INDEX);
        assertTrue(results.add(first));
        assertTrue(results.add(second));
        assertFalse(results.add(word("beta", 200, 7)));
        assertEquals(2, results.size());
        assertTrue(results.contains(first));
        assertTrue(results.contains(second));
    }

    @Test public void betterUniqueCandidateReplacesWorstAndReportsSuccess() {
        SuggestionResults results = new SuggestionResults(2, false, false);
        SuggestedWordInfo first = word("alpha", 300, SuggestedWordInfo.NOT_AN_INDEX);
        SuggestedWordInfo second = word("beta", 200, SuggestedWordInfo.NOT_AN_INDEX);
        SuggestedWordInfo better = word("gamma", 250, SuggestedWordInfo.NOT_AN_INDEX);
        results.add(first);
        results.add(second);
        assertTrue(results.add(better));
        assertEquals(2, results.size());
        assertTrue(results.contains(first));
        assertTrue(results.contains(better));
        assertFalse(results.contains(second));
        assertFalse(results.add(word("delta", 100, SuggestedWordInfo.NOT_AN_INDEX)));
        assertEquals(2, results.size());
    }

    @Test public void addAllUsesBoundedDuplicateSafeInsertion() {
        SuggestionResults results = new SuggestionResults(2, false, false);
        SuggestedWordInfo first = word("alpha", 300, SuggestedWordInfo.NOT_AN_INDEX);
        SuggestedWordInfo second = word("beta", 200, SuggestedWordInfo.NOT_AN_INDEX);
        assertTrue(results.addAll(Arrays.asList(first, second,
                word("beta", 200, SuggestedWordInfo.NOT_AN_INDEX))));
        assertEquals(2, results.size());
        assertTrue(results.contains(first));
        assertTrue(results.contains(second));
    }

    @Test public void zeroCapacityRejectsWithoutAccessingAnEmptyLastElement() {
        SuggestionResults results = new SuggestionResults(0, false, false);
        assertFalse(results.add(word("alpha", 300, SuggestedWordInfo.NOT_AN_INDEX)));
        assertTrue(results.isEmpty());
    }
}
