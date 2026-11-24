// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.content.ClipData
import android.net.Uri
import android.text.InputType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import android.view.inputmethod.EditorInfo
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ClipboardPrivacyPolicyTest {
    @Test fun noEditorMeansNoAmbientClipboardCollection() {
        assertTrue(ClipboardPrivacyPolicy.isPrivateEditor(null, false))
        assertFalse(ClipboardPrivacyPolicy.mayCollect(null, false, false))
    }
    @Test fun ordinaryInputMayCollectOnlyNonSensitiveClips() {
        val text = EditorInfo().apply { inputType = InputType.TYPE_CLASS_TEXT }
        assertTrue(ClipboardPrivacyPolicy.mayCollect(text, incognito = false, sensitiveClip = false))
        assertFalse(ClipboardPrivacyPolicy.mayCollect(text, incognito = false, sensitiveClip = true))
        assertFalse(ClipboardPrivacyPolicy.mayCollect(text, incognito = true, sensitiveClip = false))
    }

    @Test fun ambientTextNeverCoercesOrReadsCompoundClips() {
        assertEquals("ordinary text", ClipboardPrivacyPolicy.boundedPlainText(ClipData.newPlainText("label", "ordinary text")))
        assertNull(ClipboardPrivacyPolicy.boundedPlainText(ClipData.newPlainText("label", "x".repeat(4097))))
        assertNull(ClipboardPrivacyPolicy.boundedPlainText(ClipData.newHtmlText("label", "text", "<b>text</b>")))
        assertNull(ClipboardPrivacyPolicy.boundedPlainText(ClipData.newRawUri("label", Uri.parse("content://example/private"))))
        val multi = ClipData.newPlainText("label", "first").apply { addItem(ClipData.Item("second")) }
        assertNull(ClipboardPrivacyPolicy.boundedPlainText(multi))
    }

    @Test fun passwordAndNoLearningEditorsHideHistory() {
        val password = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
        }
        val visible = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD
        }
        val web = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD
        }
        val noLearning = EditorInfo().apply {
            inputType = InputType.TYPE_CLASS_TEXT
            imeOptions = EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING
        }
        listOf(password, visible, web, noLearning).forEach { editor ->
            assertTrue(ClipboardPrivacyPolicy.isPrivateEditor(editor, false))
            assertFalse(ClipboardPrivacyPolicy.mayCollect(editor, false, false))
        }
    }
}
