// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.brain;

/**
 * Pure clustered Gaussian mean personalization, independent of Android, word
 * storage, and gesture decoding. Inspired by the published Gboard spatial
 * personalization paper, not proprietary production parameters.
 *
 * Only normalized sufficient statistics are retained. Callers must supply
 * explicit confirmed tap evidence and keep separate instances per geometry.
 * Fixed covariance is intentional for the initial conservative model.
 */
public final class AdaptiveTouchModel {
    public static final int CLUSTERS = 6;
    public static final int MIN_EVIDENCE = 32;
    private static final int MAX_EVIDENCE = 512;
    private static final double PRIOR_WEIGHT = 32.0;
    private static final double MAX_MEAN = .18;
    private static final double MAX_SAMPLE = .80;
    private static final double VARIANCE = .35 * .35;
    private final int[] count = new int[CLUSTERS];
    private final double[] sumX = new double[CLUSTERS];
    private final double[] sumY = new double[CLUSTERS];

    /** Normalized key center within the letter area's bounding rectangle. */
    public static int cluster(double x, double y) {
        if (!Double.isFinite(x) || !Double.isFinite(y)
                || x < 0 || x > 1 || y < 0 || y > 1) return -1;
        return Math.min(2, (int) (y * 3)) * 2 + (x < .5 ? 0 : 1);
    }

    /** Accept a confirmed offset in units of the intended key width and height. */
    public boolean observe(int group, double x, double y) {
        if (!valid(group) || !finiteBound(x, MAX_SAMPLE) || !finiteBound(y, MAX_SAMPLE))
            return false;
        // Bounded exponential forgetting after the initial evidence window.
        if (count[group] == MAX_EVIDENCE) {
            final double retain = (MAX_EVIDENCE - 1.0) / MAX_EVIDENCE;
            sumX[group] *= retain;
            sumY[group] *= retain;
        } else {
            count[group]++;
        }
        sumX[group] += x;
        sumY[group] += y;
        return true;
    }

    /** Merge a bounded word-sized aggregate without storing any tap sequence. */
    public boolean merge(int group, int n, double x, double y) {
        if (!valid(group) || n < 1 || n > 32
                || !finiteBound(x, n * MAX_SAMPLE + 1e-6)
                || !finiteBound(y, n * MAX_SAMPLE + 1e-6)) return false;
        if (count[group] + n > MAX_EVIDENCE) {
            double keep = (MAX_EVIDENCE - n) / (double) count[group];
            sumX[group] *= keep;
            sumY[group] *= keep;
            count[group] = MAX_EVIDENCE - n;
        }
        count[group] += n;
        sumX[group] += x;
        sumY[group] += y;
        return true;
    }

    public void writeTo(java.io.DataOutput out) throws java.io.IOException {
        for (int i = 0; i < CLUSTERS; i++) {
            out.writeInt(count[i]); out.writeDouble(sumX[i]); out.writeDouble(sumY[i]);
        }
    }

    public static AdaptiveTouchModel readFrom(java.io.DataInput in) throws java.io.IOException {
        int[] n = new int[CLUSTERS];
        double[] x = new double[CLUSTERS], y = new double[CLUSTERS];
        for (int i = 0; i < CLUSTERS; i++) {
            n[i] = in.readInt(); x[i] = in.readDouble(); y[i] = in.readDouble();
        }
        return fromAggregates(n, x, y);
    }

    public Snapshot snapshot() {
        return new Snapshot(count.clone(), sumX.clone(), sumY.clone());
    }

    public void clear() {
        java.util.Arrays.fill(count, 0);
        java.util.Arrays.fill(sumX, 0);
        java.util.Arrays.fill(sumY, 0);
    }

    private static boolean valid(int group) { return group >= 0 && group < CLUSTERS; }
    private static boolean finiteBound(double value, double bound) {
        return Double.isFinite(value) && Math.abs(value) <= bound;
    }
    private static double clamp(double value) {
        return Math.max(-MAX_MEAN, Math.min(MAX_MEAN, value));
    }

    /** Immutable, allocation-free lookup for the eventual tap hot path. */
    public static final class Snapshot {
        private final int[] count;
        private final double[] sumX;
        private final double[] sumY;
        private final double[] meanX = new double[CLUSTERS];
        private final double[] meanY = new double[CLUSTERS];

        private Snapshot(int[] count, double[] sumX, double[] sumY) {
            this.count = count;
            this.sumX = sumX;
            this.sumY = sumY;
            for (int i = 0; i < CLUSTERS; i++) {
                if (count[i] >= MIN_EVIDENCE) {
                    meanX[i] = clamp(sumX[i] / (count[i] + PRIOR_WEIGHT));
                    meanY[i] = clamp(sumY[i] / (count[i] + PRIOR_WEIGHT));
                }
            }
        }

        public boolean ready(int group) { return valid(group) && count[group] >= MIN_EVIDENCE; }
        public int evidence(int group) { return valid(group) ? count[group] : 0; }
        public double offsetX(int group) { return valid(group) ? meanX[group] : 0; }
        public double offsetY(int group) { return valid(group) ? meanY[group] : 0; }

        /** Negative log likelihood up to the constant shared by all keys. */
        public double cost(int group, double rx, double ry) {
            if (!valid(group) || !Double.isFinite(rx) || !Double.isFinite(ry))
                return Double.POSITIVE_INFINITY;
            final double dx = rx - meanX[group];
            final double dy = ry - meanY[group];
            return (dx * dx + dy * dy) / (2 * VARIANCE);
        }

        /**
         * Conservative adjacent-key preference. A confident literal center,
         * untrained cluster, distant key or near tie remains unchanged.
         * Caller still verifies actual neighboring letter geometry and origin.
         */
        public boolean preferNeighbor(int literalGroup, double literalX, double literalY,
                                      int neighborGroup, double neighborX, double neighborY) {
            if (!ready(literalGroup) || !ready(neighborGroup)
                    || !finiteBound(literalX, .8) || !finiteBound(literalY, .8)
                    || !finiteBound(neighborX, .68) || !finiteBound(neighborY, .68)
                    || (Math.abs(literalX) <= .30 && Math.abs(literalY) <= .30))
                return false;
            final double adapted = cost(literalGroup, literalX, literalY)
                    - cost(neighborGroup, neighborX, neighborY);
            // Require an actual personalization benefit, not just a generic
            // reimplementation of the existing geometric nearest-key choice.
            final double baseline = (literalX * literalX + literalY * literalY
                    - neighborX * neighborX - neighborY * neighborY) / (2 * VARIANCE);
            return adapted >= .15 && adapted - baseline >= .10;
        }
    }

    /** Decode only validated aggregates; never accept malformed or unbounded records. */
    public static AdaptiveTouchModel fromAggregates(int[] counts, double[] xs, double[] ys) {
        if (counts == null || xs == null || ys == null || counts.length != CLUSTERS
                || xs.length != CLUSTERS || ys.length != CLUSTERS)
            throw new IllegalArgumentException("Invalid touch aggregate dimensions");
        final AdaptiveTouchModel model = new AdaptiveTouchModel();
        for (int i = 0; i < CLUSTERS; i++) {
            final int n = counts[i];
            if (n < 0 || n > MAX_EVIDENCE || !finiteBound(xs[i], n * MAX_SAMPLE + 1e-6)
                    || !finiteBound(ys[i], n * MAX_SAMPLE + 1e-6)
                    || (n == 0 && (xs[i] != 0 || ys[i] != 0)))
                throw new IllegalArgumentException("Invalid touch aggregate");
            model.count[i] = n;
            model.sumX[i] = xs[i];
            model.sumY[i] = ys[i];
        }
        return model;
    }
}
