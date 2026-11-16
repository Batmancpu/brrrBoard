// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard

/** The wallpaper accent is scoped to explicit action keys, not ordinary modifiers. */
object FrostAccentRoles {
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
