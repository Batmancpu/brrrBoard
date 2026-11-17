// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import java.util.List;

public class EmojiContextScorerTest {
    @Test public void thirtyPositiveContextExamplesHaveExpectedFirstCandidate() {
        final String[][] examples = {
                {"happy birthday", "🎂"}, {"Happy birthday!!", "🎂"},
                {"birthday party", "🎉"}, {"congrats", "🎉"},
                {"congratulations", "🎉"}, {"well done", "👏"},
                {"great job", "👏"}, {"good luck", "🍀"},
                {"good night", "🌙"}, {"sweet dreams", "😴"},
                {"sleep well", "😴"}, {"i love you", "❤️"},
                {"miss you", "🥺"}, {"big hug", "🤗"},
                {"thanks a lot", "🙏"}, {"thank you", "🙏"},
                {"so funny", "😂"}, {"rofl", "🤣"},
                {"heartbroken", "💔"}, {"feeling sick", "🤒"},
                {"pizza", "🍕"}, {"coffee", "☕"},
                {"movie night", "🍿"}, {"football match", "⚽"},
                {"cricket match", "🏏"}, {"beautiful sunset", "🌅"},
                {"happy diwali", "🪔"}, {"yaar mast", "😎"},
                {"shukriya", "🙏"}, {"happy new year", "🎆"},
        };
        assertEquals(30, examples.length);
        for (final String[] example : examples) {
            final List<EmojiContextScorer.Candidate> result = EmojiContextScorer.rank(example[0], false);
            assertFalse(example[0], result.isEmpty());
            assertEquals(example[0], example[1], result.get(0).emoji);
        }
    }

    @Test public void thirtyNegativeAndAmbiguousContextsDoNotProduceNoise() {
        final String[] examples = {
                "", " ", "some words", "work project", "schedule meeting",
                "birthdayparty", "congratulationss", "good nightshift", "good morning meeting",
                "please share the movie", "we talked about a match", "coffeehouse", "pizza hut booking",
                "i am sick of bugs", "heavy rainfall map", "the word lovebirds", "mysterious firewalls",
                "he said rofling", "not a cake", "no context here", "id 1234",
                "me@example.com", "https://example.com", "file/path", "x=y", "foo:bar",
                "JavaClassName", "the quick brown fox", "yaarmast", "username123",
        };
        assertEquals(30, examples.length);
        for (final String example : examples) {
            assertTrue(example, EmojiContextScorer.rank(example, false).isEmpty());
        }
    }

    @Test public void sensitiveFieldsAndUntrustedLongInputsAlwaysRefuse() {
        assertTrue(EmojiContextScorer.rank("happy birthday", true).isEmpty());
        assertTrue(EmojiContextScorer.rank("my password: love you", false).isEmpty());
        assertTrue(EmojiContextScorer.rank("x".repeat(4097) + " happy birthday", false).isEmpty());
    }

    @Test public void multiPhraseCandidatesAreStableDistinctAndImmutable() {
        final List<EmojiContextScorer.Candidate> results = EmojiContextScorer.rank(
                "I wish you a happy birthday!", false);
        assertEquals(2, results.size());
        assertEquals("🎂", results.get(0).emoji);
        assertEquals("🎉", results.get(1).emoji);
        assertTrue(results.get(0).confidence > results.get(1).confidence);
        assertEquals("happy birthday", results.get(0).matchedTrigger);
        assertEquals(results.get(0).emoji,
                EmojiContextScorer.rank("I wish you a happy birthday!", false).get(0).emoji);
    }
}
