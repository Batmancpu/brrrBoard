// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard;

/**
 * Allocation-free, two-axis analogue-to-caret step conversion for the iOS
 * motion preset. Keep fractions across callbacks so small movements are never
 * rounded away. The editor still controls its actual caret via InputConnection.
 */
final class TouchpadMotionFilter {
    private static final int MAX_STEPS_PER_EVENT = 3;
    private float mResidualX;
    private float mResidualY;
    private float mSmoothedSpeedPxPerSecond;
    private long mLastMotionTime;
    private int mHorizontalSteps;
    private int mVerticalSteps;

    void reset() {
        mResidualX = 0f;
        mResidualY = 0f;
        mSmoothedSpeedPxPerSecond = 0f;
        mLastMotionTime = 0L;
        mHorizontalSteps = 0;
        mVerticalSteps = 0;
    }

    void advance(final int dx, final int dy, final long nowMs, final int sensitivity) {
        mHorizontalSteps = 0;
        mVerticalSteps = 0;
        if (dx == 0 && dy == 0) return;

        final long gap = mLastMotionTime == 0L ? 16L : Math.max(1L, nowMs - mLastMotionTime);
        mLastMotionTime = nowMs;
        if (gap > 80L) mSmoothedSpeedPxPerSecond = 0f;
        final float velocity = (float) Math.hypot(dx, dy) * 1000f / Math.max(8f, gap);
        mSmoothedSpeedPxPerSecond = mSmoothedSpeedPxPerSecond * .55f + velocity * .45f;

        // Accelerate only when the finger really moves quickly. No fling on UP.
        final float acceleration = Math.min(.55f,
                Math.max(0f, (mSmoothedSpeedPxPerSecond - 260f) / 1350f));
        final float gain = 1f + acceleration;
        final int tuned = Math.max(0, Math.min(100, sensitivity));
        final float thresholdX = 26f - (tuned - 50) * .19f; // 35.5..16.5 px/character
        final float thresholdY = thresholdX * 1.40f;       // gentler line jumps

        // Never discard the other axis as the finger turns a corner/diagonal.
        mResidualX += dx * gain;
        mResidualY += dy * gain;
        mHorizontalSteps = clamp((int) (mResidualX / thresholdX));
        mVerticalSteps = clamp((int) (mResidualY / thresholdY));
        mResidualX -= mHorizontalSteps * thresholdX;
        mResidualY -= mVerticalSteps * thresholdY;
    }

    private static int clamp(final int steps) {
        return Math.max(-MAX_STEPS_PER_EVENT, Math.min(MAX_STEPS_PER_EVENT, steps));
    }

    int horizontalSteps() { return mHorizontalSteps; }
    int verticalSteps() { return mVerticalSteps; }
}
