// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.content.SharedPreferences
import androidx.core.content.edit

/** One-time removal of pre-B05 raw copied text / URI preference values. */
internal object ClipboardDismissalPreferences {
    private const val OLD_TIMESTAMP = "clipboard_dismissed_clip_timestamp"
    private const val OLD_CONTENT = "clipboard_dismissed_clip_content"

    fun removeLegacyRawContent(prefs: SharedPreferences) {
        if (!prefs.contains(OLD_TIMESTAMP) && !prefs.contains(OLD_CONTENT)) return
        prefs.edit {
            remove(OLD_TIMESTAMP)
            remove(OLD_CONTENT)
        }
    }
}
