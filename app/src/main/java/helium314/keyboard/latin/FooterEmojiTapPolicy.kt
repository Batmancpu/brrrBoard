// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

/** Only complete, spatially close, consecutive tap pairs switch privacy mode. */
internal object FooterEmojiTapPolicy {
    fun isSecondTap(pendingFirstClick: Boolean, elapsedMs: Long,
        deltaX: Float, deltaY: Float, timeoutMs: Long, slopPx: Float): Boolean {
        if (!pendingFirstClick || elapsedMs < 0 || elapsedMs > timeoutMs) return false
        val distanceSquared = deltaX * deltaX + deltaY * deltaY
        return distanceSquared <= slopPx * slopPx
    }
}
