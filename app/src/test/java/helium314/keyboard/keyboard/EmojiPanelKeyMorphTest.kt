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

    @Test fun reversingFromThePresentedPositionDoesNotJump() {
        val rendered = EmojiPanelKeyMorph.position(300f, 40f, .4f)
        assertEquals(196f, rendered, .001f)
        assertEquals(rendered, EmojiPanelKeyMorph.position(rendered, 300f, 0f), 0f)
        assertEquals(300f, EmojiPanelKeyMorph.position(rendered, 300f, 1f), 0f)
    }

    @Test fun returnSearchMappingPreservesRealReturnAndAllSharedControls() {
        val enter = helium314.keyboard.latin.common.Constants.CODE_ENTER
        val search = helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode.EMOJI_SEARCH
        val delete = helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode.DELETE
        val space = helium314.keyboard.latin.common.Constants.CODE_SPACE
        val shifted = helium314.keyboard.keyboard.internal.keyboard_parser.floris.KeyCode.SHIFT_ENTER
        val normal = listOf(enter,delete,space)
        val compact = listOf(search,delete,space)
        val withReturn = listOf(search,delete,space,enter)
        assertEquals(setOf(enter to search,delete to delete,space to space),
            EmojiPanelKeyMorph.pairedCodes(normal,compact).toSet())
        assertEquals(setOf(search to enter,delete to delete,space to space),
            EmojiPanelKeyMorph.pairedCodes(compact,normal).toSet())
        assertEquals(setOf(enter to enter,delete to delete,space to space),
            EmojiPanelKeyMorph.pairedCodes(normal,withReturn).toSet())
        assertEquals(setOf(shifted to search,delete to delete,space to space),
            EmojiPanelKeyMorph.pairedCodes(listOf(shifted,delete,space),compact).toSet())
        assertEquals(setOf(enter to shifted,delete to delete,space to space),
            EmojiPanelKeyMorph.pairedCodes(normal,listOf(shifted,delete,space)).toSet())
    }

    @Test fun coldLayoutWaitsOnceAndPendingLayoutIsBounded() {
        org.junit.Assert.assertTrue(EmojiPanelKeyMorph.deferTargetCapture(true, false, true, 0))
        org.junit.Assert.assertFalse(EmojiPanelKeyMorph.deferTargetCapture(true, false, true, 1))
        org.junit.Assert.assertFalse(EmojiPanelKeyMorph.deferTargetCapture(false, false, true, 0))
        org.junit.Assert.assertTrue(EmojiPanelKeyMorph.deferTargetCapture(false, true, true, 1))
        org.junit.Assert.assertTrue(EmojiPanelKeyMorph.deferTargetCapture(false, false, false, 0))
        org.junit.Assert.assertFalse(EmojiPanelKeyMorph.deferTargetCapture(true, true, false, 2))
    }

    @Test fun motionClampsWithoutOvershoot() {
        assertEquals(0f, EmojiPanelKeyMorph.position(0f, 100f, -1f), 0f)
        assertEquals(100f, EmojiPanelKeyMorph.position(0f, 100f, 2f), 0f)
    }
}
