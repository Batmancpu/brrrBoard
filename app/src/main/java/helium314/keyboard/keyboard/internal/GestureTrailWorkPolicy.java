// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.internal;

final class GestureTrailWorkPolicy {
    private GestureTrailWorkPolicy() {}
    static boolean shouldDraw(boolean pending, boolean dirty, int width, int height) {
        return width > 0 && height > 0 && (pending || dirty);
    }
}
