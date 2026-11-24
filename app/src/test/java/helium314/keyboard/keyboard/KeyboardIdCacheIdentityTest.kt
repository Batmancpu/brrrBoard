// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.keyboard

import android.text.InputType
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodSubtype
import helium314.keyboard.latin.RichInputMethodSubtype
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class KeyboardIdCacheIdentityTest {
    private fun id(numberRowInSymbols: Boolean = false, oneHanded: Boolean = false,
                   emojiSearch: Boolean = false): KeyboardId {
        val params = KeyboardLayoutSet.Params()
        params.mEditorInfo = EditorInfo().apply { inputType = InputType.TYPE_CLASS_TEXT }
        params.mMode = KeyboardId.MODE_TEXT
        params.mKeyboardWidth = 1080
        params.mKeyboardHeight = 460
        params.mSubtype = RichInputMethodSubtype.get(
            InputMethodSubtype.InputMethodSubtypeBuilder()
                .setSubtypeLocale("en_US").setSubtypeMode("keyboard")
                .setIsAsciiCapable(true).build())
        params.mNumberRowInSymbols = numberRowInSymbols
        params.mOneHandedModeEnabled = oneHanded
        params.mEmojiSearchAvailable = emojiSearch
        return KeyboardId(KeyboardId.ELEMENT_SYMBOLS, params)
    }

    @Test fun layoutVariantsCannotReuseTheWrongCachedKeyboard() {
        val base = id()
        val same = id()
        assertEquals(base, same)
        assertEquals(base.hashCode(), same.hashCode())
        val variants = listOf(id(numberRowInSymbols = true), id(oneHanded = true),
            id(emojiSearch = true))
        for (variant in variants) {
            assertNotEquals(base, variant)
            assertNotEquals(variant, base)
        }
        val cache = LayoutRetentionCache<KeyboardId, String>(8, 2)
        cache.put(base, "original", true)
        variants.forEachIndexed { index, key -> cache.put(key, "variant-$index", false) }
        assertEquals("original", cache.get(same, true))
        variants.forEachIndexed { index, key -> assertEquals("variant-$index", cache.get(key, false)) }
        assertEquals(4, cache.size())
    }
}
