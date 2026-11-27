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

    @Test public void identicalFinalResultsReuseOnlyAnIntactCurrentSurface() {
        assertTrue(EmojiSearchSurfacePolicy.canReuseResults(true,false,true,false,true,true,true));
        assertFalse(EmojiSearchSurfacePolicy.canReuseResults(true,true,true,false,true,true,true));
        assertFalse(EmojiSearchSurfacePolicy.canReuseResults(true,false,true,true,true,true,true));
        assertFalse(EmojiSearchSurfacePolicy.canReuseResults(true,false,true,false,false,true,true));
        assertFalse(EmojiSearchSurfacePolicy.canReuseResults(true,false,true,false,true,false,true));
        assertFalse(EmojiSearchSurfacePolicy.canReuseResults(true,false,true,false,true,true,false));
        assertFalse(EmojiSearchSurfacePolicy.canReuseResults(false,false,true,false,true,true,true));
        assertFalse(EmojiSearchSurfacePolicy.canReuseResults(true,false,false,false,true,true,true));
    }
    @Test public void replacementStartsWithComplementaryOpacity() {
        org.junit.Assert.assertEquals(0f,EmojiSearchSurfacePolicy.replacementStartAlpha(1f),0f);
        org.junit.Assert.assertEquals(.75f,EmojiSearchSurfacePolicy.replacementStartAlpha(.25f),.001f);
        org.junit.Assert.assertEquals(1f,EmojiSearchSurfacePolicy.replacementStartAlpha(-1f),0f);
    }
    @Test public void searchShowsExactlyOneOfResultsOrEmptyState() {
        assertTrue(EmojiSearchSurfacePolicy.resultsVisible(true, false));
        assertFalse(EmojiSearchSurfacePolicy.emptyVisible(true, false));
        assertFalse(EmojiSearchSurfacePolicy.resultsVisible(true, true));
        assertTrue(EmojiSearchSurfacePolicy.emptyVisible(true, true));
    }
}
