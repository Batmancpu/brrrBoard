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

    fun progress(elapsed: Float, duration: Long): Float {
        val t = (elapsed / duration).coerceIn(0f, 1f)
        return t * t * (3f - 2f * t)
    }

    fun delay(preset: SuggestionMotionPreset, ordinal: Int): Long =
        preset.staggerMs * ordinal.coerceIn(0, 5)

    /** Critically damped response with no oscillation. Finite endpoint snaps to exact native ink. */
    fun glyphProgress(preset: SuggestionMotionPreset, elapsed: Float, duration: Long): Float {
        val t = (elapsed / duration).coerceIn(0f, 1f)
        return when (preset) {
            SuggestionMotionPreset.CLASSIC, SuggestionMotionPreset.SCALE -> progress(elapsed, duration)
            SuggestionMotionPreset.SPRING -> if (t >= 1f) 1f else
                (1f - (1f + 7f * t) * kotlin.math.exp(-7f * t)) /
                    (1f - 8f * kotlin.math.exp(-7f))
            SuggestionMotionPreset.RIPPLE -> 1f - (1f - t) * (1f - t) * (1f - t)
        }
    }

    /** Carry visible x/y velocity through a retarget; bound a transient inherited impulse. */
    fun position(preset: SuggestionMotionPreset, from: Float, to: Float,
                 velocity: Float, elapsed: Float, duration: Long): Float {
        if (elapsed <= 0f) return from
        if (elapsed >= duration) return to
        if (preset != SuggestionMotionPreset.SPRING)
            return interpolate(from, to, glyphProgress(preset, elapsed, duration))
        val time = elapsed / 1000f
        val omega = 7f / (duration / 1000f)
        val displacement = from - to
        val v = velocity.coerceIn(-2400f, 2400f)
        return to + (displacement + (v + omega * displacement) * time) *
            kotlin.math.exp(-omega * time)
    }

    fun velocity(preset: SuggestionMotionPreset, from: Float, to: Float,
                 initial: Float, elapsed: Float, duration: Long): Float {
        if (preset != SuggestionMotionPreset.SPRING || elapsed >= duration) return 0f
        val v = initial.coerceIn(-2400f, 2400f)
        if (elapsed <= 0f) return v
        val time = elapsed / 1000f
        val omega = 7f / (duration / 1000f)
        val displacement = from - to
        val coefficient = v + omega * displacement
        return (coefficient - omega * (displacement + coefficient * time)) *
            kotlin.math.exp(-omega * time)
    }

    fun interpolate(from: Float, to: Float, progress: Float): Float =
        from + (to - from) * progress
}
