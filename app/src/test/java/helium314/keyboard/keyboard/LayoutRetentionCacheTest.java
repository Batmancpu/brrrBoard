// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard;

import org.junit.Test;
import static org.junit.Assert.*;

public class LayoutRetentionCacheTest {
    @Test public void invalidationClearsBothTiers() {
        LayoutRetentionCache<Integer, Object> c = new LayoutRetentionCache<>(8, 2);
        Object a = new Object();
        c.put(1, a, true);
        assertSame(a, c.get(1, true));
        assertEquals(1, c.warmSize());
        c.clear();
        assertEquals(0, c.size());
        assertEquals(0, c.warmSize());
        assertNull(c.get(1, true));
    }
    @Test public void manySecondaryLayoutsDoNotEvictWarmAlphabet() {
        LayoutRetentionCache<Integer, Object> c = new LayoutRetentionCache<>(8, 2);
        Object alphabet = new Object();
        c.put(1, alphabet, true);
        for (int i = 2; i < 100; i++) c.put(i, new Object(), false);
        assertEquals(8, c.size());
        assertSame(alphabet, c.get(1, true));
        assertEquals(1, c.warmSize());
        assertNull(c.get(2, false));
    }
    @Test public void warmTierIsBoundedAndHitsPromote() {
        LayoutRetentionCache<Integer, Object> c = new LayoutRetentionCache<>(4, 2);
        Object a = new Object(), b = new Object(), d = new Object();
        c.put(1, a, true); c.put(2, b, true);
        assertSame(a, c.get(1, true));
        c.put(3, d, true);
        for (int i = 4; i < 12; i++) c.put(i, new Object(), false);
        assertEquals(2, c.warmSize());
        assertSame(a, c.get(1, false));
        assertSame(d, c.get(3, false));
        assertNull(c.get(2, false));
    }
}
