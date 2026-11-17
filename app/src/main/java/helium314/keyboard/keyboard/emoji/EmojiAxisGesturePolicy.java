// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji;

/** Deliberate horizontal category swipes must not steal vertical scrolling. */
final class EmojiAxisGesturePolicy {
    private EmojiAxisGesturePolicy() {}

    static boolean shouldCapture(final float dx, final float dy, final int touchSlop) {
        return Math.abs(dx) >= Math.max(1, touchSlop) * 2f
                && Math.abs(dx) > Math.abs(dy) * 1.55f;
    }

    static boolean shouldSwitch(final float dx, final float dy,
            final float velocityX, final float velocityY,
            final int width, final int touchSlop) {
        if (!shouldCapture(dx, dy, touchSlop)) return false;
        final boolean distanceEnough = Math.abs(dx) >= Math.max(48, width * 0.22f);
        final boolean deliberateFling = Math.abs(dx) >= Math.max(1, touchSlop) * 4f
                && Math.abs(velocityX) >= 750f
                && Math.abs(velocityX) > Math.abs(velocityY) * 1.55f;
        return distanceEnough || deliberateFling;
    }
}
