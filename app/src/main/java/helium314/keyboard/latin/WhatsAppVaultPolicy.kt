// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.content.SharedPreferences
import android.text.InputType
import android.view.inputmethod.EditorInfo

/** Package-exact scope and conservative, content-free field protection. Never inspect the secret or host text. */
object WhatsAppVaultPolicy {
    const val PACKAGE = "com.whatsapp"
    const val PREF_CONFIRMED = "brr_vault_whatsapp_confirmed_v1"
    const val PREF_SEARCH_ONLY = "brr_vault_whatsapp_search_only_v1"
    private const val PREF_GUARDED_FIELDS = "brr_vault_whatsapp_private_fields_v1"
    private const val ALL = "*"

    @JvmStatic fun isTextTarget(packageName: String?, inputType: Int): Boolean =
        packageName == PACKAGE && inputType and InputType.TYPE_MASK_CLASS == InputType.TYPE_CLASS_TEXT

    @JvmStatic fun reportsSearch(inputType: Int, imeOptions: Int): Boolean =
        imeOptions and EditorInfo.IME_MASK_ACTION == EditorInfo.IME_ACTION_SEARCH ||
            inputType and InputType.TYPE_MASK_VARIATION == InputType.TYPE_TEXT_VARIATION_FILTER

    @JvmStatic fun eligible(prefs: SharedPreferences, editor: EditorInfo?): Boolean =
        editor != null && isTextTarget(editor.packageName, editor.inputType) &&
            prefs.getBoolean(VaultGestureStore.PREF_ENABLED, false) &&
            prefs.getBoolean(PREF_CONFIRMED, false) &&
            (!prefs.getBoolean(PREF_SEARCH_ONLY, false) || reportsSearch(editor.inputType, editor.imeOptions))

    // A resource-backed view id is more reliable than a generated id. Also remember
    // class/variation/action in case WhatsApp changes resource ids on an update.
    internal fun fieldTokens(fieldId: Int, inputType: Int, imeOptions: Int): Set<String> {
        if (fieldId < 0x01000000) return setOf(ALL)
        return setOf("id:$fieldId",
            "type:${inputType and (InputType.TYPE_MASK_CLASS or InputType.TYPE_MASK_VARIATION)}:${imeOptions and EditorInfo.IME_MASK_ACTION}")
    }

    internal fun guarded(tokens: Set<String>, fieldId: Int, inputType: Int, imeOptions: Int): Boolean =
        ALL in tokens || fieldTokens(fieldId, inputType, imeOptions).any { it in tokens } ||
            (tokens.isNotEmpty() && fieldId < 0x01000000)

    @JvmStatic fun isPrivateEditor(prefs: SharedPreferences, editor: EditorInfo?): Boolean {
        if (editor == null || !isTextTarget(editor.packageName, editor.inputType)) return false
        if (eligible(prefs, editor) && reportsSearch(editor.inputType, editor.imeOptions)) return true
        return guarded(prefs.getStringSet(PREF_GUARDED_FIELDS, emptySet()).orEmpty(),
            editor.fieldId, editor.inputType, editor.imeOptions)
    }

    /** Persist the guard BEFORE committing any code. Unknown IDs fail closed across WhatsApp. */
    @JvmStatic fun protectBeforeInsert(prefs: SharedPreferences, editor: EditorInfo?): Boolean {
        if (editor == null || !eligible(prefs, editor)) return false
        val old = prefs.getStringSet(PREF_GUARDED_FIELDS, emptySet()).orEmpty()
        val combined = old + fieldTokens(editor.fieldId, editor.inputType, editor.imeOptions)
        val next = if (ALL in combined || combined.size > 32) setOf(ALL) else combined
        return prefs.edit().putStringSet(PREF_GUARDED_FIELDS, next).commit()
    }

    fun hasRememberedFields(prefs: SharedPreferences): Boolean =
        !prefs.getStringSet(PREF_GUARDED_FIELDS, emptySet()).isNullOrEmpty()
    fun clearRememberedFields(prefs: SharedPreferences): Boolean =
        prefs.edit().remove(PREF_GUARDED_FIELDS).commit()
}
