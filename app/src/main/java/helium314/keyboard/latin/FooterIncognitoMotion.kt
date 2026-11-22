// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import kotlin.math.max

/** Warm gold shared by the two matching privacy badges. */
internal object FooterPrivacyPalette {
    val GOLD: Int = 0xFFD8B766.toInt()
    val HIGHLIGHT: Int = 0xFFF4DC98.toInt()
    val DEPTH: Int = 0xFF916B2B.toInt()
}

/** One compact badge animation drives the Emoji and microphone together. */
internal object FooterIncognitoMotion {
    const val ENTER_MS = 220L
    const val EXIT_MS = 180L

    fun shouldAnimatePrivacyChange(requested: Boolean, footerVisible: Boolean): Boolean =
        requested && footerVisible

    data class Frame(val shield: Float = 0f, val glow: Float = 0f)

    fun settled(incognito: Boolean) = Frame(
        shield = if (incognito) 1f else 0f,
        glow = if (incognito) .16f else 0f,
    )

    private fun easeOut(t: Float): Float {
        val x = t.coerceIn(0f, 1f)
        val remaining = 1f - x
        return 1f - remaining * remaining * remaining
    }

    private fun blend(from: Float, to: Float, amount: Float): Float = from + (to - from) * amount

    /** Reversal continues from the last rendered pose without jumping to an endpoint. */
    fun at(from: Frame, entering: Boolean, progress: Float): Frame {
        val t = easeOut(progress)
        val destination = if (entering) settled(true) else settled(false)
        return Frame(
            shield = max(0f, blend(from.shield, destination.shield, t)).coerceIn(0f, 1f),
            glow = max(0f, blend(from.glow, destination.glow, t)).coerceIn(0f, 1f),
        )
    }
}
