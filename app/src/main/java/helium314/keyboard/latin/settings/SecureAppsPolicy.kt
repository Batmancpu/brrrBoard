// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.settings

import android.content.SharedPreferences

/** Private, package-exact secure-app selection. Never infer from typed text or app labels. */
object SecureAppsPolicy {
    @JvmStatic fun contains(packages: Set<String>, packageName: String?): Boolean =
        !packageName.isNullOrBlank() && packageName in packages

    @JvmStatic fun isSecure(prefs: SharedPreferences, packageName: String?): Boolean =
        contains(prefs.getStringSet(Settings.PREF_SECURE_APPS, emptySet()).orEmpty(), packageName)
}
