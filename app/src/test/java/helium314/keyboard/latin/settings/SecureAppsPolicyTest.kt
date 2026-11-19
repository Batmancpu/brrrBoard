// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.settings

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecureAppsPolicyTest {
    @Test fun exactInstalledPackageOnlyNeverAppLabelOrPrefix() {
        val secure = setOf("com.example.bank", "com.example.secure")
        assertTrue(SecureAppsPolicy.contains(secure, "com.example.bank"))
        assertFalse(SecureAppsPolicy.contains(secure, "com.example.bank.debug"))
        assertFalse(SecureAppsPolicy.contains(secure, "Bank"))
        assertFalse(SecureAppsPolicy.contains(secure, ""))
        assertFalse(SecureAppsPolicy.contains(secure, null))
    }
}
