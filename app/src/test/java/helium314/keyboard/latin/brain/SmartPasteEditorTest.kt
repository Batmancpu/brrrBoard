package helium314.keyboard.latin.brain
import android.text.InputType
import android.view.inputmethod.EditorInfo
import helium314.keyboard.latin.ClipboardEntityExtractor
import helium314.keyboard.latin.suggestions.AccessoryPriorityPolicy
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class SmartPasteEditorTest {
    private fun editor(type: Int, hint: String? = null, options: Int = 0) =
        EditorInfo().apply { inputType=type; hintText=hint; imeOptions=options }
    @Test fun otpRequiresExplicitFieldAndNoPrivacyVeto() {
        assertTrue(SmartPastePolicy.isOtpEditor(editor(InputType.TYPE_CLASS_NUMBER,"Enter OTP")))
        assertTrue(SmartPastePolicy.isOtpEditor(editor(InputType.TYPE_CLASS_TEXT,"verificationCode")))
        assertFalse(SmartPastePolicy.isOtpEditor(editor(InputType.TYPE_CLASS_NUMBER,"Enter number")))
        assertFalse(SmartPastePolicy.isOtpEditor(editor(InputType.TYPE_CLASS_TEXT,"Card security code")))
        assertFalse(SmartPastePolicy.isOtpEditor(editor(InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD,"OTP")))
        assertFalse(SmartPastePolicy.isOtpEditor(editor(InputType.TYPE_CLASS_NUMBER,"OTP",EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING)))
    }
    @Test fun codeHistoryExclusion() {
        assertTrue(ClipboardEntityExtractor.excludeFromAutomaticHistory("Authentication code: 123456"))
        assertNotNull(SmartPastePolicy.copiedOtp("One-time password: 123456"))
        assertNull(SmartPastePolicy.copiedOtp("Card security code 1234"))
        assertNull(SmartPastePolicy.copiedOtp("OTP 123456; password: mysecret"))
    }
    @Test fun providerSurfaceGenerationAndEditorVeto() {
        assertFalse(AccessoryPriorityPolicy.canReplaceVisible(true,AccessoryPriorityPolicy.Incoming.CLIPBOARD))
        assertFalse(AccessoryPriorityPolicy.canReplaceVisible(true,AccessoryPriorityPolicy.Incoming.WORDS))
        assertTrue(AccessoryPriorityPolicy.renderBelongsToVisibleEditor(true,4,4,8,8,true))
        assertFalse(AccessoryPriorityPolicy.renderBelongsToVisibleEditor(true,4,5,8,8,true))
        assertFalse(AccessoryPriorityPolicy.renderBelongsToVisibleEditor(true,4,4,8,9,true))
        assertFalse(AccessoryPriorityPolicy.renderBelongsToVisibleEditor(false,4,4,8,8,true))
    }
}
