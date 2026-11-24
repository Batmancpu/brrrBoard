package helium314.keyboard.keyboard

import org.junit.Assert.assertEquals
import org.junit.Test

class EmojiPanelKeyMorphTest {
    @Test fun searchSlidesHorizontallyAtExistingRowHeight() {
        assertEquals(300f, EmojiPanelKeyMorph.position(300f, 40f, 0f), 0f)
        assertEquals(170f, EmojiPanelKeyMorph.position(300f, 40f, .5f), 0f)
        assertEquals(40f, EmojiPanelKeyMorph.position(300f, 40f, 1f), 0f)
        assertEquals(250f, EmojiPanelKeyMorph.position(250f, 250f, .5f), 0f)
    }

    @Test fun deleteMovesDownAndReversesOnReturn() {
        assertEquals(230f, EmojiPanelKeyMorph.position(210f, 250f, .5f), 0f)
        assertEquals(230f, EmojiPanelKeyMorph.position(250f, 210f, .5f), 0f)
        assertEquals(210f, EmojiPanelKeyMorph.position(250f, 210f, 1f), 0f)
    }

    @Test fun stationaryKeysDoNotNeedAnOverlayEvenIfTheirBoundsChange() {
        org.junit.Assert.assertFalse(EmojiPanelKeyMorph.moves(40f, 60f, 40f, 60f))
        org.junit.Assert.assertTrue(EmojiPanelKeyMorph.moves(40f, 60f, 40f, 90f))
        org.junit.Assert.assertTrue(EmojiPanelKeyMorph.moves(40f, 60f, 80f, 60f))
    }

    @Test fun motionClampsWithoutOvershoot() {
        assertEquals(0f, EmojiPanelKeyMorph.position(0f, 100f, -1f), 0f)
        assertEquals(100f, EmojiPanelKeyMorph.position(0f, 100f, 2f), 0f)
    }
}
