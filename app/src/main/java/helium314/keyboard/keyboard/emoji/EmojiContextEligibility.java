// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji;

import android.text.InputType;
import android.view.inputmethod.EditorInfo;

/** Strict, local-only eligibility gate before reading the active editor's text. */
final class EmojiContextEligibility {
    private EmojiContextEligibility() {}

    static boolean isEligible(final EditorInfo editorInfo, final boolean incognito) {
        return !incognito && editorInfo != null
                && isEligibleInput(editorInfo.inputType, editorInfo.imeOptions);
    }

    static boolean isEligibleInput(final int inputType, final int imeOptions) {
        if ((imeOptions & EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING) != 0
                || (inputType & InputType.TYPE_MASK_CLASS) != InputType.TYPE_CLASS_TEXT
                || (inputType & InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS) != 0) {
            return false;
        }
        final int variation = inputType & InputType.TYPE_MASK_VARIATION;
        // Exclude passwords, email, URIs, usernames, search/code fields, and any
        // unrecognized variants. Only conventional prose/chat accepts context.
        return variation == InputType.TYPE_TEXT_VARIATION_NORMAL
                || variation == InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE
                || variation == InputType.TYPE_TEXT_VARIATION_LONG_MESSAGE;
    }
}
