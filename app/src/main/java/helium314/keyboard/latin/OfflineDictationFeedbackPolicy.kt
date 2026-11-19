// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

/** Presentation must follow the actual voice-session exit, not elapsed time or a stale label. */
internal enum class OfflineDictationExit { USER_CANCEL, OTHER }

internal object OfflineDictationFeedbackPolicy {
    fun showDiscard(reason: OfflineDictationExit, wasActive: Boolean): Boolean =
        reason == OfflineDictationExit.USER_CANCEL && wasActive
}

/** Dimensionless, testable keyframes; no interpolation can insert or cancel speech. */
internal object FooterMicMorphPolicy {
    const val DURATION_MS = 520L

    private fun ease(value: Float): Float {
        val t = value.coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    private fun between(p: Float, begin: Float, end: Float): Float =
        ease((p.coerceIn(0f, 1f) - begin) / (end - begin))

    fun dropFraction(p: Float): Float = between(p, .0f, .29f)
    fun swallowFraction(p: Float): Float = between(p, .36f, .68f)
    fun returnFraction(p: Float): Float = between(p, .74f, 1f)
    fun lidOpenFraction(p: Float): Float =
        between(p, .21f, .39f) * (1f - between(p, .65f, .79f))
    fun binAlpha(p: Float): Float =
        between(p, .0f, .08f) * (1f - between(p, .77f, 1f))
    fun redMicAlpha(p: Float): Float = 1f - between(p, .45f, .68f)
    fun whiteMicAlpha(p: Float): Float = between(p, .77f, 1f)
    fun idleRevealFraction(p: Float): Float = between(p, .92f, 1f)
}
