// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji;

import org.junit.Test;
import java.util.List;
import static org.junit.Assert.*;

public class BrrEmojiPatternModelTest {
    private static final long NOW = 1_800_000_000_000L;

    @Test public void learnsRomanHindiAndEnglishPhrasingInScope() {
        final BrrEmojiPatternModel model = new BrrEmojiPatternModel();
        final String chat = BrrEmojiPatternModel.appScope("com.example.chat", 64);
        final String work = BrrEmojiPatternModel.appScope("com.example.work", 64);
        model.learn(chat, "kal office jaana hai", "😴", NOW);
        model.learn(chat, "kal office jaana hai", "😴", NOW + 1000);
        model.learn(work, "kal office jaana hai", "💼", NOW + 2000);
        final List<BrrEmojiPatternModel.Prediction> chatRank = model.predict(chat,
                "kal office jaana hai", NOW + 3000);
        final List<BrrEmojiPatternModel.Prediction> workRank = model.predict(work,
                "kal office jaana hai", NOW + 3000);
        assertEquals("😴", chatRank.get(0).emoji);
        assertEquals("💼", workRank.get(0).emoji);
        assertTrue(chatRank.get(0).confidence >= 500);
    }

    @Test public void neverPersistsRawSentenceOrPackageName() {
        final BrrEmojiPatternModel model = new BrrEmojiPatternModel();
        model.learn(BrrEmojiPatternModel.appScope("com.secret.chat", 0),
                "personal private jaana kal", "❤️", NOW);
        final String data = model.serialize(NOW);
        assertFalse(data.contains("private"));
        assertFalse(data.contains("jaana"));
        assertFalse(data.contains("com.secret.chat"));
        final BrrEmojiPatternModel reloaded = new BrrEmojiPatternModel();
        reloaded.deserialize(data, NOW + 100);
        assertEquals("❤️", reloaded.predict(BrrEmojiPatternModel.appScope("com.secret.chat", 0),
                "personal private jaana kal", NOW + 100).get(0).emoji);
    }

    @Test public void sameAppDifferentFieldsHaveDistinctContextScopes() {
        final String message = BrrEmojiPatternModel.appScope("com.example.app#101", 64);
        final String notes = BrrEmojiPatternModel.appScope("com.example.app#202", 64);
        assertNotEquals(message, notes);
        final BrrEmojiPatternModel model = new BrrEmojiPatternModel();
        model.learn(message, "I am going home", "🏠", NOW);
        model.learn(notes, "I am going home", "💼", NOW);
        assertEquals("🏠", model.predict(message, "I am going home", NOW + 1).get(0).emoji);
        assertEquals("💼", model.predict(notes, "I am going home", NOW + 1).get(0).emoji);
    }

    @Test public void refusesCodesAndOldPatterns() {
        final BrrEmojiPatternModel model = new BrrEmojiPatternModel();
        model.learn("", "https://example.com private", "🔥", NOW);
        model.learn("", "mail@example.com private", "🔥", NOW);
        assertEquals(0, model.size());
        model.learn("", "happy birthday", "🎂", NOW - BrrEmojiPatternModel.MAX_AGE_MS - 1);
        assertTrue(model.predict("", "happy birthday", NOW).isEmpty());
        model.serialize(NOW);
        assertEquals(0, model.size());
    }

    @Test public void boundedAndMalformedPersistedEntriesIgnored() {
        final BrrEmojiPatternModel model = new BrrEmojiPatternModel();
        for (int i = 0; i < 200; ++i) {
            final String varied = "hello hello word" + (char) ('a' + i % 26)
                    + (char) ('a' + i / 26);
            model.learn("", varied, "🎉", NOW);
        }
        assertTrue(model.size() <= BrrEmojiPatternModel.MAX_RECORDS);
        model.deserialize("v1\nnot-valid\t10000\tno-time\n", NOW);
        assertEquals(0, model.size());
    }
}
