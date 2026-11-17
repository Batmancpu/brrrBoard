// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import helium314.keyboard.keyboard.KeyboardTheme
import helium314.keyboard.latin.R
import helium314.keyboard.latin.utils.prefs
import org.junit.Assert.assertEquals
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
}
