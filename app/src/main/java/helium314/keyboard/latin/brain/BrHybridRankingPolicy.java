// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.brain;

/**
 * Conservative native-anchored experiment; not an accuracy-approved production ranker.
 * At most one adjacent near-tied native pair may switch, never across a pinned slot.
 * Thresholds are initial experimental guardrails, not trained or device-validated calibration.
 */
final class BrHybridRankingPolicy {
    private BrHybridRankingPolicy() { }

    static int adjacentSwap(int[] nativeScores, int[] slots, float[] neuralScores) {
        final int n = nativeScores.length;
        if (n < 2 || n > BrPersonalContextFeatures.MAX_CANDIDATES
                || slots.length != n || neuralScores.length != n) return -1;
        float lo = Float.POSITIVE_INFINITY, hi = Float.NEGATIVE_INFINITY;
        for (int i = 0; i < n; i++) {
            if (!Float.isFinite(neuralScores[i]) || nativeScores[i] <= 0
                    || nativeScores[i] == Integer.MAX_VALUE
                    || slots[i] < 0 || (i > 0 && slots[i] <= slots[i - 1])) return -1;
            lo = Math.min(lo, neuralScores[i]);
            hi = Math.max(hi, neuralScores[i]);
        }
        final float margin = Math.max(.05f, (hi - lo) * .25f);
        if (!Float.isFinite(margin)) return -1;
        for (int i = 0; i < n - 1; i++) {
            if (slots[i + 1] != slots[i] + 1) continue;
            final long gap = Math.abs((long) nativeScores[i] - nativeScores[i + 1]);
            if (gap > Math.max(1.0, Math.max(nativeScores[i], nativeScores[i + 1]) * .05))
                continue;
            if (neuralScores[i + 1] - neuralScores[i] > margin) return i;
        }
        return -1;
    }
}
