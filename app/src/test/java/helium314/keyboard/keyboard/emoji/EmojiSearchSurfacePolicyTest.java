// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

public class EmojiSearchSurfacePolicyTest {
    @Test public void bothFixedSearchRowsAreGoneOutsideSearchMode() {
        assertFalse(EmojiSearchSurfacePolicy.resultsVisible(false, false));
        assertFalse(EmojiSearchSurfacePolicy.resultsVisible(false, true));
        assertFalse(EmojiSearchSurfacePolicy.emptyVisible(false, false));
        assertFalse(EmojiSearchSurfacePolicy.emptyVisible(false, true));
    }

    @Test public void initialSearchLayoutNeverSnapshotsAnUnpresentedResultRow() {
        assertFalse(EmojiSearchSurfacePolicy.shouldMorphResults(true, true, true, true));
        assertFalse(EmojiSearchSurfacePolicy.shouldMorphResults(false, false, true, true));
        assertFalse(EmojiSearchSurfacePolicy.shouldMorphResults(true, false, false, true));
        assertFalse(EmojiSearchSurfacePolicy.shouldMorphResults(true, false, true, false));
        assertTrue(EmojiSearchSurfacePolicy.shouldMorphResults(true, false, true, true));
    }

    @Test public void searchShowsExactlyOneOfResultsOrEmptyState() {
        assertTrue(EmojiSearchSurfacePolicy.resultsVisible(true, false));
        assertFalse(EmojiSearchSurfacePolicy.emptyVisible(true, false));
        assertFalse(EmojiSearchSurfacePolicy.resultsVisible(true, true));
        assertTrue(EmojiSearchSurfacePolicy.emptyVisible(true, true));
    }
}
