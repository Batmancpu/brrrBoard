// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.text.InputType;
import android.view.inputmethod.EditorInfo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public class EmojiContextEligibilityTest {
    @Test public void onlyOrdinaryProseAndChatAreEligible() {
        assertTrue(EmojiContextEligibility.isEligibleInput(InputType.TYPE_CLASS_TEXT, 0));
        assertTrue(EmojiContextEligibility.isEligibleInput(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE, 0));
        assertTrue(EmojiContextEligibility.isEligibleInput(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_LONG_MESSAGE, 0));
        assertFalse(EmojiContextEligibility.isEligibleInput(InputType.TYPE_CLASS_NUMBER, 0));
        assertFalse(EmojiContextEligibility.isEligibleInput(InputType.TYPE_CLASS_PHONE, 0));
        assertFalse(EmojiContextEligibility.isEligibleInput(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_PASSWORD, 0));
        assertFalse(EmojiContextEligibility.isEligibleInput(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD, 0));
        assertFalse(EmojiContextEligibility.isEligibleInput(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD, 0));
        assertFalse(EmojiContextEligibility.isEligibleInput(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS, 0));
        assertFalse(EmojiContextEligibility.isEligibleInput(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_URI, 0));
        assertFalse(EmojiContextEligibility.isEligibleInput(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_NO_SUGGESTIONS, 0));
        assertFalse(EmojiContextEligibility.isEligibleInput(InputType.TYPE_CLASS_TEXT,
                EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING));
    }

    @Test public void incognitoAndMissingEditorAlwaysRefuse() {
        final EditorInfo editor = new EditorInfo();
        editor.inputType = InputType.TYPE_CLASS_TEXT;
        assertTrue(EmojiContextEligibility.isEligible(editor, false));
        assertFalse(EmojiContextEligibility.isEligible(editor, true));
        assertFalse(EmojiContextEligibility.isEligible(null, false));
    }
}
