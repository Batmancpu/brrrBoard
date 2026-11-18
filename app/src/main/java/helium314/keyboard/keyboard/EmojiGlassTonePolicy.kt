// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard

/**
 * A color transform of the *already sampled internal Emoji pixels* only.
 * This matrix keeps the source alpha exactly unchanged; applying it as a
 * saveLayer paint must not paint an independent black/white rectangle over
 * the host-app backdrop that the IME window already blurs.
 */
object EmojiGlassTonePolicy {
    private const val DARK_GAIN = 0.72f
    private const val LIGHT_GAIN = 0.88f
    private const val LIGHT_OFFSET = 30.6f // 12% of full white

    @JvmStatic
    fun matrix(enabled: Boolean, dark: Boolean): FloatArray? {
        if (!enabled) return null
        val gain = if (dark) DARK_GAIN else LIGHT_GAIN
        val offset = if (dark) 0f else LIGHT_OFFSET
        return floatArrayOf(
            gain, 0f, 0f, 0f, offset,
            0f, gain, 0f, 0f, offset,
            0f, 0f, gain, 0f, offset,
            0f, 0f, 0f, 1f, 0f,
        )
    }
}
