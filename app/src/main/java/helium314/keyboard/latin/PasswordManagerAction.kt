// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.content.Context
import android.content.Intent

/** Only opens an installed password manager. No editor text, app ID or password is sent. */
internal object PasswordManagerAction {
    private val PROTON_PACKAGES = listOf("proton.android.pass", "proton.android.pass.fdroid")

    fun launchIntent(context: Context): Intent? = PROTON_PACKAGES.firstNotNullOfOrNull { pkg ->
        context.packageManager.getLaunchIntentForPackage(pkg)
    }

    fun open(context: Context, intent: Intent): Boolean = try {
        context.startActivity(Intent(intent).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        true
    } catch (_: RuntimeException) {
        false
    }
}
