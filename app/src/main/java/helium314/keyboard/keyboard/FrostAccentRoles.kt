// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard

/** Accent palette and role policy; ordinary text keys and space remain neutral. */
object FrostAccentRoles {
    const val PREF_APPLE_PALETTE = "pref_board_apple_accent_palette"
    const val PREF_EXTENDED_ACCENT = "pref_board_extended_key_accent"
    const val DEFAULT_PALETTE = "wallpaper"
    const val DEFAULT_EXTENDED = true
    const val PREF_ROUNDED_KEYCAPS = "pref_board_rounded_keycaps"
    const val DEFAULT_ROUNDED_KEYCAPS = true
    val applePaletteIds = listOf("blue", "red", "orange", "yellow", "green", "mint", "teal", "cyan", "indigo", "purple", "pink", "brown")

    @JvmStatic fun tintExtended(enabled: Boolean, popup: Boolean, spacer: Boolean, space: Boolean, control: Boolean): Boolean =
        enabled && !popup && !spacer && !space && control

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
