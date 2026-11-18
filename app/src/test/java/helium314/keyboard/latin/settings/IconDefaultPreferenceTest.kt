// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import helium314.keyboard.keyboard.KeyboardTheme
import helium314.keyboard.keyboard.internal.KeyboardIconsSet
import helium314.keyboard.latin.R
import helium314.keyboard.latin.utils.ToolbarKey
import helium314.keyboard.latin.utils.prefs
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class IconDefaultPreferenceTest {
    @Test fun roundedIconsAreTheDefaultWithoutBakedInHoloOverrides() {
        assertEquals(KeyboardTheme.STYLE_ROUNDED, Defaults.PREF_ICON_STYLE)
        assertEquals("{}", Defaults.PREF_CUSTOM_ICON_NAMES)
    }

    @Test fun existingExplicitCustomIconSelectionSurvivesDefaultRefresh() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preferences = context.prefs()
        val before = preferences.getString(Settings.PREF_CUSTOM_ICON_NAMES, null)
        try {
            assertTrue(preferences.edit().putString(Settings.PREF_CUSTOM_ICON_NAMES,
                "{\"search_key\":\"sym_keyboard_search_rounded\"}").commit())
            assertEquals(R.drawable.sym_keyboard_search_rounded,
                customIconIds(context)["search_key"])
        } finally {
            val restore = preferences.edit()
            if (before == null) restore.remove(Settings.PREF_CUSTOM_ICON_NAMES)
            else restore.putString(Settings.PREF_CUSTOM_ICON_NAMES, before)
            restore.commit()
        }
    }

    @Test fun mainKeyboardAndToolbarDefaultsUseTheRoundedSurface() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preferences = context.prefs()
        val before = preferences.getString(Settings.PREF_CUSTOM_ICON_NAMES, null)
        try {
            // A saved override is a user choice; inspect defaults with an empty choice map.
            assertTrue(preferences.edit().putString(Settings.PREF_CUSTOM_ICON_NAMES, "{}").commit())
            KeyboardIconsSet.needsReload = true
            KeyboardIconsSet.instance.loadIcons(context)
            val expected = mapOf(
                KeyboardIconsSet.NAME_GO_KEY to R.drawable.sym_keyboard_go_rounded,
                KeyboardIconsSet.NAME_SEND_KEY to R.drawable.sym_keyboard_send_rounded,
                KeyboardIconsSet.NAME_DONE_KEY to R.drawable.sym_keyboard_done_rounded,
                KeyboardIconsSet.NAME_TAB_KEY to R.drawable.sym_keyboard_tab_rounded,
                KeyboardIconsSet.NAME_SPACE_KEY_FOR_NUMBER_LAYOUT to R.drawable.sym_keyboard_space_rounded,
                KeyboardIconsSet.NAME_SHORTCUT_KEY_DISABLED to R.drawable.sym_keyboard_voice_off_rounded,
                KeyboardIconsSet.NAME_ZWNJ_KEY to R.drawable.sym_keyboard_zwnj_rounded,
                KeyboardIconsSet.NAME_ZWJ_KEY to R.drawable.sym_keyboard_zwj_rounded,
                ToolbarKey.NUMPAD.name.lowercase() to R.drawable.sym_keyboard_numpad_key_rounded,
                ToolbarKey.INCOGNITO.name.lowercase() to R.drawable.sym_keyboard_incognito_rounded,
                ToolbarKey.SPLIT.name.lowercase() to R.drawable.ic_ime_switcher_rounded,
            )
            expected.forEach { (key, drawable) ->
                assertEquals("Wrong default for $key", drawable, KeyboardIconsSet.instance.iconIds[key])
                assertNotNull("Drawable for $key did not load", KeyboardIconsSet.instance.getIconDrawable(key))
            }
        } finally {
            val restore = preferences.edit()
            if (before == null) restore.remove(Settings.PREF_CUSTOM_ICON_NAMES)
            else restore.putString(Settings.PREF_CUSTOM_ICON_NAMES, before)
            restore.commit()
            KeyboardIconsSet.needsReload = true
        }
    }

    @Test fun explicitIconOverrideStillWinsOverRoundedDefault() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val preferences = context.prefs()
        val before = preferences.getString(Settings.PREF_CUSTOM_ICON_NAMES, null)
        try {
            assertTrue(preferences.edit().putString(Settings.PREF_CUSTOM_ICON_NAMES,
                "{\"go_key\":\"sym_keyboard_go_lxx\"}").commit())
            KeyboardIconsSet.needsReload = true
            KeyboardIconsSet.instance.loadIcons(context)
            assertEquals(R.drawable.sym_keyboard_go_lxx,
                KeyboardIconsSet.instance.iconIds[KeyboardIconsSet.NAME_GO_KEY])
        } finally {
            val restore = preferences.edit()
            if (before == null) restore.remove(Settings.PREF_CUSTOM_ICON_NAMES)
            else restore.putString(Settings.PREF_CUSTOM_ICON_NAMES, before)
            restore.commit()
            KeyboardIconsSet.needsReload = true
        }
    }
}
