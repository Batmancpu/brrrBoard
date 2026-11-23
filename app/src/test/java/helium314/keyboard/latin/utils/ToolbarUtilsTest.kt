// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.utils

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.test.core.app.ApplicationProvider
import helium314.keyboard.latin.common.Constants.Separators
import helium314.keyboard.latin.settings.Defaults
import helium314.keyboard.latin.settings.Settings
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ToolbarUtilsTest {
    private lateinit var prefs: SharedPreferences

    @BeforeTest
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        prefs = context.getSharedPreferences("toolbar-utils-test", Context.MODE_PRIVATE)
        prefs.edit { clear() }
    }

    @Test
    fun persistentToolbarKeyFallsBackToVoiceForInvalidValue() {
        prefs.edit { putString(Settings.PREF_PERSISTENT_TOOLBAR_KEY, "NOT_A_TOOLBAR_KEY") }

        assertEquals(ToolbarKey.VOICE, getPersistentToolbarKey(prefs))
        assertEquals(Defaults.PREF_PERSISTENT_TOOLBAR_KEY, prefs.getString(Settings.PREF_PERSISTENT_TOOLBAR_KEY, null))
    }

    @Test
    fun pinnedToolbarKeysAreLimitedToFive() {
        val enabled = listOf(
            ToolbarKey.VOICE,
            ToolbarKey.NUMPAD,
            ToolbarKey.UNDO,
            ToolbarKey.REDO,
            ToolbarKey.COPY,
        )
        prefs.edit { putString(Settings.PREF_PINNED_TOOLBAR_KEYS, pinnedPref(enabled)) }

        assertEquals(enabled.take(MAX_PINNED_TOOLBAR_KEYS), getPinnedToolbarKeys(prefs))
    }

    @Test
    fun persistentToolbarKeyIsExcludedFromMiddlePinnedKeys() {
        val enabled = listOf(
            ToolbarKey.VOICE,
            ToolbarKey.NUMPAD,
            ToolbarKey.UNDO,
            ToolbarKey.REDO,
            ToolbarKey.COPY,
        )
        prefs.edit { putString(Settings.PREF_PINNED_TOOLBAR_KEYS, pinnedPref(enabled)) }

        assertEquals(
            listOf(ToolbarKey.NUMPAD, ToolbarKey.UNDO, ToolbarKey.REDO, ToolbarKey.COPY),
            getPinnedToolbarKeys(prefs, ToolbarKey.VOICE)
        )
    }

    @Test
    fun quickPinRefusesSixthMiddlePinnedKey() {
        val enabled = listOf(ToolbarKey.NUMPAD, ToolbarKey.UNDO, ToolbarKey.REDO, ToolbarKey.COPY, ToolbarKey.PASTE)
        prefs.edit {
            putString(Settings.PREF_PERSISTENT_TOOLBAR_KEY, ToolbarKey.VOICE.name)
            putString(Settings.PREF_PINNED_TOOLBAR_KEYS, pinnedPref(enabled))
        }

        assertFalse(addPinnedKey(prefs, ToolbarKey.RESIZE))
        assertEquals(enabled, getPinnedToolbarKeys(prefs, ToolbarKey.VOICE))
    }

    @Test
    fun quickPinRefusesPersistentToolbarKeyDuplicate() {
        prefs.edit { putString(Settings.PREF_PERSISTENT_TOOLBAR_KEY, ToolbarKey.RESIZE.name) }

        assertFalse(addPinnedKey(prefs, ToolbarKey.RESIZE))
        assertTrue(getPinnedToolbarKeys(prefs, ToolbarKey.RESIZE).isEmpty())
    }

    @Test
    fun dragPinFromAccessInsertsAtIndexAndPushesOverflow() {
        val enabled = listOf(ToolbarKey.NUMPAD, ToolbarKey.UNDO, ToolbarKey.REDO, ToolbarKey.COPY, ToolbarKey.PASTE)
        prefs.edit {
            putString(Settings.PREF_PERSISTENT_TOOLBAR_KEY, ToolbarKey.VOICE.name)
            putString(Settings.PREF_PINNED_TOOLBAR_KEYS, pinnedPref(enabled))
        }

        assertEquals(
            listOf(ToolbarKey.NUMPAD, ToolbarKey.UNDO, ToolbarKey.RESIZE, ToolbarKey.REDO, ToolbarKey.COPY),
            pinToolbarKeyAt(prefs, ToolbarKey.RESIZE, 2)
        )
    }

    @Test
    fun dragPinFromAccessAppendsWhenDroppedPastShortRow() {
        val enabled = listOf(ToolbarKey.NUMPAD, ToolbarKey.UNDO, ToolbarKey.REDO)
        prefs.edit { putString(Settings.PREF_PINNED_TOOLBAR_KEYS, pinnedPref(enabled)) }

        assertEquals(
            listOf(ToolbarKey.NUMPAD, ToolbarKey.UNDO, ToolbarKey.REDO, ToolbarKey.RESIZE),
            pinToolbarKeyAt(prefs, ToolbarKey.RESIZE, 99)
        )
    }

    @Test
    fun dragPinnedKeyMovesExistingKeyWithoutDuplicating() {
        val enabled = listOf(ToolbarKey.NUMPAD, ToolbarKey.UNDO, ToolbarKey.REDO, ToolbarKey.COPY)
        prefs.edit { putString(Settings.PREF_PINNED_TOOLBAR_KEYS, pinnedPref(enabled)) }

        assertEquals(
            listOf(ToolbarKey.UNDO, ToolbarKey.REDO, ToolbarKey.NUMPAD, ToolbarKey.COPY),
            pinToolbarKeyAt(prefs, ToolbarKey.NUMPAD, 2)
        )
    }

    @Test
    fun removedToolbarShortcutsStayHiddenAndArePurgedFromSavedPrefs() {
        val legacy = listOf(ToolbarKey.SETTINGS, ToolbarKey.CLIPBOARD, ToolbarKey.EMOJI,
            ToolbarKey.AI_TOOLS, ToolbarKey.UNDO)
        prefs.edit {
            putString(Settings.PREF_TOOLBAR_KEYS, pinnedPref(legacy))
            putString(Settings.PREF_PINNED_TOOLBAR_KEYS, pinnedPref(legacy))
        }

        assertEquals(listOf(ToolbarKey.UNDO), getEnabledToolbarKeys(prefs))
        assertEquals(listOf(ToolbarKey.UNDO), getPinnedToolbarKeys(prefs))
        upgradeToolbarPrefs(prefs)

        val saved = prefs.getString(Settings.PREF_TOOLBAR_KEYS, "").orEmpty()
        assertFalse(saved.contains("SETTINGS=true"))
        assertFalse(saved.contains("CLIPBOARD=true"))
        assertFalse(saved.contains("EMOJI=true"))
        assertFalse(saved.contains("AI_TOOLS=true"))
        assertTrue(ToolbarKey.SETTINGS.isAvailableInToolbarUi().not())
        assertTrue(ToolbarKey.CLIPBOARD.isAvailableInToolbarUi().not())
        assertTrue(ToolbarKey.EMOJI.isAvailableInToolbarUi().not())
        assertTrue(ToolbarKey.AI_TOOLS.isAvailableInToolbarUi().not())
    }

    private fun pinnedPref(enabled: List<ToolbarKey>): String {
        return ToolbarKey.entries
            .filterNot { it == ToolbarKey.CLOSE_HISTORY }
            .joinToString(Separators.ENTRY) { it.name + Separators.KV + (it in enabled) }
    }
}
