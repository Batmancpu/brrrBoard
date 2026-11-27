package helium314.keyboard.latin

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ShiftIconMarkerPrivacyTest {
    private val prefs = ApplicationProvider.getApplicationContext<Context>()
        .getSharedPreferences("shift_marker_privacy_test", Context.MODE_PRIVATE)
    private val legacy = "reno10_shift_icon_migration_applied"
    private val neutral = "shift_icon_migration_applied"

    @Test fun alreadyMigratedInstallKeepsMarkerAndCustomIcons() {
        prefs.edit().clear().putBoolean(legacy, true)
            .putString("custom_icons_fixture", "explicit").commit()
        migrateShiftIconPreferenceMarker(prefs)
        assertTrue(prefs.getBoolean(neutral, false))
        assertFalse(prefs.contains(legacy))
        assertEquals("explicit", prefs.getString("custom_icons_fixture", null))
    }

    @Test fun falseMarkerAndNewMarkerArePreserved() {
        prefs.edit().clear().putBoolean(legacy, false)
            .putBoolean("other_shift_icon_migration_applied", true).commit()
        migrateShiftIconPreferenceMarker(prefs)
        assertTrue(prefs.contains(neutral))
        assertFalse(prefs.getBoolean(neutral, true))
        assertTrue(prefs.getBoolean("other_shift_icon_migration_applied", false))
        prefs.edit().putBoolean(legacy, true).commit()
        migrateShiftIconPreferenceMarker(prefs)
        assertFalse(prefs.getBoolean(neutral, true))
        assertFalse(prefs.contains(legacy))
    }

    @Test fun freshInstallDoesNotInventMarker() {
        prefs.edit().clear().commit()
        migrateShiftIconPreferenceMarker(prefs)
        assertFalse(prefs.contains(neutral))
    }
}
