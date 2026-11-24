// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard

/** Apple-style contextual return action only; modifier and normal keys remain neutral. */
object FrostAccentRoles {
    const val PREF_APPLE_PALETTE = "pref_board_apple_accent_palette"
    const val DEFAULT_PALETTE = "wallpaper"
    const val PREF_ROUNDED_KEYCAPS = "pref_board_rounded_keycaps"
    const val DEFAULT_ROUNDED_KEYCAPS = true
    val applePaletteIds = listOf("blue", "red", "orange", "yellow", "green", "mint", "teal", "cyan", "indigo", "purple", "pink", "brown")

    /** Android IME actions Go, Search, Send, Next and Done map to iOS contextual return actions. */
    @JvmStatic fun tintIosAction(popup: Boolean, spacer: Boolean, actionKey: Boolean, custom: Boolean, imeAction: Int): Boolean =
        !popup && !spacer && actionKey && !custom && imeAction in 2..6

    /** Replace RGB only; never overwrite the configured normal or pressed alpha. */
    @JvmStatic fun withBaseAlpha(accent: Int, base: Int): Int = (accent and 0x00ffffff) or (base and 0xff000000.toInt())

    data class Palette(val functional: Int, val special: Int, val action: Int)

    @JvmStatic
    fun palette(tinted: Boolean, neutralFunctional: Int, neutralSpecial: Int, accent: Int): Palette =
        Palette(neutralFunctional, neutralSpecial, if (tinted) accent else neutralSpecial)

    @JvmStatic
    fun tintControl(tinted: Boolean, popup: Boolean, spacer: Boolean, actionKey: Boolean): Boolean =
        tinted && !popup && !spacer && actionKey

    /** Keep the default 8dp visual rounding inside tiny resized or landscape keys. */
    @JvmStatic
    fun keyCornerRadius(width: Float, height: Float, density: Float): Float =
        minOf(8f * density.coerceAtLeast(0f), width.coerceAtLeast(0f) / 2f,
            height.coerceAtLeast(0f) / 2f)
}
