// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class ClipboardDismissalPreferencesTest {
    @Test fun oldRawClipboardValuesAreRemovedWithoutErasingUnrelatedPreferences() {
        val prefs = ApplicationProvider.getApplicationContext<Context>()
            .getSharedPreferences("b05_migration_test", Context.MODE_PRIVATE)
        prefs.edit().clear().putString("clipboard_dismissed_clip_content", "synthetic-secret")
            .putLong("clipboard_dismissed_clip_timestamp", 123L)
            .putBoolean("unrelated_setting", true).commit()

        ClipboardDismissalPreferences.removeLegacyRawContent(prefs)
        ClipboardDismissalPreferences.removeLegacyRawContent(prefs) // idempotent

        assertFalse(prefs.contains("clipboard_dismissed_clip_content"))
        assertFalse(prefs.contains("clipboard_dismissed_clip_timestamp"))
        assertEquals(true, prefs.getBoolean("unrelated_setting", false))
    }
}
