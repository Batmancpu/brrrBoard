// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.suggestions

/** Pure, bounded transition rules. Durations are 30-fps reference estimates, not Apple APIs. */
internal object QuickTypeMotionPolicy {
    const val GLYPH_MS = 167L
    const val REPLACEMENT_MS = 133L
    const val CHROME_MS = 67L
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

    fun interpolate(from: Float, to: Float, progress: Float): Float =
        from + (to - from) * progress
}
