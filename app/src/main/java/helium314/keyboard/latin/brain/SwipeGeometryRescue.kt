// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.brain

import helium314.keyboard.keyboard.Keyboard
import helium314.keyboard.latin.common.InputPointers
import java.lang.ref.WeakReference
import kotlin.math.hypot
import kotlin.math.max

/**
 * Small, independent geometric *candidate recall* boundary. It does not try to
 * replace the imported GlideLib ranking or infer free-form words from strokes.
 * Instead, corners of a finished trace are mapped to a few plausible typed
 * seeds, which the existing native *text* dictionaries can spell-correct and
 * complete. This can recover words absent from the native gesture candidate set.
 *
 * Raw stroke coordinates stay in the existing ephemeral InputPointers object.
 * No trace, app/editor identity, or entered sentence is persisted or logged.
 */
object SwipeGeometryRescue {
    data class Point(val x: Double, val y: Double)
    // Geometry only, never a stroke or editor text. Do not keep an old Keyboard
    // (and its native proximity table) alive just for this optional rescue.
    private var geometryOwner: WeakReference<Keyboard>? = null
    private var geometry: Map<Char, Point> = emptyMap()
    internal fun acceptsPointerCount(count: Int): Boolean = count in 3..1024

    @Synchronized private fun centersFor(keyboard: Keyboard): Map<Char, Point> {
        if (geometryOwner?.get() === keyboard) return geometry
        val centers = LinkedHashMap<Char, Point>()
        for (key in keyboard.sortedKeys) {
            val code = Character.toLowerCase(key.code)
            if (code !in 'a'.code..'z'.code) continue
            val letter = code.toChar()
            if (letter !in centers) {
                centers[letter] = Point(key.x + key.width / 2.0, key.y + key.height / 2.0)
            }
        }
        geometryOwner = WeakReference(keyboard)
        geometry = centers
        return centers
    }

    /** Deterministic and testable without an Android IME or native decoder. */
    fun hints(points: List<Point>, keyCenters: Map<Char, Point>, keyPitch: Double): List<String> {
        if (points.size < 3 || keyCenters.size < 4 || keyPitch <= 0) return emptyList()
        // Never treat a tap, a tiny jitter or a massive accidental swipe as a word.
        var length = 0.0
        for (i in 1 until points.size) {
            val a = points[i - 1]; val b = points[i]
            length += hypot(b.x - a.x, b.y - a.y)
        }
        if (length < keyPitch * 1.15 || length > keyPitch * 65) return emptyList()
        val spaced = ArrayList<Point>()
        spaced.add(points.first())
        val step = max(keyPitch * 0.28, 1.0)
        for (i in 1 until points.size) {
            val point = points[i]
            val last = spaced.last()
            if (hypot(point.x - last.x, point.y - last.y) >= step) spaced.add(point)
        }
        if (spaced.last() != points.last()) spaced.add(points.last())
        val salient = ArrayList<Point>(18)
        salient.add(spaced.first())
        for (i in 1 until spaced.lastIndex) {
            val here = spaced[i]
            val before = spaced[i - 1]
            val after = spaced[i + 1]
            val ax = here.x - before.x
            val ay = here.y - before.y
            val bx = after.x - here.x
            val by = after.y - here.y
            val denom = hypot(ax, ay) * hypot(bx, by)
            if (denom < 1e-6) continue
            val cosine = (ax * bx + ay * by) / denom
            if (cosine < 0.76 && salient.size < 16) {
                salient.add(here)
            }
        }
        salient.add(points.last())
        fun nearest(point: Point): Char? = keyCenters.minByOrNull { (_, center) ->
            val x = point.x - center.x
            val y = point.y - center.y
            x * x + y * y
        }?.key
        fun collapse(path: List<Point>): String = buildString {
            for (point in path) {
                val letter = nearest(point) ?: continue
                if (isEmpty() || last() != letter) append(letter)
            }
        }
        val corners = collapse(salient)
        val results = LinkedHashSet<String>()
        if (corners.length in 2..18) results.add(corners)
        // A little endpoint imprecision is common with thick thumbs. The
        // typed-mode native spell-checker handles neighboring substitutions.
        // Do not produce many path permutations on this latency-sensitive path.
        if (corners.length >= 4) {
            val short = corners.dropLast(1)
            if (short.length >= 3) results.add(short)
        }
        return results.take(2)
    }

    fun hints(pointers: InputPointers, keyboard: Keyboard): List<String> {
        val count = pointers.pointerSize
        if (!acceptsPointerCount(count)) return emptyList()
        val centers = centersFor(keyboard)
        if (centers.size < 20) return emptyList()
        val xs = pointers.xCoordinates
        val ys = pointers.yCoordinates
        val sampled = ArrayList<Point>(count)
        for (index in 0 until count) sampled.add(Point(xs[index].toDouble(), ys[index].toDouble()))
        return hints(sampled, centers, keyboard.mMostCommonKeyWidth.toDouble())
    }

    /** Pure bounded variants; dictionary membership verifies before suggestion. */
    fun lexicalVariants(seed: String): List<String> {
        if (seed.length !in 3..12 || seed.any { it !in 'a'..'z' }) return emptyList()
        val words = LinkedHashSet<String>()
        words.add(seed)
        for (i in 1 until seed.lastIndex) {
            words.add(seed.substring(0, i + 1) + seed[i] + seed.substring(i + 1))
        }
        for (suffix in listOf("y", "e", "s", "py", "ly", "ed", "er", "ing")) words.add(seed + suffix)
        return words.take(14)
    }

    /** Filter unrelated lexical completions without a full decoder on the hot path. */
    fun couldMatchSeed(seed: String, word: String): Boolean {
        if (seed.length !in 2..18 || word.length !in 2..24
            || !word.all { it.isLetter() }) return false
        val a = seed.lowercase()
        val b = word.lowercase()
        // An early thumb lift near P can be a genuine incomplete "hap";
        // permit the typed lexicon's short common-word completions.
        if (a.length >= 3 && b.startsWith(a) && b.length - a.length in 1..3) return true
        val budget = minOf(3, max(1, a.length / 3))
        if (kotlin.math.abs(a.length - b.length) > budget) return false
        var previous = IntArray(b.length + 1) { it }
        var current = IntArray(b.length + 1)
        for (i in a.indices) {
            current[0] = i + 1
            for (j in b.indices) {
                current[j + 1] = minOf(
                    previous[j + 1] + 1,
                    current[j] + 1,
                    previous[j] + if (a[i] == b[j]) 0 else 1)
            }
            val old = previous
            previous = current
            current = old
        }
        return previous[b.length] <= budget
    }
}
