// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import kotlin.math.max

/** Warm metallic-gold privacy accent shared by the emoji and mic shield. */
internal object FooterPrivacyPalette {
    val GOLD: Int = 0xFFD8B766.toInt() // champagne gold
    val HIGHLIGHT: Int = 0xFFF4DC98.toInt() // warm lens glint
    val DEPTH: Int = 0xFF916B2B.toInt() // restrained shadow
}

/** The emoji stays in its 31dp cell; each channel has its own continuous timing. */
internal object FooterIncognitoMotion {
    const val ENTER_MS = 760L
    const val EXIT_MS = 700L

    /** Visibility of the footer, not its current Emoji/ABC icon, gates motion. */
    fun shouldAnimatePrivacyChange(requested: Boolean, footerVisible: Boolean): Boolean =
        requested && footerVisible

    data class Frame(
        val hat: Float = 0f,
        val glasses: Float = 0f,
        val nod: Float = 0f,
        val hatDrop: Float = 0f,
        val hatBehind: Float = 0f,
        val glint: Float = 0f,
        val colorSpread: Float = 0f,
        val shield: Float = 0f,
    )

    fun settled(incognito: Boolean) = Frame(
        hat = if (incognito) 1f else 0f,
        glasses = if (incognito) 1f else 0f,
        glint = if (incognito) 1f else 0f,
        colorSpread = if (incognito) 1f else 0f,
        shield = if (incognito) 1f else 0f,
    )

    private fun window(t: Float, start: Float, end: Float): Float {
        val x = ((t - start) / (end - start)).coerceIn(0f, 1f)
        return x * x * (3f - 2f * x)
    }

    private fun blend(from: Float, to: Float, fraction: Float) =
        from + (to - from) * fraction

    /** Reversing at ANY instant starts at the current drawn pose, never at an endpoint. */
    fun at(from: Frame, entering: Boolean, progress: Float): Frame {
        val t = progress.coerceIn(0f, 1f)
        val finish = window(t, 0f, 1f)
        return if (entering) {
            val nod = window(t, .20f, .43f) * (1f - window(t, .48f, .77f))
            Frame(
                hat = blend(from.hat, 1f, window(t, .02f, .28f)),
                glasses = blend(from.glasses, 1f, window(t, .52f, .76f)),
                nod = max(0f, blend(from.nod, 0f, finish) + nod * (1f - from.nod)),
                hatDrop = blend(from.hatDrop, 0f, window(t, .02f, .34f)),
                hatBehind = blend(from.hatBehind, 0f, window(t, .02f, .34f)),
                glint = blend(from.glint, 1f, window(t, .69f, .89f)),
                colorSpread = blend(from.colorSpread, 1f, window(t, .75f, .99f)),
                // Encompass the mic from the same animator as the emoji: no second clock.
                shield = blend(from.shield, 1f, window(t, .20f, 1f)),
            )
        } else {
            val nod = window(t, .08f, .31f) * (1f - window(t, .52f, .77f))
            Frame(
                hat = blend(from.hat, 0f, window(t, .76f, 1f)),
                glasses = blend(from.glasses, 0f, window(t, .30f, .58f)),
                nod = max(0f, blend(from.nod, 0f, finish) + nod * (1f - from.nod)),
                hatDrop = blend(from.hatDrop, 7.2f, window(t, .72f, 1f)),
                hatBehind = blend(from.hatBehind, 1f, window(t, .72f, .92f)),
                glint = blend(from.glint, 0f, window(t, .10f, .35f)),
                colorSpread = blend(from.colorSpread, 0f, window(t, .05f, .42f)),
                shield = blend(from.shield, 0f, window(t, .02f, 1f)),
            )
        }
    }
}
