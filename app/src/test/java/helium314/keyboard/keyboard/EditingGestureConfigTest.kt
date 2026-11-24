package helium314.keyboard.keyboard

import helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode
import org.junit.Assert.*
import org.junit.Test

class EditingGestureConfigTest {
    private val letters=EditingGestureConfig(true,EditingGestureConfig.Mode.LETTER,
        EditingGestureConfig.Action.COPY,EditingGestureConfig.Action.PASTE,
        EditingGestureConfig.Action.UNDO,EditingGestureConfig.Action.REDO)
    private val flicks=letters.copy(mode=EditingGestureConfig.Mode.FLICK)
    @Test fun tapAndLetterSlidesUseOnlyAllowedCommands() {
        assertEquals(KeyCode.TEXT_TOOLS,letters.resolve(1f,1f,2f,48,48,80,KeyCode.TEXT_TOOLS))
        assertEquals(KeyCode.CLIPBOARD_COPY,letters.resolve(80f,-120f,145f,48,48,500,'C'.code))
        assertEquals(KeyCode.CLIPBOARD_PASTE,letters.resolve(80f,-120f,145f,48,48,500,'v'.code))
        assertEquals(KeyCode.CLIPBOARD_CUT,letters.resolve(80f,-120f,145f,48,48,500,'x'.code))
        assertEquals(KeyCode.CLIPBOARD_SELECT_ALL,letters.resolve(80f,-120f,145f,48,48,500,'A'.code))
        assertEquals(KeyCode.UNDO,letters.resolve(80f,-120f,145f,48,48,500,'z'.code))
        assertEquals(KeyCode.NOT_SPECIFIED,letters.resolve(80f,-120f,145f,48,48,500,'q'.code))
    }
    @Test fun flickDirectionsAndCustomDisable() {
        assertEquals(KeyCode.CLIPBOARD_COPY,flicks.resolve(0f,-30f,30f,48,48,200,0))
        assertEquals(KeyCode.CLIPBOARD_PASTE,flicks.resolve(0f,30f,30f,48,48,200,0))
        assertEquals(KeyCode.UNDO,flicks.resolve(-30f,0f,30f,48,48,200,0))
        assertEquals(KeyCode.REDO,flicks.resolve(30f,0f,30f,48,48,200,0))
        assertEquals(KeyCode.NOT_SPECIFIED,flicks.copy(up=EditingGestureConfig.Action.NONE)
            .resolve(0f,-30f,30f,48,48,200,0))
    }
    @Test fun canceledLikePathsAndAmbiguousFlicksAreNoops() {
        assertEquals(KeyCode.NOT_SPECIFIED,letters.copy(enabled=false).resolve(80f,-120f,145f,48,48,500,'c'.code))
        assertEquals(KeyCode.NOT_SPECIFIED,letters.resolve(0f,0f,80f,48,48,150,KeyCode.TEXT_TOOLS))
        assertEquals(KeyCode.NOT_SPECIFIED,flicks.resolve(30f,30f,43f,48,48,200,0))
        assertEquals(KeyCode.NOT_SPECIFIED,flicks.resolve(0f,-30f,30f,48,48,401,0))
        assertEquals(KeyCode.NOT_SPECIFIED,flicks.resolve(0f,-130f,130f,48,48,200,0))
        assertFalse(EditingGestureConfig.allowedCode('a'.code))
    }
}
