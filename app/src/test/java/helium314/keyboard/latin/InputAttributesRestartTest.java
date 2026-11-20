// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin;

import android.text.InputType;
import android.view.inputmethod.EditorInfo;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public class InputAttributesRestartTest {
    private static EditorInfo editor() {
        final EditorInfo info = new EditorInfo();
        info.inputType = InputType.TYPE_CLASS_TEXT;
        info.imeOptions = EditorInfo.IME_ACTION_SEND;
        info.packageName = "test.editor";
        info.fieldId = 1;
        return info;
    }

    @Test public void mutatedEditorInfoIsComparedAgainstImmutableSnapshot() {
        final EditorInfo info = editor();
        final InputAttributes initial = new InputAttributes(info, false, null);
        assertTrue(initial.isSameInputType(info));
        info.imeOptions = EditorInfo.IME_ACTION_SEARCH;
        assertFalse(initial.isSameInputType(info));
        info.imeOptions = EditorInfo.IME_ACTION_SEND | EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING;
        assertFalse(initial.isSameInputType(info));
    }

    @Test public void fieldChangeAndPrivateOptionsTriggerReconfiguration() {
        final EditorInfo info = editor();
        final InputAttributes initial = new InputAttributes(info, false, null);
        info.fieldId = 2;
        assertFalse(initial.isSameInputType(info));
        info.fieldId = 1;
        info.privateImeOptions = "test.editor.noMicrophoneKey";
        assertFalse(initial.isSameInputType(info));
    }

    @Test public void noLearningAppliesToNonProseFieldsToo() {
        final EditorInfo info = editor();
        info.inputType = InputType.TYPE_CLASS_NUMBER;
        info.imeOptions |= EditorInfo.IME_FLAG_NO_PERSONALIZED_LEARNING;
        assertTrue(new InputAttributes(info, false, null).mNoLearning);
    }

    @Test public void emailAndPasswordFieldsDoNotInsertAutomaticSpaces() {
        final EditorInfo info = editor();
        for (final int variation : new int[]{
                InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS,
                InputType.TYPE_TEXT_VARIATION_WEB_EMAIL_ADDRESS,
                InputType.TYPE_TEXT_VARIATION_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD,
                InputType.TYPE_TEXT_VARIATION_WEB_PASSWORD}) {
            info.inputType = InputType.TYPE_CLASS_TEXT | variation;
            final InputAttributes attributes = new InputAttributes(info, false, null);
            assertFalse(attributes.mShouldInsertSpacesAutomatically);
            assertFalse(attributes.mInputTypeShouldAutoCorrect);
            if (attributes.mIsPasswordField) {
                assertFalse(attributes.mShouldShowSuggestions);
                assertFalse(attributes.mShouldShowVoiceInputKey);
            }
        }
    }

    @Test public void uriFieldPreservesNormalTextSpacingButNotAutomaticCorrection() {
        // A URI editor may also contain ordinary words and quoted phrases.
        // InputLogicTest protects the conditional spaces after suggestions.
        final EditorInfo info = editor();
        info.inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_URI;
        final InputAttributes attributes = new InputAttributes(info, false, null);
        assertTrue(attributes.mShouldInsertSpacesAutomatically);
        assertFalse(attributes.mInputTypeShouldAutoCorrect);
    }

    @Test public void numberDecimalAndTypeNullNeverRunWordPrediction() {
        final EditorInfo info = editor();
        for (final int inputType : new int[]{InputType.TYPE_CLASS_NUMBER,
                InputType.TYPE_CLASS_NUMBER | InputType.TYPE_NUMBER_FLAG_DECIMAL,
                InputType.TYPE_NULL}) {
            info.inputType = inputType;
            final InputAttributes attributes = new InputAttributes(info, false, null);
            assertFalse(attributes.mShouldShowSuggestions);
            assertFalse(attributes.mInputTypeShouldAutoCorrect);
            assertFalse(attributes.mShouldShowVoiceInputKey);
        }
    }

    @Test public void appCompletionsAreRestrictedToFullscreenEditors() {
        final EditorInfo info = editor();
        info.inputType = InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_FLAG_AUTO_COMPLETE;
        assertFalse(new InputAttributes(info, false, null).mApplicationSpecifiedCompletionOn);
        assertTrue(new InputAttributes(info, true, null).mApplicationSpecifiedCompletionOn);
    }
}
