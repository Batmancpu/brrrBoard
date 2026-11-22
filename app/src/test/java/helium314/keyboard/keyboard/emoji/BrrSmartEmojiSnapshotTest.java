// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji;

import org.junit.Assert;
import org.junit.Test;

import java.util.List;

public class BrrSmartEmojiSnapshotTest {
    @Test public void snapshotPreservesRankAndCapsTheRenderedPicks() {
        final List<BrrSmartEmojiEngine.Pick> picks =
                BrrSmartEmojiEngine.picksFromSnapshot(List.of("🎂", "🎉", "💛"), 2);

        Assert.assertEquals(2, picks.size());
        Assert.assertEquals("🎂", picks.get(0).emoji);
        Assert.assertEquals("🎉", picks.get(1).emoji);
        Assert.assertTrue(picks.get(0).confidence > picks.get(1).confidence);
    }

    @Test public void emptySnapshotDoesNotCreateGenericRecommendations() {
        Assert.assertTrue(BrrSmartEmojiEngine.picksFromSnapshot(List.of(), 5).isEmpty());
        Assert.assertTrue(BrrSmartEmojiEngine.picksFromSnapshot(List.of("🎂"), 0).isEmpty());
    }
}
