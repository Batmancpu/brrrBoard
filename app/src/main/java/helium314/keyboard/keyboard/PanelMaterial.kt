// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard

import android.content.Context
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.view.View
import androidx.core.graphics.ColorUtils
import helium314.keyboard.latin.common.Colors
import helium314.keyboard.latin.common.ColorType

/**
 * One in-IME surface language for Clipboard, Toolbar and Emoji.
 * Top: progressive occlusion beneath fixed controls. Bottom: straight-edged
 * translucent material beneath crisp controls. Does not blur/capture the host
 * application or copy clipboard content into off-screen bitmaps.
 */
object PanelMaterial {
    const val HEADER_HEIGHT_DP = 46
    const val HEADER_FADE_DP = 72
    const val EMOJI_FADE_DP = 28
    const val BOTTOM_GLASS_DP = 56
    const val CONTENT_EDGE_GAP_DP = 10
    const val CORNER_RADIUS_DP = 13
    const val TRANSITION_MS = 160L

    private fun alpha(rgb: Int, opacity: Int): Int =
        (rgb and 0x00FFFFFF) or (opacity.coerceIn(0, 255) shl 24)

    fun headerStops(rgb: Int): IntArray = intArrayOf(
        alpha(rgb, 242), alpha(rgb, 225), alpha(rgb, 184),
        alpha(rgb, 110), alpha(rgb, 0),
    )

    // No transparent top ramp: its divider is straight. The subdued content
    // underneath remains visible; this is a simulated material, not live blur.
    fun bottomStops(rgb: Int): IntArray = intArrayOf(
        alpha(rgb, 205), alpha(rgb, 213), alpha(rgb, 222), alpha(rgb, 228),
    )

    private fun baseColor(context: Context, colors: Colors): Int {
        val base = colors.get(ColorType.MAIN_BACKGROUND)
        return if (Color.alpha(base) < 24) {
            if (KeyboardTheme.isDarkThemeActive(context)) 0xFF24252B.toInt()
            else 0xFFF0F1F4.toInt()
        } else ColorUtils.setAlphaComponent(base, 255)
    }

    @JvmStatic
    fun apply(context: Context, colors: Colors, header: View?, bottom: View?) {
        val base = baseColor(context, colors)
        header?.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM, headerStops(base))
        bottom?.background = GradientDrawable(
            GradientDrawable.Orientation.TOP_BOTTOM, bottomStops(base))
        header?.isClickable = false
        header?.isFocusable = false
        bottom?.isClickable = false
        bottom?.isFocusable = false
    }
}
