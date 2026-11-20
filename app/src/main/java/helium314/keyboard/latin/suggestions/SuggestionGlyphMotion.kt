// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.suggestions

/** One *visible* character, measured relative to the center of its fixed suggestion slot. */
internal data class SuggestionGlyphSnapshot(
    val character: Char,
    val x: Float,
    val opacity: Float = 1f,
    val scale: Float = 1f,
)

internal data class SuggestionGlyphTrack(
    val character: Char,
    val fromX: Float,
    val toX: Float,
    val fromOpacity: Float,
    val toOpacity: Float,
    val fromScale: Float,
    val toScale: Float,
    val kind: Kind,
    val order: Int,
) {
    enum class Kind { RETAINED, REMOVED, INSERTED }
    // Avoid allocating one String per character on every display frame.
    val ink: String = character.toString()
}

/**
 * Small, deterministic, allocation-free-on-frame layout policy for ASCII QuickType words.
 * Matching by longest common subsequence preserves moving letters through interior edits.
 * Complex-script shaping remains with Android's ordinary TextView renderer.
 */
internal object SuggestionGlyphMotion {
    const val MAX_CHARACTERS = 32

    fun supported(value: String): Boolean = value.length in 1..MAX_CHARACTERS &&
        value.all { it in ' '..'~' }

    fun plan(previous: List<SuggestionGlyphSnapshot>, next: String,
             newPositions: List<Float>): List<SuggestionGlyphTrack> {
        if (previous.size > MAX_CHARACTERS || !supported(next) ||
            newPositions.size != next.length) return emptyList()
        val old = previous.map { it.character }
        val n = old.size
        val m = next.length
        // At most 33×33 cells, computed once per suggestion replacement.
        val lcs = Array(n + 1) { IntArray(m + 1) }
        for (i in n - 1 downTo 0) for (j in m - 1 downTo 0) {
            lcs[i][j] = if (old[i] == next[j]) 1 + lcs[i + 1][j + 1]
                else maxOf(lcs[i + 1][j], lcs[i][j + 1])
        }
        val matchingOld = IntArray(m) { -1 }
        var i = 0
        var j = 0
        while (i < n && j < m) {
            when {
                old[i] == next[j] -> { matchingOld[j] = i; i++; j++ }
                lcs[i + 1][j] >= lcs[i][j + 1] -> i++
                else -> j++
            }
        }
        val matchedOld = BooleanArray(n)
        val result = ArrayList<SuggestionGlyphTrack>(n + m)
        for (index in 0 until m) {
            val oldIndex = matchingOld[index]
            if (oldIndex < 0) continue
            matchedOld[oldIndex] = true
            val glyph = previous[oldIndex]
            result.add(SuggestionGlyphTrack(next[index], glyph.x, newPositions[index],
                glyph.opacity, 1f, glyph.scale, 1f,
                SuggestionGlyphTrack.Kind.RETAINED, index))
        }
        // Outgoing ink is never drawn a second time for a matched letter.
        for (index in previous.indices) {
            if (matchedOld[index]) continue
            val glyph = previous[index]
            result.add(SuggestionGlyphTrack(glyph.character, glyph.x, glyph.x,
                glyph.opacity, 0f, glyph.scale, .88f,
                SuggestionGlyphTrack.Kind.REMOVED, index))
        }
        for (index in 0 until m) {
            if (matchingOld[index] >= 0) continue
            result.add(SuggestionGlyphTrack(next[index], newPositions[index], newPositions[index],
                0f, 1f, .86f, 1f, SuggestionGlyphTrack.Kind.INSERTED, index))
        }
        return result
    }

    fun progress(track: SuggestionGlyphTrack, fraction: Float): Float {
        val elapsed = fraction.coerceIn(0f, 1f)
        val delay = when (track.kind) {
            SuggestionGlyphTrack.Kind.RETAINED -> 0f
            SuggestionGlyphTrack.Kind.REMOVED -> .02f
            SuggestionGlyphTrack.Kind.INSERTED -> .10f + (track.order.coerceAtMost(8) * .018f)
        }
        val normalized = ((elapsed - delay) / (1f - delay)).coerceIn(0f, 1f)
        val remaining = 1f - normalized
        return 1f - remaining * remaining * remaining * remaining
    }

    fun interpolate(start: Float, end: Float, progress: Float): Float =
        start + (end - start) * progress
}
