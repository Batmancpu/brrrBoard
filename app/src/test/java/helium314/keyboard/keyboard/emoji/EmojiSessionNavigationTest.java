// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

/** Content-free navigation contracts, independent of stored user Emoji history. */
public class EmojiSessionNavigationTest {
    @Test public void ordinaryNewSessionStartsInRecentsWhenAvailable() {
        assertEquals(EmojiCategory.ID_RECENTS,
                EmojiCategory.sessionStartCategory(false, true, false));
    }

    @Test public void smartPicksCanBeFirstWithoutPriorRecents() {
        assertEquals(EmojiCategory.ID_RECENTS,
                EmojiCategory.sessionStartCategory(false, false, true));
    }

    @Test public void emptyRecentsAndNoSmartPicksUseUsableEmojiFallback() {
        assertEquals(1, EmojiCategory.sessionStartCategory(false, false, false));
    }

    @Test public void privateEditorNeverOpensPreviousEditorsRecents() {
        assertEquals(1, EmojiCategory.sessionStartCategory(true, true, true));
    }

    @Test public void staleDetachedPagerCannotRestorePreviousSessionScroll() {
        assertTrue(EmojiCategoryPagerAdapter.acceptsScrollMemory(4L, 4L));
        assertFalse(EmojiCategoryPagerAdapter.acceptsScrollMemory(4L, 5L));
    }

    @Test public void togglingAbcAndEmojiKeepsPositionUntilNewEditorSession() {
        final EmojiNavigationSession session = new EmojiNavigationSession();
        assertTrue(session.consumeResetOnOpen());
        assertFalse(session.consumeResetOnOpen());
        assertFalse(session.consumeResetOnOpen());
        session.beginNewEditorSession();
        assertTrue(session.consumeResetOnOpen());
        assertFalse(session.consumeResetOnOpen());
    }
}
