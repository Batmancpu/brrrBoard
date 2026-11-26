// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.brain;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class BrHybridRankingPolicyTest {
    @Test public void strongNeuralEvidenceMayBreakOneNativeNearTie() {
        assertEquals(0, BrHybridRankingPolicy.adjacentSwap(
                new int[]{1000, 980, 970}, new int[]{0, 1, 2}, new float[]{0, 1, 2}));
    }

    @Test public void decisiveNativeRankingAndPinnedSlotsArePreserved() {
        assertEquals(-1, BrHybridRankingPolicy.adjacentSwap(
                new int[]{1000, 700}, new int[]{0, 1}, new float[]{0, 3}));
        assertEquals(-1, BrHybridRankingPolicy.adjacentSwap(
                new int[]{1000, 990}, new int[]{0, 2}, new float[]{0, 3}));
        assertEquals(-1, BrHybridRankingPolicy.adjacentSwap(
                new int[]{1000, 990}, new int[]{0, 1}, new float[]{0, .01f}));
    }

    @Test public void invalidAndSentinelScoresNeverRerank() {
        assertEquals(-1, BrHybridRankingPolicy.adjacentSwap(
                new int[]{1000, 990}, new int[]{0, 1}, new float[]{0, Float.NaN}));
        assertEquals(-1, BrHybridRankingPolicy.adjacentSwap(
                new int[]{Integer.MAX_VALUE, 990}, new int[]{0, 1}, new float[]{0, 3}));
        assertEquals(-1, BrHybridRankingPolicy.adjacentSwap(
                new int[]{1000, 990}, new int[]{0, 0}, new float[]{0, 3}));
    }

    @Test public void inferenceIsEligibleOnlyForVisibleNearTies() {
        assertEquals(true, BrHybridRankingPolicy.hasPotentialAdjacentSwap(
                new int[]{1000, 980, 700}, new int[]{0, 1, 2}));
        assertEquals(false, BrHybridRankingPolicy.hasPotentialAdjacentSwap(
                new int[]{1000, 700}, new int[]{0, 1}));
        assertEquals(false, BrHybridRankingPolicy.hasPotentialAdjacentSwap(
                new int[]{1000, 990}, new int[]{0, 2}));
        assertEquals(false, BrHybridRankingPolicy.hasPotentialAdjacentSwap(
                new int[]{Integer.MAX_VALUE, 990}, new int[]{0, 1}));
    }

    @Test public void legalSwapPolicyAlwaysPassesCheapEligibilityGate() {
        final int[][] scores = {{1000, 980, 970}, {1000, 700}, {900, 890, 700}};
        final int[][] slots = {{0, 1, 2}, {0, 1}, {0, 1, 2}};
        final float[][] neural = {{0, 1, 2}, {0, 3}, {0, 2, 1}};
        for (int i = 0; i < scores.length; i++) {
            if (BrHybridRankingPolicy.adjacentSwap(scores[i], slots[i], neural[i]) >= 0) {
                assertEquals(true, BrHybridRankingPolicy.hasPotentialAdjacentSwap(scores[i], slots[i]));
            }
        }
    }
}
