// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.brain;

import org.junit.Test;
import static org.junit.Assert.*;

public class AdaptiveTouchModelTest {
    @Test public void learnsBoundedClusterMeansWithoutSharingMutableState() {
        final var model = new AdaptiveTouchModel();
        for (int i = 0; i < 31; i++) assertTrue(model.observe(0, .25, -.08));
        final var immature = model.snapshot();
        assertFalse(immature.ready(0));
        assertEquals(0, immature.offsetX(0), 0);
        for (int i = 31; i < 400; i++) model.observe(0, .25, -.08);
        final var ready = model.snapshot();
        assertTrue(ready.ready(0));
        assertEquals(.18, ready.offsetX(0), 1e-9);
        assertTrue(ready.offsetY(0) < 0);
        assertFalse(ready.ready(1));
        assertFalse(immature.ready(0));
        model.clear();
        assertFalse(model.snapshot().ready(0));
        assertTrue(ready.ready(0));
    }

    @Test public void syntheticShiftedBoundaryCanRecoverWithoutChangingCenteredTaps() {
        final var model = new AdaptiveTouchModel();
        for (int i = 0; i < 200; i++) model.observe(0, .22, 0);
        final var snap = model.snapshot();
        // Actual key to the left: finger lands .58 widths right of its center,
        // just inside the neighboring literal key's left edge.
        assertTrue(snap.preferNeighbor(0, -.42, 0, 0, .58, 0));
        assertFalse(snap.preferNeighbor(0, .05, .02, 0, -.95, .02));
        assertFalse(snap.preferNeighbor(0, -.42, 0, 1, .58, 0));
        assertFalse(snap.preferNeighbor(0, -.42, 0, 0, .9, 0));
        assertFalse(new AdaptiveTouchModel().snapshot()
                .preferNeighbor(0, -.42, 0, 0, .58, 0));
    }

    @Test public void boundedDecayAndInvalidSamples() {
        final var model = new AdaptiveTouchModel();
        assertFalse(model.observe(-1, 0, 0));
        assertFalse(model.observe(1, Double.NaN, 0));
        assertFalse(model.observe(1, 1.2, 0));
        for (int i = 0; i < 5000; i++) model.observe(0, .16, 0);
        assertEquals(512, model.snapshot().evidence(0));
        for (int i = 0; i < 5000; i++) model.observe(0, -.16, 0);
        assertTrue(model.snapshot().offsetX(0) < -.1);
        assertEquals(0, AdaptiveTouchModel.cluster(0, 0));
        assertEquals(5, AdaptiveTouchModel.cluster(1, 1));
        assertEquals(-1, AdaptiveTouchModel.cluster(Double.NaN, 0));
    }

    @Test public void rejectsInvalidAggregate() {
        final int[] counts = new int[6];
        final double[] xs = new double[6], ys = new double[6];
        counts[0] = 32;
        xs[0] = 6.4;
        assertEquals(.1, AdaptiveTouchModel.fromAggregates(counts, xs, ys)
                .snapshot().offsetX(0), 1e-9);
        xs[0] = Double.NaN;
        try {
            AdaptiveTouchModel.fromAggregates(counts, xs, ys);
            fail("Malformed statistics must fail closed");
        } catch (IllegalArgumentException expected) { }
    }

    @Test public void perKeyHistogramRefinesRegionalPriorOnlyAfterMinimumEvidence() {
        final var model = new AdaptiveTouchModel();
        for (int i = 0; i < 128; i++) assertTrue(model.observe(0, 0, 0));
        final var evidence = new AdaptiveTouchEvidence();
        for (int i = 0; i < 64; i++) assertTrue(evidence.observe(4, 0, .48, 0, false));

        final var snapshot = model.snapshot(evidence);
        assertEquals(64, snapshot.keyEvidence(4));
        assertEquals(0, snapshot.offsetX(0), 1e-9);
        assertTrue("key shift is shrunk and bounded", snapshot.keyOffsetX(0, 4) > 0
                && snapshot.keyOffsetX(0, 4) <= .18);
        assertTrue("key evidence improves fit to its held-out shifted distribution",
                snapshot.cost(0, 4, .48, 0) < snapshot.cost(0, .48, 0));

        final var sparse = new AdaptiveTouchEvidence();
        for (int i = 0; i < 20; i++) assertTrue(sparse.observe(4, 0, .48, 0, false));
        final var sparseSnapshot = model.snapshot(sparse);
        assertEquals(20, sparseSnapshot.keyEvidence(4));
        assertEquals(sparseSnapshot.offsetX(0), sparseSnapshot.keyOffsetX(0, 4), 0);
        assertEquals(sparseSnapshot.offsetY(0), sparseSnapshot.keyOffsetY(0, 4), 0);
    }

    @Test public void perKeyRefinementPreservesCenteredLiteralGuard() {
        final var model = new AdaptiveTouchModel();
        for (int i = 0; i < 128; i++) model.observe(0, 0, 0);
        final var evidence = new AdaptiveTouchEvidence();
        for (int i = 0; i < 64; i++) evidence.observe(4, 0, -.48, 0, false);
        final var snapshot = model.snapshot(evidence);
        // Held-out taps for key 4 are shifted left into the adjacent key-3 box.
        assertTrue(snapshot.preferNeighbor(0, 3, .55, 0, 0, 4, -.45, 0));
        // A precise literal tap remains literal even when its neighbor is trained.
        assertFalse(snapshot.preferNeighbor(0, 3, .05, .02, 0, 4, -.95, .02));
        // A different cluster cannot accidentally consume key 4's statistics.
        assertEquals(snapshot.offsetX(1), snapshot.keyOffsetX(1, 4), 0);
    }
}
