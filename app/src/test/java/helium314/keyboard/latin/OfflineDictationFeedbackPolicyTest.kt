// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfflineDictationFeedbackPolicyTest {
    @Test fun discardIsExclusivelyAnExplicitActiveUserCancel() {
        assertTrue(OfflineDictationFeedbackPolicy.showDiscard(
            OfflineDictationExit.USER_CANCEL, wasActive = true))
        assertFalse(OfflineDictationFeedbackPolicy.showDiscard(
            OfflineDictationExit.USER_CANCEL, wasActive = false))
        assertFalse(OfflineDictationFeedbackPolicy.showDiscard(
            OfflineDictationExit.OTHER, wasActive = true))
        assertFalse(OfflineDictationFeedbackPolicy.showDiscard(
            OfflineDictationExit.OTHER, wasActive = false))
    }

    @Test fun binDropsBeforeMicIsSwallowedThenMorphsToIdleGlyph() {
        assertEquals(0f, FooterMicMorphPolicy.dropFraction(0f), 0f)
        assertEquals(1f, FooterMicMorphPolicy.dropFraction(.3f), 0f)
        assertEquals(0f, FooterMicMorphPolicy.swallowFraction(.3f), 0f)
        assertEquals(1f, FooterMicMorphPolicy.swallowFraction(.7f), 0f)
        assertEquals(1f, FooterMicMorphPolicy.lidOpenFraction(.5f), 0f)
        assertEquals(0f, FooterMicMorphPolicy.lidOpenFraction(.85f), 0f)
        assertEquals(1f, FooterMicMorphPolicy.redMicAlpha(0f), 0f)
        assertEquals(0f, FooterMicMorphPolicy.redMicAlpha(1f), 0f)
        assertEquals(1f, FooterMicMorphPolicy.whiteMicAlpha(1f), 0f)
        assertEquals(0f, FooterMicMorphPolicy.binAlpha(0f), 0f)
        assertEquals(1f, FooterMicMorphPolicy.binAlpha(.1f), 0f)
        assertEquals(0f, FooterMicMorphPolicy.binAlpha(1f), 0f)
        assertEquals(1f, FooterMicMorphPolicy.idleRevealFraction(1f), 0f)
        assertTrue((0..100).all { step ->
            val p = step / 100f
            listOf(FooterMicMorphPolicy.dropFraction(p), FooterMicMorphPolicy.swallowFraction(p),
                FooterMicMorphPolicy.lidOpenFraction(p), FooterMicMorphPolicy.binAlpha(p),
                FooterMicMorphPolicy.redMicAlpha(p), FooterMicMorphPolicy.whiteMicAlpha(p),
                FooterMicMorphPolicy.idleRevealFraction(p)).all { it in 0f..1f }
        })
    }

    @Test fun autoSendRequiresExplicitHoldValidEditorAndSendAction() {
        val send = android.view.inputmethod.EditorInfo.IME_ACTION_SEND
        val go = android.view.inputmethod.EditorInfo.IME_ACTION_GO
        val done = android.view.inputmethod.EditorInfo.IME_ACTION_DONE
        val search = android.view.inputmethod.EditorInfo.IME_ACTION_SEARCH
        val noEnterAction = android.view.inputmethod.EditorInfo.IME_FLAG_NO_ENTER_ACTION
        assertTrue(OfflineDictationAutoSendPolicy.shouldSend(true, true, false, send))
        assertTrue(OfflineDictationAutoSendPolicy.shouldSend(true, true, false, send or noEnterAction))
        assertFalse(OfflineDictationAutoSendPolicy.shouldSend(false, true, false, send))
        assertFalse(OfflineDictationAutoSendPolicy.shouldSend(true, false, false, send))
        assertFalse(OfflineDictationAutoSendPolicy.shouldSend(true, true, true, send))
        for (action in listOf(go, done, search, android.view.inputmethod.EditorInfo.IME_ACTION_NONE)) {
            assertFalse(OfflineDictationAutoSendPolicy.shouldSend(true, true, false, action))
        }
    }
}
