// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

class PrivateGestureTraceTest {
    @Test fun invalidAndStraightLineGesturesAreRejected() {
        val xs = FloatArray(16) { it * 5f }
        val flat = FloatArray(16)
        assertNull(PrivateGestureTrace.normalize(xs, flat, xs.size, 1f))
        assertNull(PrivateGestureTrace.normalize(xs, flat, 5, 1f))
        assertNull(PrivateGestureTrace.normalize(xs, flat, 100, 1f))
    }

    @Test fun identicalResampledGestureMatchesButUnrelatedGestureDoesNot() {
        val count = 32
        val xs = FloatArray(count) { index ->
            (60f + 35f * cos(index.toDouble() * 2.0 * PI / (count - 1))).toFloat()
        }
        val ys = FloatArray(count) { index ->
            (65f + 30f * sin(index.toDouble() * 2.0 * PI / (count - 1))).toFloat()
        }
        val sample = PrivateGestureTrace.normalize(xs, ys, count, 1f)
        assertNotNull(sample)
        val enrolled = PrivateGestureTrace.average(listOf(sample!!, sample.copyOf(), sample.copyOf()))
        assertNotNull(enrolled)
        assertTrue(PrivateGestureTrace.matches(enrolled!!, sample))
        val offset = sample.copyOf()
        for (index in offset.indices step 2) {
            offset[index] += 0.5f
        }
        assertFalse(PrivateGestureTrace.matches(enrolled, offset))
        assertFalse(PrivateGestureTrace.matches(enrolled, FloatArray(12)))
    }
}
