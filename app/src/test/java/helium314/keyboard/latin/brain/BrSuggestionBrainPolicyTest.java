// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.brain;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.text.InputType;
import android.view.inputmethod.EditorInfo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;

@RunWith(RobolectricTestRunner.class)
public final class BrSuggestionBrainPolicyTest {
    @Test public void normalChatCanUseTheSharedBrainPolicy() {
        final EditorInfo editor = new EditorInfo();
        editor.inputType = InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE;

        assertTrue(BrSuggestionBrain.isPrivacyEligible(editor, false, false, true));
    }

    @Test public void privateAndNonProseEditorsFailClosed() {
        final EditorInfo chat = new EditorInfo();
        chat.inputType = InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_SHORT_MESSAGE;
        final EditorInfo password = new EditorInfo();
        password.inputType = InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_VARIATION_PASSWORD;
        final EditorInfo noLearning = new EditorInfo();
        noLearning.inputType = InputType.TYPE_CLASS_TEXT;
        noLearning.imeOptions = EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING;

        assertFalse(BrSuggestionBrain.isPrivacyEligible(chat, true, false, true));
        assertFalse(BrSuggestionBrain.isPrivacyEligible(chat, false, true, true));
        assertFalse(BrSuggestionBrain.isPrivacyEligible(chat, false, false, false));
        assertFalse(BrSuggestionBrain.isPrivacyEligible(password, false, false, true));
        assertFalse(BrSuggestionBrain.isPrivacyEligible(noLearning, false, false, true));
        assertFalse(BrSuggestionBrain.isPrivacyEligible(null, false, false, true));
    }
}
