package helium314.keyboard.latin.inputlogic

import org.junit.Assert.*
import org.junit.Test

class DynamicTextTemplateTest {
    @Test fun literalTokensAndCursor() {
        val r = DynamicTextTemplate.render("Hi {cursor}🙂 {date} {time} {other}","24/11/26","10:30")!!
        assertEquals("Hi 🙂 24/11/26 10:30 {other}",r.text)
        assertEquals(3,r.cursor)
        assertEquals(3,DynamicTextTemplate.render("abc","","")!!.cursor)
    }
    @Test fun rejectsRepeatedCursorAndOversize() {
        assertNull(DynamicTextTemplate.render("{cursor}x{cursor}","",""))
        assertNull(DynamicTextTemplate.render("x".repeat(506)+"{date}","2026-11-24",""))
        assertNull(DynamicTextTemplate.render("{cursor}","",""))
        assertFalse(DynamicTextTemplate.validList(listOf(
            DynamicTextTemplate.Template("Name","x"),DynamicTextTemplate.Template("name","y"))))
    }
}
