package helium314.keyboard.latin.database
import org.junit.Test
import org.junit.Assert.*
class ClipboardPayloadCacheTest {
    @Test fun fixedWindowDoesNotSlide() {
        var now=100L
        val c=ClipboardPayloadCache({now},2,8,30L)
        c.put(1,"abc"); now=120; c.put(2,"def")
        assertEquals("abc",c.get(1)); now=130
        assertNull(c.get(1)); assertNull(c.get(2)); assertEquals(0L,c.expiry())
    }
    @Test fun boundedCountAndCharactersAndExplicitClear() {
        val c=ClipboardPayloadCache({100L},2,5,30L)
        c.put(1,"abc"); c.put(2,"def"); assertNull(c.get(1))
        c.put(3,"x"); c.put(4,"y"); assertNull(c.get(2))
        c.put(5,"longer"); assertNull(c.get(5))
        c.clear(); assertNull(c.get(4)); assertEquals(0L,c.expiry())
    }
}