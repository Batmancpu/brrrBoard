// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import kotlin.math.max

/** The emoji stays in its 31dp cell; each channel has its own continuous timing. */
internal object FooterIncognitoMotion {
    const val ENTER_MS = 680L
    const val EXIT_MS = 630L

    data class Frame(
        val hat: Float = 0f,
        val glasses: Float = 0f,
        val nod: Float = 0f,
        val hatDrop: Float = 0f,
        val hatBehind: Float = 0f,
        val glint: Float = 0f,
    )

    fun settled(incognito: Boolean) = Frame(
        hat = if (incognito) 1f else 0f,
        glasses = if (incognito) 1f else 0f,
        glint = if (incognito) 1f else 0f,
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
            val nod = window(t, .19f, .49f) * (1f - window(t, .53f, .85f))
            Frame(
                hat = blend(from.hat, 1f, window(t, .02f, .28f)),
                glasses = blend(from.glasses, 1f, window(t, .59f, .85f)),
                nod = max(0f, blend(from.nod, 0f, finish) + nod * (1f - from.nod)),
                hatDrop = blend(from.hatDrop, 0f, window(t, .02f, .34f)),
                hatBehind = blend(from.hatBehind, 0f, window(t, .02f, .34f)),
                glint = blend(from.glint, 1f, window(t, .80f, 1f)),
            )
        } else {
            val nod = window(t, .06f, .32f) * (1f - window(t, .57f, .81f))
            Frame(
                hat = blend(from.hat, 0f, window(t, .75f, 1f)),
                glasses = blend(from.glasses, 0f, window(t, .32f, .58f)),
                nod = max(0f, blend(from.nod, 0f, finish) + nod * (1f - from.nod)),
                hatDrop = blend(from.hatDrop, 8f, window(t, .73f, 1f)),
                hatBehind = blend(from.hatBehind, 1f, window(t, .70f, .88f)),
                glint = blend(from.glint, 0f, window(t, .12f, .52f)),
            )
        }
    }
}
