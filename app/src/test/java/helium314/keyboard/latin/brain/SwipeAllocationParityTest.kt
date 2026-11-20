// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.brain

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlin.math.abs
import kotlin.math.max

/** Unshipped two-row candidate; parity ONLY, not a measured speedup. */
class SwipeAllocationParityTest {
    private fun candidate(seed: String, word: String): Boolean {
        if (seed.length !in 2..18 || word.length !in 2..24 || !word.all { it.isLetter() }) return false
        val a = seed.lowercase()
        val b = word.lowercase()
        if (a.length >= 3 && b.startsWith(a) && b.length - a.length in 1..3) return true
        val budget = minOf(3, max(1, a.length / 3))
        if (abs(a.length - b.length) > budget) return false
        var previous = IntArray(b.length + 1) { it }
        var current = IntArray(b.length + 1)
        for (i in a.indices) {
            current[0] = i + 1
            for (j in b.indices) current[j + 1] = minOf(previous[j + 1] + 1,
                current[j] + 1, previous[j] + if (a[i] == b[j]) 0 else 1)
            val old = previous
            previous = current
            current = old
        }
        return previous[b.length] <= budget
    }

    @Test fun syntheticPairCrossProductIsIdenticalToCurrentDistance() {
        val words = listOf("happy", "hapy", "happyy", "hap", "birthday", "good", "goodnight",
            "android", "github", "hello", "helllo", "colour", "color", "tomorrow",
            "coffee", "coffe", "qwerty", "abc", "ab", "a", "123", "ca!", "Über")
        for (seed in words) for (word in words) {
            assertEquals("synthetic seed/word $seed / $word", SwipeGeometryRescue.couldMatchSeed(seed, word),
                candidate(seed, word))
        }
    }
}
