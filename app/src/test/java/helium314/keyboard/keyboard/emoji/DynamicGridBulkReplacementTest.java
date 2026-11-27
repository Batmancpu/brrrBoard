// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard.emoji;

import helium314.keyboard.keyboard.Key;
import helium314.keyboard.keyboard.Keyboard;
import helium314.keyboard.keyboard.KeyboardId;
import helium314.keyboard.keyboard.KeyboardLayoutSet;
import helium314.keyboard.keyboard.internal.KeyboardBuilder;
import helium314.keyboard.keyboard.internal.KeyboardParams;
import helium314.keyboard.keyboard.internal.keyboard_parser.EmojiParserKt;
import helium314.keyboard.latin.LatinIME;
import helium314.keyboard.latin.RichInputMethodSubtype;
import helium314.keyboard.latin.utils.KtxKt;
import java.util.ArrayList;
import java.util.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.Robolectric;
import org.robolectric.RobolectricTestRunner;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
public class DynamicGridBulkReplacementTest {
    @Test public void bulkMatchesSequentialGridIncludingDuplicatesAndPopupKeys() {
        final LatinIME ime = Robolectric.setupService(LatinIME.class);
        final int width = 900;
        final Keyboard template = new KeyboardLayoutSet.Builder(ime, null)
                .setSubtype(RichInputMethodSubtype.Companion.getEmojiSubtype())
                .setKeyboardGeometry(width, 320).build().getKeyboard(KeyboardId.ELEMENT_EMOJI_RECENTS);
        final DynamicGridKeyboard sequential = DynamicGridKeyboard.ofPagedRowCount(
                KtxKt.prefs(ime), template, 1, KeyboardId.ELEMENT_EMOJI_CATEGORY16, width * 2, width);
        final DynamicGridKeyboard bulk = DynamicGridKeyboard.ofPagedRowCount(
                KtxKt.prefs(ime), template, 1, KeyboardId.ELEMENT_EMOJI_CATEGORY16, width * 2, width);
        final KeyboardBuilder builder = new KeyboardBuilder(ime, new KeyboardParams());
        builder.load(bulk.mId);
        final kotlin.Pair<Float, Float> dims = EmojiParserKt.getEmojiKeyDimensions(builder.mParams, ime);
        final List<Key> keys = new ArrayList<>();
        for (String label : List.of("😀", "👍", "👍🏻", "👨‍👩‍👧", "😀", "🎉", "🌟", "🎂", "🎈", "🌈", "💙", "🚀")) {
            final String popup = EmojiParserKt.getEmojiPopupSpec(label);
            final Key.KeyParams params = new Key.KeyParams(label, EmojiParserKt.getCode(label),
                    popup != null ? EmojiParserKt.EMOJI_HINT_LABEL : null, popup,
                    Key.LABEL_FLAGS_FONT_NORMAL, builder.mParams);
            params.mAbsoluteWidth = dims.getFirst();
            params.mAbsoluteHeight = dims.getSecond();
            keys.add(params.createKey());
        }
        for (Key key : keys) sequential.addKeyLast(key);
        bulk.replaceSearchKeys(keys);
        final List<Key> expected = sequential.getSortedKeys();
        final List<Key> actual = bulk.getSortedKeys();
        assertFalse(expected.isEmpty());
        assertEquals(expected.size(), actual.size());
        for (int i = 0; i < expected.size(); i++) {
            final Key a = expected.get(i), b = actual.get(i);
            assertEquals(a.getLabel(), b.getLabel());
            assertEquals(a.getOutputText(), b.getOutputText());
            assertEquals(a.getHintLabel(), b.getHintLabel());
            assertEquals(a.getCode(), b.getCode());
            assertEquals(a.getX(), b.getX());
            assertEquals(a.getY(), b.getY());
            assertEquals(a.getWidth(), b.getWidth());
            assertEquals(a.getHeight(), b.getHeight());
            assertEquals(a.getPopupKeys() == null ? 0 : a.getPopupKeys().length,
                    b.getPopupKeys() == null ? 0 : b.getPopupKeys().length);
        }
        bulk.replaceSearchKeys(List.of());
        assertTrue(bulk.getSortedKeys().isEmpty());
    }
}
