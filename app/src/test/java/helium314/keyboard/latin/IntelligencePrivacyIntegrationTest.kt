package helium314.keyboard.latin

import android.text.InputType
import android.view.inputmethod.EditorInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** Synthetic cross-boundary QA; it cannot certify an Oppo IME window or Keystore. */
class IntelligencePrivacyIntegrationTest {
    @Test fun ordinaryAndRestrictedEditorsHaveDifferentClipboardButSameEpochSafety() {
        val generation = EditorSessionGeneration()
        val editor = EditorInfo().apply { inputType = InputType.TYPE_CLASS_TEXT }
        val start = generation.current()
        assertTrue(ClipboardPrivacyPolicy.mayCollect(editor, false, false))
        assertEquals(ClipboardPasteSafety.Action.INSERT,
            ClipboardPasteSafety.action(true, true, false, false))
        editor.imeOptions = EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
        generation.invalidate() // private editor transition
        assertFalse(generation.accepts(start))
        assertFalse(ClipboardPrivacyPolicy.mayCollect(editor, false, false))
        assertEquals(ClipboardPasteSafety.Action.CONFIRM,
            ClipboardPasteSafety.action(true, true, true, false))
    }

    @Test fun sensitiveClipIsNeverStoredAndNeedsConsentEvenInOrdinaryEditor() {
        val editor = EditorInfo().apply { inputType = InputType.TYPE_CLASS_TEXT }
        assertFalse(ClipboardPrivacyPolicy.mayCollect(editor, false, true))
        assertEquals(ClipboardPasteSafety.Action.CONFIRM,
            ClipboardPasteSafety.action(true, true, false, true))
    }
}
