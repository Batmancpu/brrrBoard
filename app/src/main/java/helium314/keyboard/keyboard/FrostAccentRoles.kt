// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard

/** Apple-style action accent, plus owner's explicit plain Return preference. Other keys stay neutral. */
object FrostAccentRoles {
    /** A single versioned default update; never re-enable a later deliberate opt-out. */
    fun actionTintAfterMigration(alreadyMigrated: Boolean, stored: Boolean): Boolean =
        !alreadyMigrated || stored

    const val PREF_APPLE_PALETTE = "pref_board_apple_accent_palette"
    const val DEFAULT_PALETTE = "wallpaper"
    const val PREF_ROUNDED_KEYCAPS = "pref_board_rounded_keycaps"
    const val DEFAULT_ROUNDED_KEYCAPS = true
    val applePaletteIds = listOf("blue", "red", "orange", "yellow", "green", "mint", "teal", "cyan", "indigo", "purple", "pink", "brown")

    /** The actual Return key is eligible even when the layout uses a neutral background flag.
     * Plain Return is intentionally included at the owner's request; custom labels remain neutral. */
    @JvmStatic @Suppress("UNUSED_PARAMETER")
    fun tintIosAction(popup: Boolean, spacer: Boolean, actionKey: Boolean, custom: Boolean, imeAction: Int): Boolean =
        !popup && !spacer && actionKey && !custom

    /** Explicit local search launchers, not arbitrary search-looking keys or popup actions. */
    @JvmStatic fun tintPanelSearch(popup: Boolean, spacer: Boolean, searchKey: Boolean, panelBottomRow: Boolean): Boolean =
        !popup && !spacer && searchKey && panelBottomRow

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
