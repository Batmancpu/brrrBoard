// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.brain

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class SwipeGeometryRescueTest {
    private val keys = mapOf(
        'h' to SwipeGeometryRescue.Point(5.0, 10.0),
        'a' to SwipeGeometryRescue.Point(0.0, 10.0),
        'p' to SwipeGeometryRescue.Point(9.0, 0.0),
        'y' to SwipeGeometryRescue.Point(5.0, 0.0),
        'q' to SwipeGeometryRescue.Point(0.0, 0.0)
    )

    private fun segment(a: SwipeGeometryRescue.Point, b: SwipeGeometryRescue.Point) =
        (0..10).map { fraction ->
            val portion = fraction / 10.0
            SwipeGeometryRescue.Point(a.x + (b.x - a.x) * portion,
                a.y + (b.y - a.y) * portion)
        }

    @Test fun cornersRecoverWordShapeMissedByExistingNativeCandidates() {
        val trace = listOf('h', 'a', 'p', 'y').zipWithNext().flatMap { (a, b) ->
            segment(keys.getValue(a), keys.getValue(b))
        }
        val hints = SwipeGeometryRescue.hints(trace, keys, 2.0)
        assertTrue("expected hapy spelling seed, got $hints", hints.contains("hapy"))
    }

    @Test fun denseTouchSamplingRetainsCornersWithoutDiscardingTheStroke() {
        val trace = listOf('h', 'a', 'p', 'y').zipWithNext().flatMap { (a, b) ->
            val from = keys.getValue(a); val to = keys.getValue(b)
            (0..120).map { i -> val t = i / 120.0
                SwipeGeometryRescue.Point(from.x + (to.x - from.x) * t,
                    from.y + (to.y - from.y) * t)
            }
        }
        assertTrue(trace.size > 256)
        assertTrue(SwipeGeometryRescue.acceptsPointerCount(trace.size))
        assertTrue(SwipeGeometryRescue.hints(trace, keys, 2.0).contains("hapy"))
        assertTrue(!SwipeGeometryRescue.acceptsPointerCount(1025))
    }

    @Test fun rejectsTapJitterAndUnboundedGesture() {
        assertEquals(emptyList<String>(), SwipeGeometryRescue.hints(
            listOf(keys.getValue('h')), keys, 2.0))
        assertEquals(emptyList<String>(), SwipeGeometryRescue.hints(
            listOf(keys.getValue('h'), keys.getValue('h'), keys.getValue('h')), keys, 2.0))
        assertEquals(emptyList<String>(), SwipeGeometryRescue.hints(
            listOf(keys.getValue('h'), SwipeGeometryRescue.Point(1000.0, 1000.0),
                keys.getValue('p')), keys, 2.0))
    }

    @Test fun typedLexicalRescueIsBoundedWithoutNativeDecoderReentry() {
        val variants = SwipeGeometryRescue.lexicalVariants("hapy")
        assertTrue(variants.contains("happy"))
        assertTrue(SwipeGeometryRescue.lexicalVariants("hap").contains("happy"))
        assertTrue(variants.size <= 14)
        assertTrue(SwipeGeometryRescue.lexicalVariants("ha!").isEmpty())
    }

    @Test fun independentLexicalRescueMayRecoverMissingHappy() {
        assertTrue(SwipeGeometryRescue.couldMatchSeed("hapy", "happy"))
        assertTrue(SwipeGeometryRescue.couldMatchSeed("hap", "happy"))
        assertTrue(!SwipeGeometryRescue.couldMatchSeed("hap", "zapt"))
        assertTrue(!SwipeGeometryRescue.couldMatchSeed("hapy", "birthday"))
    }
}
