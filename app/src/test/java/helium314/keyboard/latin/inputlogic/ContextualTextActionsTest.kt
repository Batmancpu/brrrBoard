package helium314.keyboard.latin.inputlogic
import org.junit.Assert.*
import org.junit.Test
import java.util.Locale
import helium314.keyboard.latin.inputlogic.ContextualTextActions.Action

class ContextualTextActionsTest {
    @Test fun localeAndUnicode() {
        assertEquals("İSTANBUL",ContextualTextActions.transform("istanbul",Action.UPPER,Locale.forLanguageTag("tr")))
        assertEquals("Hello World",ContextualTextActions.transform("HELLO world",Action.TITLE,Locale.ENGLISH))
        assertEquals("नमस्ते 👨‍👩‍👧",ContextualTextActions.transform(" नमस्ते  👨‍👩‍👧 ",Action.TRIM_SPACES,Locale.ROOT))
    }
    @Test fun linesAndBounds() {
        assertEquals("b\r\na\r\n",ContextualTextActions.transform("b\r\na\r\nb\r\n",Action.DEDUP,Locale.ENGLISH))
        assertEquals("a\r\nb\r\n",ContextualTextActions.transform("b\r\na\r\n",Action.SORT,Locale.ENGLISH))
        assertNull(ContextualTextActions.transform("a\r\nb\nc",Action.SORT,Locale.ROOT))
        assertNull(ContextualTextActions.transform("x".repeat(2049),Action.UPPER,Locale.ROOT))
    }
}
