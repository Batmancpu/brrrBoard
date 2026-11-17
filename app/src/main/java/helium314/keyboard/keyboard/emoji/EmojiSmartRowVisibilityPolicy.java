// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji;

/** Defer new in-grid rows during a gesture, but never leave stale space. */
final class EmojiSmartRowVisibilityPolicy {
    private EmojiSmartRowVisibilityPolicy() { }

    static boolean deferAppearance(final boolean scrolling, final boolean requestedVisible,
            final boolean currentlyVisible) {
        return scrolling && requestedVisible && !currentlyVisible;
    }
}
