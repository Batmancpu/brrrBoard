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
}
