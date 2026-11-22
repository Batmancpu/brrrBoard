// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.suggestions;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import android.view.inputmethod.EditorInfo;
import org.junit.After;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 28)
public final class ChatGptVoiceSuggestionStabilityTest {
    @After public void resetMetrics() { ChatGptVoiceSuggestionStability.reset(); }

    @Test public void chatGptVoiceGrowthHoldsSuggestionsUntilTheTranscriptSettles() {
        final EditorInfo editor = editor("com.openai.chatgpt");
        assertTrue(ChatGptVoiceSuggestionStability.noteSelectionUpdate(editor,
                20, 28, 28, 1_000));
        ChatGptVoiceSuggestionStability.recordDeferredSuggestion();
        assertTrue(ChatGptVoiceSuggestionStability.shouldHold(editor, 1_259));
        assertFalse(ChatGptVoiceSuggestionStability.shouldHold(editor, 1_260));
        assertEquals(1, ChatGptVoiceSuggestionStability.snapshot().hostEditCount);
        assertEquals(1, ChatGptVoiceSuggestionStability.snapshot().deferredSuggestionCount);
    }

    @Test public void localKeyboardTypingAndOtherAppsNeverEnterTheVoiceGate() {
        final EditorInfo chatGpt = editor("com.openai.chatgpt");
        ChatGptVoiceSuggestionStability.noteKeyboardInput(1_000);
        assertFalse(ChatGptVoiceSuggestionStability.noteSelectionUpdate(chatGpt,
                20, 28, 28, 1_300));
        assertFalse(ChatGptVoiceSuggestionStability.noteSelectionUpdate(editor("example.other"),
                20, 28, 28, 2_000));
        assertEquals(0, ChatGptVoiceSuggestionStability.snapshot().hostEditCount);
    }

    @Test public void selectionReplacementsAndNonCollapsedSelectionsAreIgnored() {
        final EditorInfo chatGpt = editor("com.openai.chatgpt");
        assertFalse(ChatGptVoiceSuggestionStability.noteSelectionUpdate(chatGpt,
                40, 22, 22, 1_000));
        assertFalse(ChatGptVoiceSuggestionStability.noteSelectionUpdate(chatGpt,
                20, 28, 27, 2_000));
    }

    private static EditorInfo editor(String packageName) {
        final EditorInfo editor = new EditorInfo();
        editor.packageName = packageName;
        return editor;
    }
}
