// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.Test;

/** No Android state, model, network or modified Emoji Unicode sequences. */
public class EmojiSkinToneSearchPolicyTest {
    private static final Set<String> CORPUS = Set.of("👍", "👍🏻", "👍🏼", "👍🏽", "👍🏾", "👍🏿", "🖤", "💡");
    private static final Map<String, List<String>> FAMILIES = EmojiSkinToneSearchPolicy.families(CORPUS);

    @Test public void defaultCollapsesEvenWhenAShadeRankedFirst() {
        final List<String> result = EmojiSkinToneSearchPolicy.select(
                EmojiSkinToneSearchPolicy.parse("thumbs up"),
                List.of("👍🏿", "👍🏼", "👍", "💡", "👍🏻"), CORPUS, FAMILIES, 8);
        assertEquals(List.of("👍", "💡"), result);
    }

    @Test public void requestedToneWorksAtEitherEndAndNeverInventsSequences() {
        final List<String> ranked = List.of("👍", "👍🏻", "👍🏼", "👍🏽", "👍🏾", "👍🏿", "🖤");
        for (final String query : List.of("dark thumbs up", "thumbs up dark skin", "thumbs up black")) {
            final var request = EmojiSkinToneSearchPolicy.parse(query);
            assertEquals("thumbs up", request.lookup);
            assertEquals(List.of("👍🏿"), EmojiSkinToneSearchPolicy.select(
                    request, ranked, CORPUS, FAMILIES, 8));
        }
        assertEquals(List.of("👍"), EmojiSkinToneSearchPolicy.select(
                EmojiSkinToneSearchPolicy.parse("yellow thumbs up"), ranked, CORPUS, FAMILIES, 8));
        assertFalse(FAMILIES.containsKey("🖤"));
    }

    @Test public void allShadesOnlyWhenTheUserAsksForThem() {
        final var request = EmojiSkinToneSearchPolicy.parse("all the different thumbs up skin tones");
        assertEquals(EmojiSkinToneSearchPolicy.Mode.ALL, request.mode);
        final List<String> variants = EmojiSkinToneSearchPolicy.select(request,
                List.of("👍", "🖤"), CORPUS, FAMILIES, 10);
        assertEquals(List.of("👍", "👍🏻", "👍🏼", "👍🏽", "👍🏾", "👍🏿"), variants);
    }

    @Test public void variationSelectorsAndJoinersStayInTheFamilyKey() {
        assertEquals("🧑‍⚕️", EmojiSkinToneSearchPolicy.family("🧑🏿‍⚕️"));
        assertEquals("🫱‍🫲", EmojiSkinToneSearchPolicy.family("🫱🏻‍🫲🏿"));
        assertEquals("❤️", EmojiSkinToneSearchPolicy.family("❤️"));
        assertEquals(EmojiSkinToneSearchPolicy.Mode.DEFAULT,
                EmojiSkinToneSearchPolicy.parse("black").mode);
        assertTrue(EmojiSkinToneSearchPolicy.parse("black heart").explicitlyRequestedTone());
    }
}
