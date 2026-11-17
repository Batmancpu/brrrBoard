// SPDX-License-Identifier: GPL-3.0-only
package helium314.keyboard.latin

import android.content.Context
import helium314.keyboard.latin.dictionary.BrrPersonalDictionary
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment

/** Actual packaged valid-only rules, not owner text or an artificial word list. */
@RunWith(RobolectricTestRunner::class)
class BrrCorrectionSafetyTest {
    @Test fun validOnlySeedCannotSilentlyBecomeAutocorrectionTarget() {
        val loader = BrrPersonalDictionary::class.java.getDeclaredMethod(
            "loadNoSuggestionWords", Context::class.java)
        loader.isAccessible = true
        loader.invoke(null, RuntimeEnvironment.getApplication())

        assertTrue(BrrCorrectionSafety.blocksCandidate("bhai"))
        assertTrue(BrrCorrectionSafety.blocksCandidate("BHAI"))
        assertTrue(BrrCorrectionSafety.blocksCandidate("yaar"))
        assertFalse(BrrCorrectionSafety.blocksCandidate("ordinary"))
        assertFalse(BrrCorrectionSafety.blocksCandidate(null))
    }
}
