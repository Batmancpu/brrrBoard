// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.content.ClipData
import android.view.inputmethod.EditorInfo
import helium314.keyboard.compat.ClipboardManagerCompat
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

    /** Ambient text paths must never coerce provider URIs, HTML or intents into text. */
    fun boundedPlainText(clip: ClipData): String? {
        if (clip.itemCount != 1 || !clip.description.hasMimeType("text/plain") ||
            ClipboardManagerCompat.getClipSensitivity(clip.description) == true) return null
        val item = clip.getItemAt(0)
        if (item.uri != null || item.intent != null || item.htmlText != null) return null
        val value = item.text ?: return null
        return value.takeIf { it.length in 1..ClipboardEntityExtractor.MAX_CHARS && it.isNotBlank() }?.toString()
    }

    @JvmStatic
    fun mayCollect(editor: EditorInfo?, incognito: Boolean, sensitiveClip: Boolean): Boolean =
        !sensitiveClip && !isPrivateEditor(editor, incognito)
}
