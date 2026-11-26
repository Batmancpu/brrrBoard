// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.brain;

/**
 * Conservative native-anchored experiment; not an accuracy-approved production ranker.
 * At most one adjacent near-tied native pair may switch, never across a pinned slot.
 * Thresholds are initial experimental guardrails, not trained or device-validated calibration.
 */
final class BrHybridRankingPolicy {
    private BrHybridRankingPolicy() { }

    /** True only when native scores and visible slots permit any policy-approved swap. */
    static boolean hasPotentialAdjacentSwap(int[] nativeScores, int[] slots) {
        if (nativeScores == null || slots == null) return false;
        final int n = nativeScores.length;
        if (n < 2 || n > BrPersonalContextFeatures.MAX_CANDIDATES || slots.length != n) return false;
        for (int i = 0; i < n; i++) {
            if (nativeScores[i] <= 0 || nativeScores[i] == Integer.MAX_VALUE
                    || slots[i] < 0 || (i > 0 && slots[i] <= slots[i - 1])) return false;
        }
        for (int i = 0; i < n - 1; i++) {
            if (slots[i + 1] != slots[i] + 1) continue;
            final long gap = Math.abs((long) nativeScores[i] - nativeScores[i + 1]);
            if (gap <= Math.max(1.0, Math.max(nativeScores[i], nativeScores[i + 1]) * .05))
                return true;
        }
        return false;
    }

    static int adjacentSwap(int[] nativeScores, int[] slots, float[] neuralScores) {
        if (nativeScores == null || slots == null || neuralScores == null
                || neuralScores.length != nativeScores.length
                || !hasPotentialAdjacentSwap(nativeScores, slots)) return -1;
        float lo = Float.POSITIVE_INFINITY, hi = Float.NEGATIVE_INFINITY;
        for (float score : neuralScores) {
            if (!Float.isFinite(score)) return -1;
            lo = Math.min(lo, score);
            hi = Math.max(hi, score);
        }
        final float margin = Math.max(.05f, (hi - lo) * .25f);
        if (!Float.isFinite(margin)) return -1;
        for (int i = 0; i < nativeScores.length - 1; i++) {
            if (slots[i + 1] != slots[i] + 1) continue;
            final long gap = Math.abs((long) nativeScores[i] - nativeScores[i + 1]);
            if (gap > Math.max(1.0, Math.max(nativeScores[i], nativeScores[i + 1]) * .05))
                continue;
            if (neuralScores[i + 1] - neuralScores[i] > margin) return i;
        }
        return -1;
    }
}
