// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.view.inputmethod.EditorInfo
import helium314.keyboard.latin.utils.InputTypeUtils

/** Applies one policy to collection, history panels, and clipboard suggestions. */
object ClipboardPrivacyPolicy {
    @JvmStatic
    fun isPrivateEditor(editor: EditorInfo?, incognito: Boolean): Boolean {
        if (incognito) return true
        // A newly created IME has no trusted editor yet. Never ingest the
        // system clipboard merely because the keyboard service was started.
        val input = editor ?: return true
        return InputTypeUtils.isAnyPasswordInputType(input.inputType) ||
            input.imeOptions and EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING != 0
    }

    @JvmStatic
    fun mayCollect(editor: EditorInfo?, incognito: Boolean, sensitiveClip: Boolean): Boolean =
        !sensitiveClip && !isPrivateEditor(editor, incognito)
}
