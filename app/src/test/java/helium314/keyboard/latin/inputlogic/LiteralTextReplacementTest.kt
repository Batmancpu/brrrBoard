package helium314.keyboard.latin.inputlogic

import org.junit.Assert.*
import org.junit.Test

class LiteralTextReplacementTest {
    @Test fun literalNotRegexAndFirstOrAll() {
        assertEquals("x x",LiteralTextReplacement.replace("a.b a.b","a.b","x",true,false,true)?.text)
        assertEquals("x a.b",LiteralTextReplacement.replace("a.b a.b","a.b","x",true,false,false)?.text)
        assertNull(LiteralTextReplacement.replace("abc","a.c","x",true,false,true))
    }
    @Test fun caseAndWordBoundary() {
        val r = LiteralTextReplacement.replace("Cat cater cat _cat cat\u0301","cat","dog",false,true,true)
        assertEquals("dog cater dog _cat cat\u0301",r?.text)
        assertEquals(2,r?.count)
    }
    @Test fun unicodeAndBounds() {
        assertEquals("x🙂",LiteralTextReplacement.replace("🙂🙂","🙂","x",true,false,false)?.text)
        assertEquals("",LiteralTextReplacement.replace("remove","remove","",true,false,true)?.text)
        assertNull(LiteralTextReplacement.replace("a".repeat(2048),"a","bb",true,false,true))
        assertNull(LiteralTextReplacement.replace("🙂","\uD83D","x",true,false,true))
        assertNull(LiteralTextReplacement.replace("abc","","x",true,false,true))
    }
}
