// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.suggestions

/** Pure, bounded transition rules. Durations are 30-fps reference estimates, not Apple APIs. */
internal object QuickTypeMotionPolicy {
    const val GLYPH_MS = 167L
    const val REPLACEMENT_MS = 133L
    const val CHROME_MS = 33L
    const val MAX_LABEL = 40
    const val MAX_VISIBLE_GLYPHS = 80

    fun supports(label: String): Boolean = label.length <= MAX_LABEL &&
        label.all { it in ' '..'~' || it in '\u2018'..'\u201d' || it == '\u2026' }

    // An ordered longest common subsequence keeps individual matching letters continuous.
    // Weak coincidental matches use an ordinary slot-local dissolve instead.
    fun matches(old: String, next: String): List<Pair<Int, Int>> {
        if (old.length > MAX_VISIBLE_GLYPHS || next.length > MAX_LABEL ||
            old.isEmpty() || next.isEmpty()) return emptyList()
        val dp = Array(old.length + 1) { IntArray(next.length + 1) }
        for (i in old.indices.reversed()) for (j in next.indices.reversed()) {
            dp[i][j] = if (old[i] == next[j]) 1 + dp[i + 1][j + 1]
                else maxOf(dp[i + 1][j], dp[i][j + 1])
        }
        if (old != next && (dp[0][0] < 2 || dp[0][0] * 3 < minOf(old.length, next.length))) {
            return emptyList()
        }
        var i = 0
        var j = 0
        val result = ArrayList<Pair<Int, Int>>()
        while (i < old.length && j < next.length) {
            if (old[i] == next[j]) {
                result.add(i to j)
                i++
                j++
            } else if (dp[i + 1][j] >= dp[i][j + 1]) i++ else j++
        }
        return result
    }

    /** Keep literal prefix/suffix anchors before resolving repeated middle letters. */
    fun improvedMatches(old: String, next: String): List<Pair<Int, Int>> {
        if (old.length > MAX_LABEL || next.length > MAX_LABEL ||
            old.isEmpty() || next.isEmpty()) return emptyList()
        var prefix = 0
        while (prefix < minOf(old.length, next.length) && old[prefix] == next[prefix]) prefix++
        var suffix = 0
        while (suffix < minOf(old.length, next.length) - prefix &&
            old[old.lastIndex - suffix] == next[next.lastIndex - suffix]) suffix++
        val result = ArrayList<Pair<Int, Int>>()
        for (i in 0 until prefix) result.add(i to i)
        val middleOld = old.substring(prefix, old.length - suffix)
        val middleNew = next.substring(prefix, next.length - suffix)
        matches(middleOld, middleNew).forEach { (a, b) -> result.add(a + prefix to b + prefix) }
        for (i in suffix downTo 1) result.add(old.length - i to next.length - i)
        if (old != next && (result.size * 3 < minOf(old.length, next.length) ||
                result.size < 2 && minOf(old.length, next.length) > 2)) return emptyList()
        return result
    }

    fun progress(elapsed: Float, duration: Long): Float {
        val t = (elapsed / duration).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    /** The original Scale contract is immutable; Improved favors readable ink. */
    fun chromeDuration(preset: SuggestionMotionPreset): Long =
        if (preset == SuggestionMotionPreset.SCALE_IMPROVED) 85L else CHROME_MS

    fun exitScale(preset: SuggestionMotionPreset): Float =
        if (preset == SuggestionMotionPreset.SCALE_IMPROVED) .82f else preset.entryScale

    fun entryOpacity(preset: SuggestionMotionPreset, previousVisible: Boolean,
                     hasMatchingInk: Boolean): Float = when {
        preset == SuggestionMotionPreset.SCALE -> 0f
        !previousVisible -> .65f
        hasMatchingInk -> .30f
        else -> 0f // unrelated words must not stack opaque ink
    }

    fun delay(preset: SuggestionMotionPreset, ordinal: Int): Long =
        preset.staggerMs * ordinal.coerceIn(0, 5)

    /** Keep the accepted Scale growth curve; do not accelerate or extend its envelope. */
    @Suppress("UNUSED_PARAMETER")
    fun glyphProgress(preset: SuggestionMotionPreset, elapsed: Float, duration: Long): Float =
        progress(elapsed, duration)

    /** Slightly decouple opacity from growth without a fast pop or extended ghost. */
    fun opacityProgress(preset: SuggestionMotionPreset, elapsed: Float, duration: Long,
                        disappearing: Boolean): Float {
        if (preset == SuggestionMotionPreset.SCALE) return progress(elapsed, duration)
        val t = (elapsed / duration).coerceIn(0f, 1f)
        val shifted = if (disappearing) t + .16f * t * (1f - t)
            else t + .08f * t * (1f - t)
        return shifted * shifted * (3f - 2f * shifted)
    }

    // Finite cubic Hermite interpolation: smoothstep for zero initial velocity, exact
    // endpoint and zero terminal velocity. Bound inherited momentum to prevent large
    // overshoots; opposite-direction momentum can briefly continue before reversing.
    private fun tangent(from: Float, to: Float, velocity: Float, duration: Long): Float {
        val delta = to - from
        val limit = kotlin.math.abs(delta)
        if (limit < .001f) return 0f
        val impulse = velocity.coerceIn(-2400f, 2400f) * duration / 1000f
        return if (impulse * delta >= 0f) impulse.coerceIn(-3f * limit, 3f * limit)
            else impulse.coerceIn(-.35f * limit, .35f * limit)
    }

    fun position(preset: SuggestionMotionPreset, from: Float, to: Float,
                 velocity: Float, elapsed: Float, duration: Long): Float {
        if (elapsed <= 0f) return from
        if (elapsed >= duration) return to
        val t = elapsed / duration
        if (preset == SuggestionMotionPreset.SCALE)
            return interpolate(from, to, progress(elapsed, duration))
        return interpolate(from, to, progress(elapsed, duration)) +
            tangent(from, to, velocity, duration) * t * (1f - t) * (1f - t)
    }

    fun velocity(preset: SuggestionMotionPreset, from: Float, to: Float,
                 initial: Float, elapsed: Float, duration: Long): Float {
        if (preset == SuggestionMotionPreset.SCALE || elapsed >= duration) return 0f
        val t = (elapsed / duration).coerceIn(0f, 1f)
        return ((to - from) * 6f * t * (1f - t) +
            tangent(from, to, initial, duration) * (1f - 4f * t + 3f * t * t)) *
            1000f / duration
    }

    /** Improved retargets from recorded draw commands; accepted Scale remains unchanged. */
    fun captureElapsed(preset: SuggestionMotionPreset,scheduled: Float,drawn: Float): Float =
        if (preset == SuggestionMotionPreset.SCALE_IMPROVED) drawn.coerceIn(0f,scheduled.coerceAtLeast(0f))
        else scheduled

    fun interpolate(from: Float, to: Float, progress: Float): Float =
        from + (to - from) * progress
}
