// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.inputlogic;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class EmojiSuggestionInsertPolicyTest {
    @Test public void appendsAfterComposingWordWithoutReplacingIt() {
        assertTrue(EmojiSuggestionInsertPolicy.needsSpace("happy", true));
        assertTrue(EmojiSuggestionInsertPolicy.needsSpace("happy birthday", true));
        assertTrue(EmojiSuggestionInsertPolicy.needsSpace("नमस्ते", true));
    }

    @Test public void neverDoublesSpaceOrAddsLeadingSpace() {
        assertFalse(EmojiSuggestionInsertPolicy.needsSpace("happy ", true));
        assertFalse(EmojiSuggestionInsertPolicy.needsSpace("", true));
        assertFalse(EmojiSuggestionInsertPolicy.needsSpace(null, true));
        assertFalse(EmojiSuggestionInsertPolicy.needsSpace("(", true));
        assertFalse(EmojiSuggestionInsertPolicy.needsSpace("wow!", true));
    }

    @Test public void honorsEditorSpacingPolicy() {
        assertFalse(EmojiSuggestionInsertPolicy.needsSpace("happy", false));
    }
}
