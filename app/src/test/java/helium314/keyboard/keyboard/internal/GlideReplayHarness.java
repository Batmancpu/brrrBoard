// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.internal;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * B17A: offline, test-only normalized touch replay boundary.
 *
 * This never binds to an editor, reads typed text, or modifies JNI/gesture
 * settings. The decoder is injected so the real imported library and an
 * independently licensed replacement can later be compared on *identical*
 * synthetic geometry/points, without an implicit switchover on a live stroke.
 */
final class GlideReplayHarness {
    private GlideReplayHarness() {}

    record Point(float x, float y, long elapsedMs) {}

    record Fixture(String id, String locale, String layout, int width, int height,
                   boolean canceled, boolean gestureEligible, String expected,
                   List<Point> points) {}

    record DecodeResult(List<String> candidates, long elapsedMicros) {}

    interface Decoder {
        DecodeResult decode(Fixture fixture);
    }

    record Report(int eligible, int decoded, int skipped, int top1, int top3,
                  int noResult, long p95Micros) {}

    static Report replay(final List<Fixture> fixtures, final Decoder decoder) {
        if (fixtures == null || fixtures.isEmpty() || decoder == null) {
            throw new IllegalArgumentException("Need synthetic fixtures and a decoder");
        }
        final Set<String> ids = new HashSet<>();
        final List<Long> elapsed = new ArrayList<>();
        int eligible = 0;
        int decoded = 0;
        int skipped = 0;
        int top1 = 0;
        int top3 = 0;
        int noResult = 0;
        for (final Fixture fixture : fixtures) {
            validate(fixture, ids);
            // The actual pointer lifecycle must decide gesture eligibility.
            // A canceled or ordinary drag may never reach the decoder at all.
            if (fixture.canceled() || !fixture.gestureEligible()) {
                skipped++;
                continue;
            }
            eligible++;
            final DecodeResult result = decoder.decode(fixture);
            decoded++;
            if (result == null) {
                noResult++;
                continue;
            }
            if (result.elapsedMicros() < 0 || result.candidates() == null
                    || result.candidates().size() > 3) {
                throw new IllegalArgumentException("Decoder returned invalid result");
            }
            final Set<String> distinct = new HashSet<>();
            for (final String candidate : result.candidates()) {
                if (candidate == null || candidate.isBlank() || candidate.length() > 48
                        || !distinct.add(candidate)) {
                    throw new IllegalArgumentException("Invalid/duplicate decoder candidate");
                }
            }
            elapsed.add(result.elapsedMicros());
            if (result.candidates().isEmpty()) noResult++;
            if (!result.candidates().isEmpty()
                    && fixture.expected().equals(result.candidates().get(0))) top1++;
            if (result.candidates().contains(fixture.expected())) top3++;
        }
        elapsed.sort(Comparator.naturalOrder());
        final long p95 = elapsed.isEmpty() ? -1
                : elapsed.get((int) Math.ceil(elapsed.size() * .95) - 1);
        return new Report(eligible, decoded, skipped, top1, top3, noResult, p95);
    }

    private static void validate(final Fixture fixture, final Set<String> ids) {
        if (fixture == null || fixture.id() == null || fixture.id().isBlank()
                || !ids.add(fixture.id()) || fixture.locale() == null || fixture.locale().isBlank()
                || fixture.layout() == null || fixture.layout().isBlank()
                || fixture.width() <= 0 || fixture.height() <= 0
                || fixture.expected() == null || fixture.expected().isBlank()
                || fixture.expected().length() > 48 || fixture.points() == null
                || fixture.points().isEmpty() || fixture.points().size() > 256) {
            throw new IllegalArgumentException("Invalid/duplicate synthetic stroke fixture");
        }
        long previousTime = -1;
        for (final Point point : fixture.points()) {
            if (point == null || !Float.isFinite(point.x()) || !Float.isFinite(point.y())
                    || point.x() < -.25f || point.x() > 1.25f
                    || point.y() < -.25f || point.y() > 1.25f
                    || point.elapsedMs() <= previousTime) {
                throw new IllegalArgumentException("Invalid stroke coordinates/timeline");
            }
            previousTime = point.elapsedMs();
        }
    }
}
