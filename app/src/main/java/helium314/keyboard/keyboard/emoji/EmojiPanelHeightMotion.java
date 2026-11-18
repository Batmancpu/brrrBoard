// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji;

/** Pure bounds for the optional, short Emoji mode-height interpolation. */
public final class EmojiPanelHeightMotion {
    public static final long DURATION_MS = 260;

    /** Critically damped spring progress, normalized to settle exactly at one. */
    public static float springProgress(final float fraction) {
        if (fraction <= 0f) return 0f;
        if (fraction >= 1f) return 1f;
        final double spring = 7.0;
        final double end = 1.0 - (1.0 + spring) * Math.exp(-spring);
        final double elapsed = spring * fraction;
        return (float) ((1.0 - (1.0 + elapsed) * Math.exp(-elapsed)) / end);
    }

    private EmojiPanelHeightMotion() { }

    public static boolean canAnimate(final int from, final int target, final int available,
            final boolean visible, final boolean motionEnabled) {
        // Don't animate a root/window constraint transition or initial layout.
        return visible && motionEnabled && from > 0 && target > 0
                && Math.abs(from - target) > 1
                && available >= Math.max(from, target);
    }

    public static int bound(final int height, final int available) {
        return Math.max(1, Math.min(height, Math.max(1, available)));
    }
}
