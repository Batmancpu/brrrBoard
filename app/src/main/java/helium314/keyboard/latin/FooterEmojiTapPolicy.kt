// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

/** Immediate Emoji/ABC action with disjoint, close tap pairs for private mode. */
internal object FooterEmojiTapPolicy {
    enum class Action { PRIMARY_NOW, PRIVACY_AND_RETURN, CONSUME_SECOND }

    fun action(secondTap: Boolean, firstOpenedEmoji: Boolean): Action = when {
        !secondTap -> Action.PRIMARY_NOW
        firstOpenedEmoji -> Action.PRIVACY_AND_RETURN
        else -> Action.CONSUME_SECOND
    }

    fun isSecondTap(firstTapAlreadyHandled: Boolean, elapsedMs: Long,
        deltaX: Float, deltaY: Float, timeoutMs: Long, slopPx: Float): Boolean {
        if (!firstTapAlreadyHandled || elapsedMs < 0 || elapsedMs > timeoutMs) return false
        val distanceSquared = deltaX * deltaX + deltaY * deltaY
        return distanceSquared <= slopPx * slopPx
    }
}
