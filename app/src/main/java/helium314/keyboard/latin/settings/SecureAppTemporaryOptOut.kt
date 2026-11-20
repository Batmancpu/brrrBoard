// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.settings

/**
 * Volatile, editor-scoped consent to leave a user-selected secure app's incognito mode.
 * Never persisted and never overrides passwords, editor NO_LEARNING or always-incognito.
 */
object SecureAppTemporaryOptOut {
    private var editorPackage: String? = null

    @JvmStatic fun applies(packageName: String?): Boolean =
        packageName != null && editorPackage == packageName

    @JvmStatic fun enableForEditor(packageName: String) {
        require(packageName.isNotBlank())
        editorPackage = packageName
    }

    @JvmStatic fun clear() { editorPackage = null }
}
