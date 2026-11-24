package helium314.keyboard.latin

import android.text.InputType
import android.view.inputmethod.EditorInfo
import org.junit.Assert.*
import org.junit.Test

class WhatsAppVaultPolicyTest {
    private val text = InputType.TYPE_CLASS_TEXT
    @Test fun exactWhatsAppPackageAndOrdinaryTextOnly() {
        assertTrue(WhatsAppVaultPolicy.isTextTarget("com.whatsapp", text))
        assertTrue(WhatsAppVaultPolicy.isTextTarget("com.whatsapp", text or InputType.TYPE_TEXT_FLAG_MULTI_LINE))
        assertTrue(WhatsAppVaultPolicy.isTextTarget("com.whatsapp", text or InputType.TYPE_TEXT_VARIATION_PASSWORD))
        assertFalse(WhatsAppVaultPolicy.isTextTarget("com.whatsapp.w4b", text))
        assertFalse(WhatsAppVaultPolicy.isTextTarget("com.whatsapp.fake", text))
        assertFalse(WhatsAppVaultPolicy.isTextTarget("com.example", text))
        assertFalse(WhatsAppVaultPolicy.isTextTarget(null, text))
        assertFalse(WhatsAppVaultPolicy.isTextTarget("com.whatsapp", InputType.TYPE_CLASS_NUMBER))
    }
    @Test fun searchMetadataIsOptionalAndExplicit() {
        assertTrue(WhatsAppVaultPolicy.reportsSearch(text, EditorInfo.IME_ACTION_SEARCH))
        assertTrue(WhatsAppVaultPolicy.reportsSearch(text or InputType.TYPE_TEXT_VARIATION_FILTER, EditorInfo.IME_ACTION_NONE))
        assertFalse(WhatsAppVaultPolicy.reportsSearch(text, EditorInfo.IME_ACTION_SEND))
    }
    @Test fun rememberedFieldsSurviveIdChangesAndUnknownIdsFailClosed() {
        val known = WhatsAppVaultPolicy.fieldTokens(0x7f010010, text, EditorInfo.IME_ACTION_SEARCH)
        assertTrue(WhatsAppVaultPolicy.guarded(known, 0x7f010010, text, EditorInfo.IME_ACTION_DONE))
        assertTrue(WhatsAppVaultPolicy.guarded(known, 0x7f010011, text, EditorInfo.IME_ACTION_SEARCH))
        assertFalse(WhatsAppVaultPolicy.guarded(known, 0x7f010012, text, EditorInfo.IME_ACTION_SEND))
        assertTrue(WhatsAppVaultPolicy.guarded(known, 0, text, EditorInfo.IME_ACTION_SEND))
        val unknown = WhatsAppVaultPolicy.fieldTokens(0, text, EditorInfo.IME_ACTION_NONE)
        assertTrue(WhatsAppVaultPolicy.guarded(unknown, 0x7f010012, text, EditorInfo.IME_ACTION_SEND))
        assertFalse(WhatsAppVaultPolicy.guarded(emptySet(), 0, text, EditorInfo.IME_ACTION_SEND))
    }
}
