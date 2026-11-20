// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin.settings

import org.junit.After
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SecureAppTemporaryOptOutTest {
    @After fun clean() { SecureAppTemporaryOptOut.clear() }

    @Test fun consentIsExactlyPackageScopedAndRevocable() {
        SecureAppTemporaryOptOut.clear()
        assertFalse(SecureAppTemporaryOptOut.applies("com.bank"))
        SecureAppTemporaryOptOut.enableForEditor("com.bank")
        assertTrue(SecureAppTemporaryOptOut.applies("com.bank"))
        assertFalse(SecureAppTemporaryOptOut.applies("com.bank.clone"))
        assertFalse(SecureAppTemporaryOptOut.applies(null))
        SecureAppTemporaryOptOut.clear()
        assertFalse(SecureAppTemporaryOptOut.applies("com.bank"))
    }

    @Test(expected = IllegalArgumentException::class)
    fun blankTargetCannotBeApproved() { SecureAppTemporaryOptOut.enableForEditor(" ") }
}
