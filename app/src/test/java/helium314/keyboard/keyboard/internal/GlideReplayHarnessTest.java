// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.internal;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import org.junit.Test;

/** Reproducible synthetic stroke boundary tests, not real glide decoder scores. */
public class GlideReplayHarnessTest {
    private static GlideReplayHarness.Fixture trace(final String id, final String expected,
            final String layout, final boolean canceled, final boolean eligible) {
        return new GlideReplayHarness.Fixture(id, "en-IN", layout, 1080, 470,
                canceled, eligible, expected, List.of(
                new GlideReplayHarness.Point(.12f, .72f, 0),
                new GlideReplayHarness.Point(.42f, .48f, 35),
                new GlideReplayHarness.Point(.81f, .52f, 85)));
    }

    @Test public void repeatedLetterAndOrdinaryStrokeUseExactlyOneDecoderBoundary() {
        final AtomicInteger calls = new AtomicInteger();
        final var report = GlideReplayHarness.replay(List.of(
                trace("typing", "typing", "portrait", false, true),
                trace("letter", "letter", "portrait", false, true),
                trace("ordinary-drag", "none", "portrait", false, false),
                trace("cancelled", "none", "portrait", true, true)), fixture -> {
                    calls.incrementAndGet();
                    return fixture.id().equals("typing")
                            ? new GlideReplayHarness.DecodeResult(List.of("typing", "taping"), 3500)
                            : new GlideReplayHarness.DecodeResult(List.of("later", "letter"), 6200);
                });
        assertEquals(2, calls.get());
        assertEquals(2, report.eligible());
        assertEquals(2, report.decoded());
        assertEquals(2, report.skipped());
        assertEquals(1, report.top1());
        assertEquals(2, report.top3());
        assertEquals(6200, report.p95Micros());
    }

    @Test public void geometryProfilesAndNullDecoderResultAreMeasuredSeparately() {
        final var portrait = trace("portrait", "typing", "portrait", false, true);
        final var landscape = new GlideReplayHarness.Fixture("landscape", "hi-Latn",
                "landscape", 2000, 320, false, true, "yaar", portrait.points());
        final var report = GlideReplayHarness.replay(List.of(portrait, landscape), f ->
                f.layout().equals("landscape") ? null
                        : new GlideReplayHarness.DecodeResult(List.of("typing"), 80));
        assertEquals(2, report.decoded());
        assertEquals(1, report.noResult());
        assertEquals(1, report.top1());
        assertEquals(1, report.top3());
        assertEquals(80, report.p95Micros());
    }

    @Test public void rejectsDuplicateIdsAndMalformedTouchTimelines() {
        final var a = trace("a", "typing", "portrait", false, true);
        assertThrows(IllegalArgumentException.class,
                () -> GlideReplayHarness.replay(List.of(a, a), f -> null));
        final var bad = new GlideReplayHarness.Fixture("bad", "en", "portrait", 900, 300,
                false, true, "typing", List.of(new GlideReplayHarness.Point(.2f, .2f, 10),
                new GlideReplayHarness.Point(.4f, .4f, 9)));
        assertThrows(IllegalArgumentException.class,
                () -> GlideReplayHarness.replay(List.of(bad), f -> null));
        final var outOfBounds = new GlideReplayHarness.Fixture("out", "en", "portrait", 900, 300,
                false, true, "typing", List.of(new GlideReplayHarness.Point(Float.NaN, .2f, 0)));
        assertThrows(IllegalArgumentException.class,
                () -> GlideReplayHarness.replay(List.of(outOfBounds), f -> null));
    }

    @Test public void rejectsDuplicateOrExcessiveCandidateResponse() {
        final var a = trace("a", "typing", "portrait", false, true);
        assertThrows(IllegalArgumentException.class,
                () -> GlideReplayHarness.replay(List.of(a), f ->
                        new GlideReplayHarness.DecodeResult(List.of("typing", "typing"), 4)));
        assertThrows(IllegalArgumentException.class,
                () -> GlideReplayHarness.replay(List.of(a), f ->
                        new GlideReplayHarness.DecodeResult(List.of("a", "b", "c", "d"), 4)));
        assertThrows(IllegalArgumentException.class,
                () -> GlideReplayHarness.replay(List.of(a), f ->
                        new GlideReplayHarness.DecodeResult(List.of("typing"), -1)));
    }

    @Test public void cancellationNeverInvokesDecoderOrAutoCommitsCandidate() {
        final AtomicInteger calls = new AtomicInteger();
        final var report = GlideReplayHarness.replay(List.of(
                trace("cancel", "typing", "portrait", true, true),
                trace("nontouch", "typing", "portrait", false, false)), fixture -> {
                    calls.incrementAndGet();
                    return new GlideReplayHarness.DecodeResult(List.of("typing"), 100);
                });
        assertEquals(0, calls.get());
        assertEquals(0, report.decoded());
        assertEquals(0, report.top1());
        assertEquals(-1, report.p95Micros());
    }
}
