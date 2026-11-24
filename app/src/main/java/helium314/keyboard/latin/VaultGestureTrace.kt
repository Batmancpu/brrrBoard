// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import kotlin.math.hypot
import kotlin.math.sqrt

/** Small deterministic normalizer for one-finger traces starting in the reserved footer strip and continuing across the keyboard. */
internal object VaultGestureTrace {
    const val POINT_COUNT = 32
    private const val MIN_POINTS = 5
    private const val MAX_RAW_POINTS = 512

    fun normalize(xs: FloatArray, ys: FloatArray, count: Int, density: Float): FloatArray? {
        if (count !in MIN_POINTS..MAX_RAW_POINTS || xs.size < count || ys.size < count || !density.isFinite() || density <= 0f) return null
        if ((0 until count).any { !xs[it].isFinite() || !ys[it].isFinite() }) return null
        var minX = xs[0]; var maxX = xs[0]; var minY = ys[0]; var maxY = ys[0]
        var length = 0.0
        for (i in 1 until count) {
            minX = minOf(minX, xs[i]); maxX = maxOf(maxX, xs[i])
            minY = minOf(minY, ys[i]); maxY = maxOf(maxY, ys[i])
            length += hypot((xs[i] - xs[i - 1]).toDouble(), (ys[i] - ys[i - 1]).toDouble())
        }
        val width = maxX - minX
        val height = maxY - minY
        val longSide = maxOf(width, height)
        val shortSide = minOf(width, height)
        if (longSide < 28f * density || shortSide < MIN_SPAN_DP * density || longSide / shortSide.coerceAtLeast(1f) > MAX_ASPECT_RATIO) return null
        if (length < longSide * MIN_PATH_RATIO) return null
        // Reject nearly straight strokes: a wider trigger must not turn a casual drag into a credential.
        val chord = hypot((xs[count - 1] - xs[0]).toDouble(), (ys[count - 1] - ys[0]).toDouble())
        if (length < chord * 1.12) return null

        // Arc-length resampling avoids depending on touch event rate or drawing speed.
        val cumulative = DoubleArray(count)
        for (i in 1 until count) {
            cumulative[i] = cumulative[i - 1] + hypot(
                (xs[i] - xs[i - 1]).toDouble(), (ys[i] - ys[i - 1]).toDouble(),
            )
        }
        if (cumulative.last() <= 0.0) return null
        val centerX = (minX + maxX) / 2f
        val centerY = (minY + maxY) / 2f
        val scale = longSide.coerceAtLeast(1f)
        val normalized = FloatArray(POINT_COUNT * 2)
        var segment = 1
        for (point in 0 until POINT_COUNT) {
            val target = cumulative.last() * point / (POINT_COUNT - 1)
            while (segment < count - 1 && cumulative[segment] < target) segment++
            val start = cumulative[segment - 1]
            val span = (cumulative[segment] - start).coerceAtLeast(1e-9)
            val fraction = ((target - start) / span).toFloat().coerceIn(0f, 1f)
            val x = xs[segment - 1] + (xs[segment] - xs[segment - 1]) * fraction
            val y = ys[segment - 1] + (ys[segment] - ys[segment - 1]) * fraction
            normalized[point * 2] = (x - centerX) / scale
            normalized[point * 2 + 1] = (y - centerY) / scale
        }
        return normalized
    }

    fun average(samples: List<FloatArray>): FloatArray? {
        if (samples.size != ENROLLMENT_SAMPLES || samples.any { it.size != POINT_COUNT * 2 }) return null
        if (samples.any { sample -> sample.any { !it.isFinite() } }) return null
        if (samples.indices.any { i -> (i + 1 until samples.size).any { j -> !matches(samples[i], samples[j]) } }) return null
        val result = FloatArray(POINT_COUNT * 2)
        samples.forEach { sample -> sample.indices.forEach { i -> result[i] += sample[i] / samples.size } }
        return result
    }

    fun matches(template: FloatArray, candidate: FloatArray): Boolean {
        if (template.size != POINT_COUNT * 2 || candidate.size != template.size) return false
        if (template.any { !it.isFinite() } || candidate.any { !it.isFinite() }) return false
        fun distance(i: Int, j: Int): Double {
            val dx = (template[i * 2] - candidate[j * 2]).toDouble()
            val dy = (template[i * 2 + 1] - candidate[j * 2 + 1]).toDouble()
            return dx * dx + dy * dy
        }
        // Preserve stroke direction and endpoints; limited monotonic warping permits
        // different corner timing without accepting a reversed or reordered pattern.
        if (distance(0, 0) > .18 * .18 || distance(POINT_COUNT - 1, POINT_COUNT - 1) > .18 * .18) return false
        val rms = sqrt((0 until POINT_COUNT).sumOf { distance(it, it) } / POINT_COUNT)
        if (rms > .19) return false
        var previous = DoubleArray(POINT_COUNT + 1) { Double.POSITIVE_INFINITY }
        var current = DoubleArray(POINT_COUNT + 1) { Double.POSITIVE_INFINITY }
        previous[0] = 0.0
        for (i in 1..POINT_COUNT) {
            current.fill(Double.POSITIVE_INFINITY)
            for (j in maxOf(1, i - 3)..minOf(POINT_COUNT, i + 3)) {
                current[j] = distance(i - 1, j - 1) + minOf(previous[j - 1], previous[j] + .003, current[j - 1] + .003)
            }
            val swap = previous; previous = current; current = swap
        }
        return sqrt(previous[POINT_COUNT] / POINT_COUNT) <= MAX_MEAN_POINT_ERROR
    }

    const val ENROLLMENT_SAMPLES = 3
    private const val MIN_SPAN_DP = 8f
    private const val MAX_ASPECT_RATIO = 7f
    private const val MIN_PATH_RATIO = 1.08f
    private const val MAX_MEAN_POINT_ERROR = 0.125
}
