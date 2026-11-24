// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import org.junit.Assert.*
import org.junit.Test

class EpochLruCacheTest {
    @Test fun inFlightRequestCannotRefillAfterInvalidation() {
        val cache = EpochLruCache<String, String>(2)
        val old = cache.epoch()
        cache.put(old, "a", "old")
        cache.clear()
        val fresh = cache.epoch()
        cache.put(old, "a", "stale")
        assertNull(cache.get(fresh, "a"))
        cache.put(fresh, "a", "fresh")
        assertNull(cache.get(old, "a"))
        assertEquals("fresh", cache.get(fresh, "a"))
    }
    @Test fun recentHitsAreRetainedWithinBound() {
        val cache = EpochLruCache<Int, Int>(2)
        val e = cache.epoch()
        cache.put(e, 1, 1); cache.put(e, 2, 2)
        assertEquals(1, cache.get(e, 1))
        cache.put(e, 3, 3)
        assertNull(cache.get(e, 2))
        assertEquals(1, cache.get(e, 1))
    }
}
