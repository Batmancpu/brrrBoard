// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.internal;

import android.graphics.Color;

/** iOS-profile-only appearance parameters. Stroke sampling and recognition are unchanged. */
final class GestureTrailVisualPolicy {
    private GestureTrailVisualPolicy() { }

    static float startWidth(final boolean iosMotion, final float themedWidth,
            final float density) {
        // The reference trail's leading tip is broad and rounded: the older
        // portion narrows continuously into a hairline.
        return iosMotion ? 20f * density : themedWidth;
    }

    static float endWidth(final boolean iosMotion, final float themedWidth,
            final float density) {
        return iosMotion ? 1.6f * density : themedWidth;
    }

    static int color(final boolean iosMotion, final int themeColor,
            final int themeKeyTextColor) {
        if (!iosMotion) return themeColor;
        // Follow the theme's ink polarity: warm-gray on dark keys; translucent
        // dark-gray on light keys. Never use a hard-coded blue swipe ribbon.
        return Color.argb(104, Color.red(themeKeyTextColor),
                Color.green(themeKeyTextColor), Color.blue(themeKeyTextColor));
    }

    static int updateInterval(final boolean iosMotion, final int configuredInterval) {
        return iosMotion ? 16 : configuredInterval;
    }
}
