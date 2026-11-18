// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji;

/** Pure bounds for the optional, short Emoji mode-height interpolation. */
final class EmojiPanelHeightMotion {
    static final long DURATION_MS = 180;

    private EmojiPanelHeightMotion() { }

    static boolean canAnimate(final int from, final int target, final int available,
            final boolean visible, final boolean motionEnabled) {
        // Don't animate a root/window constraint transition or initial layout.
        return visible && motionEnabled && from > 0 && target > 0
                && Math.abs(from - target) > 1
                && available >= Math.max(from, target);
    }

    static int bound(final int height, final int available) {
        return Math.max(1, Math.min(height, Math.max(1, available)));
    }
}
